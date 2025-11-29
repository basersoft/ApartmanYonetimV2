package com.baser.apartman

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class SikayetlerActivity : BaseActivity() {

    private lateinit var adapter: SikayetAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var isAdminOrManager: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sikayetler)

        println("🔍 SİKAYETLER ACTIVITY AÇILDI - Kullanıcı: $userEmail, Tip: $userType")

        // ÖNCE VIEW'LERİ KONTROL ET
        if (!setupViews()) {
            Toast.makeText(this, "Layout hatası! Lütfen tekrar deneyin.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupNavigation()
        setupBottomNavigation()
        setupFloatingActionButton() // BU SATIR EKLENDİ
        loadSikayetlerFromServer()
    }

    private fun setupViews(): Boolean {
        return try {
            recyclerView = findViewById(R.id.recyclerViewSikayetler)
            progressBar = findViewById(R.id.progressBar)

            // Admin/Manager kontrolü
            isAdminOrManager = userType == "admin" || userType == "manager"
            println("🔍 KULLANICI TİPİ: $userType, Admin/Manager: $isAdminOrManager")

            adapter = SikayetAdapter(isAdmin = isAdminOrManager)
            recyclerView.layoutManager = LinearLayoutManager(this)
            recyclerView.adapter = adapter

            true
        } catch (e: Exception) {
            println("🔍 SIKAYETLER VIEW HATASI: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    private fun setupFloatingActionButton() {
        try {
            val fab = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabYeniSikayet)

            println("🔍 FAB BULUNDU MU?: ${fab != null}")

            fab.setOnClickListener {
                println("🔍 FAB TIKLANDI! Yeni şikayet sayfasına gidiliyor...")
                startActivity(Intent(this, YeniSikayetActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }

        } catch (e: Exception) {
            println("🔍 FAB HATASI: ${e.message}")
            // FAB yoksa normal buton kullan
            setupNormalButton()
        }
    }

    private fun setupNormalButton() {
        try {
            // FAB yoksa normal buton oluştur
            val button = android.widget.Button(this).apply {
                text = "+ Yeni Şikayet"
                setBackgroundColor(android.graphics.Color.parseColor("#2196F3"))
                setTextColor(android.graphics.Color.WHITE)
                setPadding(32, 16, 32, 16)

                layoutParams = android.widget.RelativeLayout.LayoutParams(
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT,
                    android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM)
                    addRule(android.widget.RelativeLayout.ALIGN_PARENT_END)
                    setMargins(0, 0, 16, 16)
                }
            }

            // Layout'a butonu ekle
            val rootLayout = findViewById<android.widget.RelativeLayout>(android.R.id.content)
            rootLayout?.addView(button)

            button.setOnClickListener {
                println("🔍 NORMAL BUTON TIKLANDI!")
                startActivity(Intent(this@SikayetlerActivity, YeniSikayetActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }

        } catch (e: Exception) {
            println("🔍 BUTON OLUŞTURMA HATASI: ${e.message}")
        }
    }

    private fun loadSikayetlerFromServer() {
        progressBar.visibility = android.view.View.VISIBLE
        println("🔍 SIKAYETLER YÜKLENİYOR - Email: $userEmail")

        val params = mapOf(
            "email" to userEmail
        )

        NetworkUtils.makePostRequest(
            endpoint = "api_get_sikayetler.php",
            params = params,
            onSuccess = { response ->
                progressBar.visibility = android.view.View.GONE
                handleSikayetlerResponse(response)
            },
            onError = { error ->
                progressBar.visibility = android.view.View.GONE
                Toast.makeText(this, "Şikayet yükleme hatası: $error", Toast.LENGTH_LONG).show()
                println("🔍 SIKAYETLER HATA: $error")
                showSampleData()
            }
        )
    }

    private fun handleSikayetlerResponse(response: String) {
        try {
            println("🔍 SIKAYETLER API YANITI: $response")

            val jsonObject = JSONObject(response)
            val success = jsonObject.getBoolean("success")

            if (success) {
                val sikayetArray = jsonObject.getJSONArray("sikayetler")
                val sikayetList = mutableListOf<Sikayet>()

                println("🔍 TOPLAM SIKAYET: ${sikayetArray.length()}")

                for (i in 0 until sikayetArray.length()) {
                    val item = sikayetArray.getJSONObject(i)
                    val sikayet = Sikayet(
                        id = item.getInt("id"),
                        baslik = item.getString("baslik"),
                        aciklama = item.getString("aciklama"),
                        kategori = item.getString("kategori"),
                        durum = item.getString("durum"),
                        tarih = item.getString("tarih"),
                        cevap = if (item.has("cevap") && !item.isNull("cevap")) item.getString("cevap") else null,
                        cevapTarihi = if (item.has("cevap_tarihi") && !item.isNull("cevap_tarihi")) item.getString("cevap_tarihi") else null,

                        // Admin/Manager için ek alanlar
                        kullaniciAdi = if (item.has("kullanici_adi") && !item.isNull("kullanici_adi")) item.getString("kullanici_adi") else null,
                        konum = if (item.has("konum") && !item.isNull("konum")) item.getString("konum") else null,
                        kullaniciTipi = if (item.has("kullanici_tipi") && !item.isNull("kullanici_tipi")) item.getString("kullanici_tipi") else null,
                        ikametTipi = if (item.has("ikamet_tipi") && !item.isNull("ikamet_tipi")) item.getString("ikamet_tipi") else null,
                        telefon = if (item.has("telefon") && !item.isNull("telefon")) item.getString("telefon") else null,
                        kullaniciEmail = if (item.has("kullanici_email") && !item.isNull("kullanici_email")) item.getString("kullanici_email") else null
                    )
                    sikayetList.add(sikayet)
                    println("🔍 SIKAYET EKLENDİ: ${sikayet.baslik}")
                }

                adapter.setSikayetList(sikayetList)

                if (sikayetList.isEmpty()) {
                    val message = jsonObject.optString("message",
                        if (isAdminOrManager) "Henüz hiç şikayet bulunmamaktadır"
                        else "Henüz şikayetiniz bulunmamaktadır"
                    )
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                } else {
                    val message = if (isAdminOrManager) {
                        "${sikayetList.size} şikayet yüklendi"
                    } else {
                        "${sikayetList.size} şikayetiniz yüklendi"
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    println("🔍 SIKAYETLER BAŞARIYLA YÜKLENDİ: ${sikayetList.size} adet")
                }

            } else {
                val message = jsonObject.getString("message")
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                showSampleData()
            }

        } catch (e: Exception) {
            println("🔍 SIKAYETLER İŞLEME HATASI: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Şikayet verisi işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            showSampleData()
        }
    }

    private fun showSampleData() {
        println("🔍 ÖRNEK VERİLER GÖSTERİLİYOR")

        // Örnek veriler
        val ornekSikayetler = if (isAdminOrManager) {
            listOf(
                Sikayet(
                    id = 1,
                    baslik = "Asansör Arızası",
                    aciklama = "B binası asansörü çalışmıyor",
                    kategori = "Bakım",
                    durum = "İşlemde",
                    tarih = "24.11.2024",
                    cevap = "Teknik ekip bilgilendirildi, yarın bakım yapılacak",
                    kullaniciAdi = "Ahmet Yılmaz",
                    konum = "B Blok - 205 No'lu Daire"
                ),
                Sikayet(
                    id = 2,
                    baslik = "Çöp Alanı Temizliği",
                    aciklama = "Çöp konteynerleri taşıyor",
                    kategori = "Temizlik",
                    durum = "Çözüldü",
                    tarih = "23.11.2024",
                    cevap = "Temizlik ekibi müdahale etti, sorun çözüldü",
                    kullaniciAdi = "Ayşe Demir",
                    konum = "A Blok - 101 No'lu Daire"
                )
            )
        } else {
            listOf(
                Sikayet(
                    id = 1,
                    baslik = "Üst Kat Gürültüsü",
                    aciklama = "Üst kattan sürekli yüksek ses geliyor",
                    kategori = "Gürültü",
                    durum = "Beklemede",
                    tarih = "24.11.2024",
                    cevap = null
                ),
                Sikayet(
                    id = 2,
                    baslik = "Koridor Aydınlatması",
                    aciklama = "3. kattaki koridor lambası bozuk",
                    kategori = "Bakım",
                    durum = "Çözüldü",
                    tarih = "22.11.2024",
                    cevap = "Elektrikçi tamir etti, sorun çözüldü"
                )
            )
        }

        adapter.setSikayetList(ornekSikayetler)
        Toast.makeText(this, "Örnek veriler gösteriliyor", Toast.LENGTH_SHORT).show()
    }
}