package com.baser.apartman

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial

class RahatsizEtmeModuActivity : BaseActivity() {  // BaseActivity'den türetildi

    private lateinit var switchAktif: SwitchMaterial
    private lateinit var editMesaj: EditText
    private lateinit var btnKaydet: Button
    private lateinit var btnIzinleriKontrol: Button
    private lateinit var btnDndIzin: Button
    private lateinit var tvDurum: TextView
    private lateinit var notificationManager: NotificationManager

    companion object {
        private const val PERMISSION_REQUEST_CODE = 200
        private const val DND_SETTINGS_REQUEST_CODE = 201
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rahatsiz_etme_modu)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // BaseActivity'nin menüsünü kur
        setupNavigation()
        supportActionBar?.title = "🔕 Rahatsız Etme Modu"

        initViews()
        loadSavedSettings()
        setupListeners()
        checkPermissions()
    }



    private fun initViews() {
        switchAktif = findViewById(R.id.switchAktif)
        editMesaj = findViewById(R.id.editOtomatikMesaj)
        btnKaydet = findViewById(R.id.btnKaydet)
        btnIzinleriKontrol = findViewById(R.id.btnIzinleriKontrol)
        btnDndIzin = findViewById(R.id.btnDndIzin)
        tvDurum = findViewById(R.id.tvDurum)
    }

    private fun loadSavedSettings() {
        val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("aktif", false)
        switchAktif.isChecked = isActive
        editMesaj.setText(prefs.getString("otomatik_mesaj", "Şu anda rahatsız etme modundayım. Size daha sonra döneceğim."))

        updateDurumText(isActive)
        updateDndButtonState()
    }

    private fun updateDurumText(isActive: Boolean) {
        if (isActive) {
            tvDurum.text = "🔴 Rahatsız Etme Modu AKTİF - Telefon sessiz, SMS gönderiliyor"
            tvDurum.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            tvDurum.text = "⚪ Rahatsız Etme Modu PASİF - Normal mod"
            tvDurum.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    private fun updateDndButtonState() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (notificationManager.isNotificationPolicyAccessGranted) {
                btnDndIzin.text = "✓ Rahatsız Etme İzni Verildi"
                btnDndIzin.isEnabled = false
            } else {
                btnDndIzin.text = "Rahatsız Etme İznini Ver"
                btnDndIzin.isEnabled = true
            }
        }
    }

    private fun setupListeners() {
        btnKaydet.setOnClickListener {
            saveSettings()
        }

        btnIzinleriKontrol.setOnClickListener {
            requestPermissions()
        }

        btnDndIzin.setOnClickListener {
            requestDndPermission()
        }

        switchAktif.setOnCheckedChangeListener { _, isChecked ->
            updateDurumText(isChecked)

            if (isChecked) {
                if (checkPermissions() && checkDndPermission()) {
                    startRahatsizEtmeModu()
                } else {
                    switchAktif.isChecked = false
                    Toast.makeText(this, "Önce tüm izinleri vermelisiniz", Toast.LENGTH_SHORT).show()
                }
            } else {
                stopRahatsizEtmeModu()
            }
        }
    }

    private fun startRahatsizEtmeModu() {
        Toast.makeText(this, "Rahatsız etme modu aktif ediliyor...", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, RahatsizEtmeServisi::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("aktif", true).apply()

        Log.d("RahatsizEtmeModu", "✅ Mod AKTİF")
    }

    private fun stopRahatsizEtmeModu() {
        Toast.makeText(this, "Rahatsız etme modu pasif ediliyor...", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, RahatsizEtmeServisi::class.java)
        stopService(intent)

        val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("aktif", false).apply()

        Log.d("RahatsizEtmeModu", "⏹️ Mod PASİF")
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("otomatik_mesaj", editMesaj.text.toString())
            apply()
        }
        Toast.makeText(this, "Mesaj kaydedildi", Toast.LENGTH_SHORT).show()
    }

    private fun checkDndPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return notificationManager.isNotificationPolicyAccessGranted
        }
        return true
    }

    private fun requestDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivityForResult(intent, DND_SETTINGS_REQUEST_CODE)
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
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == DND_SETTINGS_REQUEST_CODE) {
            updateDndButtonState()
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("aktif", false)
        switchAktif.isChecked = isActive
        updateDurumText(isActive)
        updateDndButtonState()
    }
}