package com.kingofai.voicechanger

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the [VoiceEngine] alive while the app is in the
 * background (e.g. during a call), which Android requires for continuous mic use.
 */
class VoiceChangerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopEngine()
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                val effect = VoiceEffect.byNameOr(intent?.getStringExtra(EXTRA_EFFECT))
                val intensity = intent?.getFloatExtra(EXTRA_INTENSITY, 1f) ?: 1f
                val routeToCall = intent?.getBooleanExtra(EXTRA_ROUTE_CALL, false) ?: false
                startForegroundInternal(effect)
                startEngine(effect, intensity, routeToCall)
            }
        }
        return START_STICKY
    }

    private fun startEngine(effect: VoiceEffect, intensity: Float, routeToCall: Boolean) {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val engine = EngineHolder.getOrCreate()
        engine.applyEffect(effect, intensity)
        if (!engine.isRunning) engine.start(am, routeToCall)
    }

    private fun stopEngine() {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        EngineHolder.engine?.stop(am)
    }

    private fun startForegroundInternal(effect: VoiceEffect) {
        val channelId = "voice_changer"
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "مغيّر الصوت",
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("مغيّر الصوت يعمل")
            .setContentText("المؤثر الحالي: ${effect.emoji} ${effect.displayName}")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    override fun onDestroy() {
        stopEngine()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.kingofai.voicechanger.STOP"
        const val EXTRA_EFFECT = "effect"
        const val EXTRA_INTENSITY = "intensity"
        const val EXTRA_ROUTE_CALL = "route_call"
        private const val NOTIF_ID = 1001
    }
}

/** Holds a single shared engine instance across Activity and Service. */
object EngineHolder {
    @Volatile var engine: VoiceEngine? = null
        private set

    fun getOrCreate(onError: (String) -> Unit = {}): VoiceEngine {
        return engine ?: VoiceEngine(onError = onError).also { engine = it }
    }
}
