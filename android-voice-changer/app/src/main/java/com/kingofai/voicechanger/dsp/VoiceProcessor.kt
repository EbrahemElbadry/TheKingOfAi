package com.kingofai.voicechanger.dsp

import com.kingofai.voicechanger.VoiceEffect

/**
 * Offline processor for the voice-note feature: applies a [VoiceEffect] to a
 * whole recorded buffer at high quality (phase-vocoder pitch/formant shift plus
 * the character effects). Latency is irrelevant here, so quality is maximised.
 */
object VoiceProcessor {

    /**
     * @param input mono samples in [-1,1]
     * @param intensity 0..1, scales pitch/formant deviation from neutral
     */
    fun process(
        input: FloatArray,
        effect: VoiceEffect,
        intensity: Float,
        sampleRate: Int
    ): FloatArray {
        val t = intensity.coerceIn(0f, 1f)
        val pitch = 1f + (effect.pitch - 1f) * t
        val formant = 1f + (effect.formant - 1f) * t

        // High-quality pitch + formant shift.
        val shifted =
            if (pitch == 1f && formant == 1f) input.copyOf()
            else PhaseVocoder.process(input, pitch, formant, sampleRate)

        // Character effects on the whole buffer.
        if (effect.ringMod) {
            RingModulator(sampleRate).apply { enabled = true; frequency = effect.ringFreq }
                .process(shifted, shifted.size)
        }
        if (effect.tremolo) {
            Tremolo(sampleRate).apply { enabled = true }
                .process(shifted, shifted.size)
        }
        if (effect.lowPass) {
            LowPass().apply { enabled = true; alpha = effect.lowPassAlpha }
                .process(shifted, shifted.size)
        }
        if (effect.echo) {
            Echo(sampleRate).apply { enabled = true; mix = 0.35f * t; feedback = 0.3f * t }
                .process(shifted, shifted.size)
        }

        // Final soft limiter to avoid clipping after effects.
        var peak = 0f
        for (v in shifted) { val a = if (v < 0) -v else v; if (a > peak) peak = a }
        if (peak > 0.99f) {
            val g = 0.97f / peak
            for (i in shifted.indices) shifted[i] *= g
        }
        return shifted
    }
}
