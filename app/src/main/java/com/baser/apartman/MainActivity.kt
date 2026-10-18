package com.baser.apartman

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.*
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var ivBackground: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ivBackground = findViewById(R.id.ivBackground)

        // ApiManager'ı başlat
        ApiManager.initialize(this)

        // ÖNCE KAYDEDİLMİŞ KULLANICI VAR MI KONTROL ET
        checkSavedUser()

        // Webden resmi yükle
        loadBackgroundImage()
        setupClickListeners()
    }

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

        println("🔍 LOGIN BAŞARILI - EMAIL KAYDEDİLDİ: $email")

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
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

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

        // Şifremi Unuttum tıklama
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        // Yeni Kayıt Ol tıklama
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun login(
        email: String,
        password: String,
        rememberMe: Boolean,
        progressBar: android.widget.ProgressBar,
        btnLogin: android.widget.Button
    ) {
        progressBar.visibility = View.VISIBLE
        btnLogin.visibility = View.INVISIBLE

        // SQL sorgusu ile kullanıcıyı kontrol et
        val sqlQuery = "SELECT id, name, email, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing, status, password FROM apartman_users WHERE email = '$email'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                progressBar.visibility = View.GONE
                btnLogin.visibility = View.VISIBLE

                handleLoginResult(result, email, password, rememberMe)
            },
            onError = { error ->
                progressBar.visibility = View.GONE
                btnLogin.visibility = View.VISIBLE
                Toast.makeText(this, "Bağlantı hatası: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun handleLoginResult(csvData: String, email: String, password: String, rememberMe: Boolean) {
        ProfileImageManager.clearCacheForUser(this, email)
        try {
            println("🔍 LOGIN SONUCU: $csvData")

            if (csvData.isBlank() || csvData.contains("AFFECTED ROWS: 0")) {
                Toast.makeText(this, "Email veya şifre hatalı", Toast.LENGTH_LONG).show()
                return
            }

            val lines = csvData.trim().split("\n")
            if (lines.size <= 1) {
                Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_LONG).show()
                return
            }

            // CSV'yi parse et
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isNotEmpty()) {
                    try {
                        val fields = parseCSVLine(line)
                        if (fields.size >= 12) { // 12 alan (password dahil)
                            val userId = fields[0]
                            val userName = fields[1]
                            val userEmail = fields[2]
                            val userPhone = fields[3]
                            val apartmentBlock = fields[4]
                            val apartmentNumber = fields[5]
                            val userType = fields[6]
                            val residentType = fields[7]
                            val isResponsibleForDues = fields[8]
                            val isResiding = fields[9]
                            val status = fields[10]
                            val dbPassword = fields[11] // Veritabanındaki şifre

                            println("🔍 KULLANICI BİLGİLERİ - Email: $userEmail, Status: $status")

                            // Kullanıcı durumunu kontrol et
                            if (status == "pending") {
                                Toast.makeText(this, "Hesabınız henüz aktif değil. Lütfen yöneticiden aktivasyon bekleyin.", Toast.LENGTH_LONG).show()
                                return
                            }

                            if (status == "inactive") {
                                Toast.makeText(this, "Hesabınız pasif durumda. Lütfen yönetici ile iletişime geçin.", Toast.LENGTH_LONG).show()
                                return
                            }

                            // Şifre kontrolü - BCrypt desteği ile
                            if (verifyPassword(password, dbPassword)) {
                                // Başarılı giriş
                                Toast.makeText(this, "Giriş başarılı!", Toast.LENGTH_LONG).show()

                                // Kullanıcı bilgilerini kaydet
                                saveUserInfo(userId, email, userType, userName, rememberMe)

                                // Dashboard'a yönlendir
                                onLoginSuccess(email, userType, userName, rememberMe)
                            } else {
                                Toast.makeText(this, "Email veya şifre hatalı", Toast.LENGTH_LONG).show()
                            }
                            return
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            Toast.makeText(this, "Kullanıcı bulunamadı", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(this, "Veri işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            println("🔍 HATA: ${e.message}")
        }
    }

    private fun verifyPassword(inputPassword: String, dbPassword: String): Boolean {
        // BCrypt hash kontrolü (PHP password_hash() formatı)
        if (dbPassword.startsWith("$2y$") || dbPassword.startsWith("$2a$") || dbPassword.startsWith("$2b$")) {
            return verifyBcryptPassword(inputPassword, dbPassword)
        }

        // MD5 hash kontrolü
        val md5Hash = md5(inputPassword)
        if (md5Hash == dbPassword) {
            return true
        }

        // SHA-256 hash kontrolü
        val sha256Hash = sha256(inputPassword)
        if (sha256Hash == dbPassword) {
            return true
        }

        // Direkt eşleşme (plain text)
        if (inputPassword == dbPassword) {
            return true
        }

        return false
    }

    private fun verifyBcryptPassword(password: String, bcryptHash: String): Boolean {
        return try {
            // BCrypt.verifyer() ile hash kontrolü
            val result = BCrypt.verifyer().verify(password.toCharArray(), bcryptHash)
            result.verified
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun md5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digested = md.digest(input.toByteArray())
            digested.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun sha256(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val digested = md.digest(input.toByteArray())
            digested.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = ArrayList<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            when {
                line[i] == '"' -> inQuotes = !inQuotes
                line[i] == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(line[i])
            }
        }
        result.add(current.toString())
        return result
    }

    private fun saveUserInfo(userId: String, email: String, userType: String, userName: String, rememberMe: Boolean) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_id", userId)
            putString("user_email", email)
            putString("user_type", userType)
            putString("user_name", userName)
            putBoolean("beni_hatirla", rememberMe)
            apply()
        }
        println("🔍 KAYDEDİLEN BİLGİLER - ID: $userId, Email: $email, Type: $userType, Name: $userName")
    }

    override fun onResume() {
        super.onResume()
        // Sayfa her açıldığında resmi güncelle
        loadBackgroundImage()
    }
}