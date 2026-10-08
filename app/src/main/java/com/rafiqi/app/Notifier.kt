package com.rafiqi.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notifier {
    fun ensureChannels(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("alerts", "التنبيهات والتذكيرات", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel("adhan", "الأذان", NotificationManager.IMPORTANCE_LOW).apply { setSound(null, null) })
        nm.createNotificationChannel(NotificationChannel("pin", "الصلاة القادمة", NotificationManager.IMPORTANCE_LOW).apply {
            setSound(null, null)
            setShowBadge(false)
        })
    }

    fun openIntent(c: Context, tab: Int, ak: String, code: Int): PendingIntent {
        val i = Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra("tab", tab)
            .putExtra("ak", ak)
        return PendingIntent.getActivity(c, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun post(c: Context, title: String, body: String, tab: Int, ak: String) {
        try {
            ensureChannels(c)
            val id = (System.currentTimeMillis() % 1000000000L).toInt()
            val b = Notification.Builder(c, "alerts")
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(Notification.BigTextStyle().bigText(body))
                .setColor(0xFF4A2390.toInt())
                .setAutoCancel(true)
                .setContentIntent(openIntent(c, tab, ak, id))
            c.getSystemService(NotificationManager::class.java).notify(id, b.build())
        } catch (e: Exception) {
        }
    }
}
