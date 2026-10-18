package com.baser.apartman

import org.json.JSONArray
import org.json.JSONObject
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import android.util.Log

object AidatApiService {
    private const val BASE_URL = "http://baser.org/site/api/"
    private const val TAG = "AidatApiService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // ============ DATA CLASSES ============

    data class PaymentResult(
        val success: Boolean,
        val message: String,
        val payment_type: String? = null,
        val payment_amount: Double? = 0.0,
        val paid_amount: Double? = 0.0,
        val remaining_amount: Double? = 0.0,
        val receipt_number: String? = "",
        val transaction_id: String? = "",
        val new_due_id: Int? = 0,
        val late_fee_amount: Double? = 0.0,
        val original_amount: Double? = 0.0,
        val total_amount: Double? = 0.0,
        val bank_transaction_id: String? = null,
        val account_code: String? = null,
        val site_id: Int? = 1,
        val payment_details: PaymentDetails? = null
    )

    data class PaymentDetails(
        val payment_amount: Double,
        val payment_method: String,
        val receipt_number: String,
        val transaction_id: String,
        val bank_transaction_id: String?,
        val previous_remaining: Double,
        val new_remaining: Double,
        val is_full_payment: Boolean,
        val payment_status: String,
        val paid_principal: Double,
        val paid_late_fee: Double,
        val site_id: Int,
        val account_codes: Map<String, String>
    )

    data class DueDetail(
        val id: Int,
        val amount: Double,
        val late_fee_amount: Double,
        val paid_amount: Double,
        val total_amount: Double,
        val remaining_amount: Double,
        val description: String,
        val due_date: String,
        val status: String,
        val payment_status: String,
        val user_name: String,
        val apartment_code: String,
        val days_late: Int,
        val has_late_fee: Boolean,
        val is_overdue: Boolean,
        val site_id: Int
    )

    data class PaymentHistory(
        val id: Int,
        val due_id: Int,
        val due_description: String,
        val user_name: String,
        val apartment: String,
        val original_amount: Double,
        val late_fee_amount: Double,
        val payment_amount: Double,
        val payment_date: String,
        val payment_method: String,
        val receipt_number: String,
        val transaction_id: String,
        val notes: String,
        val is_partial: Int,
        val new_due_id: Int,
        val partial_remaining: Double
    )

    // ============ API FONKSİYONLARI ============

    // 1. Aidat Listesi Al
    fun getDues(
        userEmail: String,
        userType: String,
        onSuccess: (List<Aidat>) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("action", "get_android_dues")
            put("user_email", userEmail)
            put("user_type", userType)
        }.toString()

        makeRequest(json, { response ->
            try {
                val duesArray = response.optJSONArray("dues")
                val aidatList = mutableListOf<Aidat>()

                if (duesArray != null && duesArray.length() > 0) {
                    for (i in 0 until duesArray.length()) {
                        val dueJson = duesArray.getJSONObject(i)
                        val aidat = Aidat.fromApiJson(dueJson)
                        aidatList.add(aidat)
                    }
                }

                onSuccess(aidatList)
            } catch (e: Exception) {
                onError("JSON parse error: ${e.message}")
            }
        }, onError)
    }

    // 1.1 Eski fonksiyon adı için alias
    fun getAndroidDues(
        userEmail: String,
        userType: String,
        onSuccess: (List<Aidat>) -> Unit,
        onError: (String) -> Unit
    ) {
        getDues(userEmail, userType, onSuccess, onError)
    }

    // 2. Ödeme Yap (Web ile TAM SENKRON)
    fun makePayment(
        dueId: Int,
        paymentAmount: Double,
        paymentMethod: String,
        notes: String,
        userEmail: String,
        userType: String,
        onSuccess: (PaymentResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("action", "make_payment")
            put("due_id", dueId)
            put("payment_amount", paymentAmount)
            put("payment_method", paymentMethod)
            put("notes", notes)
            put("user_email", userEmail)
            put("user_type", userType)
        }.toString()

        Log.d(TAG, "📤 Ödeme isteği: $json")

        makeRequest(json, { response ->
            try {
                val success = response.optBoolean("success", false)
                val message = response.optString("message", "")

                if (success) {
                    val paymentDetailsJson = response.optJSONObject("payment_details")
                    val paymentDetails = if (paymentDetailsJson != null) {
                        val accountCodesJson = paymentDetailsJson.optJSONObject("account_codes")
                        val accountCodes = mutableMapOf<String, String>()

                        if (accountCodesJson != null) {
                            val keys = accountCodesJson.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                accountCodes[key] = accountCodesJson.optString(key, "")
                            }
                        }

                        PaymentDetails(
                            payment_amount = paymentDetailsJson.optDouble("payment_amount", paymentAmount),
                            payment_method = paymentDetailsJson.optString("payment_method", paymentMethod),
                            receipt_number = paymentDetailsJson.optString("receipt_number", ""),
                            transaction_id = paymentDetailsJson.optString("transaction_id", ""),
                            bank_transaction_id = paymentDetailsJson.optString("bank_transaction_id", null),
                            previous_remaining = paymentDetailsJson.optDouble("previous_remaining", 0.0),
                            new_remaining = paymentDetailsJson.optDouble("new_remaining", 0.0),
                            is_full_payment = paymentDetailsJson.optBoolean("is_full_payment", false),
                            payment_status = paymentDetailsJson.optString("payment_status", ""),
                            paid_principal = paymentDetailsJson.optDouble("paid_principal", 0.0),
                            paid_late_fee = paymentDetailsJson.optDouble("paid_late_fee", 0.0),
                            site_id = paymentDetailsJson.optInt("site_id", 1),
                            account_codes = accountCodes
                        )
                    } else {
                        null
                    }

                    val result = PaymentResult(
                        success = true,
                        message = message,
                        payment_type = paymentDetails?.payment_status ?: "partial",
                        payment_amount = paymentDetails?.payment_amount ?: paymentAmount,
                        paid_amount = paymentDetails?.let { it.paid_principal + it.paid_late_fee } ?: paymentAmount,
                        remaining_amount = paymentDetails?.new_remaining ?: 0.0,
                        receipt_number = paymentDetails?.receipt_number ?: "",
                        transaction_id = paymentDetails?.transaction_id ?: "",
                        bank_transaction_id = paymentDetails?.bank_transaction_id,
                        account_code = paymentDetails?.account_codes?.get("asset"),
                        site_id = paymentDetails?.site_id ?: 1,
                        payment_details = paymentDetails
                    )

                    onSuccess(result)
                } else {
                    onSuccess(PaymentResult(success = false, message = message))
                }
            } catch (e: Exception) {
                onError("Ödeme işlemi hatası: ${e.message}")
            }
        }, onError)
    }

    // 2.1 Eski ödeme fonksiyonu
    // AidatApiService.kt - payDue fonksiyonunu bul ve şu şekilde düzelt:

    fun payDue(
        dueId: Int,
        paymentAmount: Double,
        paymentMethod: String,
        notes: String,
        userEmail: String,
        userType: String,
        onSuccess: (PaymentResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            // "web_pay_due" yerine "make_payment" kullan
            put("action", "make_payment")
            put("due_id", dueId)
            put("payment_amount", paymentAmount)
            put("payment_method", paymentMethod)
            put("notes", notes)
            put("user_email", userEmail)
            put("user_type", userType)
        }.toString()

        Log.d(TAG, "📤 Ödeme isteği: $json")

        makeRequest(json, { response ->
            try {
                val success = response.optBoolean("success", false)
                val message = response.optString("message", "")

                if (success) {
                    val paymentDetailsJson = response.optJSONObject("payment_details")
                    val paymentDetails = if (paymentDetailsJson != null) {
                        val accountCodesJson = paymentDetailsJson.optJSONObject("account_codes")
                        val accountCodes = mutableMapOf<String, String>()

                        if (accountCodesJson != null) {
                            val keys = accountCodesJson.keys()
                            while (keys.hasNext()) {
                                val key = keys.next()
                                accountCodes[key] = accountCodesJson.optString(key, "")
                            }
                        }

                        PaymentDetails(
                            payment_amount = paymentDetailsJson.optDouble("payment_amount", paymentAmount),
                            payment_method = paymentDetailsJson.optString("payment_method", paymentMethod),
                            receipt_number = paymentDetailsJson.optString("receipt_number", ""),
                            transaction_id = paymentDetailsJson.optString("transaction_id", ""),
                            bank_transaction_id = paymentDetailsJson.optString("bank_transaction_id", null),
                            previous_remaining = paymentDetailsJson.optDouble("previous_remaining", 0.0),
                            new_remaining = paymentDetailsJson.optDouble("new_remaining", 0.0),
                            is_full_payment = paymentDetailsJson.optBoolean("is_full_payment", false),
                            payment_status = paymentDetailsJson.optString("payment_status", ""),
                            paid_principal = paymentDetailsJson.optDouble("paid_principal", 0.0),
                            paid_late_fee = paymentDetailsJson.optDouble("paid_late_fee", 0.0),
                            site_id = paymentDetailsJson.optInt("site_id", 1),
                            account_codes = accountCodes
                        )
                    } else {
                        null
                    }

                    val result = PaymentResult(
                        success = true,
                        message = message,
                        payment_type = paymentDetails?.payment_status ?: "partial",
                        payment_amount = paymentDetails?.payment_amount ?: paymentAmount,
                        paid_amount = paymentDetails?.let { it.paid_principal + it.paid_late_fee } ?: paymentAmount,
                        remaining_amount = paymentDetails?.new_remaining ?: 0.0,
                        receipt_number = paymentDetails?.receipt_number ?: "",
                        transaction_id = paymentDetails?.transaction_id ?: "",
                        bank_transaction_id = paymentDetails?.bank_transaction_id,
                        account_code = paymentDetails?.account_codes?.get("asset"),
                        site_id = paymentDetails?.site_id ?: 1,
                        payment_details = paymentDetails
                    )

                    onSuccess(result)
                } else {
                    onSuccess(PaymentResult(success = false, message = message))
                }
            } catch (e: Exception) {
                onError("Ödeme işlemi hatası: ${e.message}")
            }
        }, onError)
    }
    // 3. Ödeme Geçmişi Al
    fun getPaymentHistory(
        userEmail: String,
        userType: String,
        dueId: Int? = null,
        onSuccess: (List<PaymentHistory>) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("action", "get_payment_history")
            put("user_email", userEmail)
            put("user_type", userType)
            if (dueId != null) {
                put("due_id", dueId)
            }
        }.toString()

        makeRequest(json, { response ->
            try {
                val paymentsArray = response.optJSONArray("payments")
                val paymentList = mutableListOf<PaymentHistory>()

                if (paymentsArray != null && paymentsArray.length() > 0) {
                    for (i in 0 until paymentsArray.length()) {
                        val paymentJson = paymentsArray.getJSONObject(i)
                        val payment = PaymentHistory(
                            id = paymentJson.optInt("id", 0),
                            due_id = paymentJson.optInt("due_id", 0),
                            due_description = paymentJson.optString("due_description", ""),
                            user_name = paymentJson.optString("user_name", ""),
                            apartment = paymentJson.optString("apartment", ""),
                            original_amount = paymentJson.optDouble("original_amount", 0.0),
                            late_fee_amount = paymentJson.optDouble("late_fee_amount", 0.0),
                            payment_amount = paymentJson.optDouble("payment_amount", 0.0),
                            payment_date = paymentJson.optString("payment_date", ""),
                            payment_method = paymentJson.optString("payment_method", ""),
                            receipt_number = paymentJson.optString("receipt_number", ""),
                            transaction_id = paymentJson.optString("transaction_id", ""),
                            notes = paymentJson.optString("notes", ""),
                            is_partial = paymentJson.optInt("is_partial", 0),
                            new_due_id = paymentJson.optInt("new_due_id", 0),
                            partial_remaining = paymentJson.optDouble("partial_remaining", 0.0)
                        )
                        paymentList.add(payment)
                    }
                }

                onSuccess(paymentList)
            } catch (e: Exception) {
                onError("JSON parse error: ${e.message}")
            }
        }, onError)
    }

    // 4. Aidat Detayı Al
    fun getDueDetail(
        dueId: Int,
        userEmail: String,
        userType: String,
        onSuccess: (DueDetail) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("action", "get_due_detail")
            put("due_id", dueId)
            put("user_email", userEmail)
            put("user_type", userType)
        }.toString()

        makeRequest(json, { response ->
            try {
                val success = response.optBoolean("success", false)

                if (success) {
                    val dueJson = response.optJSONObject("due")
                    if (dueJson != null) {
                        val dueDetail = DueDetail(
                            id = dueJson.optInt("id", 0),
                            amount = dueJson.optDouble("amount", 0.0),
                            late_fee_amount = dueJson.optDouble("late_fee_amount", 0.0),
                            paid_amount = dueJson.optDouble("paid_amount", 0.0),
                            total_amount = dueJson.optDouble("total_amount", 0.0),
                            remaining_amount = dueJson.optDouble("remaining_amount", 0.0),
                            description = dueJson.optString("description", ""),
                            due_date = dueJson.optString("due_date", ""),
                            status = dueJson.optString("status", ""),
                            payment_status = dueJson.optString("payment_status", ""),
                            user_name = dueJson.optString("user_name", ""),
                            apartment_code = dueJson.optString("apartment_code", ""),
                            days_late = dueJson.optInt("days_late", 0),
                            has_late_fee = dueJson.optBoolean("has_late_fee", false),
                            is_overdue = dueJson.optBoolean("is_overdue", false),
                            site_id = dueJson.optInt("site_id", 1)
                        )
                        onSuccess(dueDetail)
                    } else {
                        onError("Aidat detayı alınamadı")
                    }
                } else {
                    onError(response.optString("message", "Aidat detayı alınamadı"))
                }
            } catch (e: Exception) {
                onError("JSON parse error: ${e.message}")
            }
        }, onError)
    }

    // 5. Ping Test
    fun ping(
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val json = JSONObject().apply {
            put("action", "ping")
        }.toString()

        makeRequest(json, { response ->
            val success = response.optBoolean("success", false)
            val message = response.optString("message", "")

            if (success) {
                onSuccess(message)
            } else {
                onError(message)
            }
        }, onError)
    }

    // ============ ORTAK REQUEST FONKSİYONU ============
    private fun makeRequest(
        jsonBody: String,
        onSuccess: (JSONObject) -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = jsonBody.toRequestBody(mediaType)

                val url = "${BASE_URL}admin_aidat.php"
                Log.d(TAG, "🌐 İstek URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("Content-Type", "application/json; charset=utf-8")
                    .addHeader("Accept", "application/json")
                    .build()

                Log.d(TAG, "📤 İstek gönderiliyor...")

                client.newCall(request).execute().use { response ->
                    Log.d(TAG, "📡 Yanıt Kodu: ${response.code}")

                    val responseBody = response.body?.string()
                    Log.d(TAG, "📦 Ham Yanıt: $responseBody")

                    if (!response.isSuccessful) {
                        val errorMsg = "HTTP ${response.code}: ${response.message}"
                        Log.e(TAG, "❌ HTTP Hatası: $errorMsg")
                        onError(errorMsg)
                        return@use
                    }

                    if (responseBody.isNullOrEmpty()) {
                        Log.e(TAG, "❌ Boş yanıt")
                        onError("Boş yanıt")
                        return@use
                    }

                    try {
                        val json = JSONObject(responseBody)
                        Log.d(TAG, "✅ JSON parse başarılı")
                        onSuccess(json)
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ JSON parse hatası: ${e.message}")
                        onError("API Error: Invalid JSON response")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ İstek hatası: ${e.message}")
                onError("Network Error: ${e.message}")
            }
        }.start()
    }
}