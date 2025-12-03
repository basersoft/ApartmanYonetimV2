package com.baser.apartman

data class Aidat(
    // API'den gelen alanlar
    val ay: String = "",
    val miktar: String = "",
    val durum: String = "",
    val durumRenk: String = "",
    val son_tarih: String = "",
    val odeme_tarihi: String? = null,
    val kullanici_adi: String = "",
    val kullanici_email: String = "",
    val id: Int = 0,
    val userId: Int = 0,
    val userName: String = "",
    val apartmentBlock: String = "",
    val apartmentNumber: String = "",
    val description: String = "",
    val lateFeeAmount: Double = 0.0,
    val amount: Double = 0.0
) {
    // Basit fonksiyonlar
    fun getTotalAmount(): Double = amount + lateFeeAmount

    fun isPaid(): Boolean = durum.lowercase() == "paid"
    fun isPending(): Boolean = durum.lowercase() == "pending"
    fun isOverdue(): Boolean = durum.lowercase() == "overdue"
}