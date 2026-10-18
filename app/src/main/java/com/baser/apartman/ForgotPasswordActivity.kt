package com.baser.apartman

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var btnSend: Button
    private lateinit var btnBack: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // ApiManager'ı başlat
        ApiManager.initialize(this)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        btnSend = findViewById(R.id.btnSendResetCode)
        btnBack = findViewById(R.id.btnBackToLogin)
        progressBar = findViewById(R.id.progressBar)
        tvStatus = findViewById(R.id.tvStatus)

        // Kayıtlı email varsa doldur
        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val savedEmail = sharedPref.getString("user_email", "")
        if (!savedEmail.isNullOrEmpty()) {
            etEmail.setText(savedEmail)
        }

        tvStatus.visibility = View.GONE
    }

    private fun setupClickListeners() {
        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()

            // Validasyon
            if (email.isEmpty()) {
                showErrorMessage("Lütfen e-posta adresinizi girin")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showErrorMessage("Geçerli bir e-posta adresi girin")
                return@setOnClickListener
            }

            // Onay dialog'u göster
            showConfirmationDialog(email)
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showConfirmationDialog(email: String) {
        AlertDialog.Builder(this)
            .setTitle("Şifre Sıfırlama")
            .setMessage("'$email' e-posta adresine yeni şifre gönderilsin mi?\n\nYeni şifreniz e-posta adresinize gönderilecektir.")
            .setPositiveButton("Evet, Gönder") { dialog, which ->
                startPasswordReset(email)
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun startPasswordReset(email: String) {
        // UI'ı güncelle
        progressBar.visibility = View.VISIBLE
        btnSend.isEnabled = false
        btnBack.isEnabled = false

        // Durum mesajını göster
        showStatusMessage("Şifre sıfırlama işlemi başlatılıyor...", false)

        // ApiManager.resetPassword fonksiyonunu çağır
        ApiManager.resetPassword(
            context = this,
            email = email,
            onSuccess = { message ->
                // BAŞARILI
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSend.isEnabled = true
                    btnBack.isEnabled = true

                    showSuccessMessage(message)
                }
            },
            onError = { error ->
                // HATA
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    btnSend.isEnabled = true
                    btnBack.isEnabled = true

                    showErrorMessage(error)
                }
            }
        )
    }

    private fun showSuccessMessage(message: String) {
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "✅ $message"
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        tvStatus.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))

        // Başarı dialog'u göster
        Handler(Looper.getMainLooper()).postDelayed({
            AlertDialog.Builder(this)
                .setTitle("✅ İşlem Başarılı")
                .setMessage(message)
                .setPositiveButton("Tamam") { dialog, which ->
                    finish()
                }
                .setCancelable(false)
                .show()
        }, 1000)
    }

    private fun showErrorMessage(message: String) {
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = "❌ $message"
        tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        tvStatus.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))

        // 10 saniye sonra mesajı temizle
        Handler(Looper.getMainLooper()).postDelayed({
            tvStatus.visibility = View.GONE
        }, 10000)
    }

    private fun showStatusMessage(message: String, isError: Boolean) {
        tvStatus.visibility = View.VISIBLE
        tvStatus.text = message

        if (isError) {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            tvStatus.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
        } else {
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))
            tvStatus.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_blue_light))
        }
    }

    override fun onBackPressed() {
        finish()
    }
}