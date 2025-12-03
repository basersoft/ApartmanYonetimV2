package com.baser.apartman

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object AidatApiService {

    private const val BASE_URL = "http://baser.org/apartman/api"
    private val handler = Handler(Looper.getMainLooper())

    // Tüm API istekleri için ortak fonksiyon
    private fun makeApiRequest(
        requestBody: JSONObject,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val url = URL("$BASE_URL/admin_aidat.php")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.doInput = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setRequestProperty("Accept", "application/json")

                // Log request
                println("🔍 API Request to: admin_aidat.php")
                println("🔍 Request Body: $requestBody")

                // Request gönder
                OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                println("🔍 Response Code: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = BufferedReader(InputStreamReader(connection.inputStream))
                        .use { it.readText() }

                    println("🔍 Response: $response")

                    val jsonResponse = JSONObject(response)

                    if (jsonResponse.getBoolean("success")) {
                        handler.post { onSuccess(jsonResponse) }
                    } else {
                        val errorMsg = jsonResponse.getString("message")
                        handler.post { onError(errorMsg) }
                    }
                } else {
                    val errorResponse = try {
                        BufferedReader(InputStreamReader(connection.errorStream))
                            .use { it.readText() }
                    } catch (e: Exception) {
                        "HTTP Hatası: $responseCode"
                    }
                    handler.post { onError(errorResponse) }
                }

                connection.disconnect()

            } catch (e: Exception) {
                println("❌ Exception: ${e.message}")
                handler.post { onError("Bağlantı hatası: ${e.message}") }
            }
        }.start()
    }

    // Android için aidat listesi getirme
    fun getAndroidDues(
        userEmail: String,
        userType: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("action", "get_android_dues")
            put("user_email", userEmail)
            put("user_type", userType)
        }

        makeApiRequest(requestBody, onSuccess, onError)
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
        val requestBody = JSONObject().apply {
            put("action", "add_yearly_aidat")
            put("year", year)
            put("monthly_amount", monthlyAmount)
            put("description", description)
            put("user_email", userEmail)
            put("user_type", userType)
        }

        makeApiRequest(requestBody,
            { response -> handler.post { onSuccess(response.getString("message")) } },
            onError
        )
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
        val requestBody = JSONObject().apply {
            put("action", "add_monthly_aidat")
            put("month", month)
            put("amount", amount)
            put("description", description)
            put("user_email", userEmail)
            put("user_type", userType)
        }

        makeApiRequest(requestBody,
            { response -> handler.post { onSuccess(response.getString("message")) } },
            onError
        )
    }

    // Aidat ödeme işlemi (TAM ve KISMI ÖDEME için GÜNCELLENMİŞ)
    fun payDue(
        dueId: Int,
        paymentAmount: Double,
        paymentMethod: String,
        notes: String,
        userEmail: String,
        userType: String,
        createNewDueForRemaining: Boolean = true,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        // Benzersiz transaction ID oluştur
        val transactionId = "MOB-${System.currentTimeMillis()}-${UUID.randomUUID().toString().substring(0, 8)}"

        val requestBody = JSONObject().apply {
            put("action", "pay_due")
            put("due_id", dueId)
            put("payment_amount", paymentAmount)
            put("payment_method", paymentMethod)
            put("notes", notes)
            put("user_email", userEmail)
            put("user_type", userType)
            put("transaction_id", transactionId)
            put("create_new_due_for_remaining", createNewDueForRemaining)
        }

        makeApiRequest(requestBody,
            { response ->
                val message = response.getString("message")
                val paymentType = if (response.has("payment_type")) {
                    response.getString("payment_type")
                } else "full"

                val successMessage = if (paymentType == "partial") {
                    val remainingAmount = response.getDouble("remaining_amount")
                    val receiptNumber = if (response.has("receipt_number")) {
                        "\nFiş No: ${response.getString("receipt_number")}"
                    } else ""

                    "✅ $message\n\n" +
                            "Ödenen: ${String.format("%.2f TL", paymentAmount)}\n" +
                            "Kalan: ${String.format("%.2f TL", remainingAmount)}" +
                            receiptNumber
                } else {
                    val receiptNumber = if (response.has("receipt_number")) {
                        "\nFiş No: ${response.getString("receipt_number")}"
                    } else ""
                    "✅ $message$receiptNumber"
                }

                handler.post { onSuccess(successMessage) }
            },
            onError
        )
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
        val requestBody = JSONObject().apply {
            put("action", "add_bulk_aidat")
            put("bulk_type", bulkType)
            put("bulk_amount", bulkAmount)
            put("bulk_due_date", bulkDueDate)
            put("bulk_description", bulkDescription)
            put("exclude_paid", excludePaid)
            put("user_email", userEmail)
            put("user_type", userType)
        }

        makeApiRequest(requestBody,
            { response -> handler.post { onSuccess(response.getString("message")) } },
            onError
        )
    }

    // Ödeme geçmişini getir
    fun getPaymentHistory(
        userEmail: String,
        userType: String,
        dueId: Int? = null,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("action", "get_payment_history")
            put("user_email", userEmail)
            put("user_type", userType)
            if (dueId != null) {
                put("due_id", dueId)
            }
        }

        makeApiRequest(requestBody, onSuccess, onError)
    }

    // Muhasebe kayıtlarını getir
    fun getAccountingEntries(
        userEmail: String,
        userType: String,
        startDate: String? = null,
        endDate: String? = null,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("action", "get_accounting_entries")
            put("user_email", userEmail)
            put("user_type", userType)
            if (startDate != null) put("start_date", startDate)
            if (endDate != null) put("end_date", endDate)
        }

        makeApiRequest(requestBody, onSuccess, onError)
    }

    // Kısmi ödeme kayıtlarını getir
    fun getPartialPayments(
        userEmail: String,
        userType: String,
        dueId: Int? = null,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("action", "get_partial_payments")
            put("user_email", userEmail)
            put("user_type", userType)
            if (dueId != null) {
                put("due_id", dueId)
            }
        }

        makeApiRequest(requestBody, onSuccess, onError)
    }

    // Aidat silme
    fun deleteAidat(
        dueId: Int,
        userEmail: String,
        userType: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("action", "delete_aidat")
            put("due_id", dueId)
            put("user_email", userEmail)
            put("user_type", userType)
        }

        makeApiRequest(requestBody,
            { response -> handler.post { onSuccess(response.getString("message")) } },
            onError
        )
    }

    // Test bağlantısı
    fun testConnection(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("action", "test_connection")
        }

        makeApiRequest(requestBody,
            { response -> handler.post { onSuccess(response.getString("message")) } },
            onError
        )
    }

    // Ping test
    fun ping(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val requestBody = JSONObject().apply {
            put("action", "ping")
        }

        makeApiRequest(requestBody,
            { response -> handler.post { onSuccess(response.getString("message")) } },
            onError
        )
    }
}