package com.baser.apartman

import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

// Zaten BaseActivity'den türetilmiş, bu doğru!
class EspSettingsActivity : BaseActivity() {

    private lateinit var tvEspMenuStatus: TextView
    private lateinit var tvEspInfo: TextView
    private lateinit var btnEspEnterMenu: Button
    private lateinit var btnEspExitMenu: Button
    private lateinit var btnEspNextItem: Button
    private lateinit var btnEspExecute: Button
    private lateinit var btnSaveSettings: Button

    // SeekBars
    private lateinit var sbTempCorrection: SeekBar
    private lateinit var sbHysteresis: SeekBar
    private lateinit var sbMinTemp: SeekBar
    private lateinit var sbMaxTemp: SeekBar

    // TextViews for values
    private lateinit var tvTempCorrection: TextView
    private lateinit var tvHysteresis: TextView
    private lateinit var tvMinTemp: TextView
    private lateinit var tvMaxTemp: TextView

    // Switches
    private lateinit var switchPerceivedTemp: Switch
    private lateinit var switchHomeAssistant: Switch
    private lateinit var switchScreenState: Switch
    private lateinit var switchWinterMode: Switch

    companion object {
        private const val V_MENU_SETTINGS = "V4"
        private const val V_MENU_NEXT = "V5"
        private const val V_MENU_STATUS = "V10"

        private const val V_EEPROM_TEMP_CORRECTION = "V20"
        private const val V_EEPROM_HYSTERESIS = "V21"
        private const val V_EEPROM_MIN_TEMP = "V22"
        private const val V_EEPROM_MAX_TEMP = "V23"
        private const val V_EEPROM_PERCEIVED_TEMP = "V24"
        private const val V_EEPROM_HOME_ASSISTANT = "V25"
        private const val V_EEPROM_SCREEN_STATE = "V26"
        private const val V_EEPROM_WINTER_MODE = "V27"

        private const val BLYNK_BASE_URL = "https://fra1.blynk.cloud"
    }

    private var isInMenu = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_esp_settings)

        // BaseActivity'nin menüsünü kur
        setupNavigation()

        // Toolbar başlığını emoji ile ayarla
        supportActionBar?.title = "🔧 ESP Ayarlar"

        initViews()
        setupListeners()
        loadCurrentSettings()
    }



    private fun initViews() {
        tvEspMenuStatus = findViewById(R.id.tvEspMenuStatus)
        tvEspInfo = findViewById(R.id.tvEspInfo)
        btnEspEnterMenu = findViewById(R.id.btnEspEnterMenu)
        btnEspExitMenu = findViewById(R.id.btnEspExitMenu)
        btnEspNextItem = findViewById(R.id.btnEspNextItem)
        btnEspExecute = findViewById(R.id.btnEspExecute)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)

        // SeekBars
        sbTempCorrection = findViewById(R.id.sbTempCorrection)
        sbHysteresis = findViewById(R.id.sbHysteresis)
        sbMinTemp = findViewById(R.id.sbMinTemp)
        sbMaxTemp = findViewById(R.id.sbMaxTemp)

        // TextViews
        tvTempCorrection = findViewById(R.id.tvTempCorrection)
        tvHysteresis = findViewById(R.id.tvHysteresis)
        tvMinTemp = findViewById(R.id.tvMinTemp)
        tvMaxTemp = findViewById(R.id.tvMaxTemp)

        // Switches
        switchPerceivedTemp = findViewById(R.id.switchPerceivedTemp)
        switchHomeAssistant = findViewById(R.id.switchHomeAssistant)
        switchScreenState = findViewById(R.id.switchScreenState)
        switchWinterMode = findViewById(R.id.switchWinterMode)
    }

    private fun setupListeners() {
        // ESP Menu Control
        btnEspEnterMenu.setOnClickListener { enterEspMenu() }
        btnEspExitMenu.setOnClickListener { exitEspMenu() }
        btnEspNextItem.setOnClickListener { nextMenuItem() }
        btnEspExecute.setOnClickListener { executeMenuItem() }
        btnSaveSettings.setOnClickListener { saveDirectSettings() }

        // SeekBar listeners
        sbTempCorrection.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val correctedValue = progress - 5
                tvTempCorrection.text = "Düzeltme: ${correctedValue}°C"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        sbHysteresis.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                tvHysteresis.text = "Histerezis: ${progress}°C"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        sbMinTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val minTemp = progress + 10
                tvMinTemp.text = "Min: ${minTemp}°C"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        sbMaxTemp.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                val maxTemp = progress + 20
                tvMaxTemp.text = "Max: ${maxTemp}°C"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
    }

    private fun loadCurrentSettings() {
        val kombiToken = getKombiToken()
        if (kombiToken.isEmpty() || kombiToken == "KOMBI_TOKEN_BURAYA") {
            tvEspInfo.text = "⚠ Token ayarlanmamış. Önce ayarlardan token girin."
            return
        }

        tvEspInfo.text = "ESP bağlantısı kuruluyor..."
        updateUI()
    }

    private fun enterEspMenu() {
        simulateLongPressV4()
        isInMenu = true
        updateUI()
        Toast.makeText(this, "ESP Menüye girildi", Toast.LENGTH_SHORT).show()
    }

    private fun exitEspMenu() {
        simulateLongPressV4()
        isInMenu = false
        updateUI()
        Toast.makeText(this, "ESP Menüden çıkıldı", Toast.LENGTH_SHORT).show()
    }

    private fun nextMenuItem() {
        if (!isInMenu) {
            Toast.makeText(this, "Önce menüye girin", Toast.LENGTH_SHORT).show()
            return
        }

        sendEspCommand(V_MENU_NEXT, 1) { success ->
            if (success) {
                loadMenuStatus()
            }
        }
    }

    private fun executeMenuItem() {
        if (!isInMenu) {
            Toast.makeText(this, "Önce menüye girin", Toast.LENGTH_SHORT).show()
            return
        }

        sendEspCommand(V_MENU_NEXT, 1) { success ->
            if (success) {
                loadMenuStatus()
            }
        }
    }

    private fun simulateLongPressV4() {
        sendEspCommand(V_MENU_SETTINGS, 1) { success ->
            if (success) {
                android.os.Handler().postDelayed({
                    sendEspCommand(V_MENU_SETTINGS, 0) { _ ->
                        loadMenuStatus()
                    }
                }, 400)
            }
        }
    }

    private fun loadMenuStatus() {
        val kombiToken = getKombiToken()
        if (kombiToken.isEmpty() || kombiToken == "KOMBI_TOKEN_BURAYA") {
            tvEspMenuStatus.text = "Token ayarlanmamış"
            return
        }

        val url = "$BLYNK_BASE_URL/external/api/get?token=$kombiToken&$V_MENU_STATUS"

        val stringRequest = StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> { response ->
                tvEspMenuStatus.text = "Menü: $response"
            },
            Response.ErrorListener { error ->
                tvEspMenuStatus.text = "Hata: ${error.networkResponse?.statusCode}"
            }
        )

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun saveDirectSettings() {
        val kombiToken = getKombiToken()
        if (kombiToken.isEmpty() || kombiToken == "KOMBI_TOKEN_BURAYA") {
            Toast.makeText(this, "Önce kombi token'ını ayarlardan girin", Toast.LENGTH_LONG).show()
            return
        }

        val tempCorrection = sbTempCorrection.progress - 5
        val hysteresis = sbHysteresis.progress
        val minTemp = sbMinTemp.progress + 10
        val maxTemp = sbMaxTemp.progress + 20
        val perceivedTemp = if (switchPerceivedTemp.isChecked) 1 else 0
        val homeAssistant = if (switchHomeAssistant.isChecked) 1 else 0
        val screenState = if (switchScreenState.isChecked) 1 else 0
        val winterMode = if (switchWinterMode.isChecked) 1 else 0

        var successCount = 0
        val totalSettings = 8

        sendEspCommand(V_EEPROM_TEMP_CORRECTION, tempCorrection) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        sendEspCommand(V_EEPROM_HYSTERESIS, hysteresis) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        sendEspCommand(V_EEPROM_MIN_TEMP, minTemp) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        sendEspCommand(V_EEPROM_MAX_TEMP, maxTemp) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        sendEspCommand(V_EEPROM_PERCEIVED_TEMP, perceivedTemp) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        sendEspCommand(V_EEPROM_HOME_ASSISTANT, homeAssistant) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        sendEspCommand(V_EEPROM_SCREEN_STATE, screenState) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        sendEspCommand(V_EEPROM_WINTER_MODE, winterMode) { success ->
            if (success) successCount++
            checkAllSaved(successCount, totalSettings)
        }

        Toast.makeText(this, "Ayarlar ESP'ye gönderiliyor...", Toast.LENGTH_SHORT).show()
    }

    private fun checkAllSaved(savedCount: Int, totalCount: Int) {
        if (savedCount == totalCount) {
            runOnUiThread {
                Toast.makeText(this, "✅ Tüm ayarlar kaydedildi!", Toast.LENGTH_SHORT).show()
                tvEspInfo.text = "Ayarlar başarıyla kaydedildi. ESP yeniden başlatılabilir."
            }
        }
    }

    private fun sendEspCommand(pin: String, value: Int, callback: (Boolean) -> Unit = {}) {
        val kombiToken = getKombiToken()
        val url = "$BLYNK_BASE_URL/external/api/update?token=$kombiToken&$pin=$value"

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            Response.Listener<String> {
                println("✅ ESP Komut: $pin=$value başarılı")
                callback(true)
            },
            Response.ErrorListener { error ->
                val statusCode = error.networkResponse?.statusCode ?: 0
                println("❌ ESP Komut hatası: $pin=$value, Status: $statusCode")
                runOnUiThread {
                    Toast.makeText(this, "$pin gönderilemedi: $statusCode", Toast.LENGTH_SHORT).show()
                }
                callback(false)
            }
        ) {
            override fun getRetryPolicy(): com.android.volley.RetryPolicy {
                return DefaultRetryPolicy(10000, 2, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun updateUI() {
        btnEspEnterMenu.isEnabled = !isInMenu
        btnEspExitMenu.isEnabled = isInMenu
        btnEspNextItem.isEnabled = isInMenu
        btnEspExecute.isEnabled = isInMenu

        if (isInMenu) {
            tvEspMenuStatus.text = "Menü Aktif - Ayarları değiştirebilirsiniz"
        } else {
            tvEspMenuStatus.text = "Menü Kapalı - Girmek için 'ESP Menüye Gir'"
        }
    }

    private fun getKombiToken(): String {
        val prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)
        return prefs.getString("kombi_token", "KOMBI_TOKEN_BURAYA") ?: "KOMBI_TOKEN_BURAYA"
    }
}