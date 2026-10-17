// MesajModel.kt
package com.baser.apartman

data class Message(
    val id: Int,
    val siteId: Int = 1,
    val senderId: Int,
    val receiverId: Int,
    val senderName: String,
    val receiverName: String,
    val subject: String,
    val message: String,
    val status: String, // "unread", "read", "answered", "sent"
    val parentId: Int? = null,
    val createdAt: String,
    val updatedAt: String,
    val isOwnMessage: Boolean = false,
    val isReply: Boolean = false,
    val isReminder: Boolean = false,
    val canReply: Boolean = false
)

data class MessageStats(
    val totalMessages: Int,
    val unreadMessages: Int,
    val sentMessages: Int,
    val answeredMessages: Int,
    val totalDebtors: Int = 0,
    val overdueDebtors: Int = 0,
    val pendingDebtors: Int = 0
)