package com.baser.apartman

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.navigation.NavigationView
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : BaseActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalDues: TextView
    private lateinit var tvPendingComplaints: TextView
    private lateinit var tvTotalAnnouncements: TextView
    private lateinit var tvOverdueDues: TextView
    private lateinit var tvTotalLateFees: TextView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var btnRefresh: Button
    private lateinit var btnGarajAc: Button

    // Coğrafi çember için
    private lateinit var tvDistance: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var tvGeofenceStatus: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isTracking = false
    private var isGarajOpened = false

    // SharedPreferences
    private lateinit var prefs: SharedPreferences

    // Bottom navigation butonları
    private lateinit var btnNavDashboard: Button
    private lateinit var btnNavDuyurular: Button
    private lateinit var btnNavAidat: Button
    private lateinit var btnNavSikayetler: Button

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    // Konum izinleri için
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    // Getter metodları - SharedPreferences'tan değerleri al
    private fun getBlynkToken(): String {
        return prefs.getString("blynk_token", "SİZİN_BLYNK_TOKEN_BURAYA") ?: "SİZİN_BLYNK_TOKEN_BURAYA"
    }

    private fun getBlynkPin(): String {
        return prefs.getString("blynk_pin", "V1") ?: "V1"
    }

    private val GARAJ_LATITUDE: Double
        get() = prefs.getString("garaj_lat", "36.7917535")?.toDoubleOrNull() ?: 36.7917535

    private val GARAJ_LONGITUDE: Double
        get() = prefs.getString("garaj_lon", "34.5916286")?.toDoubleOrNull() ?: 34.5916286

    private val GEOFENCE_RADIUS: Double
        get() = prefs.getString("geofence_radius", "50.0")?.toDoubleOrNull() ?: 50.0

    private fun getApiKey(): String {
        return prefs.getString("api_key", "apartman_secret_key_2024") ?: "apartman_secret_key_2024"
    }

    private fun getApiUrl(): String {
        return prefs.getString("api_url", "http://baser.org/apartman/api/api_dashboard_stats.php")
            ?: "http://baser.org/apartman/api/api_dashboard_stats.php"
    }

    // Konum hızı ayarını al
    private fun getLocationSpeed(): String {
        return prefs.getString("location_speed", "medium") ?: "medium"
    }

    // Otomatik garaj açma aktif mi kontrol et
    private fun isAutoGarageEnabled(): Boolean {
        return prefs.getBoolean("auto_garage_enabled", true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // SharedPreferences başlat
        prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)

        // Kullanıcı bilgilerini BaseActivity'den al (artık protected metodlar)
        // userEmail, userType, userName BaseActivity'den geliyor

        initViews()
        setupNavigation() // BaseActivity'den geliyor
        setupCustomBottomNavigation()
        setupDashboardButtons()
        setupGarajButton()
        setupGeofencingUI()
        checkLocationPermission()
        loadDashboardData()

        // Garaj servisini başlat
        startGarajService()
    }
    override fun setupNavigation() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

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
        supportActionBar?.title = "Dashboard"

        println("✅ Hamburger ikonu ayarlandı")

        // Navigation listener - BaseActivity'den geliyor
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

    private fun startGarajService() {
        if (hasLocationPermission()) {
            val serviceIntent = Intent(this, GarajService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            println("🔛 Garaj servisi başlatıldı - Arka planda çalışacak")
        } else {
            println("❌ Konum izni olmadığı için servis başlatılamadı")
        }
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvTotalDues = findViewById(R.id.tvTotalDues)
        tvPendingComplaints = findViewById(R.id.tvPendingComplaints)
        tvTotalAnnouncements = findViewById(R.id.tvTotalAnnouncements)
        tvOverdueDues = findViewById(R.id.tvOverdueDues)
        tvTotalLateFees = findViewById(R.id.tvTotalLateFees)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnGarajAc = findViewById(R.id.btnGarajAc)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Bottom navigation butonları
        btnNavDashboard = findViewById(R.id.btnNavDashboard)
        btnNavDuyurular = findViewById(R.id.btnNavDuyurular)
        btnNavAidat = findViewById(R.id.btnNavAidat)
        btnNavSikayetler = findViewById(R.id.btnNavSikayetler)

        // Hoşgeldin mesajını göster
        val welcomeText = when (userType) {
            "admin" -> "Yönetici Paneli - Hoşgeldiniz $userName!"
            "manager" -> "Site Sorumlusu Paneli - Hoşgeldiniz $userName!"
            else -> "Hoşgeldiniz $userName!"
        }
        tvWelcome.text = welcomeText

        // Yenile butonu
        btnRefresh.setOnClickListener {
            loadDashboardData()
        }

        // Fused Location Client'ı başlat
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupGeofencingUI() {
        // Garaj butonunun bulunduğu layout'u bul
        val garajButtonParent = btnGarajAc.parent as? LinearLayout
        garajButtonParent?.let { parent ->
            // Mesafe bilgisi için TextView oluştur
            tvDistance = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 8, 16, 8)
                }
                text = "Garaja uzaklık: hesaplanıyor..."
                setTextColor(Color.parseColor("#666666"))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
            }

            // KOORDİNAT BİLGİSİ İÇİN YENİ TEXTVIEW
            tvCoordinates = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 4, 16, 8)
                }
                text = "Konum: hesaplanıyor..."
                setTextColor(Color.parseColor("#666666"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
            }

            // Durum bilgisi için TextView oluştur
            tvGeofenceStatus = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(16, 4, 16, 16)
                }
                text = "📍 Konum takibi başlatılıyor..."
                setTextColor(Color.parseColor("#2196F3"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
            }

            // TextView'leri butondan sonra ekle
            parent.addView(tvDistance, parent.indexOfChild(btnGarajAc) + 1)
            parent.addView(tvCoordinates, parent.indexOfChild(btnGarajAc) + 2)
            parent.addView(tvGeofenceStatus, parent.indexOfChild(btnGarajAc) + 3)
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // İzin verilmişse konum takibini başlat
                startLocationTracking()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // Kullanıcıya neden izin gerektiğini açıkla
                showPermissionExplanation()
            }
            else -> {
                // İzin iste
                requestLocationPermission()
            }
        }
    }

    private fun showPermissionExplanation() {
        Toast.makeText(
            this,
            "Garaj otomatik açılması için konum erişimine ihtiyaç var",
            Toast.LENGTH_LONG
        ).show()

        Handler(Looper.getMainLooper()).postDelayed({
            requestLocationPermission()
        }, 2000)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // İzin verildi, konum takibini başlat
                    startLocationTracking()
                    // Servisi de başlat
                    startGarajService()
                    Toast.makeText(this, "Konum izni verildi", Toast.LENGTH_SHORT).show()
                } else {
                    // İzin reddedildi
                    tvGeofenceStatus.text = "❌ Konum izni gerekli"
                    tvGeofenceStatus.setTextColor(Color.RED)
                    Toast.makeText(this, "Konum izni reddedildi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Ayarlardan konum hızını al ve LocationRequest oluştur
        val locationRequest = createLocationRequest()

        // Önce son konumu bir kere al (hızlı başlangıç)
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                updateDistanceToGarage(it)
                checkGeofence(it)
                println("🚀 İlk konum hemen alındı: ${it.latitude}, ${it.longitude}")
            }
        }

        // Location callback oluştur
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateDistanceToGarage(location)
                    checkGeofence(location)
                }
            }
        }

        // Konum güncellemelerini başlat
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        isTracking = true

        // Konum hızına göre durum mesajını güncelle
        updateLocationSpeedStatus()

        println("✅ Konum takibi başlatıldı - Hız: ${getLocationSpeed()}")
        println("🔧 Otomatik garaj açma: ${if (isAutoGarageEnabled()) "AKTİF" else "PAPASİF"}")
    }

    private fun createLocationRequest(): LocationRequest {
        val speedSetting = getLocationSpeed()

        return LocationRequest.create().apply {
            when (speedSetting) {
                "slow" -> {
                    interval = 10000 // 10 saniye
                    fastestInterval = 5000 // 5 saniye
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                    smallestDisplacement = 10f // 10 metre
                }
                "fast" -> {
                    interval = 2000 // 2 saniye
                    fastestInterval = 1000 // 1 saniye
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    smallestDisplacement = 2f // 2 metre
                }
                "realtime" -> {
                    interval = 1000 // 1 saniye
                    fastestInterval = 500 // 0.5 saniye
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    smallestDisplacement = 1f // 1 metre
                }
                else -> { // medium - varsayılan
                    interval = 5000 // 5 saniye
                    fastestInterval = 3000 // 3 saniye
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    smallestDisplacement = 5f // 5 metre
                }
            }
            maxWaitTime = interval * 2
        }
    }

    private fun updateLocationSpeedStatus() {
        val speedSetting = getLocationSpeed()
        val (statusText, statusColor) = when (speedSetting) {
            "slow" -> Pair("📍 Konum takibi: YAVAŞ (10s)", "#FF9800") // Turuncu
            "fast" -> Pair("📍 Konum takibi: HIZLI (2s)", "#4CAF50") // Yeşil
            "realtime" -> Pair("📍 Konum takibi: ÇOK HIZLI (1s)", "#F44336") // Kırmızı
            else -> Pair("📍 Konum takibi: ORTA (5s)", "#2196F3") // Mavi
        }

        // Otomatik garaj durumunu da ekle
        val autoGarageStatus = if (isAutoGarageEnabled()) "✅ Otomatik: AÇIK" else "❌ Otomatik: KAPALI"
        val fullStatusText = "$statusText - $autoGarageStatus"

        runOnUiThread {
            tvGeofenceStatus.text = fullStatusText
            tvGeofenceStatus.setTextColor(Color.parseColor(statusColor))
        }
    }

    private fun updateDistanceToGarage(currentLocation: Location) {
        val garageLocation = Location("garage").apply {
            latitude = GARAJ_LATITUDE
            longitude = GARAJ_LONGITUDE
        }

        val distance = currentLocation.distanceTo(garageLocation)

        runOnUiThread {
            // Mesafe bilgisini güncelle
            tvDistance.text = "Garaja uzaklık: ${"%.1f".format(distance)} metre"

            // KOORDİNAT BİLGİSİNİ GÜNCELLE
            val currentLat = currentLocation.latitude
            val currentLon = currentLocation.longitude
            tvCoordinates.text = "Konum: ${"%.6f".format(currentLat)}, ${"%.6f".format(currentLon)}"

            // Mesafeye göre renk değiştir
            when {
                distance <= GEOFENCE_RADIUS -> {
                    tvDistance.setTextColor(Color.parseColor("#4CAF50")) // Yeşil
                    tvCoordinates.setTextColor(Color.parseColor("#4CAF50")) // Yeşil
                }
                distance <= 100 -> {
                    tvDistance.setTextColor(Color.parseColor("#FF9800")) // Turuncu
                    tvCoordinates.setTextColor(Color.parseColor("#FF9800")) // Turuncu
                }
                else -> {
                    tvDistance.setTextColor(Color.parseColor("#F44336")) // Kırmızı
                    tvCoordinates.setTextColor(Color.parseColor("#F44336")) // Kırmızı
                }
            }
        }

        println("📍 Garaja uzaklık: ${"%.1f".format(distance)} metre")
        println("📍 Mevcut konum: ${"%.6f".format(currentLocation.latitude)}, ${"%.6f".format(currentLocation.longitude)}")
    }

    private fun checkGeofence(currentLocation: Location) {
        val garageLocation = Location("garage").apply {
            latitude = GARAJ_LATITUDE
            longitude = GARAJ_LONGITUDE
        }

        val distance = currentLocation.distanceTo(garageLocation)

        println("📍 Mesafe kontrolü: ${"%.1f".format(distance)} metre (Eşik: $GEOFENCE_RADIUS m)")
        println("🔧 Otomatik garaj açma: ${if (isAutoGarageEnabled()) "AKTİF" else "PASİF"}")

        if (distance <= GEOFENCE_RADIUS) {
            // Çemberin içindeyiz, garajı aç (sadece otomatik açma aktifse)
            if (isAutoGarageEnabled() && !isGarajOpened) {
                println("🎯 ÇEMBER İÇİ! Garaj açılıyor...")

                runOnUiThread {
                    tvGeofenceStatus.text = "✅ Garaj çemberi içindesiniz - Açılıyor..."
                    tvGeofenceStatus.setTextColor(Color.parseColor("#4CAF50"))
                }

                openGarageAutomatically()
            } else if (!isAutoGarageEnabled()) {
                println("📍 Çember içi ama otomatik açma kapalı")

                runOnUiThread {
                    tvGeofenceStatus.text = "📍 Çember içi (Otomatik kapalı)"
                    tvGeofenceStatus.setTextColor(Color.parseColor("#FF9800"))
                }
            } else {
                println("📍 Çember içi, garaj zaten açık veya açılıyor")
            }
        } else {
            runOnUiThread {
                // Normal duruma dön ama konum hızı bilgisini koru
                updateLocationSpeedStatus()
            }

            // Çemberden çıkınca butonu tekrar aktif et ve durumu sıfırla
            runOnUiThread {
                btnGarajAc.isEnabled = true
                btnGarajAc.text = "GARAJ AÇ"
                isGarajOpened = false // Garaj durumunu sıfırla
            }
        }
    }

    private fun openGarageAutomatically() {
        println("🚗 Otomatik garaj açma tetiklendi - Mesafe: $GEOFENCE_RADIUS metre içinde")

        // Garajın açıldığını işaretle
        isGarajOpened = true

        runOnUiThread {
            btnGarajAc.isEnabled = false
            btnGarajAc.text = "OTOMATİK AÇILIYOR..."
            tvGeofenceStatus.text = "✅ Garaj çemberi içindesiniz - Açılıyor..."
            tvGeofenceStatus.setTextColor(Color.parseColor("#4CAF50"))
            // Veritabanına kayıt ekle
            kayitEkleVeritabanina("Otomatik Açıldı")
            // Animasyon başlat
            val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
            btnGarajAc.startAnimation(shakeAnim)
        }

        // Blynk API'sini çağır
        val blynkToken = getBlynkToken()
        val blynkPin = getBlynkPin()
        val blynkUrl = "https://sgp1.blynk.cloud/external/api/update?token=$blynkToken&$blynkPin=1"

        println("🔌 Blynk API çağrılıyor: $blynkUrl")
        println("🔑 Token: $blynkToken")
        println("📌 Pin: $blynkPin")

        val stringRequest = StringRequest(
            Request.Method.GET,
            blynkUrl,
            { response ->
                println("✅ Otomatik garaj açma başarılı: $response")

                runOnUiThread {
                    Toast.makeText(this, "✅ Garaj otomatik açıldı", Toast.LENGTH_LONG).show()
                    tvGeofenceStatus.text = "✅ Garaj açıldı"
                }
            },
            { error ->
                println("❌ Otomatik garaj açma hatası: ${error.message}")

                runOnUiThread {
                    Toast.makeText(this, "❌ Otomatik açılamadı: ${error.message}", Toast.LENGTH_LONG).show()
                    tvGeofenceStatus.text = "❌ Açılamadı"
                    btnGarajAc.isEnabled = true
                    btnGarajAc.text = "GARAJ AÇ"
                    isGarajOpened = false // Hata durumunda durumu sıfırla
                }
            }
        )

        // Timeout ayarı
        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000, // 10 saniye timeout
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun stopLocationTracking() {
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
            println("❌ Konum takibi durduruldu")
        }
    }

    private fun setupGarajButton() {
        btnGarajAc.setOnClickListener {
            startButtonAnimations()
            Handler(Looper.getMainLooper()).postDelayed({
                garajAcAPIistegiGonder()
            }, 300)
        }
    }

    private fun startButtonAnimations() {
        try {
            val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
            btnGarajAc.startAnimation(shakeAnim)

            btnGarajAc.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(150)
                .withEndAction {
                    btnGarajAc.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
                .start()

            btnGarajAc.setBackgroundColor(Color.parseColor("#2C387E"))
            Handler(Looper.getMainLooper()).postDelayed({
                btnGarajAc.setBackgroundColor(Color.parseColor("#3F51B5"))
            }, 200)
        } catch (e: Exception) {
            println("❌ Animasyon hatası: ${e.message}")
        }
    }

    private fun garajAcAPIistegiGonder() {
        val blynkToken = getBlynkToken()
        val blynkPin = getBlynkPin()
        val blynkUrl = "https://sgp1.blynk.cloud/external/api/update?token=$blynkToken&$blynkPin=1"

        println("🚗 Manuel garaj açma: $blynkUrl")

        runOnUiThread {
            btnGarajAc.isEnabled = false
            btnGarajAc.text = "GÖNDERİLİYOR..."
        }

        val stringRequest = StringRequest(
            Request.Method.GET,
            blynkUrl,
            { response ->
                runOnUiThread {
                    btnGarajAc.isEnabled = true
                    btnGarajAc.text = "GARAJ AÇ"
                    Toast.makeText(this, "✅ Garaj açma komutu gönderildi", Toast.LENGTH_LONG).show()
                    println("✅ Manuel garaj açma başarılı: '$response'")

                    // Veritabanına kayıt ekle
                    kayitEkleVeritabanina("Manuel Açıldı")
                }
            },
            { error ->
                runOnUiThread {
                    btnGarajAc.isEnabled = true
                    btnGarajAc.text = "GARAJ AÇ"

                    val errorMessage = when {
                        error.networkResponse?.statusCode == 400 -> "Geçersiz token"
                        error.networkResponse?.statusCode == 401 -> "Yetkisiz erişim"
                        error.message?.contains("Unable to resolve host") == true -> "İnternet bağlantısı yok"
                        else -> "Hata: ${error.message ?: "Bilinmeyen hata"}"
                    }

                    Toast.makeText(this, "❌ $errorMessage", Toast.LENGTH_LONG).show()
                    println("❌ Manuel garaj açma hatası: ${error.message}")
                }
            }
        )

        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun kayitEkleVeritabanina(yer: String) {
        val telefon = prefs.getString("user_phone", "Bilinmiyor") ?: "Bilinmiyor"
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val sqlQuery = "INSERT INTO butondurum (telefon, yer, email, tarih) VALUES ('5054173030', '$yer', '$userEmail', '$currentTime')"

        veritabaninaKaydet(sqlQuery)
    }

    private fun veritabaninaKaydet(sqlQuery: String) {
        val apiUrl = "http://baser.org/apartman/api/api_hepsi.php"
        val SQLKEY = "randomkey"

        val params = HashMap<String, String>()
        params["query"] = sqlQuery
        params["key"] = SQLKEY

        val stringRequest = object : StringRequest(
            Request.Method.POST,
            apiUrl,
            { response ->
                println("✅ Veritabanı kaydı başarılı: $response")
            },
            { error ->
                println("❌ Veritabanı kaydı hatası: ${error.message}")
            }
        ) {
            override fun getParams(): Map<String, String> {
                return params
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun setupCustomBottomNavigation() {
        btnNavDashboard.setOnClickListener {
            Toast.makeText(this, "Zaten Ana Sayfadasınız", Toast.LENGTH_SHORT).show()
        }

        btnNavDuyurular.setOnClickListener {
            startActivity(Intent(this, DuyurularActivity::class.java).apply {
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
            })
        }

        btnNavAidat.setOnClickListener {
            startActivity(Intent(this, AidatActivity::class.java).apply {
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
            })
        }

        btnNavSikayetler.setOnClickListener {
            startActivity(Intent(this, SikayetlerActivity::class.java).apply {
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
            })
        }
    }

    private fun setupDashboardButtons() {
        val btnAidat = findViewById<Button>(R.id.btnAidat)
        val btnDuyurular = findViewById<Button>(R.id.btnDuyurular)
        val btnBakim = findViewById<Button>(R.id.btnBakim)
        val btnSikayetler = findViewById<Button>(R.id.btnSikayetler)

        btnAidat.setOnClickListener {
            startActivity(Intent(this, AidatActivity::class.java).apply {
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
            })
        }

        btnDuyurular.setOnClickListener {
            startActivity(Intent(this, DuyurularActivity::class.java).apply {
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
            })
        }

        btnBakim.setOnClickListener {
            Toast.makeText(this, "Bakım talepleri yakında eklenecek", Toast.LENGTH_SHORT).show()
        }

        btnSikayetler.setOnClickListener {
            startActivity(Intent(this, SikayetlerActivity::class.java).apply {
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
            })
        }
    }

    private fun loadDashboardData() {
        loadingIndicator.visibility = ProgressBar.VISIBLE
        loadDataFromAPI()
    }

    private fun loadDataFromAPI() {
        val apiKey = getApiKey()
        val apiUrl = getApiUrl()
        val url = "$apiUrl?api_key=$apiKey&user_email=${userEmail}"

        println("🔍 API URL: $url")

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                loadingIndicator.visibility = ProgressBar.GONE
                println("🔍 API Response: $response")

                try {
                    if (response.getBoolean("success")) {
                        val data = response.getJSONObject("data")
                        updateDashboardUI(data)
                        Toast.makeText(this, "Gerçek veriler yüklendi", Toast.LENGTH_SHORT).show()
                    } else {
                        val errorMessage = response.getString("message")
                        Toast.makeText(this, "API Hatası: $errorMessage", Toast.LENGTH_LONG).show()
                        showDemoData()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Veri işleme hatası", Toast.LENGTH_SHORT).show()
                    showDemoData()
                }
            },
            { error ->
                loadingIndicator.visibility = ProgressBar.GONE
                println("🔍 Volley Error: ${error.message}")
                Toast.makeText(this, "Sunucu bağlantı hatası", Toast.LENGTH_SHORT).show()
                showDemoData()
            }
        )

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

    private fun showDemoData() {
        val demoData = JSONObject().apply {
            put("total_users", 48)
            put("total_dues", 156)
            put("pending_complaints", 7)
            put("total_announcements", 23)
            put("late_fee_stats", JSONObject().apply {
                put("total_overdue", 12)
                put("total_late_fees", 845.75)
                put("avg_days_late", 15.3)
            })
        }

        updateDashboardUI(demoData)
        Toast.makeText(this, "Demo verileri gösteriliyor", Toast.LENGTH_SHORT).show()
    }

    private fun updateDashboardUI(data: JSONObject) {
        try {
            // Temel veriler
            tvTotalUsers.text = data.optInt("total_users", 0).toString()
            tvTotalDues.text = data.optInt("total_dues", 0).toString()
            tvPendingComplaints.text = data.optInt("pending_complaints", 0).toString()
            tvTotalAnnouncements.text = data.optInt("total_announcements", 0).toString()

            // GECİKMİŞ AİDATLAR ve GECİKME ÜCRETLERİ
            val lateFeeStats = data.optJSONObject("late_fee_stats")

            if (lateFeeStats != null) {
                // Gecikmiş aidat sayısı - total_overdue
                val totalOverdue = lateFeeStats.optInt("total_overdue", 0)
                tvOverdueDues.text = totalOverdue.toString()

                // Gecikme ücreti YERİNE geciken aidat tutarını göster
                val totalOverdueAmount = lateFeeStats.optDouble("total_overdue_amount", 0.0)
                tvTotalLateFees.text = "₺${String.format("%.2f", totalOverdueAmount)}"

                println("✅ Gecikmiş Aidatlar: $totalOverdue adet, Tutar: ₺$totalOverdueAmount")
            } else {
                // Eski format için fallback
                tvOverdueDues.text = data.optInt("total_overdue", 0).toString()
                tvTotalLateFees.text = "₺${String.format("%.2f", data.optDouble("total_late_fees", 0.0))}"
            }

        } catch (e: Exception) {
            println("❌ UI Güncelleme Hatası: ${e.message}")
            showPlaceholderData()
        }
    }

    // GroupedDuesActivity'deki gibi gecikmiş verileri hesapla
    private fun calculateOverdueFromGroups(data: JSONObject) {
        try {
            var totalOverdueCount = 0
            var totalOverdueAmount = 0.0

            // groups array'ini kontrol et
            if (data.has("groups")) {
                val groupsArray = data.getJSONArray("groups")
                for (i in 0 until groupsArray.length()) {
                    val group = groupsArray.getJSONObject(i)
                    if (group.has("overdue_count")) {
                        totalOverdueCount += group.getInt("overdue_count")
                    }
                    if (group.has("total_overdue")) {
                        totalOverdueAmount += group.getDouble("total_overdue")
                    }
                }
            }

            // statistics objesini kontrol et
            if (data.has("statistics")) {
                val stats = data.getJSONObject("statistics")
                if (stats.has("total_overdue_count")) {
                    totalOverdueCount = stats.getInt("total_overdue_count")
                }
                if (stats.has("total_overdue_amount")) {
                    totalOverdueAmount = stats.getDouble("total_overdue_amount")
                }
            }

            tvOverdueDues.text = totalOverdueCount.toString()
            tvTotalLateFees.text = "₺${String.format("%.2f", totalOverdueAmount)}"

        } catch (e: Exception) {
            println("❌ Gruplardan gecikme hesaplama hatası: ${e.message}")
            tvOverdueDues.text = "0"
            tvTotalLateFees.text = "₺0.00"
        }
    }

    private fun showPlaceholderData() {
        tvTotalUsers.text = "-"
        tvTotalDues.text = "-"
        tvPendingComplaints.text = "-"
        tvTotalAnnouncements.text = "-"
        tvOverdueDues.text = "-"
        tvTotalLateFees.text = "-"
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
        // Servisi durdurmuyoruz, arkaplanda çalışmaya devam etsin
    }

    override fun onPause() {
        super.onPause()
        // Arkaplanda çalışması için tracking durdurulmuyor
        // Servis zaten arkaplanda çalışıyor
    }

    override fun onResume() {
        super.onResume()
        if (!isTracking && hasLocationPermission()) {
            startLocationTracking()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}