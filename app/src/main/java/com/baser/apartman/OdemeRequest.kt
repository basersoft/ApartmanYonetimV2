package com.baser.apartman

// Ödeme modeli
data class OdemeRequest(
    val dueId: Int,
    val paymentAmount: Double,
    val paymentMethod: String,
    val notes: String = ""
)