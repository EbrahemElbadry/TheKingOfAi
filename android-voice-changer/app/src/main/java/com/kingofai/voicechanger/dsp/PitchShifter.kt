package com.kingofai.voicechanger.dsp

import kotlin.math.cos
import kotlin.math.floor

/**
 * Real-time, streaming pitch shifter based on the classic two-tap granular
 * delay-line technique (a "harmonizer"). It shifts pitch while keeping the
 * duration unchanged, sample-by-sample, so it is suitable for live audio.
 *
 * Two read pointers move through a delay buffer half a window apart. Each is
 * amplitude-shaped by a Hann window; because two Hann windows offset by half a
 * period sum to a near-constant, the crossfade between grains stays smooth and
 * avoids the clicks a single moving tap would produce.
 */
class PitchShifter(private val bufferSize: Int = 8192) {

    private val buffer = FloatArray(bufferSize)
    private var writeIndex = 0

    /** Window length in samples. Larger = smoother but more latency/smear. */
    private val windowSize = 1024f

    /** Fractional grain phase in [0, windowSize). */
    private var phase = 0f

    /** 1.0 = no change, 2.0 = one octave up, 0.5 = one octave down. */
    @Volatile
    var pitchRatio: Float = 1.0f

    fun reset() {
        buffer.fill(0f)
        writeIndex = 0
        phase = 0f
    }

    /** Process a block of mono float samples in-place. */
    fun process(data: FloatArray, length: Int) {
        val ratio = pitchRatio.coerceIn(0.5f, 2.0f)
        if (ratio == 1.0f) return // passthrough, keep buffer warm not needed

        val delayStep = 1f - ratio
        val halfWindow = windowSize / 2f

        for (i in 0 until length) {
            // Write newest input into the ring buffer.
            buffer[writeIndex] = data[i]

            // Tap A delay = phase, Tap B is half a window behind.
            val delayA = phase
            var delayB = phase + halfWindow
            if (delayB >= windowSize) delayB -= windowSize

            val fracA = delayA / windowSize
            val fracB = delayB / windowSize

            val gainA = hann(fracA)
            val gainB = hann(fracB)

            val sampleA = readInterpolated(writeIndex - delayA)
            val sampleB = readInterpolated(writeIndex - delayB)

            data[i] = sampleA * gainA + sampleB * gainB

            // Advance phase; wrap into [0, windowSize).
            phase += delayStep
            if (phase >= windowSize) phase -= windowSize
            if (phase < 0f) phase += windowSize

            writeIndex++
            if (writeIndex >= bufferSize) writeIndex = 0
        }
    }

    /** Hann window over [0,1] -> [0,1], zero at both ends. */
    private fun hann(x: Float): Float = 0.5f * (1f - cos(2.0 * Math.PI * x).toFloat())

    /** Linearly-interpolated read at a fractional, possibly-wrapped position. */
    private fun readInterpolated(position: Float): Float {
        var pos = position
        while (pos < 0f) pos += bufferSize
        while (pos >= bufferSize) pos -= bufferSize

        val i0 = floor(pos).toInt()
        val i1 = if (i0 + 1 >= bufferSize) 0 else i0 + 1
        val frac = pos - i0
        return buffer[i0] * (1f - frac) + buffer[i1] * frac
    }
}
