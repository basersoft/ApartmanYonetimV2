package com.baser.apartman.weather

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.baser.apartman.R
import com.bumptech.glide.Glide
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WeatherWidget @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val weatherManager = WeatherManager(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // View'lar - TÜM TextView'lar tanımlandı
    private lateinit var ivWeatherIcon: android.widget.ImageView
    private lateinit var tvTemperature: android.widget.TextView
    private lateinit var tvWeatherDescription: android.widget.TextView
    private lateinit var tvLocation: android.widget.TextView
    private lateinit var tvFeelsLike: android.widget.TextView
    private lateinit var tvHumidity: android.widget.TextView
    private lateinit var tvWind: android.widget.TextView
    private lateinit var tvPressure: android.widget.TextView
    private lateinit var tvLastUpdate: android.widget.TextView
    private lateinit var btnRefresh: android.widget.ImageButton

    init {
        initViews()
        setupClickListeners()
        loadWeather()
    }

    private fun initViews() {
        val view = LayoutInflater.from(context).inflate(R.layout.widget_weather, this, true)

        // TÜM View'ları doğru şekilde bağla
        ivWeatherIcon = view.findViewById(R.id.ivWeatherIcon)
        tvTemperature = view.findViewById(R.id.tvTemperature)
        tvWeatherDescription = view.findViewById(R.id.tvWeatherDescription)
        tvLocation = view.findViewById(R.id.tvLocation)
        tvFeelsLike = view.findViewById(R.id.tvFeelsLike)
        tvHumidity = view.findViewById(R.id.tvHumidity)
        tvWind = view.findViewById(R.id.tvWind)
        tvPressure = view.findViewById(R.id.tvPressure)
        tvLastUpdate = view.findViewById(R.id.tvLastUpdate)
        btnRefresh = view.findViewById(R.id.btnRefresh)
    }

    private fun setupClickListeners() {
        btnRefresh.setOnClickListener {
            loadWeather()
        }
    }

    fun loadWeather() {
        tvWeatherDescription.text = "Güncelleniyor..."

        coroutineScope.launch {
            try {
                when (val result = weatherManager.getCurrentWeather()) {
                    is WeatherResult.Success -> {
                        displayWeather(result.weather)
                    }
                    is WeatherResult.Error -> {
                        tvWeatherDescription.text = result.message
                        setDefaultValues()
                    }
                }
            } catch (e: Exception) {
                tvWeatherDescription.text = "Bağlantı hatası"
                setDefaultValues()
            }
        }
    }

    private fun displayWeather(weather: WeatherResponse) {
        try {
            // Temel bilgiler
            tvTemperature.text = weatherManager.formatTemperature(weather.main.temp)

            // Açıklama
            val description = weather.weather.firstOrNull()?.description ?: "Bilinmiyor"
            tvWeatherDescription.text = description

            // Konum
            tvLocation.text = "${weather.name}, ${weather.sys.country}"

            // Detaylı bilgiler - ARTIK findViewById KULLANMIYORUZ
            tvFeelsLike.text = "${weather.main.feelsLike.toInt()}°C"
            tvHumidity.text = "${weather.main.humidity}%"
            tvWind.text = "${weather.wind.speed} m/s"
            tvPressure.text = "${weather.main.pressure} hPa"

            // İkon
            weather.weather.firstOrNull()?.icon?.let { iconCode ->
                Glide.with(context)
                    .load(weatherManager.getWeatherIconUrl(iconCode))
                    .into(ivWeatherIcon)
            }

            // Son güncelleme
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            tvLastUpdate.text = "Son güncelleme: $currentTime"

        } catch (e: Exception) {
            android.util.Log.e("WeatherWidget", "Error displaying weather: ${e.message}")
        }
    }

    private fun setDefaultValues() {
        tvTemperature.text = "--°C"
        tvLocation.text = "--"
        tvFeelsLike.text = "--°C"
        tvHumidity.text = "--%"
        tvWind.text = "-- m/s"
        tvPressure.text = "-- hPa"
        tvLastUpdate.text = "Son güncelleme: --"
    }

    // Memory leak'i önlemek için coroutine scope'u temizle
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        coroutineScope.cancel()
    }
}