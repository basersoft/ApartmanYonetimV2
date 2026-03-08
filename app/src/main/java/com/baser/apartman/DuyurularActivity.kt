package com.baser.apartman

import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class DuyurularActivity : BaseActivity() {

    private lateinit var adapter: DuyuruAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_duyurular)

        println("🔍 DUYURULAR ACTIVITY AÇILDI - Kullanıcı: $userEmail")

        setupNavigation()
        setupBottomNavigation()
        setupViews()
        loadDuyurularFromServer()
    }

    private fun setupViews() {
        recyclerView = findViewById(R.id.recyclerViewDuyurular)
        progressBar = findViewById(R.id.progressBar)

        adapter = DuyuruAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun loadDuyurularFromServer() {
        progressBar.visibility = android.view.View.VISIBLE
        println("🔍 DUYURULAR YÜKLENİYOR - Email: $userEmail")

        val params = mapOf(
            "email" to userEmail
        )

        NetworkUtils.makePostRequest(
            endpoint = "api_get_duyurular.php",
            params = params,
            onSuccess = { response ->
                progressBar.visibility = android.view.View.GONE
                handleDuyurularResponse(response)
            },
            onError = { error ->
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Duyuru yükleme hatası: $error", Toast.LENGTH_LONG).show()
                println("🔍 DUYURULAR HATA: $error")
                showSampleData()
            }
        )
    }

    private fun handleDuyurularResponse(response: String) {
        try {
            println("🔍 DUYURULAR API YANITI: $response")

            val jsonObject = JSONObject(response)
            val success = jsonObject.getBoolean("success")

            if (success) {
                val duyuruArray = jsonObject.getJSONArray("duyurular")
                val duyuruList = mutableListOf<Duyuru>()

                println("🔍 TOPLAM DUYURU: ${duyuruArray.length()}")

                var sonDuyuruBaslik = ""
                var sonDuyuruIcerik = ""
                var sonDuyuruTarih = ""
                var toplamDuyuruSayisi = duyuruArray.length()

                for (i in 0 until duyuruArray.length()) {
                    val item = duyuruArray.getJSONObject(i)

                    // SON DUYURUYU AL
                    if (i == 0) {
                        sonDuyuruBaslik = item.getString("baslik")
                        sonDuyuruIcerik = item.getString("icerik")
                        sonDuyuruTarih = item.getString("tarih")
                    }

                    // Resim URL'sini al
                    val resimUrl = item.optString("resim_url", "")
                    println("🔍 DUYURU $i - Başlık: ${item.getString("baslik")}, Resim URL: $resimUrl")

                    val duyuru = Duyuru(
                        baslik = item.getString("baslik"),
                        icerik = item.getString("icerik"),
                        tarih = item.getString("tarih"),
                        resimUrl = resimUrl
                    )
                    duyuruList.add(duyuru)
                }

                adapter.setDuyuruList(duyuruList)

                // Widget güncellemeyi geçici olarak kaldırdık
                // updateWidgetWithDuyuruData(
                //     sonBaslik = sonDuyuruBaslik,
                //     sonIcerik = sonDuyuruIcerik,
                //     sonTarih = sonDuyuruTarih,
                //     toplamDuyuruSayisi = toplamDuyuruSayisi
                // )

                if (duyuruList.isEmpty()) {
                    val message = jsonObject.optString("message", "Henüz hiç duyuru bulunmamaktadır")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "${duyuruList.size} duyuru yüklendi", Toast.LENGTH_SHORT).show()
                    println("🔍 DUYURULAR BAŞARIYLA YÜKLENDİ: ${duyuruList.size} adet")
                }

            } else {
                val message = jsonObject.getString("message")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                showSampleData()
            }

        } catch (e: Exception) {
            println("🔍 DUYURULAR İŞLEME HATASI: ${e.message}")
            Toast.makeText(this, "Duyuru verisi işleme hatası", Toast.LENGTH_LONG).show()
            showSampleData()
        }
    }

    // Widget güncelleme fonksiyonunu geçici olarak yorum satırı yapalım
    /*
    private fun updateWidgetWithDuyuruData(
        sonBaslik: String,
        sonIcerik: String,
        sonTarih: String,
        toplamDuyuruSayisi: Int
    ) {
        try {
            val sharedPreferences = getSharedPreferences("duyuru_widget_prefs", android.content.Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            editor.putString("user_email", userEmail ?: "")
            editor.putString("user_type", userType ?: "user")
            editor.putString("user_name", userName ?: "")

            editor.putString("son_duyuru_baslik", sonBaslik)
            editor.putString("son_duyuru_icerik", sonIcerik)
            editor.putString("son_duyuru_tarih", sonTarih)
            editor.putInt("toplam_duyuru_sayisi", toplamDuyuruSayisi)

            val widgetMesaj = when {
                toplamDuyuruSayisi > 0 -> {
                    val kisaBaslik = if (sonBaslik.length > 20) sonBaslik.substring(0, 20) + "..." else sonBaslik
                    "$toplamDuyuruSayisi Duyuru\n$kisaBaslik"
                }
                else -> "Duyuru Yok"
            }
            editor.putString("widget_mesaj", widgetMesaj)

            editor.apply()

            // DuyuruWidgetUtils.updateWidgets(this) // Bu satır hata veriyor

            println("🔍 DUYURU WIDGET GÜNCELLENDİ: $widgetMesaj")

        } catch (e: Exception) {
            println("🔍 DUYURU WIDGET GÜNCELLEME HATASI: ${e.message}")
        }
    }
    */

    private fun showSampleData() {
        println("🔍 ÖRNEK DUYURULAR GÖSTERİLİYOR")

        val ornekDuyurular = listOf(
            Duyuru("Yeni Aidat Sistemi", "Aidatlar artık online ödenebilir. Site yönetim panelinden ödeme yapabilirsiniz.", "15 Ocak 2024"),
            Duyuru("Asansör Bakım Çalışması", "Pazar günü 09:00-17:00 saatleri arasında asansör bakımı yapılacaktır.", "10 Ocak 2024"),
            Duyuru("Aylık Toplantı Duyurusu", "Ocak ayı genel kurul toplantısı 20 Ocak Cumartesi saat 14:00'te yapılacaktır.", "5 Ocak 2024")
        )

        adapter.setDuyuruList(ornekDuyurular)

        // Widget güncellemeyi de kaldırdık
        /*
        updateWidgetWithDuyuruData(
            sonBaslik = "Yeni Aidat Sistemi",
            sonIcerik = "Aidatlar artık online ödenebilir.",
            sonTarih = "15 Ocak 2024",
            toplamDuyuruSayisi = 3
        )
        */

        Toast.makeText(this, "Örnek veriler gösteriliyor", Toast.LENGTH_SHORT).show()
    }
}