package com.baser.apartman

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView

class SettingsActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var etBlynkToken: EditText
    private lateinit var etBlynkPin: EditText
    private lateinit var etGarajLat: EditText
    private lateinit var etGarajLon: EditText
    private lateinit var etGeofenceRadius: EditText
    private lateinit var etApiKey: EditText
    private lateinit var etApiUrl: EditText
    private lateinit var spinnerLocationSpeed: Spinner
    private lateinit var switchAutoGarage: Switch
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button
    private lateinit var btnReset: Button

    // Konum hızı seçenekleri
    private val locationSpeedOptions = arrayOf(
        "YAVAŞ (10 saniye - Pil Dostu)",
        "ORTA (5 saniye - Dengeli)",
        "HIZLI (2 saniye - Hızlı Yanıt)",
        "ÇOK HIZLI (1 saniye - Realtime)"
    )

    private val locationSpeedValues = mapOf(
        0 to "slow",    // 10 saniye
        1 to "medium",  // 5 saniye
        2 to "fast",    // 2 saniye
        3 to "realtime" // 1 saniye
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // SharedPreferences başlat
        prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)

        setupNavigation()
        initViews()
        setupLocationSpeedSpinner()
        setupAutoGarageSwitch()
        loadSettings()
    }

    // Navigation'ı BaseActivity'den override et
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
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // Önce kapat
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
        toggle.syncState() // BU ÇOK ÖNEMLİ!

        // Hamburger ikonunu zorla göster
        toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.white, null)

        // Başlık ayarla
        supportActionBar?.title = "Ayarlar"

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

    private fun initViews() {
        etBlynkToken = findViewById(R.id.etBlynkToken)
        etBlynkPin = findViewById(R.id.etBlynkPin)
        etGarajLat = findViewById(R.id.etGarajLat)
        etGarajLon = findViewById(R.id.etGarajLon)
        etGeofenceRadius = findViewById(R.id.etGeofenceRadius)
        etApiKey = findViewById(R.id.etApiKey)
        etApiUrl = findViewById(R.id.etApiUrl)
        spinnerLocationSpeed = findViewById(R.id.spinnerLocationSpeed)
        switchAutoGarage = findViewById(R.id.switchAutoGarage)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)
        btnReset = findViewById(R.id.btnReset)

        // Kaydet butonu
        btnSave.setOnClickListener {
            saveSettings()
        }

        // Geri butonu - artık drawer'ı kapatacak
        btnBack.setOnClickListener {
            onBackPressed()
        }

        // Sıfırla butonu
        btnReset.setOnClickListener {
            resetToDefaults()
        }
    }

    private fun setupLocationSpeedSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, locationSpeedOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerLocationSpeed.adapter = adapter

        // Spinner seçim listener'ı
        spinnerLocationSpeed.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedSpeed = locationSpeedValues[position] ?: "medium"
                showSpeedDescription(selectedSpeed)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Hiçbir şey seçilmediğinde
            }
        }
    }

    private fun setupAutoGarageSwitch() {
        switchAutoGarage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "✅ Otomatik garaj açma AKTİF", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "❌ Otomatik garaj açma PASİF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSpeedDescription(speed: String) {
        val description = when (speed) {
            "slow" -> "⏱️ 10 saniye - En az pil tüketimi"
            "medium" -> "⚡ 5 saniye - Dengeli performans"
            "fast" -> "🚀 2 saniye - Hızlı yanıt"
            "realtime" -> "🔥 1 saniye - Gerçek zamanlı (Pil tüketimi yüksek)"
            else -> "Dengeli performans"
        }
        // İsterseniz bu description'ı bir TextView'da gösterebilirsiniz
    }

    private fun loadSettings() {
        // Varsayılan değerlerle birlikte ayarları yükle
        etBlynkToken.setText(prefs.getString("blynk_token", "SİZİN_TOKEN_BURAYA"))
        etBlynkPin.setText(prefs.getString("blynk_pin", "V1"))
        etGarajLat.setText(prefs.getString("garaj_lat", "36.7917535"))
        etGarajLon.setText(prefs.getString("garaj_lon", "34.5916286"))
        etGeofenceRadius.setText(prefs.getString("geofence_radius", "50.0"))
        etApiKey.setText(prefs.getString("api_key", "apartman_secret_key_2024"))
        etApiUrl.setText(prefs.getString("api_url", "http://baser.org/apartman/api/api_dashboard_stats.php"))

        // Konum hızı ayarını yükle
        val savedSpeed = prefs.getString("location_speed", "medium")
        val speedPosition = when (savedSpeed) {
            "slow" -> 0
            "fast" -> 2
            "realtime" -> 3
            else -> 1 // medium varsayılan
        }
        spinnerLocationSpeed.setSelection(speedPosition)

        // Otomatik garaj açma ayarını yükle
        val isAutoGarageEnabled = prefs.getBoolean("auto_garage_enabled", true)
        switchAutoGarage.isChecked = isAutoGarageEnabled
    }

    private fun saveSettings() {
        try {
            val editor = prefs.edit()

            // Değerleri kontrol et ve kaydet
            editor.putString("blynk_token", etBlynkToken.text.toString().trim())
            editor.putString("blynk_pin", etBlynkPin.text.toString().trim())

            // Koordinatları double olarak kontrol et
            val lat = etGarajLat.text.toString().trim().toDoubleOrNull()
            val lon = etGarajLon.text.toString().trim().toDoubleOrNull()
            val radius = etGeofenceRadius.text.toString().trim().toDoubleOrNull()

            if (lat == null || lon == null) {
                Toast.makeText(this, "Geçersiz koordinat değeri", Toast.LENGTH_SHORT).show()
                return
            }

            if (radius == null || radius <= 0) {
                Toast.makeText(this, "Geçersiz çap değeri", Toast.LENGTH_SHORT).show()
                return
            }

            editor.putString("garaj_lat", lat.toString())
            editor.putString("garaj_lon", lon.toString())
            editor.putString("geofence_radius", radius.toString())
            editor.putString("api_key", etApiKey.text.toString().trim())
            editor.putString("api_url", etApiUrl.text.toString().trim())

            // Konum hızı ayarını kaydet
            val selectedSpeed = locationSpeedValues[spinnerLocationSpeed.selectedItemPosition] ?: "medium"
            editor.putString("location_speed", selectedSpeed)

            // Otomatik garaj açma ayarını kaydet
            editor.putBoolean("auto_garage_enabled", switchAutoGarage.isChecked)

            if (editor.commit()) {
                Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()

                // Dashboard'u güncellemek için result gönder
                setResult(RESULT_OK)

                // Konum hızı ve otomatik garaj bilgisini göster
                showSettingsUpdateMessage(selectedSpeed, switchAutoGarage.isChecked)
            } else {
                Toast.makeText(this, "Kaydetme hatası", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSettingsUpdateMessage(speed: String, isAutoGarageEnabled: Boolean) {
        val speedMessage = when (speed) {
            "slow" -> "Konum güncelleme: YAVAŞ (10s)"
            "medium" -> "Konum güncelleme: ORTA (5s)"
            "fast" -> "Konum güncelleme: HIZLI (2s)"
            "realtime" -> "Konum güncelleme: ÇOK HIZLI (1s)"
            else -> "Konum güncelleme ayarlandı"
        }

        val garageMessage = if (isAutoGarageEnabled) {
            "✅ Otomatik garaj açma: AKTİF"
        } else {
            "❌ Otomatik garaj açma: PASİF"
        }

        val fullMessage = "$speedMessage\n$garageMessage"
        Toast.makeText(this, fullMessage, Toast.LENGTH_LONG).show()
    }

    private fun resetToDefaults() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()

        loadSettings()
        Toast.makeText(this, "Varsayılan ayarlar yüklendi", Toast.LENGTH_SHORT).show()
    }
}