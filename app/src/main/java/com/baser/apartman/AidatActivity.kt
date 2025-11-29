package com.baser.apartman

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject

class AidatActivity : BaseActivity() {

    private lateinit var adapter: AidatAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                for (i in 0 until aidatArray.length()) {
                    val item = aidatArray.getJSONObject(i)
                    val aidat = Aidat(
                        ay = item.getString("ay"),
                        miktar = item.getString("miktar"),
                        durum = item.getString("durum"),
                        durumRenk = item.getString("durumRenk"),
                        // DÜZELTME: null veya boş gelirse boş string kullan
                        son_tarih = if (item.has("son_tarih") && !item.isNull("son_tarih") && item.getString("son_tarih").isNotEmpty())
                            item.getString("son_tarih") else "",
                        odeme_tarihi = if (item.has("odeme_tarihi") && !item.isNull("odeme_tarihi") && item.getString("odeme_tarihi").isNotEmpty())
                            item.getString("odeme_tarihi") else null,
                        // Diğer alanlar için varsayılan değerler
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