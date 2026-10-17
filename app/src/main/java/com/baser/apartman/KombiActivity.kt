package com.baser.apartman

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.baser.apartman.views.CircularProgressView
import com.baser.apartman.views.CircularSeekBar
import java.util.concurrent.Executors

class KombiActivity : BaseActivity() {

    private lateinit var tvKombiDurumu: TextView
    private lateinit var tvIstenenSicaklik: TextView
    private lateinit var tvHissedilen: TextView
    private lateinit var tvDisSicaklik: TextView
    private lateinit var tvDisNem: TextView
    private lateinit var tvDisDurum: TextView
    private lateinit var btnKombiAc: Button
    private lateinit var btnKombiKapat: Button
    private lateinit var btnYenile: Button
    private lateinit var btnDisHavaDetay: Button
    private lateinit var btnKombiAyarlar: Button
    private lateinit var tempProgressBar: CircularProgressView
    private lateinit var humidityProgressBar: CircularProgressView
    private lateinit var tvGercekSicaklikCenter: TextView
    private lateinit var tvNemCenter: TextView
    private lateinit var tvCircularTempValue: TextView
    private lateinit var circularSeekBar: CircularSeekBar

    companion object {
        private const val V_TEMP_ACTUAL = "V0"
        private const val V_TEMP_TARGET = "V3"
        private const val V_HUMIDITY = "V30"
        private const val V_COMBI_STATE = "V32"
        private const val V_COMBI_CONTROL = "V4"
        private const val V_FEELS_LIKE = "V2"
        private const val BLYNK_BASE_URL = "https://fra1.blynk.cloud"
    }

    private var refreshHandler = Handler(Looper.getMainLooper())
    private var refreshRunnable: Runnable? = null
    private var currentCombiState = "KAPALI"
    private var isActive = true

    // Önceki değerleri takip et
    private var lastDisplayedTemp = -100
    private var lastDisplayedHumidity = -1
    private var lastDisplayedTargetTemp = -100
    private var lastDisplayedFeelsLike = -100

    // Thread pool for parallel requests
    private val executor = Executors.newFixedThreadPool(2)

    // Volley request queue (singleton)
    private var requestQueueInitialized = false
    private fun getRequestQueue() = Volley.newRequestQueue(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kombi)

        setupNavigation()
        initViews()
        setupListeners()

        checkKombiToken()
        loadKombiDataFast() // Hızlı yükleme
        loadOutsideWeatherData()

        startAutoRefresh()
    }

    private fun initViews() {
        tvKombiDurumu = findViewById(R.id.tvKombiDurumu)
        tvIstenenSicaklik = findViewById(R.id.tvIstenenSicaklik)
        tvHissedilen = findViewById(R.id.tvHissedilen)
        tvDisSicaklik = findViewById(R.id.tvDisSicaklik)
        tvDisNem = findViewById(R.id.tvDisNem)
        tvDisDurum = findViewById(R.id.tvDisDurum)
        btnKombiAc = findViewById(R.id.btnKombiAc)
        btnKombiKapat = findViewById(R.id.btnKombiKapat)
        btnYenile = findViewById(R.id.btnYenile)
        btnDisHavaDetay = findViewById(R.id.btnDisHavaDetay)
        btnKombiAyarlar = findViewById(R.id.btnKombiAyarlar)
        tempProgressBar = findViewById(R.id.tempProgressBar)
        humidityProgressBar = findViewById(R.id.humidityProgressBar)
        tvGercekSicaklikCenter = findViewById(R.id.tvGercekSicaklikCenter)
        tvNemCenter = findViewById(R.id.tvNemCenter)
        tvCircularTempValue = findViewById(R.id.tvCircularTempValue)
        circularSeekBar = findViewById(R.id.circularSeekBar)
    }

    private fun setupListeners() {
        circularSeekBar.setOnChangeListener { value ->
            val intValue = value.toInt()
            tvCircularTempValue.text = "${intValue}°C"
            tvIstenenSicaklik.text = "${intValue}°C"

            Handler(Looper.getMainLooper()).postDelayed({
                setTargetTemperature(intValue)
            }, 300) // Daha kısa delay
        }

        btnKombiAc.setOnClickListener {
            println("🔘 Kombi AÇ butonuna tıklandı")
            setCombiState(true)
        }

        btnKombiKapat.setOnClickListener {
            println("🔘 Kombi KAPAT butonuna tıklandı")
            setCombiState(false)
        }

        btnYenile.setOnClickListener {
            println("🔘 Yenile butonuna tıklandı")
            loadKombiDataFast()
            loadOutsideWeatherData()
            Toast.makeText(this, "Veriler yenileniyor...", Toast.LENGTH_SHORT).show()
        }

        btnDisHavaDetay.setOnClickListener {
            val intent = Intent(this, WeatherActivity::class.java)
            intent.putExtra("user_email", userEmail)
            intent.putExtra("user_type", userType)
            intent.putExtra("user_name", userName)
            startActivity(intent)
        }

        btnKombiAyarlar.setOnClickListener {
            val intent = Intent(this, KombiSettingsActivity::class.java)
            startActivity(intent)
        }
    }

    // HIZLI VERİ YÜKLEME
    private fun loadKombiDataFast() {
        val kombiToken = getKombiToken()

        if (kombiToken.isEmpty() || kombiToken == "KOMBI_TOKEN_BURAYA") {
            showDemoKombiData()
            return
        }

        println("⚡ Kombi verileri HIZLI yükleniyor...")

        // ÖNEM SIRASINA GÖRE: Önce sıcaklık, sonra diğerleri
        loadCriticalDataFirst(kombiToken)
    }

    private fun loadCriticalDataFirst(token: String) {
        // 1. ÖNCE SICAKLIK (en kritik) - tek başına
        getBlynkDataFast(token, V_TEMP_ACTUAL) { temperature ->
            runOnUiThread {
                updateTemperatureUI(temperature)

                // 2. Sonra diğer verileri PARALEL yükle
                loadRemainingDataParallel(token)
            }
        }
    }

    private fun updateTemperatureUI(temperature: String) {
        try {
            val tempValue = temperature.toFloatOrNull() ?: 22f
            val intValue = tempValue.toInt()

            // Sadece değiştiyse güncelle (performans için)
            if (lastDisplayedTemp != intValue) {
                val progressValue = tempValue.coerceIn(0f, 40f)
                tempProgressBar.setProgress(progressValue)
                tvGercekSicaklikCenter.text = "${intValue}°C"
                lastDisplayedTemp = intValue

                // Hissedilen sıcaklığı hesapla (V2 yerine)
                val feelsLike = (tempValue * 1.05f).toInt()
                if (lastDisplayedFeelsLike != feelsLike) {
                    tvHissedilen.text = "${feelsLike}°C"
                    lastDisplayedFeelsLike = feelsLike
                }
            }
        } catch (e: Exception) {
            tvGercekSicaklikCenter.text = "${lastDisplayedTemp}°C"
        }
    }

    private fun loadRemainingDataParallel(token: String) {
        // Paralel olarak yükle
        executor.execute {
            // İstenen sıcaklık
            getBlynkDataFast(token, V_TEMP_TARGET) { targetTemp ->
                runOnUiThread {
                    updateTargetTemperatureUI(targetTemp)
                }
            }

            // Nem
            getBlynkDataFast(token, V_HUMIDITY) { humidity ->
                runOnUiThread {
                    updateHumidityUI(humidity)
                }
            }

            // Kombi durumu (en az önemli)
            getBlynkDataFast(token, V_COMBI_STATE) { combiState ->
                runOnUiThread {
                    updateCombiStateUI(combiState)
                }
            }
        }
    }

    private fun updateTargetTemperatureUI(targetTemp: String) {
        try {
            val targetValue = targetTemp.toFloatOrNull() ?: 21f
            val intValue = targetValue.toInt()

            if (lastDisplayedTargetTemp != intValue) {
                tvIstenenSicaklik.text = "${intValue}°C"
                tvCircularTempValue.text = "${intValue}°C"
                circularSeekBar.setValue(targetValue)
                lastDisplayedTargetTemp = intValue
            }
        } catch (e: Exception) {
            tvIstenenSicaklik.text = "${lastDisplayedTargetTemp}°C"
            tvCircularTempValue.text = "${lastDisplayedTargetTemp}°C"
        }
    }

    private fun updateHumidityUI(humidity: String) {
        try {
            val humidityValue = humidity.toFloatOrNull() ?: 45f
            val intValue = humidityValue.toInt()

            if (lastDisplayedHumidity != intValue) {
                humidityProgressBar.setProgress(humidityValue.coerceIn(0f, 100f))
                tvNemCenter.text = "${intValue}%"
                lastDisplayedHumidity = intValue
            }
        } catch (e: Exception) {
            tvNemCenter.text = "${lastDisplayedHumidity}%"
        }
    }

    private fun updateCombiStateUI(combiState: String) {
        try {
            val durumMetni = combiState.trim().uppercase()
            println("🔍 DEBUG: V32 pin'inden gelen veri: '$combiState' (trimmed/uppercase: '$durumMetni')")

            // Daha geniş karşılaştırma yapalım - "ÇALIŞIYOR" kelimesini ekleyin
            val isOn = when {
                durumMetni.contains("AÇIK") ||
                        durumMetni.contains("ACIK") ||
                        durumMetni.contains("ON") ||
                        durumMetni == "1" ||
                        durumMetni.contains("TRUE") ||
                        durumMetni.contains("ACTIVE") ||
                        durumMetni.contains("ÇALIŞIYOR") ||  // BU SATIRI EKLEYİN!
                        durumMetni.contains("CALISIYOR") ||  // Çsiz versiyonu da ekleyin
                        durumMetni.contains("RUNNING") -> true

                durumMetni.contains("KAPALI") ||
                        durumMetni.contains("OFF") ||
                        durumMetni == "0" ||
                        durumMetni.contains("FALSE") ||
                        durumMetni.contains("INACTIVE") ||
                        durumMetni.contains("DURDU") ||      // Durma durumu
                        durumMetni.contains("STOPPED") -> false

                else -> {
                    // Sayısal değer kontrolü
                    try {
                        val numericValue = durumMetni.toInt()
                        numericValue > 0
                    } catch (e: NumberFormatException) {
                        println("⚠️ DEBUG: Bilinmeyen format: '$durumMetni', varsayılan: KAPALI")
                        false
                    }
                }
            }

            val newState = if (isOn) "AÇIK" else "KAPALI"

            println("🔍 DEBUG: Durum yorumu: isOn=$isOn, newState=$newState, currentCombiState=$currentCombiState")

            // Her zaman UI'ı güncelle (durum değişmese bile text'i güncelleyelim)
            currentCombiState = newState

            if (isOn) {
                tvKombiDurumu.text = "KOMBİ AÇIK"
                tvKombiDurumu.setTextColor(android.graphics.Color.RED)
                btnKombiAc.isEnabled = false
                btnKombiKapat.isEnabled = true
                println("✅ UI güncellendi: KOMBİ AÇIK (Gelen veri: $durumMetni)")
            } else {
                tvKombiDurumu.text = "KOMBİ KAPALI"
                tvKombiDurumu.setTextColor(android.graphics.Color.GREEN)
                btnKombiAc.isEnabled = true
                btnKombiKapat.isEnabled = false
                println("✅ UI güncellendi: KOMBİ KAPALI (Gelen veri: $durumMetni)")
            }

        } catch (e: Exception) {
            println("❌ updateCombiStateUI hatası: ${e.message}")
            e.printStackTrace()
            tvKombiDurumu.text = "KOMBİ ${currentCombiState}"
        }
    }

    // OPTİMİZE EDİLMİŞ BLYNK DATA ÇEKME
    private fun getBlynkDataFast(token: String, virtualPin: String, callback: (String) -> Unit) {
        val url = "$BLYNK_BASE_URL/external/api/get?token=$token&$virtualPin"

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> {
                // Başarılı
                callback(it)
            },
            Response.ErrorListener { error ->
                // HATA - default değer dön
                val defaultValue = when (virtualPin) {
                    V_TEMP_ACTUAL -> lastDisplayedTemp.toString()
                    V_TEMP_TARGET -> lastDisplayedTargetTemp.toString()
                    V_HUMIDITY -> lastDisplayedHumidity.toString()
                    V_FEELS_LIKE -> lastDisplayedFeelsLike.toString()
                    V_COMBI_STATE -> "KOMBİ $currentCombiState"
                    else -> "0"
                }
                callback(defaultValue)
            }
        ) {
            override fun getRetryPolicy(): com.android.volley.RetryPolicy {
                // OPTİMİZE: 2 saniye timeout, RETRY YOK
                return DefaultRetryPolicy(2000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            }

            @Throws(com.android.volley.AuthFailureError::class)
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Connection"] = "close" // HTTP bağlantısını hemen kapat
                headers["Cache-Control"] = "no-cache" // Cache kullanma
                return headers
            }
        }

        getRequestQueue().add(stringRequest)
    }

    private fun checkKombiToken() {
        val kombiToken = getKombiToken()

        if (kombiToken.isEmpty() || kombiToken == "KOMBI_TOKEN_BURAYA") {
            Toast.makeText(this,
                "⚠ Kombi token'ı ayarlanmamış! Ayarlar butonuna tıklayın.",
                Toast.LENGTH_LONG
            ).show()
            showDemoKombiData()
        } else {
            println("✅ Kombi token bulundu")
        }
    }

    private fun loadOutsideWeatherData() {
        // Basit statik veri - hızlı olsun diye
        tvDisSicaklik.text = "18°C"
        tvDisNem.text = "48%"
        tvDisDurum.text = "Parçalı Bulutlu"
    }

    private fun setTargetTemperature(temperature: Int) {
        val kombiToken = getKombiToken()
        if (kombiToken.isEmpty() || kombiToken == "KOMBI_TOKEN_BURAYA") {
            Toast.makeText(this, "Önce kombi token'ını ayarlardan girin", Toast.LENGTH_LONG).show()
            return
        }

        val url = "$BLYNK_BASE_URL/external/api/update?token=$kombiToken&$V_TEMP_TARGET=$temperature"

        sendKombiCommandFast(url, "sıcaklık") { success, message ->
            if (success) {
                Toast.makeText(this, "✅ İstenen sıcaklık ayarlandı: $temperature°C", Toast.LENGTH_SHORT).show()
                // Hemen UI'ı güncelle
                lastDisplayedTargetTemp = temperature
                tvIstenenSicaklik.text = "${temperature}°C"
                tvCircularTempValue.text = "${temperature}°C"
            } else {
                Toast.makeText(this, "❌ Sıcaklık ayarlanamadı: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setCombiState(state: Boolean) {
        val kombiToken = getKombiToken()
        if (kombiToken.isEmpty() || kombiToken == "KOMBI_TOKEN_BURAYA") {
            Toast.makeText(this, "Önce kombi token'ını ayarlardan girin", Toast.LENGTH_LONG).show()
            return
        }

        val controlPin = V_COMBI_CONTROL
        val stateValue = if (state) 1 else 0
        val url = "$BLYNK_BASE_URL/external/api/update?token=$kombiToken&$controlPin=$stateValue"

        sendKombiCommandFast(url, "kombi") { success, message ->
            if (success) {
                val durum = if (state) "KOMBİ AÇILDI" else "KOMBİ KAPATILDI"

                // Hemen UI'ı güncelle
                if (state) {
                    currentCombiState = "AÇIK"
                    tvKombiDurumu.text = "KOMBİ AÇIK"
                    tvKombiDurumu.setTextColor(android.graphics.Color.RED)
                    btnKombiAc.isEnabled = false
                    btnKombiKapat.isEnabled = true
                } else {
                    currentCombiState = "KAPALI"
                    tvKombiDurumu.text = "KOMBİ KAPALI"
                    tvKombiDurumu.setTextColor(android.graphics.Color.GREEN)
                    btnKombiAc.isEnabled = true
                    btnKombiKapat.isEnabled = false
                }

                Toast.makeText(this, "✅ $durum", Toast.LENGTH_SHORT).show()

                // 2 saniye sonra verileri yenile
                Handler(Looper.getMainLooper()).postDelayed({ loadKombiDataFast() }, 2000)
            } else {
                Toast.makeText(this, "❌ Kombi durumu değiştirilemedi: $message", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendKombiCommandFast(url: String, commandType: String, callback: (Boolean, String) -> Unit) {
        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> {
                println("✅ Komut başarılı: $it")
                callback(true, "Başarılı")
            },
            Response.ErrorListener { error ->
                val statusCode = error.networkResponse?.statusCode ?: 0
                println("❌ Komut hatası: Status $statusCode")

                val message = when (statusCode) {
                    400 -> "Geçersiz token veya pin"
                    401 -> "Token geçersiz"
                    429 -> "Çok fazla istek - bekleyin"
                    else -> "Hata kodu: $statusCode"
                }
                callback(false, message)
            }
        ) {
            override fun getRetryPolicy(): com.android.volley.RetryPolicy {
                // HIZLI: 3 saniye timeout, 1 retry
                return DefaultRetryPolicy(3000, 1, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            }
        }

        getRequestQueue().add(stringRequest)
    }

    private fun getKombiToken(): String {
        val prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)
        return prefs.getString("kombi_token", "KOMBI_TOKEN_BURAYA") ?: "KOMBI_TOKEN_BURAYA"
    }

    private fun showDemoKombiData() {
        println("📱 Demo veriler gösteriliyor")

        // İlk değerleri set et
        lastDisplayedTemp = 22
        lastDisplayedHumidity = 45
        lastDisplayedTargetTemp = 21
        lastDisplayedFeelsLike = 24

        tempProgressBar.setProgress(22f)
        humidityProgressBar.setProgress(45f)
        circularSeekBar.setValue(21f)

        tvGercekSicaklikCenter.text = "22°C"
        tvNemCenter.text = "45%"
        tvIstenenSicaklik.text = "21°C"
        tvCircularTempValue.text = "21°C"
        tvHissedilen.text = "24°C"
        tvKombiDurumu.text = "KOMBİ KAPALI"
        tvKombiDurumu.setTextColor(android.graphics.Color.GREEN)

        btnKombiAc.isEnabled = true
        btnKombiKapat.isEnabled = false
        currentCombiState = "KAPALI"
    }

    private fun startAutoRefresh() {
        refreshRunnable = object : Runnable {
            override fun run() {
                if (isActive) {
                    loadKombiDataFast()
                    refreshHandler.postDelayed(this, 15000) // 15 saniye
                }
            }
        }
        refreshHandler.post(refreshRunnable!!)
    }

    private fun stopAutoRefresh() {
        refreshRunnable?.let {
            refreshHandler.removeCallbacks(it)
        }
    }

    override fun onResume() {
        super.onResume()
        isActive = true
        checkKombiToken()
        loadKombiDataFast() // Hemen yükle
        startAutoRefresh()
    }

    override fun onPause() {
        super.onPause()
        isActive = false
        stopAutoRefresh()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAutoRefresh()
        executor.shutdown() // Thread pool'u kapat
    }
}