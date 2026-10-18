package com.baser.apartman.weather

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WeatherManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
    private val cachePrefs: SharedPreferences = context.getSharedPreferences("weather_cache", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "1bbb3dd18368b175dc8fb10ba81b82ec"
        private const val DEFAULT_CITY = "Mersin"
    }

    suspend fun getCurrentWeather(): WeatherResult {
        return try {
            val lastCity = getLastCity()
            val weather = WeatherApiClient.service.getCurrentWeather(
                city = lastCity,
                apiKey = API_KEY
            )
            saveLastCity(weather.name)
            // Veriyi cache'le
            cacheWeatherData(weather)
            WeatherResult.Success(weather)

        } catch (e: Exception) {
            android.util.Log.e("WeatherManager", "API Error: ${e.message}")
            WeatherResult.Error(e.message ?: "Hava durumu alınamadı")
        }
    }

    // Cache'lenmiş hava durumu verilerini al
    fun getCachedWeatherData(): Map<String, String> {
        try {
            val currentTime = System.currentTimeMillis()
            val lastUpdate = cachePrefs.getLong("last_update", 0)

            // Cache 30 dakikadan eskiyse boş map döndür
            if (currentTime - lastUpdate > 30 * 60 * 1000) {
                return emptyMap()
            }

            return mapOf(
                "temp" to (cachePrefs.getString("cached_temp", "18") ?: "18"),
                "humidity" to (cachePrefs.getString("cached_humidity", "48") ?: "48"),
                "description" to (cachePrefs.getString("cached_description", "Parçalı Bulutlu") ?: "Parçalı Bulutlu")
            )
        } catch (e: Exception) {
            return emptyMap()
        }
    }

    // Hava durumu verilerini cache'le
    private fun cacheWeatherData(weather: WeatherResponse) {
        try {
            with(cachePrefs.edit()) {
                putString("cached_temp", weather.main.temp.toInt().toString())
                putString("cached_humidity", weather.main.humidity.toString())
                putString("cached_description", weather.weather.firstOrNull()?.description ?: "")
                putLong("last_update", System.currentTimeMillis())
                apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("WeatherManager", "Cache error: ${e.message}")
        }
    }

    // Cache'in geçerliliğini kontrol et
    fun isCacheValid(): Boolean {
        val lastUpdate = cachePrefs.getLong("last_update", 0)
        return (System.currentTimeMillis() - lastUpdate) < (30 * 60 * 1000) // 30 dakika
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