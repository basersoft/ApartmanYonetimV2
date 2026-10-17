// Expense.kt
package com.baser.apartman.models

import java.util.*

data class Expense(
    val id: Int = 0,
    val title: String,
    val description: String?,
    val totalAmount: Double,
    val expenseDate: Date,
    val category: String,
    val distributionType: String,
    val status: String = "pending",
    val createdBy: Int,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

data class ExpenseCategory(
    val id: Int,
    val name: String,
    val entryType: String
)

data class ExpenseDistribution(
    val id: Int,
    val expenseId: Int,
    val userId: Int,
    val shareAmount: Double,
    val sharePercentage: Double,
    val status: String,
    val userName: String,
    val apartmentCode: String
)

data class ApiResponse(
    val success: Boolean,
    val message: String,
    val data: Any? = null
)