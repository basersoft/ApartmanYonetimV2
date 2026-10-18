package com.baser.apartman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class RahatsizEtmeServisi : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private var phoneStateListener: PhoneStateListener? = null
    private var originalRingerMode = AudioManager.RINGER_MODE_NORMAL
    private var isRunning = false

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "rahatsiz_etme_kanali"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("RahatsizEtmeServisi", "🚀 Servis oluşturuluyor")

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Mevcut zil sesi modunu kaydet
        originalRingerMode = audioManager.ringerMode

        // Rahatsız etme modunu aktif et
        activateDndMode()

        // Çağrı dinleyiciyi başlat
        startCallListener()

        isRunning = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rahatsız Etme Modu",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Rahatsız etme modu aktifken çağrıları yönetir"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rahatsız Etme Modu Aktif")
            .setContentText("Gelen çağrılar sessize alınıyor ve SMS gönderiliyor")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun activateDndMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    Log.d("RahatsizEtmeServisi", "✅ Rahatsız etme modu aktif edildi")
                } else {
                    Log.w("RahatsizEtmeServisi", "⚠️ DND izni yok, sadece titreşime alınabilir")
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
            } else {
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
            }
        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "❌ Rahatsız etme modu hatası: ${e.message}")
        }
    }

    private fun deactivateDndMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (notificationManager.isNotificationPolicyAccessGranted) {
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                }
            }
            audioManager.ringerMode = originalRingerMode
            Log.d("RahatsizEtmeServisi", "✅ Normal ses moduna dönüldü")
        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "❌ Rahatsız etme modu kapatma hatası: ${e.message}")
        }
    }

    private fun startCallListener() {
        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.e("RahatsizEtmeServisi", "❌ READ_PHONE_STATE izni yok")
                return
            }

            phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    super.onCallStateChanged(state, phoneNumber)

                    // Servis hala çalışıyor mu kontrol et
                    if (!isRunning) {
                        Log.d("RahatsizEtmeServisi", "Servis durmuş, çağrı işlenmiyor")
                        return
                    }

                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING -> {
                            Log.d("RahatsizEtmeServisi", "📞 Gelen çağrı: $phoneNumber")

                            // SMS gönder
                            sendAutoReply(phoneNumber)
                        }

                        TelephonyManager.CALL_STATE_IDLE -> {
                            Log.d("RahatsizEtmeServisi", "📞 Telefon boşta")
                        }

                        TelephonyManager.CALL_STATE_OFFHOOK -> {
                            Log.d("RahatsizEtmeServisi", "📞 Görüşme var")
                        }
                    }
                }
            }

            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_CALL_STATE)
                Log.d("RahatsizEtmeServisi", "✅ Çağrı dinleyici başlatıldı")
            }

        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "❌ Hata: ${e.message}")
        }
    }

    private fun stopCallListener() {
        try {
            phoneStateListener?.let {
                telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
                phoneStateListener = null
                Log.d("RahatsizEtmeServisi", "✅ Çağrı dinleyici durduruldu")
            }
        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "❌ Dinleyici durdurma hatası: ${e.message}")
        }
    }

    private fun sendAutoReply(number: String?) {
        if (number.isNullOrEmpty()) {
            Log.e("RahatsizEtmeServisi", "❌ Telefon numarası boş")
            return
        }

        // SMS izni kontrolü
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("RahatsizEtmeServisi", "❌ SEND_SMS izni yok")
            return
        }

        // Mesajı al
        val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        val mesaj = prefs.getString("otomatik_mesaj", "Şu anda rahatsız etme modundayım. Size daha sonra döneceğim.")

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, mesaj, null, null)
            Log.d("RahatsizEtmeServisi", "✅ SMS gönderildi: $number")
        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "❌ SMS gönderilemedi: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("RahatsizEtmeServisi", "▶️ Servis başlatıldı")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("RahatsizEtmeServisi", "⏹️ Servis durduruluyor")

        // Önce çalışıyor bayrağını kapat
        isRunning = false

        // Çağrı dinleyiciyi durdur
        stopCallListener()

        // Rahatsız etme modunu kapat
        deactivateDndMode()

        // Notification'ı kaldır
        stopForeground(true)

        // WIDGET'I GÜNCELLE - YENİ EKLENDİ
        try {
            val intent = Intent("com.baser.apartman.widgets.UykuModuWidget.UPDATE_WIDGET")
            sendBroadcast(intent)
            Log.d("RahatsizEtmeServisi", "✅ Widget güncelleme isteği gönderildi")
        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "Widget güncelleme hatası: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}