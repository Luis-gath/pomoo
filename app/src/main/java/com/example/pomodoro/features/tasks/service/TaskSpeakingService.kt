package com.example.pomodoro.features.tasks.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import com.example.pomodoro.R
import java.util.*

class TaskSpeakingService : Service(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var textToSpeak: String = ""

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
        startForegroundService()
    }

    private fun startForegroundService() {
        val channelId = "tts_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicio de Voz",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Escuchando recordatorio")
            .setSmallIcon(R.drawable.ic_notification_timer)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                999,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(999, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = intent?.getStringExtra("TASK_TITLE") ?: ""
        val subject = intent?.getStringExtra("TASK_SUBJECT") ?: ""
        textToSpeak = "Recordatorio: $title. Curso $subject. Es momento de hacerlo."
        
        if (tts != null && tts?.isSpeaking == false) {
            speak()
        }
        
        return START_NOT_STICKY
    }

    private fun speak() {
        if (textToSpeak.isNotEmpty()) {
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "TASK_RECORDATORIO")
            // Schedule stop after speaking
            checkSpeakingStatus()
        }
    }

    private fun checkSpeakingStatus() {
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (tts?.isSpeaking == true) {
                checkSpeakingStatus()
            } else {
                stopForeground(true)
                stopSelf()
            }
        }, 1000)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("es", "ES")
            speak()
        } else {
            stopSelf()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
