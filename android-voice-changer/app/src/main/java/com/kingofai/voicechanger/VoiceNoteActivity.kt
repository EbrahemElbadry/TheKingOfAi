package com.kingofai.voicechanger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.button.MaterialButton
import com.kingofai.voicechanger.databinding.ActivityVoiceNoteBinding
import com.kingofai.voicechanger.dsp.VoiceProcessor
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.concurrent.thread

class VoiceNoteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVoiceNoteBinding

    private val sampleRate = 44100
    private var selected: VoiceEffect = VoiceEffect.MAN
    private val buttonByEffect = mutableMapOf<VoiceEffect, MaterialButton>()

    private var recorder: AudioRecord? = null
    private var recThread: Thread? = null
    @Volatile private var recording = false
    private val recordedBytes = ByteArrayOutputStream()
    private var recordedSamples: FloatArray? = null

    private var player: MediaPlayer? = null

    // Cache of the last processed result to avoid recomputing for play + share.
    private var processedCache: FloatArray? = null
    private var processedKey: String = ""

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else toast("إذن الميكروفون مطلوب للتسجيل")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVoiceNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buildEffectButtons()
        updateIntensityLabel(binding.intensitySeek.progress)

        binding.recordButton.setOnClickListener {
            if (recording) stopRecording() else requestAndRecord()
        }
        binding.intensitySeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, p: Int, u: Boolean) {
                updateIntensityLabel(p); invalidateCache()
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })
        binding.playButton.setOnClickListener { onPlay() }
        binding.shareButton.setOnClickListener { onShare() }
    }

    // ---------------- Effect grid ----------------

    private fun buildEffectButtons() {
        VoiceEffect.entries.forEach { effect ->
            val btn = MaterialButton(this).apply {
                text = "${effect.emoji}\n${effect.displayName}"
                isAllCaps = false
                gravity = Gravity.CENTER
                setOnClickListener { selectEffect(effect) }
            }
            btn.layoutParams = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(android.widget.GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            binding.effectsGrid.addView(btn)
            buttonByEffect[effect] = btn
        }
        selectEffect(selected)
    }

    private fun selectEffect(effect: VoiceEffect) {
        selected = effect
        buttonByEffect.forEach { (e, b) ->
            b.alpha = if (e == effect) 1.0f else 0.55f
            b.strokeWidth = if (e == effect) 6 else 0
        }
        invalidateCache()
    }

    private fun intensity(): Float = binding.intensitySeek.progress / 100f
    private fun updateIntensityLabel(p: Int) { binding.intensityLabel.text = "شدة المؤثر: $p%" }

    // ---------------- Recording ----------------

    private fun requestAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) startRecording()
        else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecording() {
        val min = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (min <= 0) { toast("جهازك لا يدعم التسجيل بهذه الإعدادات"); return }

        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 2
            )
        } catch (e: SecurityException) { toast("إذن الميكروفون مطلوب"); return }

        if (rec.state != AudioRecord.STATE_INITIALIZED) { toast("تعذّر بدء التسجيل"); rec.release(); return }

        recordedBytes.reset()
        recordedSamples = null
        invalidateCache()
        recorder = rec
        recording = true
        rec.startRecording()

        binding.recordButton.text = "⏹️ إيقاف التسجيل"
        binding.recordStatus.text = "جارٍ التسجيل…"
        binding.playButton.isEnabled = false
        binding.shareButton.isEnabled = false

        recThread = thread(name = "Recorder") {
            val buf = ShortArray(min)
            val bytes = ByteArray(min * 2)
            while (recording) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) {
                    var bi = 0
                    for (i in 0 until n) {
                        val v = buf[i].toInt()
                        bytes[bi++] = (v and 0xFF).toByte()
                        bytes[bi++] = ((v shr 8) and 0xFF).toByte()
                    }
                    recordedBytes.write(bytes, 0, n * 2)
                }
            }
        }
    }

    private fun stopRecording() {
        recording = false
        recThread?.join(500)
        recThread = null
        runCatching { recorder?.stop() }
        runCatching { recorder?.release() }
        recorder = null

        val raw = recordedBytes.toByteArray()
        val samples = FloatArray(raw.size / 2)
        var si = 0
        var i = 0
        while (i + 1 < raw.size) {
            val lo = raw[i].toInt() and 0xFF
            val hi = raw[i + 1].toInt()
            val s = (hi shl 8) or lo
            samples[si++] = s / 32768f
            i += 2
        }
        recordedSamples = samples

        val seconds = samples.size.toFloat() / sampleRate
        binding.recordButton.text = "🔴 تسجيل جديد"
        if (samples.isEmpty()) {
            binding.recordStatus.text = "لم يُسجَّل صوت"
        } else {
            binding.recordStatus.text = "تم التسجيل (%.1f ثانية) — اختر مؤثراً واستمع".format(seconds)
            binding.playButton.isEnabled = true
            binding.shareButton.isEnabled = true
        }
    }

    // ---------------- Processing / cache ----------------

    private fun invalidateCache() { processedCache = null }

    private fun cacheKey() = "${selected.name}_${binding.intensitySeek.progress}"

    private fun processAsync(onReady: (FloatArray) -> Unit) {
        val input = recordedSamples
        if (input == null || input.isEmpty()) { toast("سجّل صوتك أولاً"); return }

        val cached = processedCache
        if (cached != null && processedKey == cacheKey()) { onReady(cached); return }

        binding.recordStatus.text = "جارٍ المعالجة…"
        setButtonsEnabled(false)
        thread {
            val result = runCatching {
                VoiceProcessor.process(input, selected, intensity(), sampleRate)
            }.getOrElse {
                runOnUiThread {
                    toast("خطأ في المعالجة: ${it.message}")
                    binding.recordStatus.text = "فشلت المعالجة"
                    setButtonsEnabled(true)
                }
                return@thread
            }
            processedCache = result
            processedKey = cacheKey()
            runOnUiThread {
                binding.recordStatus.text = "جاهز ✅"
                setButtonsEnabled(true)
                onReady(result)
            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.playButton.isEnabled = enabled && recordedSamples?.isNotEmpty() == true
        binding.shareButton.isEnabled = enabled && recordedSamples?.isNotEmpty() == true
        binding.recordButton.isEnabled = enabled
    }

    // ---------------- Play / Share ----------------

    private fun onPlay() {
        processAsync { processed ->
            thread {
                val file = File(cacheDir, "preview.m4a")
                runCatching { AudioFiles.encodeM4a(file, processed, sampleRate) }
                    .onFailure { runOnUiThread { toast("تعذّر التشغيل: ${it.message}") }; return@thread }
                runOnUiThread { playFile(file) }
            }
        }
    }

    private fun playFile(file: File) {
        runCatching { player?.release() }
        player = null
        runCatching {
            val mp = MediaPlayer()
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener { it.release(); if (it == player) player = null }
            mp.prepare()
            mp.start()
            player = mp
            toast("جارٍ التشغيل…")
        }.onFailure { toast("تعذّر التشغيل: ${it.message}") }
    }

    private fun onShare() {
        processAsync { processed ->
            thread {
                val dir = File(getExternalFilesDir(null), "notes").apply { mkdirs() }
                val name = "voice_${selected.name.lowercase()}_${System.currentTimeMillis()}.m4a"
                val file = File(dir, name)
                runCatching { AudioFiles.encodeM4a(file, processed, sampleRate) }
                    .onFailure { runOnUiThread { toast("تعذّر الحفظ: ${it.message}") }; return@thread }

                val uri: Uri = FileProvider.getUriForFile(
                    this, "$packageName.fileprovider", file
                )
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "audio/mp4"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                runOnUiThread {
                    startActivity(Intent.createChooser(share, "مشاركة الرسالة الصوتية"))
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (recording) stopRecording()
        runCatching { player?.release() }
        player = null
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
