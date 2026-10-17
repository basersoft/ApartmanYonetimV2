package com.baser.apartman

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

class CagiYanitlaServisi : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var telecomManager: TelecomManager
    private var isServiceRunning = false
    private var incomingNumber: String? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "cagi_yanitla_kanali"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("CagiYanitlaServisi", "🚀 Servis oluşturuluyor")

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        isServiceRunning = true
        startCallListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Çağrı Yanıtlama Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Uyku modu aktifken çağrıları yönetir"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Uyku Modu Aktif")
            .setContentText("Gelen çağrılar otomatik reddediliyor")
            .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startCallListener() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                Log.e("CagiYanitlaServisi", "❌ READ_PHONE_STATE izni yok")
                stopSelf()
                return
            }

            val phoneStateListener = object : PhoneStateListener() {
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    super.onCallStateChanged(state, phoneNumber)

                    // Uyku modu hala aktif mi kontrol et
                    val prefs = getSharedPreferences("uyku_modu", Context.MODE_PRIVATE)
                    val uykuModuAktif = prefs.getBoolean("aktif", false)

                    if (!uykuModuAktif) {
                        Log.d("CagiYanitlaServisi", "⏸️ Uyku modu pasif, servis durduruluyor")
                        stopSelf()
                        return
                    }

                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING -> {
                            // Telefon çalıyor - hemen reddet ve SMS gönder
                            incomingNumber = phoneNumber
                            Log.d("CagiYanitlaServisi", "📞 Gelen çağrı: $phoneNumber")

                            // Çağrıyı reddet
                            rejectCall()

                            // SMS gönder
                            sendAutoReply(incomingNumber)
                        }

                        TelephonyManager.CALL_STATE_IDLE -> {
                            Log.d("CagiYanitlaServisi", "📞 Telefon boşta")
                            incomingNumber = null
                        }

                        TelephonyManager.CALL_STATE_OFFHOOK -> {
                            Log.d("CagiYanitlaServisi", "📞 Görüşme var")
                        }
                    }
                }
            }

            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Log.d("CagiYanitlaServisi", "✅ Çağrı dinleyici başlatıldı")

        } catch (e: Exception) {
            Log.e("CagiYanitlaServisi", "❌ Hata: ${e.message}")
        }
    }

    private fun rejectCall() {
        try {
            // Method 1: TelecomManager ile reddet (Android 5+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager.endCall()
                    Log.d("CagiYanitlaServisi", "✅ Çağrı reddedildi (TelecomManager)")
                    return
                }
            }

            // Method 2: TelephonyManager (reflection) ile reddet
            try {
                val tm = telephonyManager
                val method = tm.javaClass.getDeclaredMethod("getITelephony")
                method.isAccessible = true
                val telephonyInterface = method.invoke(tm)
                val endCallMethod = telephonyInterface.javaClass.getDeclaredMethod("endCall")
                endCallMethod.invoke(telephonyInterface)
                Log.d("CagiYanitlaServisi", "✅ Çağrı reddedildi (Reflection)")
            } catch (e: Exception) {
                Log.e("CagiYanitlaServisi", "❌ Reflection reddetme hatası: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e("CagiYanitlaServisi", "❌ Çağrı reddedilemedi: ${e.message}")
        }
    }

    private fun sendAutoReply(number: String?) {
        if (number.isNullOrEmpty()) {
            Log.e("CagiYanitlaServisi", "❌ Telefon numarası boş")
            return
        }

        // SMS izni kontrolü
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e("CagiYanitlaServisi", "❌ SEND_SMS izni yok")
            return
        }

        // Mesajı al
        val prefs = getSharedPreferences("uyku_modu", Context.MODE_PRIVATE)
        val mesaj = prefs.getString("otomatik_mesaj", "Şu anda uyku modundayım. Size daha sonra döneceğim.")

        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(number, null, mesaj, null, null)
            Log.d("CagiYanitlaServisi", "✅ SMS gönderildi: $number -> $mesaj")
        } catch (e: Exception) {
            Log.e("CagiYanitlaServisi", "❌ SMS gönderilemedi: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CagiYanitlaServisi", "▶️ Servis başlatıldı")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CagiYanitlaServisi", "⏹️ Servis durduruldu")
        isServiceRunning = false

        // Telefon dinleyicisini durdur
        try {
            telephonyManager.listen(object : PhoneStateListener() {}, PhoneStateListener.LISTEN_NONE)
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}