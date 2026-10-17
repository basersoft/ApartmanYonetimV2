package com.baser.apartman

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import at.favre.lib.crypto.bcrypt.BCrypt

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // ApiManager'ı başlat
        ApiManager.initialize(this)

        setupSpinners()
        setupClickListeners()
    }

    private fun setupSpinners() {
        val spUserType = findViewById<Spinner>(R.id.spUserType)
        val spResidentType = findViewById<Spinner>(R.id.spResidentType)
        val spDuesResponsibility = findViewById<Spinner>(R.id.spDuesResponsibility)

        // Kullanıcı tipi spinner'ı
        ArrayAdapter.createFromResource(
            this,
            R.array.user_types,  // R.array.user_types_register yerine
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spUserType.adapter = adapter
        }

        // Sakin tipi spinner'ı
        ArrayAdapter.createFromResource(
            this,
            R.array.resident_types,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spResidentType.adapter = adapter
        }

        // Aidat sorumluluğu spinner'ı
        ArrayAdapter.createFromResource(
            this,
            R.array.dues_responsibility,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spDuesResponsibility.adapter = adapter
        }
    }

    private fun setupClickListeners() {
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPhone = findViewById<EditText>(R.id.etPhone)
        val etBlock = findViewById<EditText>(R.id.etBlock)
        val etNumber = findViewById<EditText>(R.id.etNumber)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            val block = etBlock.text.toString().trim()
            val number = etNumber.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val spUserType = findViewById<Spinner>(R.id.spUserType)
            val spResidentType = findViewById<Spinner>(R.id.spResidentType)
            val spDuesResponsibility = findViewById<Spinner>(R.id.spDuesResponsibility)

            val userType = when (spUserType.selectedItemPosition) {
                0 -> "resident"
                1 -> "manager"
                else -> "admin"
            }
            val residentType = if (spResidentType.selectedItemPosition == 0) "owner" else "tenant"
            val duesResponsibility = if (spDuesResponsibility.selectedItemPosition == 0) "yes" else "no"

            if (name.isEmpty() || email.isEmpty() || block.isEmpty() || number.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Lütfen zorunlu alanları doldurun", Toast.LENGTH_SHORT).show()
            } else if (password != confirmPassword) {
                Toast.makeText(this, "Şifreler eşleşmiyor", Toast.LENGTH_SHORT).show()
            } else if (password.length < 6) {
                Toast.makeText(this, "Şifre en az 6 karakter olmalı", Toast.LENGTH_SHORT).show()
            } else {
                // BCrypt ile şifreyi hash'le
                registerUserWithBCrypt(
                    name, email, phone, block, number, password,
                    userType, residentType, duesResponsibility,
                    progressBar, btnRegister
                )
            }
        }

        findViewById<Button>(R.id.btnBackToLogin).setOnClickListener {
            finish()
        }
    }

    private fun registerUserWithBCrypt(
        name: String,
        email: String,
        phone: String,
        block: String,
        number: String,
        password: String,
        userType: String,
        residentType: String,
        duesResponsibility: String,
        progressBar: ProgressBar,
        btnRegister: Button
    ) {
        progressBar.visibility = View.VISIBLE
        btnRegister.visibility = View.INVISIBLE

        // Şifreyi BCrypt ile hash'le (PHP password_hash() ile uyumlu)
        val bcryptHash = BCrypt.withDefaults().hashToString(12, password.toCharArray())

        // SQL sorgusu oluştur
        val sqlQuery = "INSERT INTO apartman_users (name, email, password, phone, apartment_block, apartment_number, user_type, resident_type, is_responsible_for_dues, is_residing, receive_reminders, status) VALUES ('$name', '$email', '$bcryptHash', '$phone', '$block', '$number', '$userType', '$residentType', '$duesResponsibility', 'yes', 'yes', 'pending')"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { result ->
                progressBar.visibility = View.GONE
                btnRegister.visibility = View.VISIBLE

                if (result.contains("AFFECTED ROWS: 1")) {
                    Toast.makeText(this, "Kayıt başarılı! Hesabınız yönetici onayından sonra aktif olacaktır.", Toast.LENGTH_LONG).show()

                    // Başarılıysa login ekranına dön
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra("registered_email", email)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                    finish()
                } else {
                    val errorMessage = when {
                        result.contains("Duplicate entry") && result.contains("email") ->
                            "Bu email adresi zaten kayıtlı"
                        result.contains("Duplicate entry") ->
                            "Bu daire numarası zaten kayıtlı"
                        else ->
                            "Kayıt başarısız oldu."
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            },
            onError = { error ->
                progressBar.visibility = View.GONE
                btnRegister.visibility = View.VISIBLE

                val errorMessage = when {
                    error.contains("Duplicate entry") && error.contains("email") ->
                        "Bu email adresi zaten kayıtlı"
                    error.contains("Duplicate entry") ->
                        "Bu daire numarası zaten kayıtlı"
                    else ->
                        "Kayıt hatası: $error"
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        )
    }
}