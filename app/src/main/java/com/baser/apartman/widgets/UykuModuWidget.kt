package com.baser.apartman.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import android.widget.Toast
import com.baser.apartman.GeriSayimServisi
import com.baser.apartman.NamazVakitleriActivity
import com.baser.apartman.R
import com.baser.apartman.RahatsizEtmeServisi

class UykuModuWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_TOGGLE = "com.baser.apartman.TOGGLE_UYKU_MODU"
        const val ACTION_UPDATE_WIDGET = "com.baser.apartman.UPDATE_WIDGET"
        const val ACTION_OPEN_ACTIVITY = "com.baser.apartman.OPEN_NAMAZ_ACTIVITY"
        private const val TAG = "UykuModuWidget"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called for ${appWidgetIds.size} widgets")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        ensureGeriSayimServisiRunning(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: ${intent.action}")
        when (intent.action) {
            ACTION_TOGGLE -> {
                toggleUykuModu(context)
                updateAllWidgets(context)
            }
            ACTION_OPEN_ACTIVITY -> {
                val openIntent = Intent(context, NamazVakitleriActivity::class.java)
                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(openIntent)
            }
            ACTION_UPDATE_WIDGET -> {
                updateAllWidgets(context)
            }
            AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                updateAllWidgets(context)
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun ensureGeriSayimServisiRunning(context: Context) {
        val prefs = context.getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("aktif", false)

        val geriSayimIntent = Intent(context, GeriSayimServisi::class.java).apply {
            putExtra("aktif_mod", isActive)
            if (isActive) {
                val dndPrefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
                val dndSuresi = dndPrefs.getInt("dnd_suresi", 30)
                putExtra("bitis_zamani", System.currentTimeMillis() + (dndSuresi * 60 * 1000))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(geriSayimIntent)
        } else {
            context.startService(geriSayimIntent)
        }
    }

    private fun toggleUykuModu(context: Context) {
        val namazPrefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val isAutoEnabled = namazPrefs.getBoolean("otomatik_aktif", false)

        if (!isAutoEnabled) {
            Log.d(TAG, "toggleUykuModu: Otomatik kontrol kapalı, manuel aç/kapa devre dışı")
            Toast.makeText(context, "Önce Namaz Vakitleri'nden Otomatik Kontrol'ü açın", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = context.getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("aktif", false)
        val newState = !isActive

        Log.d(TAG, "toggleUykuModu: $isActive -> $newState")

        prefs.edit().putBoolean("aktif", newState).apply()

        val intent = Intent(context, RahatsizEtmeServisi::class.java)
        val geriSayimIntent = Intent(context, GeriSayimServisi::class.java)

        if (newState) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            val dndPrefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
            val dndSuresi = dndPrefs.getInt("dnd_suresi", 30)
            val bitisZamani = System.currentTimeMillis() + (dndSuresi * 60 * 1000)

            context.stopService(geriSayimIntent)
            geriSayimIntent.apply {
                putExtra("bitis_zamani", bitisZamani)
                putExtra("aktif_mod", true)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(geriSayimIntent)
            } else {
                context.startService(geriSayimIntent)
            }
        } else {
            context.stopService(intent)
            context.stopService(geriSayimIntent)

            geriSayimIntent.putExtra("aktif_mod", false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(geriSayimIntent)
            } else {
                context.startService(geriSayimIntent)
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, UykuModuWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isNotEmpty()) {
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val prefs = context.getSharedPreferences("rahatsiz_etme_modu", Context.MODE_PRIVATE)
        val isActive = prefs.getBoolean("aktif", false)

        val namazPrefs = context.getSharedPreferences("namaz_vakitleri", Context.MODE_PRIVATE)
        val isAutoEnabled = namazPrefs.getBoolean("otomatik_aktif", false)

        val views = RemoteViews(context.packageName, R.layout.widget_uyku_modu)

        // Widget arka plan ve durum metnini ayarla
        when {
            !isAutoEnabled -> {
                views.setTextViewText(R.id.tvWidgetDurum, "⏰ Otomatik Kontrol KAPALI")
                views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#757575"))
                views.setViewVisibility(R.id.tvGeriSayim, android.view.View.GONE)
                views.setViewVisibility(R.id.tvSonrakiVakit, android.view.View.VISIBLE)
                views.setTextViewText(R.id.tvSonrakiVakit, "Ayarları açmak için tıklayın")
                views.setImageViewResource(R.id.btnToggle, android.R.drawable.ic_menu_edit)
            }
            isActive && isAutoEnabled -> {
                views.setTextViewText(R.id.tvWidgetDurum, "🔇 Rahatsız Etme AKTİF")
                views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#2E7D32"))
                views.setViewVisibility(R.id.tvGeriSayim, android.view.View.VISIBLE)
                views.setViewVisibility(R.id.tvSonrakiVakit, android.view.View.GONE)
                views.setImageViewResource(R.id.btnToggle, android.R.drawable.ic_lock_silent_mode)
            }
            else -> {
                views.setTextViewText(R.id.tvWidgetDurum, "⏰ Rahatsız Etme PASİF")
                views.setInt(R.id.widgetLayout, "setBackgroundColor", Color.parseColor("#616161"))
                views.setViewVisibility(R.id.tvGeriSayim, android.view.View.GONE)
                views.setViewVisibility(R.id.tvSonrakiVakit, android.view.View.VISIBLE)
                views.setImageViewResource(R.id.btnToggle, android.R.drawable.ic_lock_silent_mode)
            }
        }

        // 1. BUTON TIKLAMASI (Manuel Aç/Kapa)
        val toggleIntent = Intent(context, UykuModuWidget::class.java).apply {
            action = ACTION_TOGGLE
        }

        val togglePendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                appWidgetId,
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        views.setOnClickPendingIntent(R.id.btnToggle, togglePendingIntent)

        // 2. WIDGET LAYOUT TIKLAMASI (NamazVakitleriActivity'e git)
        val openIntent = Intent(context, UykuModuWidget::class.java).apply {
            action = ACTION_OPEN_ACTIVITY
        }

        val openPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                appWidgetId + 1000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                appWidgetId + 1000,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }
        views.setOnClickPendingIntent(R.id.widgetLayout, openPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}