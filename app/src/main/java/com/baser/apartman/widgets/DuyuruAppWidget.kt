package com.baser.apartman.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.baser.apartman.DuyurularActivity
import com.baser.apartman.R

class DuyuruAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_duyuru)
            val sharedPreferences = context.getSharedPreferences("duyuru_widget_prefs", Context.MODE_PRIVATE)

            // SharedPreferences'tan verileri al
            val widgetMesaj = sharedPreferences.getString("widget_mesaj", "Duyuru Yok") ?: "Duyuru Yok"
            val sonBaslik = sharedPreferences.getString("son_duyuru_baslik", "") ?: ""
            val sonTarih = sharedPreferences.getString("son_duyuru_tarih", "") ?: ""
            val toplamDuyuruSayisi = sharedPreferences.getInt("toplam_duyuru_sayisi", 0)
            val userEmail = sharedPreferences.getString("user_email", "") ?: ""

            println("🔍 DUYURU WIDGET - KAYITLI EMAIL: $userEmail")
            println("🔍 DUYURU WIDGET - MESAJ: $widgetMesaj")

            // Widget içeriğini güncelle
            views.setTextViewText(R.id.widget_title, "Son Duyuru")

            // Duruma göre detaylı mesaj
            val statusText = widgetMesaj

            views.setTextViewText(R.id.widget_status, statusText)

            // Duruma göre arkaplan rengi
            val bgColor = when {
                toplamDuyuruSayisi > 0 -> Color.parseColor("#9b59b6") // Mor - Duyuru var
                else -> Color.parseColor("#95a5a6") // Gri - Duyuru yok
            }

            views.setInt(R.id.widget_layout, "setBackgroundColor", bgColor)

            // Tıklanma işlemi - DuyurularActivity'ye yönlendir
            val intent = Intent(context, DuyurularActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("user_email", userEmail)
                putExtra("user_type", sharedPreferences.getString("user_type", "user"))
                putExtra("user_name", sharedPreferences.getString("user_name", ""))
                putExtra("from_widget", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = android.content.ComponentName(context, DuyuruAppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}