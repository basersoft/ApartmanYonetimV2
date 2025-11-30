package com.baser.apartman.weather

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "1bbb3dd18368b175dc8fb10ba81b82ec"
        private const val DEFAULT_CITY = "Mersin"
    }

    suspend fun getCurrentWeather(): WeatherResult {
        return try {
            // SADECE GERÇEK API'Yİ KULLAN
            val lastCity = getLastCity()
            val weather = WeatherApiClient.service.getCurrentWeather(
                city = lastCity,
                apiKey = API_KEY
            )
            saveLastCity(weather.name)
            WeatherResult.Success(weather)

        } catch (e: Exception) {
            // Sadece hata döndür
            android.util.Log.e("WeatherManager", "API Error: ${e.message}")
            WeatherResult.Error(e.message ?: "Hava durumu alınamadı")
        }
    }

    private fun saveLastCity(city: String) {
        prefs.edit().putString("last_city", city).apply()
    }

    private fun getLastCity(): String {
        return prefs.getString("last_city", DEFAULT_CITY) ?: DEFAULT_CITY
    }

    fun getWeatherIconUrl(iconCode: String): String {
        return "https://openweathermap.org/img/wn/${iconCode}@2x.png"
    }

    fun formatTemperature(temp: Double): String {
        return "${temp.toInt()}°C"
    }
}

sealed class WeatherResult {
    data class Success(val weather: WeatherResponse) : WeatherResult()
    data class Error(val message: String) : WeatherResult()
}