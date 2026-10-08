package com.rafiqi.app

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager

class AdhanService : Service() {
    private var mp: MediaPlayer? = null
    private var wl: PowerManager.WakeLock? = null

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        if (i?.action == "STOP") {
            stopAll()
            return START_NOT_STICKY
        }
        Notifier.ensureChannels(this)
        val title = i?.getStringExtra("t") ?: "رفيقي"
        val file = i?.getStringExtra("f") ?: ""
        val stopPi = PendingIntent.getService(
            this, 1, Intent(this, AdhanService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE
        )
        val fr = Store.load(this).optBoolean("fr", false)
        val stopLabel = if (fr) "Arrêter l'adhan" else "إيقاف الأذان"
        val nt = Notification.Builder(this, "adhan")
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(title)
            .setContentText(stopLabel)
            .setColor(0xFF4A2390.toInt())
            .setOngoing(true)
            .setContentIntent(Notifier.openIntent(this, 0, "", 2))
            .addAction(Notification.Action.Builder(Icon.createWithResource(this, R.drawable.ic_stat), stopLabel, stopPi).build())
            .build()
        if (Build.VERSION.SDK_INT >= 29) startForeground(77, nt, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        else startForeground(77, nt)
        play(file)
        return START_NOT_STICKY
    }

    private fun play(file: String) {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "rafiqi:adhan").apply { acquire(7 * 60 * 1000L) }
            mp?.release()
            val afd = assets.openFd("www/adhan/$file")
            val m = MediaPlayer()
            m.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            m.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            m.setOnCompletionListener { stopAll() }
            m.setOnErrorListener { _, _, _ -> stopAll(); true }
            m.prepare()
            m.start()
            mp = m
        } catch (e: Exception) {
            stopAll()
        }
    }

    private fun stopAll() {
        try { mp?.release() } catch (e: Exception) {}
        mp = null
        try { if (wl?.isHeld == true) wl?.release() } catch (e: Exception) {}
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        try { mp?.release() } catch (e: Exception) {}
        try { if (wl?.isHeld == true) wl?.release() } catch (e: Exception) {}
        super.onDestroy()
    }
}
