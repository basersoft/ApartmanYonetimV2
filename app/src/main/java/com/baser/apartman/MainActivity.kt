package com.baser.apartman

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread
import android.widget.CheckBox
import com.baser.apartman.workers.WidgetUpdateWorker

class MainActivity : AppCompatActivity() {

    private lateinit var ivBackground: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //WeatherWidgetUtils.updateWidgets(this)
        val weatherWidget = findViewById<com.baser.apartman.weather.WeatherWidget>(R.id.weatherWidget)
        ivBackground = findViewById(R.id.ivBackground)

        // ÖNCE KAYDEDİLMİŞ KULLANICI VAR MI KONTROL ET
        checkSavedUser()

        // Webden resmi yükle
        loadBackgroundImage()
        startWidgetAutoUpdate()
        setupClickListeners()
    }

    // EKSİK FONKSİYONU EKLEYİN - BAŞLANGIÇ
    private fun startWidgetAutoUpdate() {
        try {
            println("🔧 Widget otomatik güncelleme başlatılıyor...")

            // ✅ 1. WorkManager'ı AKTİF ET (yorumu kaldır)
            WidgetUpdateWorker.scheduleWidgetUpdate(this)

            // ✅ 2. Anlık güncelleme de yap
            com.baser.apartman.widgets.AidatWidgetUtils.updateWidgets(this)
            com.baser.apartman.widgets.DuyuruWidgetUtils.updateWidgets(this)

            println("✅ Widget otomatik güncelleme başlatıldı")

        } catch (e: Exception) {
            println("❌ Widget güncelleme hatası: ${e.message}")
        }
    }
    // EKSİK FONKSİYONU EKLEYİN - BİTİŞ

    private fun checkSavedUser() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val savedEmail = sharedPref.getString("user_email", "")
        val savedUserType = sharedPref.getString("user_type", "")
        val savedUserName = sharedPref.getString("user_name", "")
        val beniHatirla = sharedPref.getBoolean("beni_hatirla", false)

        println("🔍 KAYITLI KULLANICI: $savedEmail, Beni Hatırla: $beniHatirla")

        // Eğer "Beni Hatırla" seçilmişse ve kullanıcı bilgileri varsa, direkt dashboard'a yönlendir
        if (beniHatirla && !savedEmail.isNullOrEmpty()) {
            println("🔍 OTOMATİK GİRİŞ YAPILIYOR: $savedEmail")

            val intent = Intent(this, DashboardActivity::class.java)
            intent.putExtra("user_email", savedEmail)
            intent.putExtra("user_type", savedUserType)
            intent.putExtra("user_name", savedUserName)
            startActivity(intent)
            finish()
        } else {
            // Kayıtlı email varsa input alanına yaz
            if (!savedEmail.isNullOrEmpty()) {
                findViewById<android.widget.EditText>(R.id.etEmail).setText(savedEmail)
            }
        }
    }

    private fun onLoginSuccess(email: String, type: String, name: String, rememberMe: Boolean) {
        // MEVCUT KOD - user_prefs'e kaydet
        val userPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        userPrefs.edit().apply {
            putString("user_email", email)
            putString("user_type", type)
            putString("user_name", name)
            putBoolean("beni_hatirla", rememberMe)
            apply()
        }

        // YENİ EKLENEN KOD - widget_prefs'e de aynı bilgileri kaydet
        val widgetPrefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        widgetPrefs.edit().apply {
            putString("user_email", email)
            putString("user_type", type)
            putString("user_name", name)
            apply()
        }

        println("🔍 LOGIN BAŞARILI - WIDGET İÇİN EMAIL KAYDEDİLDİ: $email")

        val intent = Intent(this, DashboardActivity::class.java)
        intent.putExtra("user_email", email)
        intent.putExtra("user_type", type)
        intent.putExtra("user_name", name)
        startActivity(intent)
        finish()
    }

    private fun loadBackgroundImage() {
        thread {
            try {
                val imageUrl = "http://baser.org/apartman/uploads/profile_images/user_1_1762622512.JPG"
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.connect()

                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)

                runOnUiThread {
                    ivBackground.setImageBitmap(bitmap)
                    println("🔍 Arka plan resmi başarıyla yüklendi: $imageUrl")
                }

            } catch (e: Exception) {
                println("🔍 Arka plan resmi yükleme hatası: ${e.message}")
            }
        }
    }

    private fun setupClickListeners() {
        val btnLogin = findViewById<android.widget.Button>(R.id.btnLogin)
        val etEmail = findViewById<android.widget.EditText>(R.id.etEmail)
        val etPassword = findViewById<android.widget.EditText>(R.id.etPassword)
        val progressBar = findViewById<android.widget.ProgressBar>(R.id.progressBar)
        val cbBeniHatirla = findViewById<android.widget.CheckBox>(R.id.cbBeniHatirla)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val rememberMe = cbBeniHatirla.isChecked

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            } else {
                login(email, password, rememberMe, progressBar, btnLogin)
            }
        }
    }

    private fun login(
        email: String,
        pass: String,
        rememberMe: Boolean,
        progressBar: android.widget.ProgressBar,
        btnLogin: android.widget.Button
    ) {
        progressBar.visibility = View.VISIBLE
        btnLogin.visibility = View.INVISIBLE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api_login_gemini.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("Accept", "application/json")

                val postData = "email=${URLEncoder.encode(email, "UTF-8")}&password=${URLEncoder.encode(pass, "UTF-8")}"
                println("🔍 GÖNDERİLEN VERİ: $postData")

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                responseCode = conn.responseCode
                println("🔍 HTTP YANIT KODU: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                    println("🔍 BAŞARILI YANIT: $result")
                } else {
                    try {
                        val errorStream = conn.errorStream
                        result = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                        println("🔍 HATA YANITI: $result")
                    } catch (e: Exception) {
                        result = "HTTP Hatası: $responseCode"
                    }
                }

            } catch (e: Exception) {
                result = "Bağlantı Hatası: ${e.message}"
                println("🔍 EXCEPTION: ${e.message}")
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                btnLogin.visibility = View.VISIBLE

                if (responseCode != 0 && responseCode != 200) {
                    Toast.makeText(this, "HTTP: $responseCode", Toast.LENGTH_SHORT).show()
                }

                handleLoginResult(result, email, rememberMe)
            }
        }.start()
    }

    private fun handleLoginResult(jsonResponse: String, email: String, rememberMe: Boolean) {
        try {
            println("🔍 SUNUCU CEVABI: $jsonResponse")

            if (jsonResponse.isBlank()) {
                Toast.makeText(this, "Sunucu boş cevap döndü", Toast.LENGTH_LONG).show()
                return
            }

            if (jsonResponse.startsWith("{")) {
                val jsonObject = JSONObject(jsonResponse)
                val success = jsonObject.getBoolean("success")
                val message = jsonObject.getString("message")

                if (success) {
                    val userType = if (jsonObject.has("user_type")) {
                        jsonObject.getString("user_type")
                    } else {
                        when {
                            email.contains("admin", ignoreCase = true) -> "admin"
                            email.contains("manager", ignoreCase = true) -> "manager"
                            else -> "resident"
                        }
                    }

                    val userName = if (jsonObject.has("user_name")) {
                        jsonObject.getString("user_name")
                    } else {
                        email
                    }

                    println("🔍 KULLANICI BİLGİLERİ - Type: $userType, Name: $userName, Beni Hatırla: $rememberMe")

                    Toast.makeText(this, "✓ $message", Toast.LENGTH_LONG).show()

                    // Kullanıcı bilgilerini kaydet
                    saveUserInfo(email, userType, userName, rememberMe)

                    // DÜZELTME: userEmail yerine email parametresini kullan
                    onLoginSuccess(email, userType, userName, rememberMe)

                } else {
                    Toast.makeText(this, "✗ $message", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "⚠ Beklenmeyen sunucu cevabı", Toast.LENGTH_LONG).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Veri işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            println("🔍 HATA: ${e.message}")
        }
    }

    private fun saveUserInfo(email: String, userType: String, userName: String, rememberMe: Boolean) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_email", email)
            putString("user_type", userType)
            putString("user_name", userName)
            putBoolean("beni_hatirla", rememberMe) // "Beni Hatırla" durumunu kaydet
            apply()
        }
        println("🔍 KAYDEDİLEN BİLGİLER - Email: $email, Type: $userType, Name: $userName, Beni Hatırla: $rememberMe")
    }

    override fun onResume() {
        super.onResume()
        // Sayfa her açıldığında resmi güncelle
        loadBackgroundImage()
    }
}