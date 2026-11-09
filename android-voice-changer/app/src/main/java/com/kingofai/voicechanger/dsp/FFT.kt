package com.kingofai.voicechanger.dsp

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * In-place iterative radix-2 Cooley-Tukey FFT. Array length must be a power of
 * two. Twiddle factors are computed in Double for accuracy; data stays Float.
 */
object FFT {

    fun transform(re: FloatArray, im: FloatArray, inverse: Boolean) {
        val n = re.size

        // Bit-reversal permutation.
        var j = 0
        for (i in 1 until n) {
            var bit = n shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                val tr = re[i]; re[i] = re[j]; re[j] = tr
                val ti = im[i]; im[i] = im[j]; im[j] = ti
            }
        }

        var len = 2
        while (len <= n) {
            val ang = (if (inverse) 2.0 else -2.0) * PI / len
            val wr = cos(ang)
            val wi = sin(ang)
            var i = 0
            while (i < n) {
                var cwr = 1.0
                var cwi = 0.0
                val half = len / 2
                for (k in 0 until half) {
                    val idx = i + k
                    val idx2 = idx + half
                    val vr = re[idx2] * cwr - im[idx2] * cwi
                    val vi = re[idx2] * cwi + im[idx2] * cwr
                    val ur = re[idx].toDouble()
                    val ui = im[idx].toDouble()
                    re[idx] = (ur + vr).toFloat()
                    im[idx] = (ui + vi).toFloat()
                    re[idx2] = (ur - vr).toFloat()
                    im[idx2] = (ui - vi).toFloat()
                    val ncwr = cwr * wr - cwi * wi
                    cwi = cwr * wi + cwi * wr
                    cwr = ncwr
                }
                i += len
            }
            len = len shl 1
        }

        if (inverse) {
            val inv = 1f / n
            for (i2 in 0 until n) {
                re[i2] *= inv
                im[i2] *= inv
            }
        }
    }
}
