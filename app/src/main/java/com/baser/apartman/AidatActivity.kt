package com.baser.apartman

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import com.baser.apartman.widgets.AidatWidgetUtils

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

        if (!setupViews()) {
            Toast.makeText(this, "Layout hatası! Sayfa kapatılıyor.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupNavigation()
        setupBottomNavigation()

        // YENİ: API'den aidatları yükle
        loadAidatFromApi()
    }

    private fun setupViews(): Boolean {
        return try {
            recyclerView = findViewById(R.id.recyclerViewAidat)
            progressBar = findViewById(R.id.progressBar)

            adapter = AidatAdapter(onOdemeClick = { aidat ->
                onOdemeYapClick(aidat)
            })
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter
            true
        } catch (e: Exception) {
            println("🔍 VIEW SETUP HATASI: ${e.message}")
            false
        }
    }

    // YENİ: API'den aidat yükleme
    // YENİ: API'den aidat yükleme
    private fun loadAidatFromApi() {
        progressBar.visibility = android.view.View.VISIBLE

        AidatApiService.getAndroidDues(
            userEmail = userEmail ?: "",
            userType = userType ?: "resident",
            onSuccess = { aidatList ->
                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE

                    // API'den gelen listeyi adapter'a ver
                    adapter.setAidatList(aidatList)

                    // Widget güncelle
                    updateWidgetWithApiData(aidatList)

                    if (aidatList.isEmpty()) {
                        Toast.makeText(this, "Aidat kaydı bulunamadı", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "${aidatList.size} aidat kaydı yüklendi", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Aidat yükleme hatası: $error", Toast.LENGTH_LONG).show()

                    // API başarısız olursa eski yönteme dön
                    loadAidatFromServer()
                }
            }
        )
    }

    // ESKİ: Eski API'den yükleme (geriye uyumluluk)
    // ESKİ: Eski API'den yükleme (geriye uyumluluk)
    private fun loadAidatFromServer() {
        progressBar.visibility = android.view.View.VISIBLE

        val params = mapOf(
            "email" to userEmail
        )

        NetworkUtils.makePostRequest(
            endpoint = "api/aidat_api.php",
            params = params,
            onSuccess = { response ->
                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                    handleAidatResponse(response)
                }
            },
            onError = { error ->
                runOnUiThread {
                    progressBar.visibility = android.view.View.GONE
                    Toast.makeText(this, "Aidat yükleme hatası: $error", Toast.LENGTH_LONG).show()
                    showSampleData()
                }
            }
        )
    }

    // YENİ: Widget güncelleme fonksiyonu (API verisiyle)
    private fun updateWidgetWithApiData(aidatList: List<Aidat>) {
        if (aidatList.isEmpty()) return

        try {
            // İstatistikleri hesapla
            var gecikmisAidatSayisi = 0
            var toplamBorc = 0.0
            var odenecekAidatSayisi = 0
            var enYakinSonTarih = ""
            var sonAidatAy = aidatList.lastOrNull()?.description ?: ""
            var sonAidatDurum = aidatList.lastOrNull()?.durum ?: "Bekleniyor"

            for (aidat in aidatList) {
                // Gecikmiş aidatları say
                if (aidat.isReallyOverdue || aidat.durum.lowercase() == "overdue") {
                    gecikmisAidatSayisi++
                    toplamBorc += aidat.calculatedTotalAmount - aidat.paidAmount
                }

                // Ödenmemiş aidatları say
                if (!aidat.isFullyPaid) {
                    odenecekAidatSayisi++

                    // En yakın son tarihi bul
                    if (aidat.son_tarih.isNotEmpty() &&
                        (enYakinSonTarih.isEmpty() || aidat.son_tarih < enYakinSonTarih)) {
                        enYakinSonTarih = aidat.son_tarih
                    }
                }
            }

            // Widget mesajını oluştur
            val widgetMesaj = when {
                gecikmisAidatSayisi > 0 -> "Gecikmiş: $gecikmisAidatSayisi\nBorç: ₺${"%.2f".format(toplamBorc)}"
                odenecekAidatSayisi > 0 -> "Ödenecek: $odenecekAidatSayisi\nSon Tarih: ${formatTarih(enYakinSonTarih)}"
                else -> "Güncel\n$sonAidatAy"
            }

            // SharedPreferences'a kaydet
            val sharedPreferences = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            editor.putString("user_email", userEmail ?: "")
            editor.putString("user_type", userType ?: "user")
            editor.putString("user_name", userName ?: "")
            editor.putString("aidat_durum", sonAidatDurum)
            editor.putString("son_ay", sonAidatAy)
            editor.putFloat("toplam_borc", toplamBorc.toFloat())
            editor.putInt("gecikmis_sayi", gecikmisAidatSayisi)
            editor.putInt("toplam_aidat_sayisi", aidatList.size)
            editor.putInt("odenecek_aidat_sayisi", odenecekAidatSayisi)
            editor.putString("en_yakin_son_tarih", enYakinSonTarih)
            editor.putString("widget_mesaj", widgetMesaj)

            editor.apply()

            // Widget'ı güncelle
            AidatWidgetUtils.updateWidgets(this)

            println("🔍 API'DEN WIDGET GÜNCELLENDİ: $widgetMesaj")

        } catch (e: Exception) {
            println("🔍 WIDGET GÜNCELLEME HATASI: ${e.message}")
        }
    }

    // ESKİ: Eski API yanıtını işleme
    // ESKİ: Eski API yanıtını işleme
    private fun handleAidatResponse(response: String) {
        runOnUiThread {
            try {
                println("🔍 AIDAT API YANITI: $response")
                val jsonObject = JSONObject(response)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    val aidatArray = jsonObject.getJSONArray("aidatlar")
                    val aidatList = mutableListOf<Aidat>()

                    for (i in 0 until aidatArray.length()) {
                        val item = aidatArray.getJSONObject(i)
                        val aidat = Aidat.fromJson(item)
                        aidatList.add(aidat)
                    }

                    adapter.setAidatList(aidatList)

                    // Widget güncelle
                    updateWidgetWithApiData(aidatList)

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

    // YENİ: Ödeme yap butonu tıklaması
    // YENİ: Ödeme yap butonu tıklaması
    fun onOdemeYapClick(aidat: Aidat) {
        // SADECE ADMIN/MANAGER için ödeme dialog'u göster
        if (userType == "admin" || userType == "manager") {
            // Ödeme dialogunu göster
            showOdemeDialog(aidat)
        } else {
            // Sakinler için mesaj göster
            Toast.makeText(this, "Ödeme işlemleri için yönetici ile iletişime geçin.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showOdemeDialog(aidat: Aidat) {
        val intent = Intent(this, OdemeDialogActivity::class.java)
        intent.putExtra("due_id", aidat.id)
        intent.putExtra("user_email", userEmail)
        intent.putExtra("user_type", userType)
        intent.putExtra("user_name", userName)
        intent.putExtra("total_amount", aidat.calculatedTotalAmount)
        intent.putExtra("remaining_amount", aidat.calculatedRemainingAmount)
        intent.putExtra("description", aidat.description)
        intent.putExtra("site_id", aidat.siteId)
        startActivity(intent)
    }
}