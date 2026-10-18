package com.baser.apartman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt
class ResetPasswordActivity : AppCompatActivity() {

    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        // ApiManager'ı başlat
        ApiManager.initialize(this)

        email = intent.getStringExtra("email") ?: ""

        setupClickListeners()
    }

    private fun setupClickListeners() {
        val btnResetPassword = findViewById<Button>(R.id.btnResetPassword)
        val etCode = findViewById<EditText>(R.id.etCode)
        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnResetPassword.setOnClickListener {
            val code = etCode.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (code.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Lütfen tüm alanları doldurun", Toast.LENGTH_SHORT).show()
            } else if (newPassword != confirmPassword) {
                Toast.makeText(this, "Şifreler eşleşmiyor", Toast.LENGTH_SHORT).show()
            } else if (newPassword.length < 6) {
                Toast.makeText(this, "Şifre en az 6 karakter olmalı", Toast.LENGTH_SHORT).show()
            } else {
                resetPassword(email, code, newPassword, progressBar, btnResetPassword)
            }
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun resetPassword(
        email: String,
        code: String,
        newPassword: String,
        progressBar: ProgressBar,
        btnReset: Button
    ) {
        progressBar.visibility = View.VISIBLE
        btnReset.visibility = View.INVISIBLE

        // Demo için sabit kodu kontrol et
        if (code != "123456") {
            progressBar.visibility = View.GONE
            btnReset.visibility = View.VISIBLE
            Toast.makeText(this, "Geçersiz kod", Toast.LENGTH_LONG).show()
            return
        }

        // Yeni şifreyi BCrypt ile hash'le
        val bcryptHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray())

        // Şifreyi güncelle
        val sqlQuery = "UPDATE apartman_users SET password = '$bcryptHash' WHERE email = '$email'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                progressBar.visibility = View.GONE
                btnReset.visibility = View.VISIBLE

                if (result.contains("AFFECTED ROWS: 1")) {
                    Toast.makeText(this, "Şifreniz başarıyla güncellendi", Toast.LENGTH_LONG).show()
                    // Başarılıysa login ekranına dön
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Şifre güncellenemedi. Kullanıcı bulunamadı.", Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                progressBar.visibility = View.GONE
                btnReset.visibility = View.VISIBLE
                Toast.makeText(this, "Hata: $error", Toast.LENGTH_LONG).show()
            }
        )
    }
}