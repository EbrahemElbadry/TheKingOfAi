package com.kingofai.voicechanger.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Offline, high-quality pitch shifter based on a phase vocoder, with optional
 * formant preservation via cepstral spectral-envelope correction.
 *
 * Two independent controls:
 *  - [pitch]   shifts the perceived pitch (F0).
 *  - [formant] shifts the spectral envelope (timbre / vocal-tract size).
 *
 * With formant = 1.0 the timbre stays natural while pitch changes, which avoids
 * the "chipmunk / sped-up" artefact that plain resampling produces when pitching
 * a voice up. This is offline processing (for voice notes), so quality is
 * favoured over latency.
 */
object PhaseVocoder {

    private const val N = 2048          // FFT size
    private const val OSAMP = 4         // overlap factor
    private const val STEP = N / OSAMP  // hop size
    private const val LIFTER = 40       // cepstral quefrency cutoff for envelope

    fun process(input: FloatArray, pitch: Float, formant: Float, sampleRate: Int): FloatArray {
        if (input.isEmpty()) return input
        val output = FloatArray(input.size)

        val window = FloatArray(N) { 0.5f - 0.5f * cos(2.0 * PI * it / N).toFloat() }

        val re = FloatArray(N)
        val im = FloatArray(N)

        val bins = N / 2
        val lastPhase = FloatArray(bins + 1)
        val sumPhase = FloatArray(bins + 1)

        val anaMagn = FloatArray(bins + 1)
        val anaFreq = FloatArray(bins + 1)
        val synMagn = FloatArray(bins + 1)
        val synFreq = FloatArray(bins + 1)

        val envelope = FloatArray(bins + 1)

        val freqPerBin = sampleRate.toDouble() / N
        val expct = 2.0 * PI * STEP / N

        var pos = 0
        while (pos < input.size) {
            // ---- Analysis window ----
            for (k in 0 until N) {
                val s = pos + k
                re[k] = if (s < input.size) input[s] * window[k] else 0f
                im[k] = 0f
            }
            FFT.transform(re, im, inverse = false)

            // ---- Analysis: magnitude + true frequency per bin ----
            for (k in 0..bins) {
                val real = re[k]
                val imag = im[k]
                val magn = 2f * sqrt(real * real + imag * imag)
                val phase = Math.atan2(imag.toDouble(), real.toDouble())

                var tmp = phase - lastPhase[k]
                lastPhase[k] = phase.toFloat()
                tmp -= k * expct
                // wrap to [-pi, pi]
                var qpd = (tmp / PI).toInt()
                if (qpd >= 0) qpd += qpd and 1 else qpd -= qpd and 1
                tmp -= PI * qpd
                tmp = OSAMP * tmp / (2.0 * PI)
                val trueFreq = (k + tmp) * freqPerBin

                anaMagn[k] = magn
                anaFreq[k] = trueFreq.toFloat()
            }

            // ---- Spectral envelope of the ORIGINAL frame (for formant control) ----
            computeEnvelope(anaMagn, bins, envelope)

            // ---- Processing: pitch re-binning ----
            java.util.Arrays.fill(synMagn, 0f)
            java.util.Arrays.fill(synFreq, 0f)
            for (k in 0..bins) {
                val index = (k * pitch).roundToInt()
                if (index in 0..bins) {
                    synMagn[index] += anaMagn[k]
                    synFreq[index] = anaFreq[k] * pitch
                }
            }

            // ---- Formant correction: reshape envelope back toward target ----
            // Shifted spectrum envelope ~ envOrig(k/pitch); we want envOrig(k/formant).
            for (k in 0..bins) {
                if (synMagn[k] <= 0f) continue
                val eTarget = interpEnv(envelope, bins, k / formant)
                val eCurrent = interpEnv(envelope, bins, k / pitch)
                if (eCurrent > 1e-6f) {
                    val gain = (eTarget / eCurrent).coerceIn(0.1f, 10f)
                    synMagn[k] *= gain
                }
            }

            // ---- Synthesis ----
            for (k in 0..bins) {
                val magn = synMagn[k]
                var tmp = synFreq[k] / freqPerBin
                tmp -= k.toDouble()
                tmp = 2.0 * PI * tmp / OSAMP
                tmp += k * expct
                sumPhase[k] = (sumPhase[k] + tmp).toFloat()
                val phase = sumPhase[k].toDouble()
                re[k] = (magn * cos(phase)).toFloat()
                im[k] = (magn * kotlin.math.sin(phase)).toFloat()
            }
            // Mirror the negative-frequency half for a real signal.
            for (k in 1 until bins) {
                re[N - k] = re[k]
                im[N - k] = -im[k]
            }
            im[0] = 0f
            im[bins] = 0f

            FFT.transform(re, im, inverse = true)

            // ---- Overlap-add with synthesis window ----
            for (k in 0 until N) {
                val s = pos + k
                if (s < output.size) output[s] += re[k] * window[k]
            }

            pos += STEP
        }

        // Normalise to a healthy peak so the note is loud and clean.
        var peak = 0f
        for (v in output) { val a = abs(v); if (a > peak) peak = a }
        if (peak > 1e-6f) {
            val g = 0.97f / peak
            for (i in output.indices) output[i] *= g
        }
        return output
    }

    /** Smooth spectral envelope via cepstral liftering (log-mag -> low quefrency). */
    private fun computeEnvelope(magn: FloatArray, bins: Int, out: FloatArray) {
        val re = FloatArray(N)
        val im = FloatArray(N)
        for (k in 0..bins) {
            val lm = ln((magn[k] + 1e-6f).toDouble()).toFloat()
            re[k] = lm
            if (k in 1 until bins) re[N - k] = lm
        }
        FFT.transform(re, im, inverse = true) // -> real cepstrum in re
        // Keep only low-quefrency coefficients.
        for (n in 0 until N) {
            val keep = n <= LIFTER || n >= N - LIFTER
            if (!keep) { re[n] = 0f }
            im[n] = 0f
        }
        FFT.transform(re, im, inverse = false) // -> smooth log-envelope in re
        for (k in 0..bins) out[k] = exp(re[k].toDouble()).toFloat()
    }

    private fun interpEnv(env: FloatArray, bins: Int, pos: Float): Float {
        if (pos <= 0f) return env[0]
        if (pos >= bins) return env[bins]
        val i = pos.toInt()
        val f = pos - i
        return env[i] * (1f - f) + env[i + 1] * f
    }
}
