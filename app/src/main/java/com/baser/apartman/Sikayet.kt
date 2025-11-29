package com.baser.apartman

data class Sikayet(
    val id: Int,
    val baslik: String,
    val aciklama: String,
    val kategori: String,
    val durum: String,
    val tarih: String,
    val cevap: String? = null,
    val cevapTarihi: String? = null,

    // Admin/Manager için yeni alanlar - NULLABLE olmalı
    val kullaniciAdi: String? = null,
    val konum: String? = null,
    val kullaniciTipi: String? = null,
    val ikametTipi: String? = null,
    val telefon: String? = null,
    val kullaniciEmail: String? = null
)