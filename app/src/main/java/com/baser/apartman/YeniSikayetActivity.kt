package com.baser.apartman

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// AppCompatActivity yerine BaseActivity'den türetiyoruz
class YeniSikayetActivity : BaseActivity() {

    // Lateinit değişkenleri
    private lateinit var etBaslik: EditText
    private lateinit var etAciklama: EditText
    private lateinit var spinnerKategori: Spinner
    private lateinit var btnGonder: Button
    private lateinit var btnIptal: Button
    private lateinit var tvKarakterSayaci: TextView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yeni_sikayet)

        // BaseActivity'nin menüsünü kur
        setupNavigation()

        // Toolbar başlığını emoji ile ayarla
        supportActionBar?.title = "📝 Yeni Şikayet"

        // View'leri başlat
        initViews()

        // Spinner'ı kur
        setupSpinner()

        // Karakter sayacını kur
        setupCharacterCounter()

        // Dinleyicileri kur
        setupListeners()
    }

    // BaseActivity'den gelen getActivityTitle'i override et
    override fun getActivityTitle(): String {
        return "📝 Yeni Şikayet"
    }

    private fun initViews() {
        etBaslik = findViewById(R.id.etBaslik)
        etAciklama = findViewById(R.id.etAciklama)
        spinnerKategori = findViewById(R.id.spinnerKategori)
        btnGonder = findViewById(R.id.btnGonder)
        btnIptal = findViewById(R.id.btnIptal)
        tvKarakterSayaci = findViewById(R.id.tvKarakterSayaci)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        val categories = arrayOf("Bakım/Arıza", "Temizlik", "Gürültü", "Güvenlik", "Diğer")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = categoryAdapter
    }

    private fun setupCharacterCounter() {
        etAciklama.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentLength = s?.length ?: 0
                tvKarakterSayaci.text = "$currentLength/500 karakter"

                if (currentLength > 500) {
                    tvKarakterSayaci.setTextColor(resources.getColor(android.R.color.holo_red_dark))
                } else {
                    tvKarakterSayaci.setTextColor(resources.getColor(android.R.color.darker_gray))
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupListeners() {
        // Gönder butonu
        btnGonder.setOnClickListener {
            val baslik = etBaslik.text.toString().trim()
            val aciklama = etAciklama.text.toString().trim()
            val kategori = spinnerKategori.selectedItem.toString()

            if (baslik.isEmpty()) {
                Toast.makeText(this, "Lütfen başlık giriniz", Toast.LENGTH_SHORT).show()
                etBaslik.requestFocus()
                return@setOnClickListener
            }

            if (aciklama.isEmpty()) {
                Toast.makeText(this, "Lütfen açıklama giriniz", Toast.LENGTH_SHORT).show()
                etAciklama.requestFocus()
                return@setOnClickListener
            }

            if (aciklama.length > 500) {
                Toast.makeText(this, "Açıklama 500 karakterden uzun olamaz", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val kategoriKey = when (kategori) {
                "Bakım/Arıza" -> "maintenance"
                "Temizlik" -> "cleaning"
                "Gürültü" -> "noise"
                "Güvenlik" -> "security"
                else -> "other"
            }

            gonderSikayet(baslik, aciklama, kategoriKey)
        }

        // İptal butonu
        btnIptal.setOnClickListener {
            finish()
        }
    }

    private fun gonderSikayet(baslik: String, aciklama: String, kategori: String) {
        progressBar.visibility = View.VISIBLE
        btnGonder.isEnabled = false
        btnIptal.isEnabled = false

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_create_complaint.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("title=${URLEncoder.encode(baslik, "UTF-8")}")
                    append("&description=${URLEncoder.encode(aciklama, "UTF-8")}")
                    append("&category=${URLEncoder.encode(kategori, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getCurrentUserId(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 YENİ ŞİKAYET GÖNDER: $postData")

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                responseCode = conn.responseCode

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                } else {
                    val errorStream = conn.errorStream
                    result = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                btnGonder.isEnabled = true
                btnIptal.isEnabled = true
                handleGonderResult(result)
            }
        }.start()
    }

    private fun handleGonderResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Şikayetiniz başarıyla gönderildi!", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                } else {
                    val message = jsonObject.optString("message", "Gönderme başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Sunucu hatası", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası", Toast.LENGTH_SHORT).show()
        }
    }


}