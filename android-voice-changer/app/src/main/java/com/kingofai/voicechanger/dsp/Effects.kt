package com.kingofai.voicechanger.dsp

import kotlin.math.PI
import kotlin.math.sin

/**
 * A ring modulator multiplies the signal by a sine carrier, producing the
 * metallic "robot" timbre. Carrier frequency controls how harsh it sounds.
 */
class RingModulator(private val sampleRate: Int) {
    @Volatile var enabled = false
    @Volatile var frequency = 80f
    private var phase = 0.0

    fun process(data: FloatArray, length: Int) {
        if (!enabled) return
        val inc = 2.0 * PI * frequency / sampleRate
        for (i in 0 until length) {
            data[i] = (data[i] * sin(phase)).toFloat()
            phase += inc
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
    }
}

/**
 * Tremolo: slow amplitude modulation that adds a wobbly / "alien" quality.
 */
class Tremolo(private val sampleRate: Int) {
    @Volatile var enabled = false
    @Volatile var rate = 6f       // Hz
    @Volatile var depth = 0.6f    // 0..1
    private var phase = 0.0

    fun process(data: FloatArray, length: Int) {
        if (!enabled) return
        val inc = 2.0 * PI * rate / sampleRate
        for (i in 0 until length) {
            val mod = 1f - depth * (0.5f * (1f + sin(phase).toFloat()))
            data[i] = data[i] * mod
            phase += inc
            if (phase > 2.0 * PI) phase -= 2.0 * PI
        }
    }
}

/**
 * A feedback delay line producing echo / cave effects.
 */
class Echo(sampleRate: Int, maxDelayMs: Int = 600) {
    @Volatile var enabled = false
    @Volatile var mix = 0.4f       // wet amount 0..1
    @Volatile var feedback = 0.35f // 0..0.95

    private val bufferSize = (sampleRate * maxDelayMs / 1000).coerceAtLeast(1)
    private val buffer = FloatArray(bufferSize)
    private var index = 0
    private var delaySamples = sampleRate * 250 / 1000 // 250 ms default

    fun reset() {
        buffer.fill(0f)
        index = 0
    }

    fun process(data: FloatArray, length: Int) {
        if (!enabled) return
        for (i in 0 until length) {
            val readIndex = (index - delaySamples + bufferSize) % bufferSize
            val delayed = buffer[readIndex]
            val out = data[i] + delayed * mix
            buffer[index] = data[i] + delayed * feedback
            data[i] = out
            index++
            if (index >= bufferSize) index = 0
        }
    }
}

/** Simple one-pole low-pass filter to muffle / darken the voice. */
class LowPass {
    @Volatile var enabled = false
    @Volatile var alpha = 0.25f // 0..1, lower = more muffled
    private var last = 0f

    fun process(data: FloatArray, length: Int) {
        if (!enabled) return
        for (i in 0 until length) {
            last += alpha * (data[i] - last)
            data[i] = last
        }
    }
}
