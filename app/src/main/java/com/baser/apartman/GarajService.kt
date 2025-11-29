package com.baser.apartman

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GarajService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var prefs: SharedPreferences
    private var isGarajOpened = false

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "garaj_service_channel"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startLocationTracking()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Garaj Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Garaj otomatik açma servisi"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Garaj Servisi Aktif")
            .setContentText("Konum takibi devam ediyor")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startLocationTracking() {
        val locationRequest = LocationRequest.create().apply {
            interval = 8000 // 8 saniyede bir
            fastestInterval = 5000 // En hızlı 5 saniye
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    checkGeofence(location)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
            Log.d("GarajService", "📍 Konum takibi başlatıldı")
        } catch (e: SecurityException) {
            Log.e("GarajService", "Konum izni hatası: ${e.message}")
        }
    }

    private fun checkGeofence(currentLocation: android.location.Location) {
        val GARAJ_LATITUDE = prefs.getString("garaj_lat", "36.7917535")?.toDoubleOrNull() ?: 36.7917535
        val GARAJ_LONGITUDE = prefs.getString("garaj_lon", "34.5916286")?.toDoubleOrNull() ?: 34.5916286
        val GEOFENCE_RADIUS = prefs.getString("geofence_radius", "50.0")?.toDoubleOrNull() ?: 50.0

        val garageLocation = android.location.Location("garage").apply {
            latitude = GARAJ_LATITUDE
            longitude = GARAJ_LONGITUDE
        }

        val distance = currentLocation.distanceTo(garageLocation)

        Log.d("GarajService", "📍 Mesafe: ${"%.1f".format(distance)} m (Eşik: $GEOFENCE_RADIUS m)")

        if (distance <= GEOFENCE_RADIUS && !isGarajOpened) {
            Log.d("GarajService", "🎯 ÇEMBER İÇİ! Garaj açılıyor...")
            openGarageAutomatically()
        } else if (distance > GEOFENCE_RADIUS) {
            isGarajOpened = false
        }
    }

    private fun openGarageAutomatically() {
        isGarajOpened = true

        val blynkToken = prefs.getString("blynk_token", "SİZİN_BLYNK_TOKEN_BURAYA") ?: "SİZİN_BLYNK_TOKEN_BURAYA"
        val blynkPin = prefs.getString("blynk_pin", "V1") ?: "V1"
        val blynkUrl = "https://sgp1.blynk.cloud/external/api/update?token=$blynkToken&$blynkPin=1"

        Log.d("GarajService", "🔌 Blynk API çağrılıyor: $blynkUrl")

        val stringRequest = StringRequest(
            Request.Method.GET,
            blynkUrl,
            { response ->
                Log.d("GarajService", "✅ Otomatik garaj açma başarılı: $response")
                // Veritabanına kayıt ekle
                kayitEkleVeritabanina("Otomatik Açıldı")
            },
            { error ->
                Log.e("GarajService", "❌ Otomatik garaj açma hatası: ${error.message}")
                isGarajOpened = false // Hata durumunda durumu sıfırla
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
        val userEmail = prefs.getString("user_email", "") ?: ""
        val userName = prefs.getString("user_name", "Kullanıcı") ?: "Kullanıcı"
        val telefon = prefs.getString("user_phone", "Bilinmiyor") ?: "Bilinmiyor"

        val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // SQL sorgusu oluştur
        val sqlQuery = "INSERT INTO butondurum (telefon, yer, email, tarih) VALUES ('$telefon', '$yer', '$userEmail', '$currentTime')"

        // API'ye gönder
        veritabaninaKaydet(sqlQuery)
    }

    private fun veritabaninaKaydet(sqlQuery: String) {
        val apiUrl = "http://baser.org/apartman/api/api_sql.php" // PHP dosyanızın URL'si
        val SQLKEY = "randomkey" // PHP dosyanızdaki key ile aynı olmalı

        val params = HashMap<String, String>()
        params["query"] = sqlQuery
        params["key"] = SQLKEY

        val stringRequest = object : StringRequest(
            Request.Method.POST,
            apiUrl,
            { response ->
                Log.d("GarajService", "✅ Veritabanı kaydı başarılı: $response")
            },
            { error ->
                Log.e("GarajService", "❌ Veritabanı kaydı hatası: ${error.message}")
            }
        ) {
            override fun getParams(): Map<String, String> {
                return params
            }
        }

        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(stringRequest)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("GarajService", "❌ Servis durduruldu")
    }
}