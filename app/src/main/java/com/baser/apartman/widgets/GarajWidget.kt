package com.baser.apartman.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.baser.apartman.R

class GarajWidget : AppWidgetProvider() {

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
        val views = RemoteViews(context.packageName, R.layout.widget_garaj)

        // Widget'a tıklanınca broadcast gönder
        val intent = Intent(context, GarajWidget::class.java).apply {
            action = "OPEN_GARAGE_FROM_WIDGET"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.widgetButton, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        super.onReceive(context, intent)

        if (intent?.action == "OPEN_GARAGE_FROM_WIDGET") {
            // Garaj açma işlemini başlat
            context?.let {
                val serviceIntent = Intent(it, GarajAcaService::class.java)
                it.startService(serviceIntent)
            }
        }
    }
}