package com.baser.apartman

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import com.baser.apartman.widgets.AidatWidgetUtils
import com.baser.apartman.workers.WidgetUpdateWorker
class AidatActivity : BaseActivity() {

    private lateinit var adapter: AidatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // WIDGET'DAN GELEN EMAIL BİLGİSİNİ KONTROL ET
        val widgetEmail = intent.getStringExtra("user_email")
        val fromWidget = intent.getBooleanExtra("from_widget", false)

        if (fromWidget && !widgetEmail.isNullOrEmpty()) {
            println("🔍 WIDGET'DAN GELEN EMAIL: $widgetEmail")
            userEmail = widgetEmail
            // Diğer bilgileri de güncelle
            userType = intent.getStringExtra("user_type") ?: userType
            userName = intent.getStringExtra("user_name") ?: userName
        }
        // ÖNEMLİ: Admin veya manager ise AdminAidatActivity'ye yönlendir
        if (userType == "admin" || userType == "manager") {
            val intent = Intent(this, AdminAidatActivity::class.java).apply {
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
            }
            startActivity(intent)
            finish()
            return
        }

        // Normal kullanıcı ise mevcut akışa devam et
        setContentView(R.layout.activity_aidat)

        // ÖNCE VIEW'LERİ BUL, SONRA NAVIGATION
        if (!setupViews()) {
            Toast.makeText(this, "Layout hatası! Sayfa kapatılıyor.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupNavigation()
        setupBottomNavigation()
        loadAidatFromServer()
    }

    private fun setupViews(): Boolean {
        return try {
            recyclerView = findViewById(R.id.recyclerViewAidat)
            progressBar = findViewById(R.id.progressBar)

            adapter = AidatAdapter()
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
            true
        } catch (e: Exception) {
            println("🔍 VIEW SETUP HATASI: ${e.message}")
            false
        }
    }

    // Aidat ödendiğinde
    private fun onAidatOdendi() {
        // Widget'ı güncelle
        AidatWidgetUtils.updateWidgets(this)
        Toast.makeText(this, "Aidat ödendi ve widget güncellendi", Toast.LENGTH_SHORT).show()
    }

    // Yeni aidat eklendiğinde
    private fun onYeniAidatEklendi() {
        // Widget'ı güncelle
        AidatWidgetUtils.onNewAidatAdded(this)
        Toast.makeText(this, "Yeni aidat eklendi ve widget güncellendi", Toast.LENGTH_SHORT).show()
    }

    private fun loadAidatFromServer() {
        progressBar.visibility = android.view.View.VISIBLE

        val params = mapOf(
            "email" to userEmail
        )

        NetworkUtils.makePostRequest(
            endpoint = "api/aidat_api.php",
            params = params,
            onSuccess = { response ->
                progressBar.visibility = android.view.View.GONE
                handleAidatResponse(response)
            },
            onError = { error ->
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Aidat yükleme hatası: $error", Toast.LENGTH_LONG).show()
                showSampleData()
            }
        )
    }

    private fun handleAidatResponse(response: String) {
        try {
            println("🔍 AIDAT API YANITI: $response")

            val jsonObject = JSONObject(response)
            val success = jsonObject.getBoolean("success")

            if (success) {
                val aidatArray = jsonObject.getJSONArray("aidatlar")
                val aidatList = mutableListOf<Aidat>()

                // WIDGET İÇİN DETAYLI BİLGİLER
                var sonAidatDurum = "Bekleniyor"
                var gecikmisAidatSayisi = 0
                var toplamBorc = 0.0
                var toplamAidatSayisi = aidatArray.length()
                var odenecekAidatSayisi = 0
                var enYakinSonTarih = ""
                var sonAidatAy = ""

                for (i in 0 until aidatArray.length()) {
                    val item = aidatArray.getJSONObject(i)
                    val durum = item.getString("durum")
                    val miktarStr = item.getString("miktar").replace("₺", "").replace(",", ".")
                    val miktar = miktarStr.toDoubleOrNull() ?: 0.0

                    // SON AIDAT BİLGİLERİ
                    if (i == aidatArray.length() - 1) {
                        sonAidatDurum = durum
                        sonAidatAy = item.getString("ay")
                    }

                    // GECİKMİŞ AIDATLARI SAY VE BORÇ HESAPLA
                    if (durum == "GECİKMİŞ") {
                        gecikmisAidatSayisi++
                        toplamBorc += miktar
                    }

                    // ÖDENMEMİŞ AIDATLARI SAY (Gecikmiş + Bekliyor)
                    if (durum == "GECİKMİŞ" || durum == "BEKLİYOR") {
                        odenecekAidatSayisi++
                    }

                    // EN YAKIN SON TARİHİ BUL
                    val sonTarih = if (item.has("son_tarih") && !item.isNull("son_tarih") && item.getString("son_tarih").isNotEmpty())
                        item.getString("son_tarih") else ""

                    if (sonTarih.isNotEmpty() && (durum == "BEKLİYOR" || durum == "GECİKMİŞ")) {
                        if (enYakinSonTarih.isEmpty() || sonTarih < enYakinSonTarih) {
                            enYakinSonTarih = sonTarih
                        }
                    }

                    val aidat = Aidat(
                        ay = item.getString("ay"),
                        miktar = item.getString("miktar"),
                        durum = durum,
                        durumRenk = item.getString("durumRenk"),
                        son_tarih = sonTarih,
                        odeme_tarihi = if (item.has("odeme_tarihi") && !item.isNull("odeme_tarihi") && item.getString("odeme_tarihi").isNotEmpty())
                            item.getString("odeme_tarihi") else null,
                        kullanici_adi = item.optString("kullanici_adi", ""),
                        kullanici_email = item.optString("kullanici_email", ""),
                        id = item.optInt("id", 0),
                        userId = item.optInt("user_id", 0),
                        userName = item.optString("user_name", ""),
                        apartmentBlock = item.optString("apartment_block", ""),
                        apartmentNumber = item.optString("apartment_number", ""),
                        description = item.optString("description", ""),
                        lateFeeAmount = item.optDouble("late_fee_amount", 0.0),
                        amount = item.optDouble("amount", 0.0)
                    )
                    aidatList.add(aidat)
                }

                adapter.setAidatList(aidatList)

                // WIDGET'I DETAYLI BİLGİLERLE GÜNCELLE
                updateWidgetWithDetailedData(
                    sonDurum = sonAidatDurum,
                    gecikmisSayi = gecikmisAidatSayisi,
                    toplamBorc = toplamBorc,
                    toplamAidatSayisi = toplamAidatSayisi,
                    odenecekAidatSayisi = odenecekAidatSayisi,
                    enYakinSonTarih = enYakinSonTarih,
                    sonAy = sonAidatAy
                )

                if (aidatList.isEmpty()) {
                    val message = jsonObject.optString("message", "Aidat kaydı bulunamadı")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "${aidatList.size} aidat kaydı yüklendi", Toast.LENGTH_SHORT).show()
                }

            } else {
                val message = jsonObject.getString("message")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                showSampleData()
            }

        } catch (e: Exception) {
            println("🔍 AIDAT İŞLEME HATASI: ${e.message}")
            Toast.makeText(this, "Aidat verisi işleme hatası", Toast.LENGTH_LONG).show()
            showSampleData()
        }
    }

    // DETAYLI WIDGET GÜNCELLEME FONKSİYONU
    private fun updateWidgetWithDetailedData(
        sonDurum: String,
        gecikmisSayi: Int,
        toplamBorc: Double,
        toplamAidatSayisi: Int,
        odenecekAidatSayisi: Int,
        enYakinSonTarih: String,
        sonAy: String
    ) {
        try {
            val sharedPreferences = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // Kullanıcı bilgilerini kaydet
            editor.putString("user_email", userEmail ?: "")
            editor.putString("user_type", userType ?: "user")
            editor.putString("user_name", userName ?: "")

            // Detaylı aidat bilgilerini kaydet
            editor.putString("aidat_durum", sonDurum)
            editor.putString("son_ay", sonAy)
            editor.putFloat("toplam_borc", toplamBorc.toFloat())
            editor.putInt("gecikmis_sayi", gecikmisSayi)
            editor.putInt("toplam_aidat_sayisi", toplamAidatSayisi)
            editor.putInt("odenecek_aidat_sayisi", odenecekAidatSayisi)
            editor.putString("en_yakin_son_tarih", enYakinSonTarih)

            // Widget mesajını oluştur
            val widgetMesaj = when {
                gecikmisSayi > 0 -> "Gecikmiş: $gecikmisSayi\nBorç: ₺${"%.2f".format(toplamBorc)}"
                odenecekAidatSayisi > 0 -> "Ödenecek: $odenecekAidatSayisi\nSon Tarih: ${formatTarih(enYakinSonTarih)}"
                else -> "Güncel\n$sonAy"
            }
            editor.putString("widget_mesaj", widgetMesaj)

            editor.apply()

            // Widget'ı güncelle
            AidatWidgetUtils.updateWidgets(this)

            println("🔍 DETAYLI WIDGET GÜNCELLENDİ: $widgetMesaj")
            println("🔍 KAYITLI EMAIL: ${sharedPreferences.getString("user_email", "BULUNAMADI")}")

        } catch (e: Exception) {
            println("🔍 WIDGET GÜNCELLEME HATASI: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun formatTarih(tarih: String): String {
        return if (tarih.length >= 10) {
            tarih.substring(8, 10) + "." + tarih.substring(5, 7) + "." + tarih.substring(0, 4)
        } else {
            tarih
        }
    }

    private fun showSampleData() {
        val ornekAidatlar = listOf(
            Aidat(
                ay = "Ocak 2024",
                miktar = "₺500,00",
                durum = "ÖDENDİ",
                durumRenk = "#2ecc71",
                son_tarih = "2024-01-25",
                odeme_tarihi = "2024-01-20"
            ),
            Aidat(
                ay = "Şubat 2024",
                miktar = "₺500,00",
                durum = "BEKLİYOR",
                durumRenk = "#f39c12",
                son_tarih = "2024-02-25",
                odeme_tarihi = null
            ),
            Aidat(
                ay = "Mart 2024",
                miktar = "₺550,00",
                durum = "GECİKMİŞ",
                durumRenk = "#e74c3c",
                son_tarih = "2024-03-25",
                odeme_tarihi = null
            )
        )
        adapter.setAidatList(ornekAidatlar)
        Toast.makeText(this, "Örnek veriler gösteriliyor", Toast.LENGTH_SHORT).show()
    }
}