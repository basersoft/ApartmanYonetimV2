package com.baser.apartman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.Manifest
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.SmsManager
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.baser.apartman.widgets.UykuModuWidget

class RahatsizEtmeServisi : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private lateinit var telecomManager: TelecomManager
    private lateinit var notificationManager: NotificationManager
    private lateinit var audioManager: AudioManager
    private var phoneStateListener: PhoneStateListener? = null
    private var originalRingerMode = AudioManager.RINGER_MODE_NORMAL
    private var isRunning = false

    companion object {
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "rahatsiz_etme_kanali"
        private const val PREFS_NAME = "rahatsiz_etme_modu"
        private const val KEY_ORIGINAL_RINGER = "original_ringer_mode"
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("RahatsizEtmeServisi", "🚀 Servis oluşturuluyor")

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Mevcut zil sesi modunu kalıcı olarak kaydet (servis yeniden başlasa bile kaybolmasın)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_ORIGINAL_RINGER)) {
            prefs.edit().putInt(KEY_ORIGINAL_RINGER, audioManager.ringerMode).apply()
        }
        originalRingerMode = prefs.getInt(KEY_ORIGINAL_RINGER, audioManager.ringerMode)

        // Rahatsız etme modunu aktif et
        activateDndMode()

        // Widget canlı geri sayım: servis hangi yoldan başlatıldıysa başlatılsın,
        // sayaç servisi yoksa (veya periyodik modda) sessizlik sayacını başlat
        if (!GeriSayimServisi.isRunning) {
            try {
                val dndPrefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
                val dndSuresi = dndPrefs.getInt("dnd_suresi", 30)
                val geriSayimIntent = Intent(this, GeriSayimServisi::class.java).apply {
                    putExtra("aktif_mod", true)
                    putExtra("bitis_zamani", System.currentTimeMillis() + (dndSuresi * 60 * 1000L))
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(geriSayimIntent)
                } else {
                    startService(geriSayimIntent)
                }
            } catch (e: Exception) {
                Log.e("RahatsizEtmeServisi", "GeriSayimServisi başlatılamadı: ${e.message}")
            }
        }

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
            // Kaydedilen orijinal ses modunu geri yükle ve kaydı temizle
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val restoreMode = prefs.getInt(KEY_ORIGINAL_RINGER, originalRingerMode)
            audioManager.ringerMode = restoreMode
            prefs.edit().remove(KEY_ORIGINAL_RINGER).apply()
            originalRingerMode = restoreMode
            Log.d("RahatsizEtmeServisi", "✅ Normal ses moduna dönüldü (mod: $restoreMode)")
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

                            // Çağrıyı sessizce reddet
                            rejectCall()

                            // Otomatik SMS gönder
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

    private fun rejectCall() {
        try {
            // Method 1: TelecomManager ile reddet (Android 9+, ANSWER_PHONE_CALLS izniyle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    telecomManager.endCall()
                    Log.d("RahatsizEtmeServisi", "✅ Çağrı sessizce reddedildi (TelecomManager)")
                    return
                }
            }

            // Method 2: Reflection ile reddet
            try {
                val tm = telephonyManager
                val method = tm.javaClass.getDeclaredMethod("getITelephony")
                method.isAccessible = true
                val telephonyInterface = method.invoke(tm)
                val endCallMethod = telephonyInterface.javaClass.getDeclaredMethod("endCall")
                endCallMethod.invoke(telephonyInterface)
                Log.d("RahatsizEtmeServisi", "✅ Çağrı sessizce reddedildi (Reflection)")
            } catch (e: Exception) {
                Log.e("RahatsizEtmeServisi", "❌ Reflection reddetme hatası: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "❌ Çağrı reddedilemedi: ${e.message}")
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

        // WIDGET'I GÜNCELLE
        try {
            val intent = Intent(this, UykuModuWidget::class.java).apply {
                action = UykuModuWidget.ACTION_UPDATE_WIDGET
            }
            sendBroadcast(intent)
            Log.d("RahatsizEtmeServisi", "✅ Widget güncelleme isteği gönderildi")
        } catch (e: Exception) {
            Log.e("RahatsizEtmeServisi", "Widget güncelleme hatası: ${e.message}")
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}