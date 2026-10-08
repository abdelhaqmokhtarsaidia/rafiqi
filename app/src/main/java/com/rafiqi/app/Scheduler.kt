package com.rafiqi.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle

object Scheduler {
    private fun pi(c: Context, code: Int, extras: Bundle?): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java)
        i.data = Uri.parse("rafiqi://alarm/$code")
        if (extras != null) i.putExtras(extras)
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun schedule(c: Context) {
        try {
            val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val sp = c.getSharedPreferences("rafiqi", 0)
            val old = sp.getInt("n", 0)
            for (k in 0 until old) {
                val p = pi(c, k, null)
                am.cancel(p)
                p.cancel()
            }
            val d = Store.load(c)
            val now = System.currentTimeMillis()
            val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
            var n = 0

            fun put(ms: Long, b: Bundle) {
                if (ms <= now || n >= 150) return
                val p = pi(c, n, b)
                if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, p)
                else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, ms, p)
                n++
            }

            val ev = d.optJSONArray("ev")
            if (ev != null) {
                for (k in 0 until ev.length()) {
                    val e = ev.getJSONObject(k)
                    val ad = e.optInt("ad", 0)
                    val nw = e.optString("nw", "")
                    if (ad != 1 && nw.isEmpty()) continue
                    val b = Bundle()
                    b.putString("k", "p")
                    b.putString("nm", e.optString("nm"))
                    b.putInt("ad", ad)
                    b.putString("f", e.optString("f"))
                    b.putString("nw", nw)
                    put(e.getLong("ms"), b)
                }
            }
            val rm = d.optJSONArray("rm")
            if (rm != null) {
                for (k in 0 until rm.length()) {
                    val e = rm.getJSONObject(k)
                    val b = Bundle()
                    b.putString("k", "r")
                    b.putString("t", e.optString("t"))
                    b.putString("x", e.optString("x"))
                    b.putInt("tab", e.optInt("tab", 0))
                    b.putString("ak", e.optString("ak"))
                    put(e.getLong("ms"), b)
                }
            }
            sp.edit().putInt("n", n).apply()
        } catch (e: Exception) {
        }
    }
}
