package com.baser.apartman

import org.json.JSONObject
import java.text.NumberFormat
import java.util.*

data class Aidat(
    val id: Int = 0,
    val userId: Int = 0,
    val userName: String = "",
    val kullanici_email: String = "",
    val apartmentBlock: String = "",
    val apartmentNumber: String = "",
    val ay: String = "",
    val miktar: String = "",
    val amount: Double = 0.0,
    val lateFeeAmount: Double = 0.0,
    val durum: String = "",
    val durumRenk: String = "",
    val son_tarih: String = "",
    val odeme_tarihi: String? = null,
    val description: String = "",
    val paymentStatus: String = "",
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val paymentMethod: String = "",
    val kullanici_adi: String = "",
    val totalAmount: Double = 0.0,
    val receiptNumber: String = "",
    val transactionId: String = "",
    val paymentCount: Int = 0,
    val paymentDetails: String = "",
    val partialInfo: String = "",
    val paymentPercentage: Int = 0,
    val isAccounted: Boolean = false,
    val daysLate: Int = 0,
    // YENİ ALANLAR (WEB SENKRON İÇİN)
    val siteId: Int = 1,
    val hasLateFee: Boolean = false,
    val isOverdue: Boolean = false,
    val lateSeverity: String = "success",
    val userEmail: String = "",
    val apartmentType: String = "",
    val blockNumber: String = "",
    val apartmentCode: String = ""
) {
    // Computed properties - GECİKME CEZASI DAHİL
    val calculatedTotalAmount: Double get() = amount + lateFeeAmount
    val calculatedRemainingAmount: Double get() = calculatedTotalAmount - paidAmount

    // Formatlı string'ler
    val formattedTotalAmount: String get() = formatCurrency(calculatedTotalAmount)
    val formattedAmount: String get() = formatCurrency(amount)
    val formattedLateFee: String get() = formatCurrency(lateFeeAmount)
    val formattedPaidAmount: String get() = formatCurrency(paidAmount)
    val formattedRemainingAmount: String get() = formatCurrency(calculatedRemainingAmount)

    // Ödeme durumları
    val isPartiallyPaid: Boolean get() =
        paymentStatus == "partial" || (paidAmount > 0 && paidAmount < calculatedTotalAmount)

    val isFullyPaid: Boolean get() =
        paymentStatus == "full" || paidAmount >= calculatedTotalAmount

    val isReallyOverdue: Boolean get() =
        durum.lowercase() == "overdue" || daysLate > 0 || isOverdue

    val isPending: Boolean get() =
        durum.lowercase() == "pending" && !isReallyOverdue

    val realPaymentPercentage: Int
        get() = if (calculatedTotalAmount > 0) {
            ((paidAmount / calculatedTotalAmount) * 100).toInt()
        } else {
            paymentPercentage
        }

    // Site bilgisi
    val siteInfo: String get() = "Site $siteId"

    // Formatlı durum metni
    val formattedStatus: String
        get() = buildString {
            when (durum.lowercase()) {
                "paid" -> append("ÖDENDİ")
                "pending" -> append("BEKLİYOR")
                "overdue" -> append("GECİKMİŞ")
                else -> append(durum.uppercase())
            }

            if (isReallyOverdue && daysLate > 0) {
                append(" ($daysLate gün)")
            }
        }

    // Ödeme durumu detaylı
    val formattedPaymentStatus: String
        get() = buildString {
            when {
                isFullyPaid -> append("Tam Ödendi")
                isPartiallyPaid -> append("Kısmi Ödendi (%$realPaymentPercentage)")
                isReallyOverdue -> append("Gecikmiş ($daysLate gün)")
                else -> append("Ödenmedi")
            }
        }

    // Gecikme bilgisi
    val lateFeeInfo: String
        get() = if (lateFeeAmount > 0 || hasLateFee) {
            val fee = if (lateFeeAmount > 0) formattedLateFee else "Hesaplanıyor"
            val days = if (daysLate > 0) "$daysLate gün" else "Gün sayısı hesaplanıyor"
            "Gecikme Cezası: $fee ($days)"
        } else {
            ""
        }

    // Toplam borç bilgisi
    val totalDebtInfo: String
        get() = buildString {
            append("Orijinal: ${formatCurrency(amount)}")
            if (lateFeeAmount > 0 || hasLateFee) {
                val fee = if (lateFeeAmount > 0) formattedLateFee else "Hesaplanacak"
                append(" + Gecikme: $fee")
            }
            append(" = Toplam: ${formatCurrency(calculatedTotalAmount)}")
        }

    // Ödeme detayı
    val paymentDetail: String
        get() = buildString {
            append("💰 Toplam Borç: $formattedTotalAmount\n")
            append("📋 Orijinal: $formattedAmount\n")

            if (siteId > 1) {
                append("🏢 Site: $siteId\n")
            }

            if (lateFeeAmount > 0 || hasLateFee) {
                val fee = if (lateFeeAmount > 0) formattedLateFee else "Hesaplanacak"
                val days = if (daysLate > 0) "$daysLate gün" else "Gün sayısı hesaplanıyor"
                append("⚠️ Gecikme Cezası: $fee ($days)\n")
            }

            if (paidAmount > 0) {
                append("✅ Ödenen: $formattedPaidAmount\n")
                append("📉 Kalan Borç: $formattedRemainingAmount\n")
                append("📊 Ödeme Oranı: %$realPaymentPercentage")
            }
        }

    // Kısmi ödeme durumu
    val partialPaymentInfo: String
        get() = if (isPartiallyPaid) {
            "Kısmi Ödeme: $formattedPaidAmount / $formattedTotalAmount (%$realPaymentPercentage)"
        } else {
            partialInfo
        }

    // Muhasebe durumu
    val accountingInfo: String
        get() = if (isAccounted) "✓ Muhasebeye Aktarıldı" else "✗ Muhasebe Bekliyor"

    // Daire bilgisi
    val apartmentFullInfo: String
        get() = buildString {
            if (apartmentCode.isNotEmpty()) {
                append(apartmentCode)
            } else {
                if (blockNumber.isNotEmpty()) append("Blok $blockNumber ")
                if (apartmentNumber.isNotEmpty()) append("Daire $apartmentNumber")
                if (apartmentType.isNotEmpty()) append(" ($apartmentType)")
            }
        }

    // Yardımcı fonksiyon
    private fun formatCurrency(amount: Double): String {
        return try {
            val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
            numberFormat.format(amount)
        } catch (e: Exception) {
            "₺${"%.2f".format(amount)}"
        }
    }

    companion object {
        fun fromApiJson(json: JSONObject): Aidat {
            // PHP API'den gelen JSON yapısına göre alanları doğru al
            val amount = json.optDouble("amount", 0.0)
            val lateFeeAmount = json.optDouble("late_fee_amount", 0.0)
            val paidAmount = json.optDouble("paid_amount", 0.0)

            // PHP'den gelen değerleri kullan
            val totalFromJson = json.optDouble("total_amount", amount + lateFeeAmount)
            val remainingFromJson = json.optDouble("remaining_amount", totalFromJson - paidAmount)

            // PHP'den gelen 0/1 değerlerini doğru parse et
            val hasLateFeeInt = json.optInt("has_late_fee", 0)
            val isOverdueInt = json.optInt("is_overdue", 0)

            // Days late kontrolü
            val daysLate = json.optInt("days_late", 0)

            // Site ID
            val siteId = json.optInt("site_id", 1)

            // Status ve payment status
            val status = json.optString("status", "pending")
            val paymentStatusFromJson = json.optString("payment_status", "none")

            // Ödeme durumunu belirle
            val paymentStatus = when {
                paymentStatusFromJson.isNotEmpty() -> paymentStatusFromJson
                paidAmount >= totalFromJson -> "full"
                paidAmount > 0 -> "partial"
                else -> "none"
            }

            // Muhasebe durumu (PHP'de is_accounted 0/1 geliyor)
            val isAccountedInt = json.optInt("is_accounted", 0)

            return Aidat(
                id = json.optInt("id", 0),
                userId = json.optInt("user_id", 0),
                userName = json.optString("user_name", ""),
                kullanici_email = json.optString("user_email", ""),
                userEmail = json.optString("user_email", ""),
                apartmentBlock = json.optString("apartment_block", ""),
                apartmentNumber = json.optString("apartment_number", ""),
                apartmentCode = json.optString("apartment_code", ""),
                apartmentType = json.optString("apartment_type", ""),
                blockNumber = json.optString("apartment_block", ""),
                ay = json.optString("description", ""),
                miktar = "₺${"%.2f".format(amount)}",
                amount = amount,
                lateFeeAmount = lateFeeAmount,
                durum = status,
                durumRenk = getStatusColor(status, daysLate, isOverdueInt == 1),
                son_tarih = json.optString("due_date", ""),
                odeme_tarihi = if (json.has("payment_date") && !json.isNull("payment_date"))
                    json.optString("payment_date") else null,
                description = json.optString("description", ""),
                paymentStatus = paymentStatus,
                paidAmount = paidAmount,
                remainingAmount = remainingFromJson,
                paymentMethod = json.optString("payment_method", ""),
                kullanici_adi = json.optString("user_name", ""),
                totalAmount = totalFromJson,
                receiptNumber = json.optString("receipt_number", ""),
                transactionId = json.optString("transaction_id", ""),
                paymentCount = 0, // Bu bilgi JSON'dan gelmiyor
                paymentDetails = "",
                partialInfo = "",
                paymentPercentage = if (totalFromJson > 0) ((paidAmount / totalFromJson) * 100).toInt() else 0,
                isAccounted = isAccountedInt == 1,
                daysLate = daysLate,
                siteId = siteId,
                hasLateFee = hasLateFeeInt == 1,
                isOverdue = isOverdueInt == 1,
                lateSeverity = when {
                    daysLate > 30 -> "danger"
                    daysLate > 7 -> "warning"
                    else -> "success"
                }
            )
        }

        private fun getStatusColor(status: String, daysLate: Int, isOverdue: Boolean): String {
            return when {
                status.lowercase() == "paid" -> "#2ecc71"
                status.lowercase() == "overdue" || daysLate > 0 || isOverdue -> "#e74c3c"
                status.lowercase() == "pending" -> "#f39c12"
                else -> "#95a5a6"
            }
        }

        // Eski fonksiyon - geriye uyumluluk için
        fun fromJson(json: JSONObject): Aidat {
            return fromApiJson(json)
        }
    }
}