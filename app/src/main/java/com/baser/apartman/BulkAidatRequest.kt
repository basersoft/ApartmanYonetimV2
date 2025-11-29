package com.baser.apartman

// Toplu aidat modeli
data class BulkAidatRequest(
    val bulkType: String, // "monthly" veya "yearly"
    val bulkAmount: Double,
    val bulkDueDate: String,
    val bulkDescription: String,
    val excludePaid: Boolean = true
)