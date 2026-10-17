package com.baser.apartman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.baser.apartman.widgets.UykuModuWidget
import java.util.*

class GeriSayimServisi : Service() {

    private var countDownTimer: CountDownTimer? = null
    private var bitisZamani: Long = 0
    private var aktifMod = false
    private var sonrakiVakitAdi = ""
    private var sonrakiVakitZamani: Long = 0
    private var updateCounter = 0

    companion object {
        private const val TAG = "GeriSayimServisi"
        var isRunning = false
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "geri_sayim_channel"
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundIfNeeded()
    }

    private fun startForegroundIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Geri Sayım Servisi",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Rahatsız etme modu geri sayımını takip eder"
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🕐 Geri Sayım Aktif")
                .setContentText("Rahatsız etme modu takip ediliyor")
                .setSmallIcon(android.R.drawable.ic_menu_send)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val bitis = intent?.getLongExtra("bitis_zamani", 0) ?: 0
        val aktif = intent?.getBooleanExtra("aktif_mod", false) ?: false

        this.aktifMod = aktif

        if (bitis > 0 && bitis <= System.currentTimeMillis()) {
            this.aktifMod = false
            val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("aktif", false).apply()
        } else if (bitis > 0) {
            this.bitisZamani = bitis
        }

        isRunning = true
        Log.d(TAG, "🚀 GeriSayimServisi başlatıldı - aktifMod: $aktifMod")

        updateNextPrayerTimeFromPrefs()

        if (this.aktifMod && bitisZamani > 0) {
            startCountDown()
        } else {
            startPeriodicUpdate()
        }

        return START_STICKY
    }

    private fun updateNextPrayerTimeFromPrefs() {
        try {
            val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
            val savedName = prefs.getString("sonraki_vakit_adi", "")
            val savedTime = prefs.getLong("sonraki_vakit_zamani", 0)
            val now = System.currentTimeMillis()

            val isValid = savedTime > now && (savedTime - now) < 24 * 60 * 60 * 1000

            if (isValid && savedName != null && savedName.isNotEmpty()) {
                sonrakiVakitAdi = savedName
                sonrakiVakitZamani = savedTime
                Log.d(TAG, "📅 Kayıtlı sonraki vakit: $sonrakiVakitAdi")
            } else {
                Log.d(TAG, "📅 Kayıtlı vakit geçersiz, yeniden hesaplanıyor...")
                calculateDefaultNextPrayer()
            }

            val aktifVakitAdi = prefs.getString("aktif_vakit_adi", "")
            val aktifVakitBitis = prefs.getLong("aktif_vakit_bitis", 0)

            if (aktifVakitBitis > now && aktifVakitAdi != null && aktifVakitAdi.isNotEmpty()) {
                aktifMod = true
                bitisZamani = aktifVakitBitis
            }

        } catch (e: Exception) {
            Log.e(TAG, "Vakit güncelleme hatası: ${e.message}")
            calculateDefaultNextPrayer()
        }
    }

    private fun calculateDefaultNextPrayer() {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val vakitler = listOf(
            Pair("İmsak", "05:51"),
            Pair("Öğle", "12:59"),
            Pair("İkindi", "16:08"),
            Pair("Akşam", "18:39"),
            Pair("Yatsı", "19:53")
        )

        var nextName = "Akşam"
        var nextTime = 0L

        for ((name, timeStr) in vakitler) {
            val parts = timeStr.split(":")
            calendar.set(Calendar.HOUR_OF_DAY, parts[0].toInt())
            calendar.set(Calendar.MINUTE, parts[1].toInt())
            calendar.set(Calendar.SECOND, 0)

            var time = calendar.timeInMillis
            if (time <= now) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                time = calendar.timeInMillis
            }

            if (nextTime == 0L || time < nextTime) {
                nextTime = time
                nextName = name
            }
        }

        sonrakiVakitAdi = nextName
        sonrakiVakitZamani = nextTime

        val prefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("sonraki_vakit_adi", sonrakiVakitAdi)
            .putLong("sonraki_vakit_zamani", sonrakiVakitZamani)
            .apply()
    }

    private fun startCountDown() {
        countDownTimer?.cancel()

        val now = System.currentTimeMillis()
        val kalanSure = bitisZamani - now

        if (kalanSure <= 0) {
            disableRahatsizEtmeModu()
            return
        }

        countDownTimer = object : CountDownTimer(kalanSure, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                updateWidget()
            }

            override fun onFinish() {
                disableRahatsizEtmeModu()
            }
        }.start()
    }

    private fun disableRahatsizEtmeModu() {
        aktifMod = false

        val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("aktif", false).apply()

        val serviceIntent = Intent(this, RahatsizEtmeServisi::class.java)
        stopService(serviceIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()

        updateNextPrayerTimeFromPrefs()
        updateWidget()
        startPeriodicUpdate()

        Log.d(TAG, "✅ Rahatsız etme modu devre dışı bırakıldı")
    }

    private fun startPeriodicUpdate() {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val namazPrefs = this@GeriSayimServisi.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
                val isAutoEnabled = namazPrefs.getBoolean("otomatik_aktif", false)

                if (!isAutoEnabled) {
                    updateWidgetPassive()
                    return
                }

                val now = System.currentTimeMillis()

                if (sonrakiVakitZamani <= now && sonrakiVakitZamani > 0) {
                    updateNextPrayerTimeFromPrefs()
                }

                updateCounter++
                if (updateCounter >= 30) {
                    updateCounter = 0
                    updateWidget()
                } else {
                    updateWidget()
                }

                val prefs = this@GeriSayimServisi.getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
                val isActive = prefs.getBoolean("aktif", false)

                if (isActive != aktifMod) {
                    aktifMod = isActive
                    if (aktifMod) {
                        val dndPrefs = this@GeriSayimServisi.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
                        val dndSuresi = dndPrefs.getInt("dnd_suresi", 30)
                        bitisZamani = System.currentTimeMillis() + (dndSuresi * 60 * 1000)
                        startCountDown()
                    }
                }
            }

            override fun onFinish() {
                startPeriodicUpdate()
            }
        }.start()
    }

    private fun updateWidgetPassive() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = ComponentName(this, UykuModuWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isEmpty()) return

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(packageName, R.layout.widget_uyku_modu)
                views.setTextViewText(R.id.tvWidgetDurum, "⏰ Otomatik Kontrol KAPALI")
                views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#757575"))
                views.setViewVisibility(R.id.tvGeriSayim, View.GONE)
                views.setViewVisibility(R.id.tvSonrakiVakit, View.VISIBLE)
                views.setTextViewText(R.id.tvSonrakiVakit, "Ayarları açmak için tıklayın")
                views.setImageViewResource(R.id.btnToggle, android.R.drawable.ic_menu_edit)
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Widget pasif güncelleme hatası: ${e.message}")
        }
    }

    private fun updateWidget() {
        try {
            val prefs = getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
            val isActive = prefs.getBoolean("aktif", false)

            val namazPrefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
            val isAutoEnabled = namazPrefs.getBoolean("otomatik_aktif", false)

            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = ComponentName(this, UykuModuWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isEmpty()) return

            val now = System.currentTimeMillis()

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(packageName, R.layout.widget_uyku_modu)

                if (!isAutoEnabled) {
                    views.setTextViewText(R.id.tvWidgetDurum, "⏰ Otomatik Kontrol KAPALI")
                    views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#757575"))
                    views.setViewVisibility(R.id.tvGeriSayim, View.GONE)
                    views.setViewVisibility(R.id.tvSonrakiVakit, View.VISIBLE)
                    views.setTextViewText(R.id.tvSonrakiVakit, "Namaz vakitleri kapalı")
                } else if (isActive) {
                    views.setTextViewText(R.id.tvWidgetDurum, "🔇 Rahatsız Etme AKTİF")
                    views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#2E7D32"))
                    views.setViewVisibility(R.id.tvGeriSayim, View.VISIBLE)
                    views.setViewVisibility(R.id.tvSonrakiVakit, View.GONE)

                    val currentBitisZamani = if (bitisZamani > 0) bitisZamani else {
                        val dndPrefs = getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
                        val dndSuresi = dndPrefs.getInt("dnd_suresi", 30)
                        now + (dndSuresi * 60 * 1000)
                    }

                    val kalanSure = currentBitisZamani - now
                    if (kalanSure > 0) {
                        val saat = kalanSure / (1000 * 60 * 60)
                        val dakika = (kalanSure % (1000 * 60 * 60)) / (1000 * 60)
                        val saniye = (kalanSure % (1000 * 60)) / 1000
                        val timeText = if (saat > 0) {
                            String.format("%02d:%02d:%02d", saat, dakika, saniye)
                        } else {
                            String.format("%02d:%02d", dakika, saniye)
                        }
                        views.setTextViewText(R.id.tvGeriSayim, timeText)
                    } else {
                        views.setTextViewText(R.id.tvGeriSayim, "00:00")
                    }

                } else {
                    views.setTextViewText(R.id.tvWidgetDurum, "⏰ Rahatsız Etme PASİF")
                    views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#616161"))
                    views.setViewVisibility(R.id.tvGeriSayim, View.GONE)
                    views.setViewVisibility(R.id.tvSonrakiVakit, View.VISIBLE)

                    if (sonrakiVakitZamani > 0 && sonrakiVakitAdi.isNotEmpty()) {
                        val kalanSure = sonrakiVakitZamani - now

                        if (kalanSure > 0) {
                            val saat = kalanSure / (1000 * 60 * 60)
                            val dakika = (kalanSure % (1000 * 60 * 60)) / (1000 * 60)
                            val saniye = (kalanSure % (1000 * 60)) / 1000

                            val kalanText = if (saat > 0) {
                                String.format("%d:%02d:%02d", saat, dakika, saniye)
                            } else {
                                String.format("%02d:%02d", dakika, saniye)
                            }

                            views.setTextViewText(R.id.tvSonrakiVakit, "$sonrakiVakitAdi: $kalanText")
                        } else {
                            views.setTextViewText(R.id.tvSonrakiVakit, "$sonrakiVakitAdi: Hesaplanıyor...")
                            updateNextPrayerTimeFromPrefs()
                        }
                    } else {
                        views.setTextViewText(R.id.tvSonrakiVakit, "Vakit hesaplanıyor...")
                        updateNextPrayerTimeFromPrefs()
                    }
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Widget güncelleme hatası: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        isRunning = false
        Log.d(TAG, "🛑 GeriSayimServisi durduruldu")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}