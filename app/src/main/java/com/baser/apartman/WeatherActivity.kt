package com.baser.apartman

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageButton
import com.baser.apartman.weather.WeatherWidget

class WeatherActivity : BaseActivity() {

    private lateinit var weatherWidget: WeatherWidget
    private lateinit var btnRefresh: AppCompatImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        initViews()
        setupListeners()

        // Hava durumu verilerini yükle
        weatherWidget.loadWeather()
    }

    private fun initViews() {
        weatherWidget = findViewById(R.id.weatherWidget)
        btnRefresh = findViewById(R.id.btnRefresh)
    }

    private fun setupListeners() {
        btnRefresh.setOnClickListener {
            weatherWidget.loadWeather()
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
}