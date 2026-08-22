package com.kingofai.voicechanger

import java.io.DataOutputStream

/**
 * Minimal helper for running shell commands as root via `su`.
 *
 * Root is required for any attempt to route processed audio into a live
 * cellular call, because the voice uplink mixer is a privileged, vendor- and
 * SoC-specific path (Qualcomm ALSA mixer controls, `incall_music`, etc.).
 * Whether injection is even possible depends entirely on the device's audio
 * HAL — hence the diagnostics below, which report what routing exists.
 */
object RootShell {

    data class Result(val exitCode: Int, val output: String)

    /** True if a `su` binary is present and grants a root shell. */
    fun isRootAvailable(): Boolean {
        return runCatching {
            val r = exec("id")
            r.output.contains("uid=0")
        }.getOrDefault(false)
    }

    /** Run a shell script as root; returns combined stdout+stderr and exit code. */
    fun exec(script: String): Result {
        val process = ProcessBuilder("su")
            .redirectErrorStream(true)
            .start()

        DataOutputStream(process.outputStream).use { os ->
            os.writeBytes(script)
            os.writeBytes("\n")
            os.writeBytes("exit\n")
            os.flush()
        }

        val output = process.inputStream.bufferedReader().readText()
        val code = process.waitFor()
        return Result(code, output)
    }

    /**
     * Collect audio-routing diagnostics needed to design an in-call injection
     * route for THIS device. The output is meant to be shared with the developer.
     */
    fun collectAudioDiagnostics(): Result {
        val script = buildString {
            appendLine("echo '===== IDENTITY ====='")
            appendLine("id")
            appendLine("echo '===== DEVICE ====='")
            appendLine("getprop ro.product.manufacturer")
            appendLine("getprop ro.product.model")
            appendLine("getprop ro.product.device")
            appendLine("getprop ro.build.version.release")
            appendLine("getprop ro.board.platform")
            appendLine("getprop ro.hardware")
            appendLine("getprop ro.soc.model")
            appendLine("getprop ro.soc.manufacturer")
            appendLine("echo '===== AUDIO PROPS ====='")
            appendLine("getprop | grep -i audio")
            appendLine("echo '===== SND DEVICES ====='")
            appendLine("ls -l /dev/snd 2>/dev/null")
            appendLine("cat /proc/asound/cards 2>/dev/null")
            appendLine("cat /proc/asound/pcm 2>/dev/null")
            appendLine("echo '===== TINYTOOLS ====='")
            appendLine("for b in tinymix tinyplay tinypcminfo tinycap; do command -v \$b; done")
            appendLine("echo '===== MIXER FILES ====='")
            appendLine("ls -1 /vendor/etc/ 2>/dev/null | grep -i mixer")
            appendLine("ls -1 /system/etc/ 2>/dev/null | grep -i mixer")
            appendLine("echo '===== MIXER CONTROLS (voice/incall/tx) ====='")
            appendLine("tinymix 2>/dev/null | grep -iE 'voice|incall|tx|mmul|record|capture' | head -80")
        }
        return exec(script)
    }
}
