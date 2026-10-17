package com.baser.apartman

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
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
import com.baser.apartman.widgets.AidatAppWidget
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import java.net.URL
import javax.net.ssl.HttpsURLConnection

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
    private lateinit var backgroundImage: ImageView

    private lateinit var tvDistance: TextView
    private lateinit var tvCoordinates: TextView
    private lateinit var tvGeofenceStatus: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var isTracking = false
    private var isGarajOpened = false

    private lateinit var prefs: SharedPreferences

    private lateinit var btnNavDashboard: Button
    private lateinit var btnNavDuyurular: Button
    private lateinit var btnNavAidat: Button
    private lateinit var btnNavSikayetler: Button

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

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
        return prefs.getString("api_url", "http://baser.org/site/api/api_dashboard_stats.php")
            ?: "http://baser.org/site/api/api_dashboard_stats.php"
    }

    private fun getLocationSpeed(): String {
        return prefs.getString("location_speed", "medium") ?: "medium"
    }

    private fun isAutoGarageEnabled(): Boolean {
        return prefs.getBoolean("auto_garage_enabled", true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)

        initViews()
        setupNavigation()
        setupCustomBottomNavigation()
        setupDashboardButtons()
        setupGarajButton()
        setupGeofencingUI()
        checkLocationPermission()
        loadDashboardData()
        loadBackgroundFromUrl()
        startGarajService()

        updateWidgetWithUserData()
        saveCurrentUserToPrefs()
    }

    override fun onResume() {
        super.onResume()
        updateNavigationHeader()

        // Dashboard'daki mevcut verileri widget'a gönder
        val widget = AidatAppWidget()
        widget.updateWidgetData(
            context = this,
            userEmail = userEmail,
            userName = userName,
            userType = userType,
            gecikmisSayi = tvOverdueDues.text.toString().toIntOrNull() ?: 0,
            toplamBorc = tvTotalLateFees.text.toString().replace("₺", "").replace(",", ".").toFloatOrNull() ?: 0f,
            odenecekSayi = 0,
            sonAy = getCurrentMonth(),
            mesaj = ""
        )
    }

    private fun startGarajService() {
        if (hasLocationPermission()) {
            val serviceIntent = Intent(this, GarajService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            println("🔛 Garaj servisi başlatıldı")
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
        backgroundImage = findViewById(R.id.backgroundImage)

        btnNavDashboard = findViewById(R.id.btnNavDashboard)
        btnNavDuyurular = findViewById(R.id.btnNavDuyurular)
        btnNavAidat = findViewById(R.id.btnNavAidat)
        btnNavSikayetler = findViewById(R.id.btnNavSikayetler)

        val welcomeText = when (userType) {
            "admin" -> "Yönetici Paneli - Hoşgeldiniz $userName!"
            "manager" -> "Site Sorumlusu Paneli - Hoşgeldiniz $userName!"
            else -> "Hoşgeldiniz $userName!"
        }
        tvWelcome.text = welcomeText

        btnRefresh.setOnClickListener {
            loadDashboardData()
            refreshProfileImage()
            updateWidgetWithUserData()
            saveCurrentUserToPrefs()
            Toast.makeText(this, "Veriler yenileniyor...", Toast.LENGTH_SHORT).show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        updateNavigationHeader()
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
        val garajButtonParent = btnGarajAc.parent as? LinearLayout
        garajButtonParent?.let { parent ->
            tvDistance = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16, 8, 16, 8) }
                text = "Garaja uzaklık: hesaplanıyor..."
                setTextColor(Color.parseColor("#666666"))
                textSize = 14f
                gravity = android.view.Gravity.CENTER
            }

            tvCoordinates = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16, 4, 16, 8) }
                text = "Konum: hesaplanıyor..."
                setTextColor(Color.parseColor("#666666"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
            }

            tvGeofenceStatus = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(16, 4, 16, 16) }
                text = "📍 Konum takibi başlatılıyor..."
                setTextColor(Color.parseColor("#2196F3"))
                textSize = 12f
                gravity = android.view.Gravity.CENTER
            }

            parent.addView(tvDistance, parent.indexOfChild(btnGarajAc) + 1)
            parent.addView(tvCoordinates, parent.indexOfChild(btnGarajAc) + 2)
            parent.addView(tvGeofenceStatus, parent.indexOfChild(btnGarajAc) + 3)
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                startLocationTracking()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionExplanation()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun showPermissionExplanation() {
        Toast.makeText(this, "Garaj otomatik açılması için konum erişimine ihtiyaç var", Toast.LENGTH_LONG).show()
        Handler(Looper.getMainLooper()).postDelayed({ requestLocationPermission() }, 2000)
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationTracking()
                    startGarajService()
                    Toast.makeText(this, "Konum izni verildi", Toast.LENGTH_SHORT).show()
                } else {
                    tvGeofenceStatus.text = "❌ Konum izni gerekli"
                    tvGeofenceStatus.setTextColor(Color.RED)
                    Toast.makeText(this, "Konum izni reddedildi", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationTracking() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val locationRequest = createLocationRequest()

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                updateDistanceToGarage(it)
                checkGeofence(it)
            }
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateDistanceToGarage(location)
                    checkGeofence(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        isTracking = true
        updateLocationSpeedStatus()
    }

    private fun createLocationRequest(): LocationRequest {
        val speedSetting = getLocationSpeed()
        return LocationRequest.create().apply {
            when (speedSetting) {
                "slow" -> {
                    interval = 10000
                    fastestInterval = 5000
                    priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
                    smallestDisplacement = 10f
                }
                "fast" -> {
                    interval = 2000
                    fastestInterval = 1000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    smallestDisplacement = 2f
                }
                "realtime" -> {
                    interval = 1000
                    fastestInterval = 500
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    smallestDisplacement = 1f
                }
                else -> {
                    interval = 5000
                    fastestInterval = 3000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                    smallestDisplacement = 5f
                }
            }
            maxWaitTime = interval * 2
        }
    }

    private fun updateLocationSpeedStatus() {
        val speedSetting = getLocationSpeed()
        val (statusText, statusColor) = when (speedSetting) {
            "slow" -> Pair("📍 Konum takibi: YAVAŞ (10s)", "#FF9800")
            "fast" -> Pair("📍 Konum takibi: HIZLI (2s)", "#4CAF50")
            "realtime" -> Pair("📍 Konum takibi: ÇOK HIZLI (1s)", "#F44336")
            else -> Pair("📍 Konum takibi: ORTA (5s)", "#2196F3")
        }
        val autoGarageStatus = if (isAutoGarageEnabled()) "✅ Otomatik: AÇIK" else "❌ Otomatik: KAPALI"
        runOnUiThread {
            tvGeofenceStatus.text = "$statusText - $autoGarageStatus"
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
            tvDistance.text = "Garaja uzaklık: ${"%.1f".format(distance)} metre"
            tvCoordinates.text = "Konum: ${"%.6f".format(currentLocation.latitude)}, ${"%.6f".format(currentLocation.longitude)}"
            when {
                distance <= GEOFENCE_RADIUS -> {
                    tvDistance.setTextColor(Color.parseColor("#4CAF50"))
                    tvCoordinates.setTextColor(Color.parseColor("#4CAF50"))
                }
                distance <= 100 -> {
                    tvDistance.setTextColor(Color.parseColor("#FF9800"))
                    tvCoordinates.setTextColor(Color.parseColor("#FF9800"))
                }
                else -> {
                    tvDistance.setTextColor(Color.parseColor("#F44336"))
                    tvCoordinates.setTextColor(Color.parseColor("#F44336"))
                }
            }
        }
    }

    private fun checkGeofence(currentLocation: Location) {
        val garageLocation = Location("garage").apply {
            latitude = GARAJ_LATITUDE
            longitude = GARAJ_LONGITUDE
        }
        val distance = currentLocation.distanceTo(garageLocation)
        if (distance <= GEOFENCE_RADIUS) {
            if (isAutoGarageEnabled() && !isGarajOpened) {
                runOnUiThread {
                    tvGeofenceStatus.text = "✅ Garaj çemberi içindesiniz - Açılıyor..."
                    tvGeofenceStatus.setTextColor(Color.parseColor("#4CAF50"))
                }
                openGarageAutomatically()
            } else if (!isAutoGarageEnabled()) {
                runOnUiThread {
                    tvGeofenceStatus.text = "📍 Çember içi (Otomatik kapalı)"
                    tvGeofenceStatus.setTextColor(Color.parseColor("#FF9800"))
                }
            }
        } else {
            runOnUiThread { updateLocationSpeedStatus() }
            runOnUiThread {
                btnGarajAc.isEnabled = true
                btnGarajAc.text = "GARAJ AÇ"
                isGarajOpened = false
            }
        }
    }

    private fun openGarageAutomatically() {
        playSoundAndVibration()
        isGarajOpened = true
        runOnUiThread {
            btnGarajAc.isEnabled = false
            btnGarajAc.text = "OTOMATİK AÇILIYOR..."
            kayitEkleVeritabanina("Otomatik Açıldı")
            val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
            btnGarajAc.startAnimation(shakeAnim)
        }
        val blynkToken = getBlynkToken()
        val blynkPin = getBlynkPin()
        val blynkUrl = "https://sgp1.blynk.cloud/external/api/update?token=$blynkToken&$blynkPin=1"
        val stringRequest = StringRequest(Request.Method.GET, blynkUrl,
            { response ->
                println("✅ Otomatik garaj açma başarılı: $response")
                runOnUiThread { Toast.makeText(this, "✅ Garaj otomatik açıldı", Toast.LENGTH_LONG).show() }
            },
            { error ->
                println("❌ Otomatik garaj açma hatası: ${error.message}")
                runOnUiThread {
                    Toast.makeText(this, "❌ Otomatik açılamadı: ${error.message}", Toast.LENGTH_LONG).show()
                    btnGarajAc.isEnabled = true
                    btnGarajAc.text = "GARAJ AÇ"
                    isGarajOpened = false
                }
            }
        )
        stringRequest.retryPolicy = DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun stopLocationTracking() {
        if (isTracking) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            isTracking = false
        }
    }

    private fun setupGarajButton() {
        btnGarajAc.setOnClickListener {
            startButtonAnimations()
            Handler(Looper.getMainLooper()).postDelayed({
                garajAcAPIistegiGonder()
                playSoundAndVibration()
            }, 150)
        }
    }

    private fun startButtonAnimations() {
        try {
            val shakeAnim = AnimationUtils.loadAnimation(this, R.anim.shake)
            btnGarajAc.startAnimation(shakeAnim)
            btnGarajAc.animate().scaleX(0.9f).scaleY(0.9f).setDuration(150).withEndAction {
                btnGarajAc.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
            }.start()
            btnGarajAc.setBackgroundColor(Color.parseColor("#2C387E"))
            Handler(Looper.getMainLooper()).postDelayed({ btnGarajAc.setBackgroundColor(Color.parseColor("#3F51B5")) }, 200)
        } catch (e: Exception) { println("❌ Animasyon hatası: ${e.message}") }
    }

    private fun playSoundAndVibration() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else { vibrator.vibrate(150) }
            }
            btnGarajAc.playSoundEffect(android.view.SoundEffectConstants.CLICK)
        } catch (e: Exception) { println("❌ Titreşim/ses hatası: ${e.message}") }
    }

    private fun garajAcAPIistegiGonder() {
        playSoundAndVibration()
        val blynkToken = getBlynkToken()
        val blynkPin = getBlynkPin()
        val blynkUrl = "https://sgp1.blynk.cloud/external/api/update?token=$blynkToken&$blynkPin=1"
        runOnUiThread {
            btnGarajAc.isEnabled = false
            btnGarajAc.text = "GÖNDERİLİYOR..."
        }
        val stringRequest = StringRequest(Request.Method.GET, blynkUrl,
            { response ->
                runOnUiThread {
                    btnGarajAc.isEnabled = true
                    btnGarajAc.text = "GARAJ AÇ"
                    Toast.makeText(this, "✅ Garaj açma komutu gönderildi", Toast.LENGTH_LONG).show()
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
                }
            }
        )
        stringRequest.retryPolicy = DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun kayitEkleVeritabanina(yer: String) {
        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val sqlQuery = "INSERT INTO butondurum (telefon, yer, email, tarih) VALUES ('5054173030', '$yer', '$userEmail', '$currentTime')"
        veritabaninaKaydet(sqlQuery)
    }

    private fun veritabaninaKaydet(sqlQuery: String) {
        val params = HashMap<String, String>()
        params["query"] = sqlQuery
        params["key"] = "randomkey"
        val stringRequest = object : StringRequest(Request.Method.POST, "http://baser.org/apartman/api/api_hepsi.php",
            { response -> println("✅ Veritabanı kaydı başarılı: $response") },
            { error -> println("❌ Veritabanı kaydı hatası: ${error.message}") }
        ) { override fun getParams(): Map<String, String> = params }
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun setupCustomBottomNavigation() {
        btnNavDashboard.setOnClickListener { Toast.makeText(this, "Zaten Ana Sayfadasınız", Toast.LENGTH_SHORT).show() }
        btnNavDuyurular.setOnClickListener { startActivity(Intent(this, DuyurularActivity::class.java).apply { putExtra("user_email", userEmail); putExtra("user_type", userType); putExtra("user_name", userName) }) }
        btnNavAidat.setOnClickListener { startActivity(Intent(this, AidatActivity::class.java).apply { putExtra("user_email", userEmail); putExtra("user_type", userType); putExtra("user_name", userName) }) }
        btnNavSikayetler.setOnClickListener { startActivity(Intent(this, SikayetlerActivity::class.java).apply { putExtra("user_email", userEmail); putExtra("user_type", userType); putExtra("user_name", userName) }) }
    }

    private fun setupDashboardButtons() {
        findViewById<Button>(R.id.btnAidat).setOnClickListener { startActivity(Intent(this, AidatActivity::class.java).apply { putExtra("user_email", userEmail); putExtra("user_type", userType); putExtra("user_name", userName) }) }
        findViewById<Button>(R.id.btnDuyurular).setOnClickListener { startActivity(Intent(this, DuyurularActivity::class.java).apply { putExtra("user_email", userEmail); putExtra("user_type", userType); putExtra("user_name", userName) }) }
        findViewById<Button>(R.id.btnBakim).setOnClickListener { startActivity(Intent(this, ButonDurumActivity::class.java).apply { putExtra("user_email", userEmail); putExtra("user_type", userType); putExtra("user_name", userName) }) }
        findViewById<Button>(R.id.btnSikayetler).setOnClickListener { startActivity(Intent(this, SikayetlerActivity::class.java).apply { putExtra("user_email", userEmail); putExtra("user_type", userType); putExtra("user_name", userName) }) }
    }

    private fun loadDashboardData() {
        loadingIndicator.visibility = ProgressBar.VISIBLE
        loadDataFromAPI()
        updateWidgetWithUserData()
    }

    private fun loadDataFromAPI() {
        val url = "${getApiUrl()}?api_key=${getApiKey()}&user_email=$userEmail"
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                loadingIndicator.visibility = ProgressBar.GONE
                if (response.getBoolean("success")) {
                    updateDashboardUI(response.getJSONObject("data"))
                    Toast.makeText(this, "Gerçek veriler yüklendi", Toast.LENGTH_SHORT).show()
                } else { showDemoData() }
            },
            { error ->
                loadingIndicator.visibility = ProgressBar.GONE
                showDemoData()
            }
        )
        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

    private fun showDemoData() {
        val demoData = JSONObject().apply {
            put("total_users", 48); put("total_dues", 156); put("pending_complaints", 7); put("total_announcements", 23)
            put("late_fee_stats", JSONObject().apply { put("total_overdue", 12); put("total_late_fees", 845.75) })
        }
        updateDashboardUI(demoData)
        Toast.makeText(this, "Demo verileri gösteriliyor", Toast.LENGTH_SHORT).show()
    }

    private fun updateDashboardUI(data: JSONObject) {
        try {
            val totalUsers = data.optInt("total_users", 0)
            val totalDues = data.optInt("total_dues", 0)
            val pendingComplaints = data.optInt("pending_complaints", 0)
            val totalAnnouncements = data.optInt("total_announcements", 0)

            tvTotalUsers.text = totalUsers.toString()
            tvTotalDues.text = totalDues.toString()
            tvPendingComplaints.text = pendingComplaints.toString()
            tvTotalAnnouncements.text = totalAnnouncements.toString()

            val lateFeeStats = data.optJSONObject("late_fee_stats")
            var gecikmisSayi = 0
            var toplamBorc = 0f
            var odenecekSayi = 0
            var sonAy = ""

            if (lateFeeStats != null) {
                gecikmisSayi = lateFeeStats.optInt("total_overdue", 0)
                toplamBorc = lateFeeStats.optDouble("total_overdue_amount", 0.0).toFloat()
                tvOverdueDues.text = gecikmisSayi.toString()
                tvTotalLateFees.text = "₺${String.format("%.2f", toplamBorc)}"
            } else {
                tvOverdueDues.text = data.optInt("total_overdue", 0).toString()
                tvTotalLateFees.text = "₺${String.format("%.2f", data.optDouble("total_late_fees", 0.0))}"
            }

            // *** WIDGET'I GÜNCELLE - Dashboard'daki verileri kullan ***
            val widget = AidatAppWidget()
            widget.updateWidgetData(
                context = this,
                userEmail = userEmail,
                userName = userName,
                userType = userType,
                gecikmisSayi = gecikmisSayi,
                toplamBorc = toplamBorc,
                odenecekSayi = odenecekSayi,
                sonAy = getCurrentMonth(),
                mesaj = ""
            )

            println("✅ Widget güncellendi - Gecikmiş: $gecikmisSayi, Borç: $toplamBorc")

        } catch (e: Exception) {
            println("❌ UI Güncelleme Hatası: ${e.message}")
            showPlaceholderData()
        }
    }

    // Yardımcı metod - içinde bulunduğumuz ayı al
    private fun getCurrentMonth(): String {
        val format = SimpleDateFormat("MMMM yyyy", Locale("tr", "TR"))
        return format.format(Date())
    }

    private fun showPlaceholderData() {
        tvTotalUsers.text = "-"; tvTotalDues.text = "-"; tvPendingComplaints.text = "-"
        tvTotalAnnouncements.text = "-"; tvOverdueDues.text = "-"; tvTotalLateFees.text = "-"
    }

    private fun loadBackgroundFromUrl() {
        Thread {
            try {
                val url = URL("https://baser.free.nf/apartman/arkaplan.jpg")
                val connection = url.openConnection() as HttpsURLConnection
                connection.connect()
                if (connection.responseCode == 200) {
                    val bitmap = BitmapFactory.decodeStream(connection.inputStream)
                    runOnUiThread { backgroundImage.setImageBitmap(bitmap) }
                }
                connection.disconnect()
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }

    private fun updateNavigationHeader() {
        try {
            val headerView = navigationView.getHeaderView(0)
            // BaseActivity'deki ID'leri kullan (R.id.tvUserName, R.id.tvUserEmail)
            val tvUserName = headerView.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = headerView.findViewById<TextView>(R.id.tvUserEmail)

            tvUserName?.text = userName.ifEmpty { "Kullanıcı" }
            tvUserEmail?.text = userEmail.ifEmpty { "email@ornek.com" }
        } catch (e: Exception) {
            println("❌ Navigation header güncelleme hatası: ${e.message}")
        }
    }

    private fun updateWidgetWithUserData() {
        if (userEmail.isNotEmpty()) {
            fetchAidatDataForWidget(userEmail, userName, userType)
        } else {
            AidatAppWidget().clearWidgetData(this)
        }
    }

    private fun fetchAidatDataForWidget(email: String, name: String, type: String) {
        val sqlQuery = """
            SELECT 
                COUNT(CASE WHEN status != 'paid' AND due_date < date('now') THEN 1 END) as gecikmis_sayi,
                COALESCE(SUM(CASE WHEN status != 'paid' THEN amount ELSE 0 END), 0) as toplam_borc,
                COUNT(CASE WHEN status != 'paid' AND due_date >= date('now') THEN 1 END) as odenecek_sayi,
                MAX(due_date) as son_tarih
            FROM aidatlar 
            WHERE user_email = '$email'
        """.trimIndent()

        ApiManager.executeSQLQuery(this, sqlQuery,
            onSuccess = { response -> parseAidatDataAndUpdateWidget(response, email, name, type) },
            onError = { error ->
                println("❌ Aidat verisi çekilemedi: $error")
                AidatAppWidget().updateWidgetData(this, email, name, type, 0, 0f, 0, "", "Veri yüklenemedi")
            }
        )
    }

    private fun parseAidatDataAndUpdateWidget(response: String, email: String, name: String, userType: String) {
        try {
            val lines = response.trim().split("\n")
            if (lines.size > 1) {
                val data = lines[1].split(",")
                val gecikmisSayi = data.getOrNull(0)?.toIntOrNull() ?: 0
                val toplamBorc = data.getOrNull(1)?.toFloatOrNull() ?: 0f
                val odenecekSayi = data.getOrNull(2)?.toIntOrNull() ?: 0
                val sonTarih = data.getOrNull(3) ?: ""
                AidatAppWidget().updateWidgetData(
                    this, email, name, userType,
                    gecikmisSayi, toplamBorc, odenecekSayi,
                    if (sonTarih.length >= 7) sonTarih.take(7) else sonTarih,
                    ""
                )
            } else {
                AidatAppWidget().updateWidgetData(this, email, name, userType, 0, 0f, 0, "", "Aidat kaydı bulunamadı")
            }
        } catch (e: Exception) {
            println("❌ Parse hatası: ${e.message}")
        }
    }

    private fun saveCurrentUserToPrefs() {
        getSharedPreferences("user_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("user_email", userEmail)
            putString("user_name", userName)
            putString("user_type", userType)
            apply()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationTracking()
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}