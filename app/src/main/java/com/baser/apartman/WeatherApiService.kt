package com.baser.apartman.weather

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApiService {

    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("q") city: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "tr",
        @Query("appid") apiKey: String
    ): WeatherResponse
}

object WeatherApiClient {
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    val service: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}