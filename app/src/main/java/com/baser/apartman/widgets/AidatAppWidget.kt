package com.baser.apartman.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.widget.RemoteViews
import com.baser.apartman.AidatActivity
import com.baser.apartman.R

class AidatAppWidget : AppWidgetProvider() {

    companion object {
        private const val TAG = "AidatAppWidget"
        private const val PREFS_NAME = "widget_prefs"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_GECIKMIS_SAYI = "gecikmis_sayi"
        private const val KEY_TOPLAM_BORC = "toplam_borc"
        private const val KEY_ODENECEK_SAYI = "odenecek_aidat_sayisi"
        private const val KEY_SON_AY = "son_ay"
        private const val KEY_WIDGET_MESAJ = "widget_mesaj"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            updateAllWidgets(context)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        try {
            val views = RemoteViews(context.packageName, R.layout.widget_aidat)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val userEmail = prefs.getString(KEY_USER_EMAIL, "") ?: ""
            val userName = prefs.getString(KEY_USER_NAME, "") ?: ""
            val userType = prefs.getString(KEY_USER_TYPE, "user") ?: "user"
            val gecikmisSayi = prefs.getInt(KEY_GECIKMIS_SAYI, -1)
            val toplamBorc = prefs.getFloat(KEY_TOPLAM_BORC, -1f)
            val odenecekSayi = prefs.getInt(KEY_ODENECEK_SAYI, -1)
            val sonAy = prefs.getString(KEY_SON_AY, "") ?: ""
            val widgetMesaj = prefs.getString(KEY_WIDGET_MESAJ, "") ?: ""

            Log.d(TAG, "Widget: Email=$userEmail, Gecikmiş=$gecikmisSayi, Borç=$toplamBorc")

            if (userEmail.isEmpty()) {
                views.setTextViewText(R.id.widget_title, "🏢 Aidat Takibi")
                views.setTextViewText(R.id.widget_status, "👤 Giriş yapın")
                views.setInt(R.id.widget_layout, "setBackgroundColor", Color.parseColor("#7f8c8d"))
            }
            else if (gecikmisSayi == -1 || toplamBorc == -1f) {
                views.setTextViewText(R.id.widget_title, "🏢 $userName")
                views.setTextViewText(R.id.widget_status, "🔄 Veri yükleniyor...")
                views.setInt(R.id.widget_layout, "setBackgroundColor", Color.parseColor("#f39c12"))
            }
            else {
                views.setTextViewText(R.id.widget_title, "🏢 $userName")

                val ayText = if (sonAy.length > 7) sonAy.take(7) else sonAy

                val statusText = when {
                    gecikmisSayi > 0 -> "⚠️ $gecikmisSayi gecikmiş aidat!\n💰 ${String.format("%.2f", toplamBorc)} TL"
                    odenecekSayi > 0 -> "📅 $odenecekSayi aidat bekliyor\n$ayText"
                    widgetMesaj.isNotEmpty() && widgetMesaj != "Yükleniyor..." -> widgetMesaj
                    else -> "✅ Tüm aidatlar güncel\n$ayText"
                }

                views.setTextViewText(R.id.widget_status, statusText)

                val bgColor = when {
                    gecikmisSayi > 0 -> Color.parseColor("#e74c3c")
                    odenecekSayi > 0 -> Color.parseColor("#f39c12")
                    else -> Color.parseColor("#2ecc71")
                }
                views.setInt(R.id.widget_layout, "setBackgroundColor", bgColor)
            }

            val intent = Intent(context, AidatActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                putExtra("user_email", userEmail)
                putExtra("user_type", userType)
                putExtra("user_name", userName)
                putExtra("from_widget", true)
            }

            val pendingIntent = PendingIntent.getActivity(
                context, appWidgetId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_layout, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)

        } catch (e: Exception) {
            Log.e(TAG, "Widget hatası: ${e.message}")
        }
    }

    fun updateAllWidgets(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, AidatAppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            if (appWidgetIds.isNotEmpty()) {
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateAllWidgets hatası: ${e.message}")
        }
    }

    fun updateWidgetData(
        context: Context,
        userEmail: String,
        userName: String,
        userType: String,
        gecikmisSayi: Int,
        toplamBorc: Float,
        odenecekSayi: Int,
        sonAy: String,
        mesaj: String = ""
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_USER_EMAIL, userEmail)
            putString(KEY_USER_NAME, userName)
            putString(KEY_USER_TYPE, userType)
            putInt(KEY_GECIKMIS_SAYI, gecikmisSayi)
            putFloat(KEY_TOPLAM_BORC, toplamBorc)
            putInt(KEY_ODENECEK_SAYI, odenecekSayi)
            putString(KEY_SON_AY, sonAy)
            putString(KEY_WIDGET_MESAJ, mesaj)
            apply()
        }
        updateAllWidgets(context)
    }

    fun clearWidgetData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        updateAllWidgets(context)
    }
}