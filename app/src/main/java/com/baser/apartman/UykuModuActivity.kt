package com.baser.apartman

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

// AppCompatActivity yerine BaseActivity'den türetiyoruz
class UykuModuActivity : BaseActivity() {

    private lateinit var switchAktif: SwitchMaterial
    private lateinit var editMesaj: EditText
    private lateinit var btnKaydet: Button
    private lateinit var btnIzinleriKontrol: Button
    private lateinit var btnWidgetEkle: Button
    private lateinit var tvDurum: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_uyku_modu)

        // BaseActivity'nin menüsünü kur
        setupNavigation()

        // Toolbar başlığını emoji ile ayarla
        supportActionBar?.title = "😴 Uyku Modu"

        initViews()
        loadSavedSettings()
        setupListeners()
        checkPermissions()
    }



    private fun initViews() {
        switchAktif = findViewById(R.id.switchUykuModuAktif)
        editMesaj = findViewById(R.id.editOtomatikMesaj)
        btnKaydet = findViewById(R.id.btnKaydet)
        btnIzinleriKontrol = findViewById(R.id.btnIzinleriKontrol)
        btnWidgetEkle = findViewById(R.id.btnWidgetEkle)
        tvDurum = findViewById(R.id.tvDurum)
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences("uyku_modu", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("aktif", false)
        switchAktif.isChecked = isActive
        editMesaj.setText(prefs.getString("otomatik_mesaj", "Şu anda uyku modundayım. Size daha sonra döneceğim."))

        updateDurumText(isActive)
    }

    private fun updateDurumText(isActive: Boolean) {
        if (isActive) {
            tvDurum.text = "🟢 Uyku Modu AKTİF - Gelen çağrılar reddedilecek"
            tvDurum.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            tvDurum.text = "⚪ Uyku Modu PASİF - Normal çağrı alımı"
            tvDurum.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun setupListeners() {
        btnKaydet.setOnClickListener {
            saveSettings()
        }

        btnIzinleriKontrol.setOnClickListener {
            requestPermissions()
        }

        btnWidgetEkle.setOnClickListener {
            Toast.makeText(this, "Ana ekrana uzun basıp 'Widget'lar' > 'Uyku Modu' seçin", Toast.LENGTH_LONG).show()
        }

        switchAktif.setOnCheckedChangeListener { _, isChecked ->
            updateDurumText(isChecked)

            if (isChecked) {
                // Tüm izinler var mı kontrol et
                if (checkPermissions()) {
                    startUykuModu()
                } else {
                    // İzin yoksa switch'i geri çevir ve izin iste
                    switchAktif.isChecked = false
                    requestPermissions()
                }
            } else {
                stopUykuModu()
            }
        }
    }

    private fun startUykuModu() {
        Toast.makeText(this, "Uyku modu aktif ediliyor...", Toast.LENGTH_SHORT).show()

        // Servisi başlat
        val intent = Intent(this, CagiYanitlaServisi::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        // Pil optimizasyonundan muaf tut
        requestBatteryOptimizationDisabled()

        // Ayarları kaydet
        val prefs = getSharedPreferences("uyku_modu", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("aktif", true).apply()

        Log.d("UykuModu", "✅ Uyku modu AKTİF")
    }

    private fun stopUykuModu() {
        Toast.makeText(this, "Uyku modu pasif ediliyor...", Toast.LENGTH_SHORT).show()

        // Servisi durdur
        val intent = Intent(this, CagiYanitlaServisi::class.java)
        stopService(intent)

        // Ayarları kaydet
        val prefs = getSharedPreferences("uyku_modu", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("aktif", false).apply()

        Log.d("UykuModu", "⏹️ Uyku modu PASİF")
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("uyku_modu", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("otomatik_mesaj", editMesaj.text.toString())
            apply()
        }
        Toast.makeText(this, "Mesaj kaydedildi", Toast.LENGTH_SHORT).show()
    }

    private fun requestBatteryOptimizationDisabled() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName

            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e("UykuModu", "Pil optimizasyonu hatası: ${e.message}")
                }
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.SEND_SMS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
        }

        val allPermissionsGranted = permissions.isEmpty()

        if (!allPermissionsGranted) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), PERMISSION_REQUEST_CODE)
        }

        return allPermissionsGranted
    }

    private fun requestPermissions() {
        checkPermissions()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (i in permissions.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    Toast.makeText(this, "${permissions[i]} izni gerekli", Toast.LENGTH_SHORT).show()
                }
            }

            if (allGranted && switchAktif.isChecked) {
                startUykuModu()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Sayfa her açıldığında güncel durumu kontrol et
        val prefs = getSharedPreferences("uyku_modu", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("aktif", false)
        switchAktif.isChecked = isActive
        updateDurumText(isActive)
    }
}