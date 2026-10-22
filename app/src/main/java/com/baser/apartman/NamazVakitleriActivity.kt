package com.baser.apartman

import android.Manifest
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import java.text.SimpleDateFormat
import java.util.*
import com.baser.apartman.widgets.UykuModuWidget
class NamazVakitleriActivity : BaseActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar

    private lateinit var switchOtomatik: SwitchMaterial
    private lateinit var spinnerIl: Spinner
    private lateinit var spinnerIlce: Spinner
    private lateinit var sliderSure: Slider
    private lateinit var sliderErkenSure: Slider
    private lateinit var tvSureDegeri: TextView
    private lateinit var tvErkenSureDegeri: TextView
    private lateinit var btnKaydet: MaterialButton
    private lateinit var btnIzinVer: MaterialButton
    private lateinit var tvDurum: TextView
    private lateinit var btnKonumAl: MaterialButton
    private lateinit var btnManuelSec: MaterialButton
    private lateinit var layoutManuel: LinearLayout
    private lateinit var layoutKonum: LinearLayout
    private lateinit var btnTestAc: Button
    private lateinit var btnTestKapat: Button
    private lateinit var btnSimdiTest: Button
    private lateinit var btnVakitleriGoster: Button

    private var iller: List<String> = Sehirler.iller
    private var ilceler: List<String> = listOf()
    private lateinit var konumServisi: KonumServisi

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 300
        private const val TAG = "NamazVakitleri"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_namaz_vakitleri)

        setupMenu()
        setupBottomNavigation()
        konumServisi = KonumServisi(this)
        initViews()
        setupSpinners()
        loadSettings()
        setupListeners()
    }

    override fun setupBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)

        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_dashboard -> {
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_duyurular -> {
                    startActivity(Intent(this, DuyurularActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_aidat -> {
                    startActivity(Intent(this, AidatActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_sikayetler -> {
                    startActivity(Intent(this, SikayetlerActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupMenu() {
        // BaseActivity.setupNavigation: toolbar + drawer + TÜM menü öğeleri
        // (diğer sayfalarla aynı ortak yapı; her öğe user_email/user_type/user_name ile açılır)
        setupNavigation()
        supportActionBar?.title = "🕌 Namaz Vakitleri"
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        switchOtomatik = findViewById(R.id.switchOtomatik)
        spinnerIl = findViewById(R.id.spinnerIl)
        spinnerIlce = findViewById(R.id.spinnerIlce)
        sliderSure = findViewById(R.id.sliderSure)
        sliderErkenSure = findViewById(R.id.sliderErkenSure)
        tvSureDegeri = findViewById(R.id.tvSureDegeri)
        tvErkenSureDegeri = findViewById(R.id.tvErkenSureDegeri)
        btnKaydet = findViewById(R.id.btnKaydet)
        btnIzinVer = findViewById(R.id.btnIzinVer)
        tvDurum = findViewById(R.id.tvDurum)
        btnKonumAl = findViewById(R.id.btnKonumAl)
        btnManuelSec = findViewById(R.id.btnManuelSec)
        layoutManuel = findViewById(R.id.layoutManuel)
        layoutKonum = findViewById(R.id.layoutKonum)
        btnTestAc = findViewById(R.id.btnTestAc)
        btnTestKapat = findViewById(R.id.btnTestKapat)
        btnSimdiTest = findViewById(R.id.btnSimdiTest)
        btnVakitleriGoster = findViewById(R.id.btnVakitleriGoster)

        sliderSure.addOnChangeListener { _, value, _ ->
            tvSureDegeri.text = "${value.toInt()} dakika"
        }

        sliderErkenSure.addOnChangeListener { _, value, _ ->
            tvErkenSureDegeri.text = "${value.toInt()} dakika önce"
        }
    }

    private fun setupSpinners() {
        val ilAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, iller)
        ilAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIl.adapter = ilAdapter

        spinnerIl.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val secilenIl = iller[position]
                updateIlceler(secilenIl)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateIlceler(il: String) {
        ilceler = Sehirler.getIlceler(il)
        val ilceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, ilceler)
        ilceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIlce.adapter = ilceAdapter
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)

        switchOtomatik.isChecked = prefs.getBoolean("otomatik_aktif", false)

        val kayitliIl = prefs.getString("il", "MERSİN")
        val ilIndex = iller.indexOf(kayitliIl)
        if (ilIndex >= 0) {
            spinnerIl.setSelection(ilIndex)
        }

        val kayitliIlce = prefs.getString("ilce", "Yenişehir")

        val sure = prefs.getInt("dnd_suresi", 30)
        sliderSure.value = sure.toFloat()
        tvSureDegeri.text = "$sure dakika"

        val erkenSure = prefs.getInt("erken_baslatma_suresi", 5)
        sliderErkenSure.value = erkenSure.toFloat()
        tvErkenSureDegeri.text = "$erkenSure dakika önce"

        updateDurum()

        spinnerIl.post {
            val ilceIndex = ilceler.indexOf(kayitliIlce)
            if (ilceIndex >= 0) {
                spinnerIlce.setSelection(ilceIndex)
            }
        }
    }

    private fun setupListeners() {
        btnKaydet.setOnClickListener {
            saveSettings()
        }

        btnIzinVer.setOnClickListener {
            requestDndPermission()
        }

        btnKonumAl.setOnClickListener {
            checkLocationPermissionAndGetLocation()
        }

        btnManuelSec.setOnClickListener {
            showManualSelection()
        }

        btnTestAc.setOnClickListener {
            testRahatsizEtmeAc()
        }

        btnTestKapat.setOnClickListener {
            testRahatsizEtmeKapat()
        }

        btnSimdiTest.setOnClickListener {
            testAlarmTetikle()
        }

        btnVakitleriGoster.setOnClickListener {
            fetchAndShowNamazVakitleri()
        }

        switchOtomatik.setOnCheckedChangeListener { _, isChecked ->
            val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("otomatik_aktif", isChecked).apply()

            if (isChecked) {
                checkExactAlarmPermission()
                startNamazVaktiServisi()
                stopRahatsizEtmeServisi()
                NamazVaktiHelper.refreshVakitlerFromApi(this)
                Toast.makeText(this, "🕌 Otomatik kontrol aktif - Namaz vakitlerinde rahatsız etme modu açılacak", Toast.LENGTH_LONG).show()
            } else {
                NamazVaktiHelper.cancelAllAlarms(this)
                stopNamazVaktiServisi()
                stopRahatsizEtmeServisi()
                stopGeriSayimServisi()

                val dndPrefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
                dndPrefs.edit().putBoolean("aktif", false).apply()

                updateWidgetToPassive()

                Toast.makeText(this, "⚪ Otomatik kontrol pasif - Rahatsız etme modu tamamen kapalı", Toast.LENGTH_LONG).show()
            }
            updateDurum()
        }
    }

    private fun showManualSelection() {
        layoutManuel.visibility = View.VISIBLE
        layoutKonum.visibility = View.GONE
        btnManuelSec.isEnabled = false
        btnKonumAl.isEnabled = true
    }

    private fun testRahatsizEtmeAc() {
        val intent = Intent(this, RahatsizEtmeServisi::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
            .edit().putBoolean("aktif", true).apply()
        Toast.makeText(this, "TEST: Rahatsız etme AÇILDI", Toast.LENGTH_SHORT).show()
    }

    private fun testRahatsizEtmeKapat() {
        val intent = Intent(this, RahatsizEtmeServisi::class.java)
        stopService(intent)
        getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
            .edit().putBoolean("aktif", false).apply()
        Toast.makeText(this, "TEST: Rahatsız etme KAPATILDI", Toast.LENGTH_SHORT).show()
    }

    private fun testAlarmTetikle() {
        Log.d(TAG, "🔴 TEST ALARM TETİKLENİYOR")
        val intent = Intent(this, NamazVaktiReceiver::class.java).apply {
            action = "ACMA_Test"
            putExtra("vakit", "Test Vakti")
            putExtra("dnd_suresi", 1)
        }
        sendBroadcast(intent)
        Toast.makeText(this, "TEST: Alarm gönderildi", Toast.LENGTH_SHORT).show()
    }

    private fun fetchAndShowNamazVakitleri() {
        val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val il = prefs.getString("il", "MERSİN") ?: "MERSİN"
        val ilce = prefs.getString("ilce", "Yenişehir") ?: "Yenişehir"

        Toast.makeText(this, "Vakitler alınıyor...", Toast.LENGTH_SHORT).show()

        NamazVaktiApi(this).getBugununVakti(
            il = il,
            ilce = ilce,
            onSuccess = { vakit ->
                runOnUiThread {
                    showNamazVakitleriModal(vakit)
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Hata: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun showNamazVakitleriModal(vakit: NamazVakti) {
        val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val erkenSure = prefs.getInt("erken_baslatma_suresi", 5)

        val mesaj = """
            
            📍 ${vakit.ilce} - MERSİN
            
            ▬▬▬▬▬▬▬▬▬▬▬▬
            🌙 İmsak     : ${vakit.imsak}
            ☀️ Güneş     : ${vakit.gunes}
            🕰 Öğle      : ${vakit.ogle}
            🕰 İkindi    : ${vakit.ikindi}
            🌅 Akşam     : ${vakit.aksam}
            🌃 Yatsı     : ${vakit.yatsi}
            ▬▬▬▬▬▬▬▬▬▬▬▬
            
            ⏰ Erken başlatma: ${erkenSure} dakika önce
            📅 Tarih: ${vakit.tarih}
        """.trimIndent()

        val dialog = AlertDialog.Builder(this)
            .setTitle("🕌 Bugünün Namaz Vakitleri")
            .setMessage(mesaj)
            .setPositiveButton("Tamam", null)
            .setNegativeButton("Alarm Kur") { _, _ ->
                if (switchOtomatik.isChecked) {
                    Toast.makeText(this, "Zaten otomatik mod aktif", Toast.LENGTH_SHORT).show()
                } else {
                    switchOtomatik.isChecked = true
                    startNamazVaktiServisi()
                }
            }
            .create()

        dialog.show()
        dialog.window?.setBackgroundDrawableResource(android.R.color.white)
    }

    private fun checkLocationPermissionAndGetLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            getLocationAndUpdate()
        }
    }

    private fun getLocationAndUpdate() {
        Toast.makeText(this, "Konum alınıyor...", Toast.LENGTH_SHORT).show()

        konumServisi.getCurrentLocation { il, ilce ->
            runOnUiThread {
                val secilecekIl = il ?: "MERSİN"
                val secilecekIlce = ilce ?: "Yenişehir"

                val ilIndex = iller.indexOf(secilecekIl)
                if (ilIndex >= 0) {
                    spinnerIl.setSelection(ilIndex)

                    spinnerIl.postDelayed({
                        val ilcelerList = Sehirler.getIlceler(secilecekIl)
                        val ilceIndex = ilcelerList.indexOf(secilecekIlce)
                        if (ilceIndex >= 0) {
                            spinnerIlce.setSelection(ilceIndex)
                            Toast.makeText(this, "Konum: $secilecekIl - $secilecekIlce", Toast.LENGTH_SHORT).show()
                        } else {
                            spinnerIlce.setSelection(0)
                            Toast.makeText(this, "İlçe bulunamadı, varsayılan seçildi", Toast.LENGTH_SHORT).show()
                        }
                    }, 500)
                } else {
                    val mersinIndex = iller.indexOf("MERSİN")
                    spinnerIl.setSelection(mersinIndex)
                    Toast.makeText(this, "İl bulunamadı, Mersin seçildi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocationAndUpdate()
            } else {
                Toast.makeText(this, "Konum izni gerekli", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)

        val il = spinnerIl.selectedItem?.toString() ?: "MERSİN"
        val ilce = spinnerIlce.selectedItem?.toString() ?: "Yenişehir"
        val sure = sliderSure.value.toInt()
        val erkenSure = sliderErkenSure.value.toInt()

        prefs.edit().apply {
            putBoolean("otomatik_aktif", switchOtomatik.isChecked)
            putString("il", il)
            putString("ilce", ilce)
            putInt("dnd_suresi", sure)
            putInt("erken_baslatma_suresi", erkenSure)
            apply()
        }

        Toast.makeText(this, "Ayarlar kaydedildi: $il - $ilce (Erken: $erkenSure dk)", Toast.LENGTH_LONG).show()

        if (switchOtomatik.isChecked) {
            checkExactAlarmPermission()
            stopNamazVaktiServisi()
            startNamazVaktiServisi()
            NamazVaktiHelper.refreshVakitlerFromApi(this)
        }
    }

    private fun startNamazVaktiServisi() {
        val intent = Intent(this, NamazVaktiServisi::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Log.d(TAG, "Namaz servisi başlatıldı")
    }

    private fun stopNamazVaktiServisi() {
        val intent = Intent(this, NamazVaktiServisi::class.java)
        stopService(intent)
        Log.d(TAG, "Namaz servisi durduruldu")
    }

    private fun stopRahatsizEtmeServisi() {
        val intent = Intent(this, RahatsizEtmeServisi::class.java)
        stopService(intent)
        Log.d(TAG, "Rahatsız etme servisi durduruldu")
    }

    private fun stopGeriSayimServisi() {
        val intent = Intent(this, GeriSayimServisi::class.java)
        stopService(intent)
        Log.d(TAG, "Geri sayım servisi durduruldu")
    }

    private fun updateWidgetToPassive() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = ComponentName(this, UykuModuWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = android.widget.RemoteViews(packageName, R.layout.widget_uyku_modu)
                views.setTextViewText(R.id.tvWidgetDurum, "⏰ Otomatik Kontrol KAPALI")
                views.setInt(R.id.widgetLayout, "setBackgroundColor", android.graphics.Color.parseColor("#757575"))
                views.setViewVisibility(R.id.tvGeriSayim, android.view.View.GONE)
                views.setViewVisibility(R.id.tvSonrakiVakit, android.view.View.VISIBLE)
                views.setTextViewText(R.id.tvSonrakiVakit, "Namaz vakitleri kapalı")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Widget güncelleme hatası: ${e.message}")
        }
    }

    private fun requestDndPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Rahatsız etme izni zaten verilmiş", Toast.LENGTH_SHORT).show()
            }
        }
        checkExactAlarmPermission()
    }

    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(this, "Namaz vakitlerinde sessize alma için alarm izni veriniz", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Exact alarm intent hatası: ${e.message}")
                }
            }
        }
    }

    private fun updateDurum() {
        if (switchOtomatik.isChecked) {
            tvDurum.text = "🟢 Otomatik mod AKTİF"
            tvDurum.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        } else {
            tvDurum.text = "⚪ Otomatik mod PASİF"
            tvDurum.setBackgroundColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onResume() {
        super.onResume()
        updateDurum()
    }
}