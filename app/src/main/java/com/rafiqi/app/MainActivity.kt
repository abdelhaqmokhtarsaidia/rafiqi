package com.rafiqi.app

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.webkit.GeolocationPermissions
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {
    private lateinit var web: WebView
    private var loaded = false
    private var pendingOrigin: String? = null
    private var pendingCb: GeolocationPermissions.Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannels(this)
        web = WebView(this)
        web.setBackgroundColor(Color.parseColor("#0B0714"))
        setContentView(web)

        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setGeolocationEnabled(true)
        }

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val u = request.url
                val host = u.host ?: return null
                if (host == "appassets.androidplatform.net") return loader.shouldInterceptRequest(u)
                if (request.method == "GET" && (host.endsWith("dorar.net") || host.endsWith("open-meteo.com"))) {
                    return try {
                        val c = URL(u.toString()).openConnection() as HttpURLConnection
                        c.connectTimeout = 15000
                        c.readTimeout = 20000
                        c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) Rafiqi")
                        val code = c.responseCode
                        val stream = if (code < 400) c.inputStream else c.errorStream
                        val mime = (c.contentType ?: "application/json").substringBefore(";")
                        WebResourceResponse(
                            mime, "utf-8", code, if (code < 400) "OK" else "Error",
                            mapOf("Access-Control-Allow-Origin" to "*"), stream
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                return null
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == "appassets.androidplatform.net") return false
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, request.url))
                } catch (e: Exception) {
                }
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                loaded = true
                deliver(intent)
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    callback.invoke(origin, true, false)
                } else {
                    pendingOrigin = origin
                    pendingCb = callback
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 2
                    )
                }
            }
        }

        web.addJavascriptInterface(Bridge(applicationContext), "RafiqiNative")
        web.loadUrl("https://appassets.androidplatform.net/assets/www/index.html")

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        if (Build.VERSION.SDK_INT >= 31) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                try {
                    startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
                    )
                } catch (e: Exception) {
                }
            }
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (code == 1) PinService.update(this)
        if (code == 2) {
            val ok = res.isNotEmpty() && res.any { it == PackageManager.PERMISSION_GRANTED }
            try {
                pendingCb?.invoke(pendingOrigin, ok, false)
            } catch (e: Exception) {
            }
            pendingCb = null
            pendingOrigin = null
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (loaded) deliver(intent)
    }

    private fun deliver(i: Intent?) {
        if (i == null || !i.hasExtra("tab")) return
        val tab = i.getIntExtra("tab", 0)
        val ak = (i.getStringExtra("ak") ?: "").replace("\\", "").replace("'", "")
        i.removeExtra("tab")
        web.evaluateJavascript("try{gotab({tab:$tab,ak:'$ak'})}catch(e){}", null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else moveTaskToBack(true)
    }

    override fun onResume() {
        super.onResume()
        PinService.update(this)
    }

    class Bridge(private val c: Context) {
        @JavascriptInterface
        fun sync(json: String) {
            Store.save(c, json)
            Scheduler.schedule(c)
            PinService.update(c)
        }

        @JavascriptInterface
        fun notify(title: String, body: String, tab: Int, ak: String) {
            Notifier.post(c, title, body, tab, ak)
        }
    }
}
