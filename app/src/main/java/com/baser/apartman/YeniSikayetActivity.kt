package com.baser.apartman

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

class YeniSikayetActivity : BaseActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_yeni_sikayet)

        setupNavigation()
        setupViews()
    }

    override fun setupNavigation() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        if (toolbar == null || drawerLayout == null) {
            println("❌ Toolbar veya DrawerLayout bulunamadı")
            return
        }

        // Toolbar'ı ayarla
        setSupportActionBar(toolbar)

        // HAMBURGER İKONU İÇİN ÖNEMLİ: Display options'ı sıfırla
        supportActionBar?.setDisplayHomeAsUpEnabled(false)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // ActionBarDrawerToggle oluştur - HAMBURGER İÇİN
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Hamburger ikonunu zorla göster
        toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.white, null)

        // Başlık ayarla
        supportActionBar?.title = "Yeni Şikayet"

        println("✅ Hamburger ikonu ayarlandı")

        // Navigation listener
        navigationView?.setNavigationItemSelectedListener(this)

        // Header bilgilerini güncelle
        val headerView = navigationView?.getHeaderView(0)
        headerView?.let {
            val tvUserName = it.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = it.findViewById<TextView>(R.id.tvUserEmail)

            tvUserName.text = userName
            tvUserEmail.text = userEmail
        }
    }

    private fun setupViews() {
        // Drawer layout ve navigation view
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        progressBar = findViewById(R.id.progressBar)
        val etBaslik = findViewById<EditText>(R.id.etBaslik)
        val etAciklama = findViewById<EditText>(R.id.etAciklama)
        val spinnerKategori = findViewById<Spinner>(R.id.spinnerKategori)
        val btnGonder = findViewById<Button>(R.id.btnGonder)
        val tvKarakterSayaci = findViewById<TextView>(R.id.tvKarakterSayaci)

        // Kategori spinner'ını ayarla
        val kategoriler = arrayOf("Temizlik", "Bakım", "Gürültü", "Güvenlik", "Diğer")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, kategoriler)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerKategori.adapter = adapter

        // Karakter sayacı
        etAciklama.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val length = s?.length ?: 0
                tvKarakterSayaci.text = "$length/10 karakter"

                if (length < 10) {
                    tvKarakterSayaci.setTextColor(ContextCompat.getColor(this@YeniSikayetActivity, android.R.color.holo_red_dark))
                } else {
                    tvKarakterSayaci.setTextColor(ContextCompat.getColor(this@YeniSikayetActivity, android.R.color.holo_green_dark))
                }
            }
        })

        // Gönder butonu
        btnGonder.setOnClickListener {
            val baslik = etBaslik.text.toString().trim()
            val aciklama = etAciklama.text.toString().trim()
            val kategori = spinnerKategori.selectedItem.toString()

            if (baslik.isEmpty() || aciklama.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (aciklama.length < 10) {
                Toast.makeText(this, "Açıklama en az 10 karakter olmalıdır", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            yeniSikayetGonder(baslik, aciklama, kategori)
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    private fun yeniSikayetGonder(baslik: String, aciklama: String, kategori: String) {
        progressBar.visibility = View.VISIBLE
        findViewById<Button>(R.id.btnGonder).isEnabled = false

        thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api_yeni_sikayet.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("Accept", "application/json")

                // Kategoriyi İngilizce'ye çevir
                val kategoriIng = when (kategori) {
                    "Temizlik" -> "cleaning"
                    "Bakım" -> "maintenance"
                    "Gürültü" -> "noise"
                    "Güvenlik" -> "security"
                    else -> "other"
                }

                val postData = "email=${URLEncoder.encode(userEmail, "UTF-8")}" +
                        "&baslik=${URLEncoder.encode(baslik, "UTF-8")}" +
                        "&aciklama=${URLEncoder.encode(aciklama, "UTF-8")}" +
                        "&kategori=${URLEncoder.encode(kategoriIng, "UTF-8")}"

                println("🔍 GÖNDERİLEN ŞİKAYET VERİSİ: $postData")

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                responseCode = conn.responseCode
                println("🔍 ŞİKAYET HTTP YANIT KODU: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    result = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    println("🔍 ŞİKAYET BAŞARILI YANIT: $result")
                } else {
                    try {
                        result = BufferedReader(InputStreamReader(conn.errorStream)).use { it.readText() }
                        println("🔍 ŞİKAYET HATA YANITI: $result")
                    } catch (e: Exception) {
                        result = "HTTP Hatası: $responseCode"
                    }
                }

            } catch (e: Exception) {
                result = "Bağlantı Hatası: ${e.message}"
                println("🔍 ŞİKAYET EXCEPTION: ${e.message}")
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                findViewById<Button>(R.id.btnGonder).isEnabled = true
                handleSikayetResponse(result)
            }
        }
    }

    private fun handleSikayetResponse(jsonResponse: String) {
        try {
            println("🔍 ŞİKAYET SUNUCU CEVABI: $jsonResponse")

            if (jsonResponse.isBlank()) {
                Toast.makeText(this, "Sunucu boş cevap döndü", Toast.LENGTH_LONG).show()
                return
            }

            if (jsonResponse.startsWith("{")) {
                val jsonObject = JSONObject(jsonResponse)
                val success = jsonObject.getBoolean("success")
                val message = jsonObject.getString("message")

                if (success) {
                    Toast.makeText(this, "✓ $message", Toast.LENGTH_LONG).show()
                    // Başarılı olursa şikayetler listesine dön
                    val intent = Intent(this, SikayetlerActivity::class.java)
                    intent.putExtra("user_email", userEmail)
                    intent.putExtra("user_type", userType)
                    intent.putExtra("user_name", userName)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "✗ $message", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "⚠ Beklenmeyen sunucu cevabı", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Veri işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            println("🔍 ŞİKAYET HATA: ${e.message}")
        }
    }
}