package com.baser.apartman

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object NetworkUtils {

    private const val BASE_URL = "http://baser.org/apartman/"
    private const val TIMEOUT = 15000

    fun makePostRequest(
        endpoint: String,
        params: Map<String, String>,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL(BASE_URL + endpoint)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("Accept", "application/json")

                // POST data oluştur
                val postData = params.entries.joinToString("&") {
                    "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
                }

                println("🔍 API İSTEĞİ: $endpoint")
                println("🔍 GÖNDERİLEN VERİ: $postData")

                // Veriyi gönder
                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                println("🔍 HTTP YANIT KODU: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream))
                        .use { it.readText() }
                    println("🔍 BAŞARILI YANIT: $response")

                    // UI thread'de callback çağır
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onSuccess(response)
                    }
                } else {
                    val errorResponse = try {
                        BufferedReader(InputStreamReader(connection.errorStream))
                            .use { it.readText() }
                    } catch (e: Exception) {
                        "HTTP Hatası: $responseCode"
                    }

                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onError(errorResponse)
                    }
                }

            } catch (e: Exception) {
                println("🔍 NETWORK HATASI: ${e.message}")
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Bağlantı hatası: ${e.message}")
                }
            }
        }.start()
    }
}