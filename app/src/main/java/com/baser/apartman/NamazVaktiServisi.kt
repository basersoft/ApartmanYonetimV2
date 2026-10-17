package com.baser.apartman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class NamazVaktiServisi : Service() {

    private lateinit var notificationManager: NotificationManager
    private var isRunning = false

    companion object {
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "namaz_kanali"
        private const val TAG = "NamazVaktiServisi"

        var sonrakiVakit: String = ""
        var sonrakiVakitZamani: Long = 0
        var sonrakiVakitAdi: String = ""
        var sonrakiVakitSaat: String = ""

        var aktifVakitAdi: String = ""
        var aktifVakitBitisZamani: Long = 0
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 NAMAZ SERVİSİ BAŞLATILDI")

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        isRunning = true

        // 1. Önce kayıtlı vakitleri kontrol et ve hemen alarmları kur (Varsayılan sabit vakitlerle ezme!)
        val kayitli = NamazVaktiHelper.getVakitlerFromPrefs(this)
        if (kayitli != null) {
            NamazVaktiHelper.scheduleNamazAlarms(this, kayitli)
        }

        // 2. Ardından API'den taze vakitleri çek
        fetchNamazVakitleri()

        val geriSayimIntent = Intent(this, GeriSayimServisi::class.java).apply {
            putExtra("aktif_mod", false)
            putExtra("sonraki_vakit_adi", sonrakiVakitAdi)
            putExtra("sonraki_vakit_zamani", sonrakiVakitZamani)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(geriSayimIntent)
            } else {
                startService(geriSayimIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "GeriSayimServisi başlatılamadı: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Namaz Vakti Servisi",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🕌 Apartman Namaz Vakti")
            .setContentText("Diyanet namaz vakitleri takibi aktif")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    fun fetchNamazVakitleri() {
        NamazVaktiHelper.refreshVakitlerFromApi(this) { success ->
            if (success) {
                updateGeriSayimServisi()
                updateWidget()
                val kayitli = NamazVaktiHelper.getVakitlerFromPrefs(this)
                if (kayitli != null) {
                    showVakitlerNotification(kayitli)
                }
            } else {
                val kayitli = NamazVaktiHelper.getVakitlerFromPrefs(this)
                if (kayitli != null) {
                    NamazVaktiHelper.calculateNextPrayerTime(this, kayitli)
                    updateGeriSayimServisi()
                    updateWidget()
                }
            }
        }
    }

    fun alarmlariYenidenKur() {
        Log.d(TAG, "🔄 Alarmlar yeniden kuruluyor...")
        val kayitliVakit = NamazVaktiHelper.getVakitlerFromPrefs(this)
        if (kayitliVakit != null) {
            NamazVaktiHelper.scheduleNamazAlarms(this, kayitliVakit)
            updateGeriSayimServisi()
            updateWidget()
            Log.d(TAG, "✅ Alarmlar yeniden kuruldu, sonraki vakit: $sonrakiVakit")
        } else {
            fetchNamazVakitleri()
        }
    }

    fun setAktifVakit(vakitAdi: String, bitisZamani: Long) {
        aktifVakitAdi = vakitAdi
        aktifVakitBitisZamani = bitisZamani
        Log.d(TAG, "🔴 AKTİF VAKİT SET EDİLDİ: $vakitAdi, bitiş zamanı: $bitisZamani")

        val intent = Intent(this, GeriSayimServisi::class.java).apply {
            putExtra("aktif_mod", true)
            putExtra("bitis_zamani", bitisZamani)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "GeriSayimServisi başlatılamadı: ${e.message}")
        }
    }

    fun setAktifVakitBitti() {
        aktifVakitAdi = ""
        aktifVakitBitisZamani = 0
        Log.d(TAG, "🟢 AKTİF VAKİT SONA ERDİ")

        val kayitliVakit = NamazVaktiHelper.getVakitlerFromPrefs(this)
        if (kayitliVakit != null) {
            NamazVaktiHelper.calculateNextPrayerTime(this, kayitliVakit)
        }

        updateGeriSayimServisi()
        updateWidget()
    }

    private fun updateGeriSayimServisi() {
        try {
            val intent = Intent(this, GeriSayimServisi::class.java).apply {
                putExtra("aktif_mod", false)
                putExtra("sonraki_vakit_adi", sonrakiVakitAdi)
                putExtra("sonraki_vakit_zamani", sonrakiVakitZamani)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            Log.d(TAG, "🔄 GeriSayimServisi güncellendi: $sonrakiVakit")
        } catch (e: Exception) {
            Log.e(TAG, "GeriSayimServisi güncelleme hatası: ${e.message}")
        }
    }

    private fun updateWidget() {
        val intent = Intent("com.baser.apartman.UPDATE_WIDGET")
        sendBroadcast(intent)
        Log.d(TAG, "🔄 Widget güncelleme isteği gönderildi")
    }

    private fun showVakitlerNotification(vakit: NamazVakti) {
        val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val erkenSure = prefs.getInt("erken_baslatma_suresi", 5)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🕌 Bugünün Namaz Vakitleri")
            .setContentText("İmsak: ${vakit.imsak} - Akşam: ${vakit.aksam}")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("İmsak: ${vakit.imsak} (${erkenSure}dk erken)\nGüneş: ${vakit.gunes}\nÖğle: ${vakit.ogle}\nİkindi: ${vakit.ikindi}\nAkşam: ${vakit.aksam}\nYatsı: ${vakit.yatsi}\n\nSonraki vakit: $sonrakiVakit")
            )
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(1004, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "▶️ Servis start command alındı")

        val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val isAutoEnabled = prefs.getBoolean("otomatik_aktif", false)

        if (!isAutoEnabled) {
            Log.d(TAG, "⚠️ Otomatik kontrol kapalı, servis kapatılıyor")
            stopSelf()
            return START_NOT_STICKY
        }

        intent?.let {
            when (it.action) {
                "GUNCELLE" -> {
                    val kayitliVakit = NamazVaktiHelper.getVakitlerFromPrefs(this)
                    if (kayitliVakit != null) {
                        NamazVaktiHelper.calculateNextPrayerTime(this, kayitliVakit)
                        updateGeriSayimServisi()
                        updateWidget()
                    }
                }
                "ALARMLARI_YENILE" -> {
                    alarmlariYenidenKur()
                }
                "AKTIF_VAKIT_BASLADI" -> {
                    val vakitAdi = it.getStringExtra("vakit") ?: ""
                    val bitisZamani = it.getLongExtra("bitis_zamani", 0)
                    setAktifVakit(vakitAdi, bitisZamani)
                }
                "AKTIF_VAKIT_BITTI" -> {
                    setAktifVakitBitti()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "⏹️ NAMAZ SERVİSİ DURDURULDU")
        isRunning = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}