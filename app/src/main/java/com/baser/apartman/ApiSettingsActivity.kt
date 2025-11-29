package com.baser.apartman

import android.app.AlertDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ApiSettingsActivity : AppCompatActivity() {

    private lateinit var etBaseUrl: EditText
    private lateinit var etApiPath: EditText
    private lateinit var etSqlKey: EditText
    private lateinit var etBackupUrl: EditText
    private lateinit var switchUseBackup: Switch
    private lateinit var btnTest: Button
    private lateinit var btnSave: Button
    private lateinit var btnReset: Button
    private lateinit var tvCurrentUrl: TextView
    private lateinit var tvConnectionStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_api_settings)

        // ApiManager'ı başlat
        ApiManager.initialize(this)

        initViews()
        loadCurrentSettings()
        setupListeners()
    }

    private fun initViews() {
        etBaseUrl = findViewById(R.id.etBaseUrl)
        etApiPath = findViewById(R.id.etApiPath)
        etSqlKey = findViewById(R.id.etSqlKey)
        etBackupUrl = findViewById(R.id.etBackupUrl)
        switchUseBackup = findViewById(R.id.switchUseBackup)
        btnTest = findViewById(R.id.btnTest)
        btnSave = findViewById(R.id.btnSave)
        btnReset = findViewById(R.id.btnReset)
        tvCurrentUrl = findViewById(R.id.tvCurrentUrl)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
    }

    private fun loadCurrentSettings() {
        val settings = ApiManager.getAllSettings()

        etBaseUrl.setText(settings["base_url"])
        etApiPath.setText(settings["api_path"])
        etSqlKey.setText(settings["sql_key"])
        etBackupUrl.setText(settings["backup_url"])
        switchUseBackup.isChecked = settings["use_backup"] == "true"
        tvCurrentUrl.text = "Mevcut URL: ${settings["current_url"]}"
    }

    private fun setupListeners() {
        btnTest.setOnClickListener {
            testConnection()
        }

        btnSave.setOnClickListener {
            saveSettings()
        }

        btnReset.setOnClickListener {
            resetToDefaults()
        }

        switchUseBackup.setOnCheckedChangeListener { _, isChecked ->
            ApiManager.setUseBackup(isChecked)
            loadCurrentSettings() // Mevcut URL'yi güncelle
        }
    }

    private fun testConnection() {
        tvConnectionStatus.text = "Bağlantı test ediliyor..."
        tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_blue_dark))

        ApiManager.testConnection(
            context = this,
            onSuccess = {
                tvConnectionStatus.text = "✅ Bağlantı başarılı!"
                tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            },
            onError = { error ->
                tvConnectionStatus.text = "❌ Bağlantı hatası: $error"
                tvConnectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        )
    }

    private fun saveSettings() {
        val baseUrl = etBaseUrl.text.toString().trim()
        val apiPath = etApiPath.text.toString().trim()
        val sqlKey = etSqlKey.text.toString().trim()
        val backupUrl = etBackupUrl.text.toString().trim()

        if (baseUrl.isEmpty() || apiPath.isEmpty() || sqlKey.isEmpty()) {
            Toast.makeText(this, "Lütfen zorunlu alanları doldurun", Toast.LENGTH_SHORT).show()
            return
        }

        ApiManager.setBaseUrl(baseUrl)
        ApiManager.setApiPath(apiPath)
        ApiManager.setSqlKey(sqlKey)
        ApiManager.setBackupUrl(backupUrl)

        Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
        loadCurrentSettings()
        testConnection() // Yeni ayarları test et
    }

    private fun resetToDefaults() {
        AlertDialog.Builder(this)
            .setTitle("Varsayılanlara Sıfırla")
            .setMessage("Tüm API ayarları varsayılan değerlere sıfırlanacak. Emin misiniz?")
            .setPositiveButton("Evet") { dialog, which ->
                ApiManager.resetToDefaults()
                loadCurrentSettings()
                Toast.makeText(this, "Ayarlar varsayılanlara sıfırlandı", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Hayır", null)
            .show()
    }
}