package com.baser.apartman

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class PaymentHistory(
    val id: Int,
    val dueId: Int,
    val dueDescription: String,
    val userName: String,
    val paymentAmount: Double,
    val paymentDate: String,
    val paymentMethod: String,
    val receiptNumber: String,
    val transactionId: String,
    val notes: String,
    val isPartial: Boolean
) {
    val formattedPaymentAmount: String
        get() = String.format("₺%.2f", paymentAmount)
    
    val formattedPaymentDate: String
        get() = try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(paymentDate)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            paymentDate
        }
    
    val paymentTypeText: String
        get() = if (isPartial) "Kısmi Ödeme" else "Tam Ödeme"
    
    companion object {
        fun fromJson(json: JSONObject): PaymentHistory {
            return PaymentHistory(
                id = json.optInt("id", 0),
                dueId = json.optInt("due_id", 0),
                dueDescription = json.optString("due_description", ""),
                userName = json.optString("user_name", ""),
                paymentAmount = json.optDouble("payment_amount", 0.0),
                paymentDate = json.optString("payment_date", ""),
                paymentMethod = json.optString("payment_method", ""),
                receiptNumber = json.optString("receipt_number", ""),
                transactionId = json.optString("transaction_id", ""),
                notes = json.optString("notes", ""),
                isPartial = json.optInt("is_partial", 0) == 1
            )
        }
        
        fun createListFromJson(jsonArray: org.json.JSONArray): List<PaymentHistory> {
            val list = mutableListOf<PaymentHistory>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(fromJson(item))
            }
            return list
        }
    }
}