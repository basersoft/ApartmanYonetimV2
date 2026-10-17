package com.baser.apartman

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "📱 Cihaz açılışı algılandı: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
            val isAutoEnabled = prefs.getBoolean("otomatik_aktif", false)

            if (isAutoEnabled) {
                Log.d(TAG, "🕌 Otomatik kontrol aktif, namaz alarmları yeniden kuruluyor...")

                // 1. Önce kayıtlı vakitlerle alarmları hemen kur (gecikme olmasın)
                val kayitli = NamazVaktiHelper.getVakitlerFromPrefs(context)
                if (kayitli != null) {
                    NamazVaktiHelper.scheduleNamazAlarms(context, kayitli)
                }

                // 2. Ardından API'den taze vakitleri çek
                NamazVaktiHelper.refreshVakitlerFromApi(context)

                // 3. Namaz servisini başlat
                try {
                    val serviceIntent = Intent(context, NamazVaktiServisi::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Servis başlatma hatası: ${e.message}")
                }
            }
        }
    }
}
