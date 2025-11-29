package com.baser.apartman

// Yeni istatistik modeli
data class AidatIstatistik(
    val totalAmount: Double,
    val paidAmount: Double,
    val pendingAmount: Double,
    val overdueAmount: Double,
    val lateFeeTotal: Double,
    val totalUsers: Int,
    val totalDues: Int
)