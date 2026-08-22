package com.kingofai.voicechanger

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import java.io.File

/** Encoding helpers for the voice-note feature. */
object AudioFiles {

    /**
     * Encode mono float samples ([-1,1]) to an AAC .m4a file, which chat apps
     * (WhatsApp/Telegram) play inline as a voice message.
     */
    fun encodeM4a(out: File, samples: FloatArray, sampleRate: Int, bitRate: Int = 96000) {
        // Convert to 16-bit PCM bytes (little-endian).
        val pcm = ByteArray(samples.size * 2)
        var p = 0
        for (s in samples) {
            val v = (s * 32767f).toInt().coerceIn(-32768, 32767)
            pcm[p++] = (v and 0xFF).toByte()
            pcm[p++] = ((v shr 8) and 0xFF).toByte()
        }

        val format = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 1
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)
        }

        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()

        val muxer = MediaMuxer(out.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var trackIndex = -1
        var muxerStarted = false

        val bufferInfo = MediaCodec.BufferInfo()
        var inputOffset = 0
        var inputDone = false
        var presentationUs = 0L
        val bytesPerUsDenom = sampleRate.toDouble() * 2.0 // mono 16-bit

        while (true) {
            if (!inputDone) {
                val inIndex = codec.dequeueInputBuffer(10000)
                if (inIndex >= 0) {
                    val remaining = pcm.size - inputOffset
                    if (remaining <= 0) {
                        // Signal end of stream on its own buffer.
                        codec.queueInputBuffer(
                            inIndex, 0, 0, presentationUs,
                            MediaCodec.BUFFER_FLAG_END_OF_STREAM
                        )
                        inputDone = true
                    } else {
                        val inBuf = codec.getInputBuffer(inIndex)!!
                        inBuf.clear()
                        val chunk = minOf(inBuf.capacity(), remaining)
                        inBuf.put(pcm, inputOffset, chunk)
                        val ptsUs = (inputOffset / bytesPerUsDenom * 1_000_000).toLong()
                        codec.queueInputBuffer(inIndex, 0, chunk, ptsUs, 0)
                        inputOffset += chunk
                        presentationUs = ptsUs
                    }
                }
            }

            val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                trackIndex = muxer.addTrack(codec.outputFormat)
                muxer.start()
                muxerStarted = true
            } else if (outIndex >= 0) {
                val outBuf = codec.getOutputBuffer(outIndex)!!
                if (bufferInfo.size > 0 && muxerStarted &&
                    (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                ) {
                    outBuf.position(bufferInfo.offset)
                    outBuf.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIndex, outBuf, bufferInfo)
                }
                codec.releaseOutputBuffer(outIndex, false)
                if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) break
            }
        }

        runCatching { codec.stop() }
        runCatching { codec.release() }
        runCatching { muxer.stop() }
        runCatching { muxer.release() }
    }
}
