package com.baser.apartman

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.concurrent.timerTask

class NotificationService : Service() {

    private lateinit var userEmail: String
    private var timer: Timer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        userEmail = intent?.getStringExtra("email") ?: ""

        // Her 10 dakikada bir kontrol et (test için)
        timer = Timer()
        timer?.scheduleAtFixedRate(timerTask {
            checkForNotifications()
        }, 0, 10 * 60 * 1000) // 10 dakika

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun checkForNotifications() {
        Thread {
            try {
                val url = URL("http://baser.org/apartman/api_check_notifications.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = "email=${URLEncoder.encode(userEmail, "UTF-8")}"

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    handleNotificationResponse(response)
                }

            } catch (e: Exception) {
                Log.e("NotificationService", "Kontrol hatası: ${e.message}")
            }
        }.start()
    }

    private fun handleNotificationResponse(response: String) {
        try {
            val jsonObject = JSONObject(response)
            if (jsonObject.getBoolean("success")) {
                val notifications = jsonObject.getJSONArray("notifications")

                for (i in 0 until notifications.length()) {
                    val notification = notifications.getJSONObject(i)
                    val title = notification.getString("title")
                    val message = notification.getString("message")

                    // ✅ DÜZELTİLDİ: showSimpleNotification kullan
                    NotificationHelper(this).showSimpleNotification(title, message)
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationService", "Yanıt işleme hatası: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}