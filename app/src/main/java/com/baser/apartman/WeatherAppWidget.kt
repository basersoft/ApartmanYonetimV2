package com.baser.apartman.weather

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.baser.apartman.MainActivity
import com.baser.apartman.R
import com.baser.apartman.WeatherActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherAppWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // İlk widget eklendiğinde
    }

    override fun onDisabled(context: Context) {
        // Son widget kaldırıldığında
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_weather_app)

        // Tıklanınca uygulamayı aç
        val intent = Intent(context, WeatherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

        // Varsayılan değerleri göster
        views.setTextViewText(R.id.tvTemperature, "--°C")
        views.setTextViewText(R.id.tvWeatherDescription, "Yükleniyor...")
        views.setTextViewText(R.id.tvLocation, "Mersin")
        views.setTextViewText(R.id.tvFeelsLike, "--°C")
        views.setTextViewText(R.id.tvHumidity, "--%")
        views.setTextViewText(R.id.tvWind, "--m/s")
        views.setTextViewText(R.id.tvLastUpdate, "--:--")

        // Widget'ı güncelle
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // Hava durumu verilerini yükle
        loadWeatherData(context, views, appWidgetManager, appWidgetId)
    }

    private fun loadWeatherData(
        context: Context,
        views: RemoteViews,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val weatherManager = WeatherManager(context)
                when (val result = weatherManager.getCurrentWeather()) {
                    is WeatherResult.Success -> {
                        updateWidgetViews(context, views, result.weather)
                    }
                    is WeatherResult.Error -> {
                        views.setTextViewText(R.id.tvWeatherDescription, "Hata")
                    }
                }
            } catch (e: Exception) {
                views.setTextViewText(R.id.tvWeatherDescription, "Bağlantı yok")
            }

            // Widget'ı güncelle
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun updateWidgetViews(
        context: Context,
        views: RemoteViews,
        weather: WeatherResponse
    ) {
        try {
            // Temel bilgiler
            views.setTextViewText(R.id.tvTemperature, "${weather.main.temp.toInt()}°C")

            // Açıklama
            val description = weather.weather.firstOrNull()?.description ?: "Bilinmiyor"
            views.setTextViewText(R.id.tvWeatherDescription, description)

            // Konum
            views.setTextViewText(R.id.tvLocation, weather.name)

            // Detaylı bilgiler
            views.setTextViewText(R.id.tvFeelsLike, "${weather.main.feelsLike.toInt()}°C")
            views.setTextViewText(R.id.tvHumidity, "${weather.main.humidity}%")
            views.setTextViewText(R.id.tvWind, "${weather.wind.speed} m/s")

            // Güncelleme zamanı
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            views.setTextViewText(R.id.tvLastUpdate, currentTime)

            // İkon
            val iconCode = weather.weather.firstOrNull()?.icon ?: "01d"
            val iconRes = getWeatherIconResource(iconCode)
            views.setImageViewResource(R.id.ivWeatherIcon, iconRes)

        } catch (e: Exception) {
            android.util.Log.e("WeatherWidget", "Error updating widget: ${e.message}")
        }
    }

    private fun getWeatherIconResource(iconCode: String): Int {
        return when (iconCode) {
            "01d" -> R.drawable.ic_sunny
            "01n" -> R.drawable.ic_clear_night
            "02d", "03d", "04d" -> R.drawable.ic_cloudy
            "02n", "03n", "04n" -> R.drawable.ic_cloudy_night
            "09d", "09n", "10d", "10n" -> R.drawable.ic_rain
            "11d", "11n" -> R.drawable.ic_storm
            "13d", "13n" -> R.drawable.ic_snow
            "50d", "50n" -> R.drawable.ic_fog
            else -> R.drawable.ic_sunny
        }
    }

    // Manuel güncelleme için
    fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, WeatherAppWidget::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        onUpdate(context, appWidgetManager, appWidgetIds)
    }
}