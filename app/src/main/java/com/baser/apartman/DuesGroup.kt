package com.baser.apartman.models

import org.json.JSONObject

data class DuesGroup(
    var groupKey: String = "",
    var groupName: String = "",
    var userCount: Int = 0,
    var totalDues: Int = 0,
    var paidCount: Int = 0,
    var pendingCount: Int = 0,
    var overdueCount: Int = 0,
    var totalPaid: Double = 0.0,
    var totalPending: Double = 0.0,
    var totalOverdue: Double = 0.0,
    var totalLateFees: Double = 0.0,
    var userName: String = "",
    var apartmentBlock: String = "",
    var apartmentNumber: String = ""
) {

    val paidPercentage: Double
        get() = if (totalDues > 0) (paidCount.toDouble() / totalDues.toDouble()) * 100 else 0.0

    val debtStatusText: String
        get() = when {
            totalPending + totalOverdue == 0.0 -> "Borç Yok"
            totalOverdue > 0 -> "Gecikmiş Borç"
            totalPending > 0 -> "Bekleyen Borç"
            else -> "Ödenmiş"
        }

    val debtStatusColor: String
        get() = when {
            totalPending + totalOverdue == 0.0 -> "#4CAF50" // Yeşil
            totalOverdue > 0 -> "#F44336" // Kırmızı
            totalPending > 0 -> "#FF9800" // Turuncu
            else -> "#4CAF50" // Yeşil
        }

    fun formatCurrency(amount: Double): String {
        return "₺${String.format("%.2f", amount)}"
    }

    companion object {
        fun fromJson(json: JSONObject): DuesGroup {
            return DuesGroup(
                groupKey = json.optString("group_key", ""),
                groupName = json.optString("group_name", json.optString("group_key", "")),
                userCount = json.optInt("user_count", 0),
                totalDues = json.optInt("total_dues", 0),
                paidCount = json.optInt("paid_count", 0),
                pendingCount = json.optInt("pending_count", 0),
                overdueCount = json.optInt("overdue_count", 0),
                totalPaid = json.optDouble("total_paid", 0.0),
                totalPending = json.optDouble("total_pending", 0.0),
                totalOverdue = json.optDouble("total_overdue", 0.0),
                totalLateFees = json.optDouble("total_late_fees", 0.0),
                userName = json.optString("user_name", ""),
                apartmentBlock = json.optString("apartment_block", ""),
                apartmentNumber = json.optString("apartment_number", "")
            )
        }
    }
}