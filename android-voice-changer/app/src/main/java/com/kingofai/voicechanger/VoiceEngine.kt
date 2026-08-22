package com.kingofai.voicechanger

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.util.Log
import com.kingofai.voicechanger.dsp.Echo
import com.kingofai.voicechanger.dsp.LowPass
import com.kingofai.voicechanger.dsp.PitchShifter
import com.kingofai.voicechanger.dsp.RingModulator
import com.kingofai.voicechanger.dsp.Tremolo
import kotlin.concurrent.thread

/**
 * Captures microphone audio, applies the selected [VoiceEffect] in real time,
 * and plays the transformed signal back through the output (speaker/earpiece).
 *
 * NOTE: Android does not let a normal app inject audio into a native cellular
 * call. This engine transforms the mic in real time; during a call put the call
 * on speaker so the other party hears the processed output.
 */
class VoiceEngine(
    private val sampleRate: Int = 44100,
    private val onError: (String) -> Unit = {}
) {

    private var record: AudioRecord? = null
    private var track: AudioTrack? = null
    private var worker: Thread? = null
    @Volatile private var running = false

    private val pitchShifter = PitchShifter()
    private val ringMod = RingModulator(sampleRate)
    private val tremolo = Tremolo(sampleRate)
    private val echo = Echo(sampleRate)
    private val lowPass = LowPass()

    @Volatile private var intensity = 1.0f

    /** Smoothed mic input level in 0..1, for a live meter in the UI. */
    @Volatile var inputLevel: Float = 0f
        private set

    val isRunning: Boolean get() = running

    /** Apply an effect preset. [intensity] in 0..1 scales pitch deviation and wet mix. */
    fun applyEffect(effect: VoiceEffect, intensity: Float) {
        this.intensity = intensity.coerceIn(0f, 1f)
        // Scale the pitch deviation from neutral by intensity.
        pitchShifter.pitchRatio = 1.0f + (effect.pitch - 1.0f) * this.intensity

        ringMod.enabled = effect.ringMod
        ringMod.frequency = effect.ringFreq

        tremolo.enabled = effect.tremolo

        echo.enabled = effect.echo
        echo.mix = 0.45f * this.intensity
        echo.feedback = 0.35f * this.intensity

        lowPass.enabled = effect.lowPass
        lowPass.alpha = effect.lowPassAlpha
    }

    @SuppressLint("MissingPermission")
    fun start(audioManager: AudioManager?, routeToCall: Boolean) {
        if (running) return

        val minRecBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val minPlayBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minRecBuf <= 0 || minPlayBuf <= 0) {
            onError("جهازك لا يدعم إعدادات الصوت المطلوبة")
            return
        }

        val bufSize = maxOf(minRecBuf, minPlayBuf) * 2

        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            onError("تعذّر تهيئة الميكروفون")
            rec.release()
            return
        }

        // Only suppress noise; keep the acoustic echo canceler OFF because in
        // live-monitor mode it would treat the processed playback as "echo" and
        // cancel the very effect we are trying to hear.
        runCatching {
            if (NoiseSuppressor.isAvailable()) {
                NoiseSuppressor.create(rec.audioSessionId)?.enabled = true
            }
        }

        val usage = if (routeToCall) AudioAttributes.USAGE_VOICE_COMMUNICATION
        else AudioAttributes.USAGE_MEDIA
        val contentType = if (routeToCall) AudioAttributes.CONTENT_TYPE_SPEECH
        else AudioAttributes.CONTENT_TYPE_MUSIC

        val trk = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(contentType)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        if (routeToCall) {
            runCatching {
                audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager?.isSpeakerphoneOn = true
                // Max out the call stream so acoustic coupling to the call mic
                // has the best possible chance on the speaker-trick path.
                audioManager?.let { am ->
                    val max = am.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
                    am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, max, 0)
                }
            }
        }

        pitchShifter.reset()
        echo.reset()

        record = rec
        track = trk
        running = true

        rec.startRecording()
        trk.play()

        worker = thread(name = "VoiceEngineLoop", priority = Thread.MAX_PRIORITY) {
            loop(bufSize)
        }
    }

    private fun loop(bufSize: Int) {
        val frames = bufSize / 2 // 16-bit -> shorts
        val shortBuf = ShortArray(frames)
        val floatBuf = FloatArray(frames)
        val rec = record ?: return
        val trk = track ?: return

        try {
            while (running) {
                val read = rec.read(shortBuf, 0, frames)
                if (read <= 0) continue

                // Convert to float [-1,1] and measure input RMS (0..1) so the
                // UI can show whether the mic is actually being captured — this
                // is the key diagnostic during a phone call.
                var sumSq = 0.0
                for (i in 0 until read) {
                    val f = shortBuf[i] / 32768f
                    floatBuf[i] = f
                    sumSq += (f * f).toDouble()
                }
                val rms = Math.sqrt(sumSq / read).toFloat()
                // Smooth and scale a bit for a livelier meter.
                inputLevel = (inputLevel * 0.6f + rms * 0.4f * 3f).coerceIn(0f, 1f)

                // DSP chain.
                pitchShifter.process(floatBuf, read)
                ringMod.process(floatBuf, read)
                tremolo.process(floatBuf, read)
                lowPass.process(floatBuf, read)
                echo.process(floatBuf, read)

                // Convert back with clipping.
                for (i in 0 until read) {
                    val v = (floatBuf[i] * 32768f)
                    shortBuf[i] = when {
                        v > 32767f -> 32767
                        v < -32768f -> -32768
                        else -> v.toInt().toShort()
                    }
                }

                trk.write(shortBuf, 0, read)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "audio loop crashed", t)
            onError("توقّفت معالجة الصوت: ${t.message}")
        }
    }

    fun stop(audioManager: AudioManager?) {
        running = false
        worker?.join(500)
        worker = null

        runCatching { record?.stop() }
        runCatching { record?.release() }
        record = null

        runCatching { track?.stop() }
        runCatching { track?.release() }
        track = null

        runCatching {
            audioManager?.isSpeakerphoneOn = false
            audioManager?.mode = AudioManager.MODE_NORMAL
        }
    }

    companion object {
        private const val TAG = "VoiceEngine"
    }
}
