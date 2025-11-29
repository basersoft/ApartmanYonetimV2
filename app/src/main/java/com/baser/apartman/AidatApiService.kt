package com.baser.apartman

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object AidatApiService {

    private const val BASE_URL = "http://baser.org/apartman/api"

    // Android için aidat listesi getirme
    fun getAndroidDues(
        userEmail: String,
        userType: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$BASE_URL/admin_aidat.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject().apply {
                    put("action", "get_android_dues")
                    put("user_email", userEmail)
                    put("user_type", userType)
                }.toString()

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()

                    val jsonResponse = JSONObject(response.toString())
                    if (jsonResponse.getBoolean("success")) {
                        onSuccess(jsonResponse)
                    } else {
                        onError(jsonResponse.getString("message"))
                    }
                } else {
                    onError("HTTP Hatası: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                onError("Bağlantı hatası: ${e.message}")
            }
        }.start()
    }

    // Yıllık aidat ekleme
    fun addYearlyAidatAndroid(
        year: Int,
        monthlyAmount: Double,
        description: String,
        userEmail: String,
        userType: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$BASE_URL/admin_aidat.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject().apply {
                    put("action", "add_yearly_aidat")
                    put("year", year)
                    put("monthly_amount", monthlyAmount)
                    put("description", description)
                    put("user_email", userEmail)
                    put("user_type", userType)
                }.toString()

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()

                    val jsonResponse = JSONObject(response.toString())
                    if (jsonResponse.getBoolean("success")) {
                        onSuccess(jsonResponse.getString("message"))
                    } else {
                        onError(jsonResponse.getString("message"))
                    }
                } else {
                    onError("HTTP Hatası: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                onError("Bağlantı hatası: ${e.message}")
            }
        }.start()
    }

    // Aylık aidat ekleme
    fun addMonthlyAidatAndroid(
        month: String,
        amount: Double,
        description: String,
        userEmail: String,
        userType: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$BASE_URL/admin_aidat.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject().apply {
                    put("action", "add_monthly_aidat")
                    put("month", month)
                    put("amount", amount)
                    put("description", description)
                    put("user_email", userEmail)
                    put("user_type", userType)
                }.toString()

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()

                    val jsonResponse = JSONObject(response.toString())
                    if (jsonResponse.getBoolean("success")) {
                        onSuccess(jsonResponse.getString("message"))
                    } else {
                        onError(jsonResponse.getString("message"))
                    }
                } else {
                    onError("HTTP Hatası: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                onError("Bağlantı hatası: ${e.message}")
            }
        }.start()
    }
    // Aidat ödeme işlemi
    fun payDue(
        dueId: Int,
        paymentAmount: Double,
        paymentMethod: String,
        notes: String,
        userEmail: String,
        userType: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$BASE_URL/admin_aidat.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val requestBody = JSONObject().apply {
                    put("action", "pay_due")
                    put("due_id", dueId)
                    put("payment_amount", paymentAmount)
                    put("payment_method", paymentMethod)
                    put("notes", notes)
                    put("user_email", userEmail)
                    put("user_type", userType)
                }.toString()

                println("🔍 Ödeme İsteği: $requestBody")

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()

                val responseCode = connection.responseCode
                println("🔍 Ödeme Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()

                    val responseText = response.toString()
                    println("🔍 Ödeme Yanıtı: $responseText")

                    try {
                        val jsonResponse = JSONObject(responseText)
                        if (jsonResponse.getBoolean("success")) {
                            onSuccess(jsonResponse.getString("message"))
                        } else {
                            onError(jsonResponse.getString("message"))
                        }
                    } catch (e: Exception) {
                        println("❌ Ödeme JSON Parse Hatası: ${e.message}")
                        onError("JSON parse hatası: ${e.message}")
                    }
                } else {
                    onError("HTTP Hatası: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                println("❌ Ödeme Bağlantı hatası: ${e.message}")
                onError("Bağlantı hatası: ${e.message}")
            }
        }.start()
    }
    // Toplu aidat ekleme
    fun addBulkAidat(
        bulkType: String,
        bulkAmount: Double,
        bulkDueDate: String,
        bulkDescription: String,
        userEmail: String,
        userType: String,
        excludePaid: Boolean,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$BASE_URL/admin_aidat.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")

                val requestBody = JSONObject().apply {
                    put("action", "add_bulk_aidat")
                    put("bulk_type", bulkType)
                    put("bulk_amount", bulkAmount)
                    put("bulk_due_date", bulkDueDate)
                    put("bulk_description", bulkDescription)
                    put("exclude_paid", excludePaid)
                    put("user_email", userEmail)
                    put("user_type", userType)
                }.toString()

                val outputStream = OutputStreamWriter(connection.outputStream)
                outputStream.write(requestBody)
                outputStream.flush()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var line: String?
                    while (inputStream.readLine().also { line = it } != null) {
                        response.append(line)
                    }
                    inputStream.close()

                    val jsonResponse = JSONObject(response.toString())
                    if (jsonResponse.getBoolean("success")) {
                        onSuccess(jsonResponse.getString("message"))
                    } else {
                        onError(jsonResponse.getString("message"))
                    }
                } else {
                    onError("HTTP Hatası: $responseCode")
                }

                connection.disconnect()
            } catch (e: Exception) {
                onError("Bağlantı hatası: ${e.message}")
            }
        }.start()
    }
}