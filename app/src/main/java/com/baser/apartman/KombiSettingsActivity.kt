package com.baser.apartman

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class KombiSettingsActivity : BaseActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kombi_settings)
        
        setupNavigation()
        
        val etKombiToken = findViewById<EditText>(R.id.etKombiToken)
        val btnSave = findViewById<Button>(R.id.btnSave)
        
        // Mevcut token'ı yükle
        val prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)
        etKombiToken.setText(prefs.getString("kombi_token", ""))
        
        btnSave.setOnClickListener {
            val token = etKombiToken.text.toString().trim()
            if (token.isNotEmpty()) {
                prefs.edit().putString("kombi_token", token).apply()
                Toast.makeText(this, "✅ Kombi token kaydedildi", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "❌ Token boş olamaz", Toast.LENGTH_SHORT).show()
            }
        }
    }
}