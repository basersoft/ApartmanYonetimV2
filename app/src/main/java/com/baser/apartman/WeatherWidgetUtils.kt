package com.baser.apartman.weather

import android.content.Context

object WeatherWidgetUtils {

    fun updateWidgets(context: Context) {
        try {
            val widget = WeatherAppWidget()
            val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
            val componentName = android.content.ComponentName(context, WeatherAppWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            widget.onUpdate(context, appWidgetManager, appWidgetIds)
        } catch (e: Exception) {
            // Widget henüz eklenmemiş olabilir, hata yut
        }
    }

    fun forceRefresh(context: Context) {
        updateWidgets(context)
    }
}