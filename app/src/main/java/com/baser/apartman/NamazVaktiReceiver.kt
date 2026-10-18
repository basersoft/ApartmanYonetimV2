package com.baser.apartman

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.baser.apartman.widgets.UykuModuWidget

class NamazVaktiReceiver : BroadcastReceiver() {

    private val TAG = "NamazVaktiReceiver"
    private val CHANNEL_ID = "namaz_vakitleri_kanali"
    private val NOTIFICATION_ID_VAKIT = 2001

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val vakit = intent.getStringExtra("vakit") ?: "Vakit"
        val dndSuresi = intent.getIntExtra("dnd_suresi", 30)

        Log.d(TAG, "🔥 ALARM TETİKLENDİ: $action - $vakit")

        val prefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val isAutoEnabled = prefs.getBoolean("otomatik_aktif", false)

        if (!isAutoEnabled) {
            Log.d(TAG, "⚠️ Otomatik kontrol kapalı, alarm işlenmiyor: $action")
            return
        }

        // Günlük yenileme alarmı (Gece 00:05)
        if (action == "YENILE_GUNLUK") {
            Log.d(TAG, "🌙 Günlük vakit yenileme tetiklendi.")
            NamazVaktiHelper.refreshVakitlerFromApi(context)
            return
        }

        when {
            action.startsWith("ERKEN_") -> {
                Toast.makeText(context, "🕌 $vakit - Rahatsız etme erken başlatıldı", Toast.LENGTH_LONG).show()
                playNotification(context, "erken")

                // DND modunu aktif et
                context.getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
                    .edit().putBoolean("aktif", true).apply()

                updateWidgetDirectly(context, true)
                startRahatsizEtmeServisi(context)

                val bitisZamani = intent.getLongExtra("bitis_zamani", System.currentTimeMillis() + (dndSuresi * 60 * 1000L))
                startGeriSayimServisi(context, bitisZamani, true)

                sendNotification(context, "🕌 Namaz Vakti Yaklaşıyor",
                    "$vakit vakti için rahatsız etme modu erken başlatıldı.")
            }

            action.startsWith("ACMA_") -> {
                val erkenBaslatildi = intent.getBooleanExtra("erken_baslatildi", false)
                val dndPrefs = context.getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
                val zatenAktif = dndPrefs.getBoolean("aktif", false)

                Toast.makeText(context, "🕌 $vakit vakti girdi", Toast.LENGTH_SHORT).show()
                playNotification(context, "vakit")

                if (!zatenAktif) {
                    dndPrefs.edit().putBoolean("aktif", true).apply()
                    updateWidgetDirectly(context, true)
                    startRahatsizEtmeServisi(context)

                    val bitisZamani = intent.getLongExtra("bitis_zamani", System.currentTimeMillis() + (dndSuresi * 60 * 1000L))
                    startGeriSayimServisi(context, bitisZamani, true)
                }

                sendVakitGirdiNotification(context, vakit)
            }

            action.startsWith("KAPATMA_") -> {
                Toast.makeText(context, "🕌 $vakit vakti bitti", Toast.LENGTH_LONG).show()
                playNotification(context, "kisa")

                // DND modunu kapat
                context.getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
                    .edit().putBoolean("aktif", false).apply()

                updateWidgetDirectly(context, false)
                stopRahatsizEtmeServisi(context)
                stopGeriSayimServisi(context)

                // Güvenli şekilde alarmları ve sonraki vakti yenile (arka plan çökmesi olmadan)
                try {
                    val kayitliVakit = NamazVaktiHelper.getVakitlerFromPrefs(context)
                    if (kayitliVakit != null) {
                        NamazVaktiHelper.calculateNextPrayerTime(context, kayitliVakit)
                        // Eğer Yatsı bittiyse yeni günün vakitlerini internetten çek
                        if (vakit == "Yatsı") {
                            NamazVaktiHelper.refreshVakitlerFromApi(context)
                        } else {
                            NamazVaktiHelper.scheduleNamazAlarms(context, kayitliVakit)
                        }
                    } else {
                        NamazVaktiHelper.refreshVakitlerFromApi(context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Kapatma sonrası alarm yenileme hatası: ${e.message}")
                }

                sendNotification(context, "🕌 $vakit Vakti Bitti", "Rahatsız etme modu kapatıldı.")
            }
        }
    }

    private fun startRahatsizEtmeServisi(context: Context) {
        try {
            val serviceIntent = Intent(context, RahatsizEtmeServisi::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "RahatsizEtmeServisi başlatılamadı: ${e.message}")
        }
    }

    private fun stopRahatsizEtmeServisi(context: Context) {
        try {
            val serviceIntent = Intent(context, RahatsizEtmeServisi::class.java)
            context.stopService(serviceIntent)
        } catch (e: Exception) {
            Log.e(TAG, "RahatsizEtmeServisi durdurulamadı: ${e.message}")
        }
    }

    private fun startGeriSayimServisi(context: Context, bitisZamani: Long, aktif: Boolean) {
        try {
            val intent = Intent(context, GeriSayimServisi::class.java).apply {
                putExtra("bitis_zamani", bitisZamani)
                putExtra("aktif_mod", aktif)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "GeriSayimServisi başlatılamadı: ${e.message}")
        }
    }

    private fun stopGeriSayimServisi(context: Context) {
        try {
            val durdurIntent = Intent(context, GeriSayimServisi::class.java)
            context.stopService(durdurIntent)
        } catch (e: Exception) {
            Log.e(TAG, "GeriSayimServisi durdurulamadı: ${e.message}")
        }
    }

    private fun playNotification(context: Context, tip: String) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            when (tip) {
                "vakit" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            android.os.VibrationEffect.createWaveform(
                                longArrayOf(0, 500, 200, 500, 200, 500),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 500, 200, 500, 200, 500), -1)
                    }
                }
                "erken" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            android.os.VibrationEffect.createWaveform(
                                longArrayOf(0, 200, 100, 200),
                                -1
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
                    }
                }
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            android.os.VibrationEffect.createOneShot(
                                300,
                                android.os.VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(300)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Titreşim hatası: ${e.message}")
        }
    }

    private fun sendVakitGirdiNotification(context: Context, vakit: String) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID + "_vakit",
                    "Namaz Vakti",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Namaz vakti girdiğinde bildirim"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = Intent(context, NamazVakitleriActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID + "_vakit")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
                .setContentTitle("🕌 $vakit Vakti Girdi")
                .setContentText("Hayırlı ibadetler. Rahatsız etme modu aktif.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("$vakit vakti girdi. Telefonunuz sessize alındı."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(NOTIFICATION_ID_VAKIT, notification)

        } catch (e: Exception) {
            Log.e(TAG, "Vakit bildirimi hatası: ${e.message}")
        }
    }

    private fun updateWidgetDirectly(context: Context, isActive: Boolean) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, UykuModuWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_uyku_modu)

                if (isActive) {
                    views.setTextViewText(R.id.tvWidgetDurum, "🔇 Rahatsız Etme AKTİF")
                    views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#2E7D32"))
                    views.setViewVisibility(R.id.tvGeriSayim, android.view.View.VISIBLE)
                    views.setViewVisibility(R.id.tvSonrakiVakit, android.view.View.GONE)
                } else {
                    views.setTextViewText(R.id.tvWidgetDurum, "⏰ Rahatsız Etme PASİF")
                    views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#616161"))
                    views.setViewVisibility(R.id.tvGeriSayim, android.view.View.GONE)
                    views.setViewVisibility(R.id.tvSonrakiVakit, android.view.View.VISIBLE)
                }

                appWidgetManager.updateAppWidget(appWidgetId, views)
            }

            Log.d(TAG, "✅ Widget doğrudan güncellendi: $isActive")
        } catch (e: Exception) {
            Log.e(TAG, "Widget güncelleme hatası: ${e.message}")
        }
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify((1000..2000).random(), notification)
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim hatası: ${e.message}")
        }
    }
}