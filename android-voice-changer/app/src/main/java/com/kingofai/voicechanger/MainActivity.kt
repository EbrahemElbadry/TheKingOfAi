package com.kingofai.voicechanger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.kingofai.voicechanger.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var selected: VoiceEffect = VoiceEffect.NONE
    private var isRunning = false

    private val meterHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val meterTick = object : Runnable {
        override fun run() {
            val level = EngineHolder.engine?.inputLevel ?: 0f
            binding.levelBar.progress = (level * 100f).toInt()
            meterHandler.postDelayed(this, 80)
        }
    }

    private val buttonByEffect = mutableMapOf<VoiceEffect, MaterialButton>()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) startEngine()
        else toast("لا يمكن تشغيل مغيّر الصوت بدون إذن الميكروفون")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buildEffectButtons()
        updateIntensityLabel(binding.intensitySeek.progress)

        binding.intensitySeek.setOnSeekBarChangeListener(object :
            android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: android.widget.SeekBar, p: Int, u: Boolean) {
                updateIntensityLabel(p)
                if (isRunning) reapplyLive()
            }
            override fun onStartTrackingTouch(sb: android.widget.SeekBar) {}
            override fun onStopTrackingTouch(sb: android.widget.SeekBar) {}
        })

        binding.toggleButton.setOnClickListener {
            if (isRunning) stopEngine() else requestAndStart()
        }

        binding.rootDiagButton.setOnClickListener { runRootDiagnostics() }

        binding.voiceNoteButton.setOnClickListener {
            startActivity(Intent(this, VoiceNoteActivity::class.java))
        }

        refreshUi()
    }

    private fun buildEffectButtons() {
        binding.effectsGrid.removeAllViews()
        buttonByEffect.clear()
        VoiceEffect.entries.forEach { effect ->
            val btn = MaterialButton(this).apply {
                text = "${effect.emoji}\n${effect.displayName}"
                isAllCaps = false
                gravity = Gravity.CENTER
                setOnClickListener { selectEffect(effect) }
            }
            val lp = android.widget.GridLayout.LayoutParams().apply {
                width = 0
                height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = android.widget.GridLayout.spec(
                    android.widget.GridLayout.UNDEFINED, 1f
                )
                setMargins(8, 8, 8, 8)
            }
            btn.layoutParams = lp
            binding.effectsGrid.addView(btn)
            buttonByEffect[effect] = btn
        }
        selectEffect(VoiceEffect.NONE)
    }

    private fun selectEffect(effect: VoiceEffect) {
        selected = effect
        buttonByEffect.forEach { (e, b) ->
            b.alpha = if (e == effect) 1.0f else 0.55f
            b.strokeWidth = if (e == effect) 6 else 0
        }
        if (isRunning) reapplyLive()
    }

    private fun intensity(): Float = binding.intensitySeek.progress / 100f

    private fun updateIntensityLabel(progress: Int) {
        binding.intensityLabel.text = "شدة المؤثر: $progress%"
    }

    private fun requestAndStart() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.RECORD_AUDIO

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.POST_NOTIFICATIONS

        if (needed.isEmpty()) startEngine()
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun startEngine() {
        val intent = Intent(this, VoiceChangerService::class.java).apply {
            putExtra(VoiceChangerService.EXTRA_EFFECT, selected.name)
            putExtra(VoiceChangerService.EXTRA_INTENSITY, intensity())
            putExtra(VoiceChangerService.EXTRA_ROUTE_CALL, binding.callModeSwitch.isChecked)
        }
        ContextCompat.startForegroundService(this, intent)
        isRunning = true
        meterHandler.post(meterTick)
        refreshUi()
    }

    private fun reapplyLive() {
        // Restart-less live update: push new params via the shared engine.
        EngineHolder.engine?.applyEffect(selected, intensity())
    }

    private fun stopEngine() {
        val intent = Intent(this, VoiceChangerService::class.java)
            .apply { action = VoiceChangerService.ACTION_STOP }
        startService(intent)
        isRunning = false
        meterHandler.removeCallbacks(meterTick)
        binding.levelBar.progress = 0
        refreshUi()
    }

    override fun onDestroy() {
        meterHandler.removeCallbacks(meterTick)
        super.onDestroy()
    }

    private fun refreshUi() {
        binding.toggleButton.text = if (isRunning) "⏹️ إيقاف" else "▶️ تشغيل"
        binding.statusText.text = if (isRunning)
            "يعمل الآن — تحدّث في الميكروفون"
        else
            "متوقف"
    }

    private fun runRootDiagnostics() {
        toast("جارٍ طلب صلاحية الروت وجمع المعلومات…")
        Thread {
            val hasRoot = RootShell.isRootAvailable()
            if (!hasRoot) {
                runOnUiThread {
                    toast("لم يتم منح صلاحية الروت (تأكد من وجود Magisk/su والموافقة على الطلب)")
                }
                return@Thread
            }
            val result = RootShell.collectAudioDiagnostics()
            val text = result.output.ifBlank { "لا يوجد ناتج (exit=${result.exitCode})" }

            // Save a copy to the app's external files dir for easy sharing.
            val saved = runCatching {
                val dir = getExternalFilesDir(null)
                val f = java.io.File(dir, "voicechanger_diag.txt")
                f.writeText(text)
                f.absolutePath
            }.getOrNull()

            runOnUiThread { showDiagnosticsDialog(text, saved) }
        }.start()
    }

    private fun showDiagnosticsDialog(text: String, savedPath: String?) {
        val scroll = android.widget.ScrollView(this)
        val tv = android.widget.TextView(this).apply {
            setTextIsSelectable(true)
            typeface = android.graphics.Typeface.MONOSPACE
            textSize = 11f
            setPadding(32, 32, 32, 32)
            setText(text)
        }
        scroll.addView(tv)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("تشخيص الصوت (انسخه وابعته للمطوّر)")
            .setView(scroll)
            .setPositiveButton("نسخ") { _, _ ->
                val cb = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cb.setPrimaryClip(android.content.ClipData.newPlainText("diag", text))
                toast("تم النسخ" + (savedPath?.let { "\nمحفوظ في: $it" } ?: ""))
            }
            .setNegativeButton("إغلاق", null)
            .show()
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
