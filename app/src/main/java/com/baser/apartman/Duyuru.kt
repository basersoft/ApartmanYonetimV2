package com.baser.apartman

data class Duyuru(
    val baslik: String,
    val icerik: String,
    val tarih: String,
    val resimUrl: String = "" // Resim URL'si için
)