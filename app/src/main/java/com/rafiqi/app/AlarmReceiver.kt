package com.rafiqi.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        try {
            if (i.getStringExtra("k") == "p") {
                val fr = Store.load(c).optBoolean("fr", false)
                val nm = i.getStringExtra("nm") ?: ""
                val nw = i.getStringExtra("nw") ?: ""
                val f = i.getStringExtra("f") ?: ""
                val title = if (fr) "C'est l'heure de $nm" else "حان الآن موعد صلاة $nm"
                val hasFile = f.matches(Regex("[A-Za-z0-9_.-]+"))
                if (i.getIntExtra("ad", 0) == 1) {
                    if (hasFile) {
                        c.startForegroundService(
                            Intent(c, AdhanService::class.java).putExtra("f", f).putExtra("t", title)
                        )
                    } else {
                        Notifier.post(c, title, "", 0, "")
                    }
                    if (nw.isNotEmpty()) {
                        Notifier.post(c, if (fr) "Rappel des sunnas" else "تذكير بالنوافل", nw, 0, "")
                    }
                } else if (nw.isNotEmpty()) {
                    Notifier.post(c, title, nw, 0, "")
                }
            } else {
                Notifier.post(
                    c,
                    i.getStringExtra("t") ?: "رفيقي",
                    i.getStringExtra("x") ?: "",
                    i.getIntExtra("tab", 0),
                    i.getStringExtra("ak") ?: ""
                )
            }
        } catch (e: Exception) {
        }
        Scheduler.schedule(c)
        PinService.update(c)
    }
}
