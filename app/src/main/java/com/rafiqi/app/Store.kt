package com.rafiqi.app

import android.content.Context
import org.json.JSONObject

object Store {
    private const val P = "rafiqi"
    fun save(c: Context, json: String) {
        c.getSharedPreferences(P, 0).edit().putString("data", json).apply()
    }
    fun load(c: Context): JSONObject = try {
        JSONObject(c.getSharedPreferences(P, 0).getString("data", "{}") ?: "{}")
    } catch (e: Exception) {
        JSONObject()
    }
}
