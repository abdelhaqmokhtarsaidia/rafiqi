package com.rafiqi.app

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.widget.RemoteViews
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

class PinService : Service() {
    private val h = Handler(Looper.getMainLooper())
    private val tick = Runnable { show() }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        if (!show()) return START_NOT_STICKY
        return START_STICKY
    }

    /** يعرض البطاقة، ويعيد false إذا كانت الميزة مطفأة */
    private fun show(): Boolean {
        val d = Store.load(this)
        if (d.optInt("pin", 0) != 1) {
            Notifier.ensureChannels(this)
            val dummy = Notification.Builder(this, "pin").setSmallIcon(R.drawable.ic_stat).setContentTitle("رفيقي").build()
            if (Build.VERSION.SDK_INT >= 34) startForeground(ID, dummy, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(ID, dummy)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return false
        }
        Notifier.ensureChannels(this)
        val fr = d.optBoolean("fr", false)
        val now = System.currentTimeMillis()
        var next: JSONObject? = null
        val ev = d.optJSONArray("ev")
        if (ev != null) {
            for (k in 0 until ev.length()) {
                val e = ev.getJSONObject(k)
                if (e.getLong("ms") > now) { next = e; break }
            }
        }
        val rv = RemoteViews(packageName, R.layout.notif_next)
        rv.setTextViewText(R.id.lbl, if (fr) "PROCHAINE PRIÈRE" else "الصلاة القادمة")
        rv.setTextViewText(R.id.date, dateText(fr))
        if (next != null) {
            val ms = next.getLong("ms")
            rv.setTextViewText(R.id.name, next.optString("nm"))
            rv.setTextViewText(R.id.time, next.optString("hm"))
            rv.setChronometerCountDown(R.id.chrono, true)
            rv.setChronometer(R.id.chrono, SystemClock.elapsedRealtime() + (ms - now), null, true)
            h.removeCallbacks(tick)
            h.postDelayed(tick, maxOf(1000L, ms - now + 1500L))
        } else {
            rv.setTextViewText(R.id.name, "—")
            rv.setTextViewText(R.id.time, if (fr) "Ouvrez l'application" else "افتح التطبيق للتحديث")
            rv.setChronometer(R.id.chrono, SystemClock.elapsedRealtime(), null, false)
        }
        val qibla = if (fr) "Qibla" else "القبلة"
        val nt = Notification.Builder(this, "pin")
            .setSmallIcon(R.drawable.ic_stat)
            .setStyle(Notification.DecoratedCustomViewStyle())
            .setCustomContentView(rv)
            .setCustomBigContentView(rv)
            .setColor(0xFF4A2390.toInt())
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setContentIntent(Notifier.openIntent(this, 0, "", 10))
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, R.drawable.ic_stat), qibla, Notifier.openIntent(this, 4, "", 11)
                ).build()
            )
        if (Build.VERSION.SDK_INT >= 31) nt.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        val built = nt.build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, built, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID, built)
        return true
    }

    private fun dateText(fr: Boolean): String {
        val loc = if (fr) Locale.FRENCH else Locale("ar")
        val g = SimpleDateFormat("EEEE d MMMM yyyy", loc).format(Date())
        val hj = try {
            DateTimeFormatter.ofPattern("d MMMM yyyy", loc)
                .withChronology(HijrahChronology.INSTANCE)
                .format(HijrahDate.from(LocalDate.now()))
        } catch (e: Exception) {
            ""
        }
        return if (hj.isEmpty()) g else "$g  ·  $hj"
    }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        super.onDestroy()
    }

    companion object {
        const val ID = 4242

        fun update(c: Context) {
            try {
                val i = Intent(c, PinService::class.java)
                if (Store.load(c).optInt("pin", 0) == 1) c.startForegroundService(i) else c.stopService(i)
            } catch (e: Exception) {
            }
        }
    }
}
