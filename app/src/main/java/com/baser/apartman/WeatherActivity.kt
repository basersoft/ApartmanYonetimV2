package com.baser.apartman

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.baser.apartman.weather.WeatherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : BaseActivity() {

    private lateinit var weatherManager: WeatherManager

    // View'lar
    private lateinit var tvCityTitle: TextView
    private lateinit var btnRefresh: ImageButton
    private lateinit var tvTemperature: TextView
    private lateinit var tvWeatherDescription: TextView
    private lateinit var ivWeatherIcon: ImageView
    private lateinit var tvTempMin: TextView
    private lateinit var tvTempMax: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvPressure: TextView
    private lateinit var tvSunrise: TextView
    private lateinit var tvSunset: TextView
    private lateinit var tvLastUpdate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        initViews()
        setupNavigation()
        supportActionBar?.title = "☀️ HAVA DURUMU"
        weatherManager = WeatherManager(this)
        loadWeatherData()
    }

    private fun initViews() {
        tvCityTitle = findViewById(R.id.tvCityTitle)
        btnRefresh = findViewById(R.id.btnRefresh)
        tvTemperature = findViewById(R.id.tvTemperature)
        tvWeatherDescription = findViewById(R.id.tvWeatherDescription)
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon)
        tvTempMin = findViewById(R.id.tvTempMin)
        tvTempMax = findViewById(R.id.tvTempMax)
        tvHumidity = findViewById(R.id.tvHumidity)
        tvPressure = findViewById(R.id.tvPressure)
        tvSunrise = findViewById(R.id.tvSunrise)
        tvSunset = findViewById(R.id.tvSunset)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)

        btnRefresh.setOnClickListener {
            loadWeatherData()
            Toast.makeText(this, "Hava durumu yenileniyor...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Navigation drawer'da bu sayfayı seçili göster
        try {
            val navigationView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.navigation_view)
            navigationView.setCheckedItem(R.id.nav_weather)
        } catch (e: Exception) {
            // NavigationView bulunamazsa hata verme
        }
    }

    private fun loadWeatherData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = weatherManager.getCurrentWeather()
                when (result) {
                    is com.baser.apartman.weather.WeatherResult.Success -> {
                        updateUI(result.weather)
                    }
                    is com.baser.apartman.weather.WeatherResult.Error -> {
                        Toast.makeText(this@WeatherActivity, result.message, Toast.LENGTH_SHORT).show()
                        showSampleData()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@WeatherActivity, "Hava durumu alınamadı", Toast.LENGTH_SHORT).show()
                showSampleData()
            }
        }
    }

    private fun updateUI(weather: com.baser.apartman.weather.WeatherResponse) {
        try {
            tvCityTitle.text = weather.name
            tvTemperature.text = "${weather.main.temp.toInt()}°C"

            val description = weather.weather.firstOrNull()?.description ?: "Bilinmiyor"
            tvWeatherDescription.text = description.uppercase()

            tvTempMin.text = "${weather.main.tempMin.toInt()}°C"
            tvTempMax.text = "${weather.main.tempMax.toInt()}°C"
            tvHumidity.text = "${weather.main.humidity}%"
            tvPressure.text = "${weather.main.pressure} hPa"
            tvSunrise.text = convertTimestampToTimeShort(weather.sys.sunrise)
            tvSunset.text = convertTimestampToTimeShort(weather.sys.sunset)

            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            tvLastUpdate.text = "Son güncelleme: $currentTime"

            // İkon yükleme - Glide eklerseniz kullanabilirsiniz
            // weather.weather.firstOrNull()?.icon?.let { iconCode ->
            //     Glide.with(this).load(weatherManager.getWeatherIconUrl(iconCode)).into(ivWeatherIcon)
            // }

        } catch (e: Exception) {
            e.printStackTrace()
            showSampleData()
        }
    }

    private fun showSampleData() {
        tvCityTitle.text = "Mersin"
        tvTemperature.text = "16°C"
        tvWeatherDescription.text = "HAFİF YAĞMUR"
        tvTempMin.text = "15.9°C"
        tvTempMax.text = "15.9°C"
        tvHumidity.text = "73%"
        tvPressure.text = "1014 hPa"
        tvSunrise.text = "12:16"
        tvSunset.text = "12:17"

        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        tvLastUpdate.text = "Son güncelleme: $currentTime"

        // Örnek ikon göster
        ivWeatherIcon.setImageResource(R.drawable.ic_weather_rain)
    }

    private fun convertTimestampToTimeShort(timestamp: Long): String {
        return try {
            val date = Date(timestamp * 1000)
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault()) // 24 saat formatı
            sdf.format(date)
        } catch (e: Exception) {
            "--:--"
        }
    }
}