package com.baser.apartman

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

class NamazVaktiApi(private val context: Context) {

    private val queue = Volley.newRequestQueue(context)
    private val TAG = "NamazVaktiApi"

    // E-mushaf API (Diyanet verisi)
    private val EMUSHAF_API = "https://ezanvakti.emushaf.net/vakitler/"

    // Aladhan API (yedek)
    private val ALADHAN_API = "http://api.aladhan.com/v1/timingsByCity"

    // Şehir ID'leri
    private val sehirIdMap = mapOf(
        "MERSİN" to "9737",
        "ADANA" to "9544",
        "ANTALYA" to "9588",
        "ANKARA" to "9525",
        "İSTANBUL" to "9554",
        "İZMİR" to "9727",
        "BURSA" to "9614",
        "KONYA" to "9690"
    )

    fun getNamazVakitleri(
        il: String,
        ilce: String,
        onSuccess: (List<NamazVakti>) -> Unit,
        onError: (String) -> Unit
    ) {
        val sehirId = sehirIdMap[il.uppercase()] ?: "9737"
        val url = "$EMUSHAF_API$sehirId"

        Log.d(TAG, "📡 E-mushaf API çağrısı: $url")

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val vakitler = parseEmushafResponse(response, ilce)
                    if (vakitler.isNotEmpty()) {
                        Log.d(TAG, "✅ E-mushaf'tan ${vakitler.size} günlük vakit alındı")
                        onSuccess(vakitler)
                    } else {
                        onError("Vakit bilgisi bulunamadı")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "JSON parse hatası: ${e.message}")
                    onError("Veri işlenirken hata oluştu")
                }
            },
            { error ->
                Log.e(TAG, "E-mushaf bağlantı hatası: ${error.message}")
                getYedekNamazVakitleri(il, ilce, onSuccess, onError)
            }
        )

        queue.add(request)
    }

    private fun parseEmushafResponse(response: JSONArray, ilceAdi: String): List<NamazVakti> {
        val vakitler = mutableListOf<NamazVakti>()

        for (i in 0 until response.length()) {
            try {
                val gun = response.getJSONObject(i)

                val vakti = NamazVakti(
                    ilce = ilceAdi,
                    imsak = gun.getString("Imsak"),
                    gunes = gun.getString("Gunes"),
                    ogle = gun.getString("Ogle"),
                    ikindi = gun.getString("Ikindi"),
                    aksam = gun.getString("Aksam"),
                    yatsi = gun.getString("Yatsi"),
                    tarih = gun.getString("MiladiTarihKisa")
                )
                vakitler.add(vakti)

            } catch (e: Exception) {
                Log.e(TAG, "Gün parse hatası: ${e.message}")
            }
        }

        return vakitler
    }

    fun getBugununVakti(
        il: String,
        ilce: String,
        onSuccess: (NamazVakti) -> Unit,
        onError: (String) -> Unit
    ) {
        getNamazVakitleri(il, ilce, { vakitler ->
            if (vakitler.isNotEmpty()) {
                val bugun = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale("tr")).format(java.util.Date())
                val bugununVakti = vakitler.find { it.tarih == bugun } ?: vakitler[0]
                onSuccess(bugununVakti)
            } else {
                onError("Vakit bulunamadı")
            }
        }, onError)
    }

    fun getYedekNamazVakitleri(
        il: String,
        ilce: String,
        onSuccess: (List<NamazVakti>) -> Unit,
        onError: (String) -> Unit
    ) {
        val url = "$ALADHAN_API?city=$ilce&country=Türkiye&method=13"

        Log.d(TAG, "📡 Aladhan yedek API çağrısı: $url")

        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    val data = response.getJSONObject("data")
                    val timings = data.getJSONObject("timings")
                    val dateInfo = data.getJSONObject("date")
                    val gregorian = dateInfo.getJSONObject("gregorian")

                    val namazVakti = NamazVakti(
                        ilce = ilce,
                        imsak = timings.getString("Imsak"),
                        gunes = timings.getString("Sunrise"),
                        ogle = timings.getString("Dhuhr"),
                        ikindi = timings.getString("Asr"),
                        aksam = timings.getString("Maghrib"),
                        yatsi = timings.getString("Isha"),
                        tarih = gregorian.getString("date")
                    )

                    onSuccess(listOf(namazVakti))

                } catch (e: Exception) {
                    Log.e(TAG, "Aladhan parse hatası: ${e.message}")
                    onError("Yedek API hatası")
                }
            },
            { error ->
                Log.e(TAG, "Aladhan bağlantı hatası: ${error.message}")
                onError("Yedek API bağlantı hatası")
            }
        )

        queue.add(request)
    }
}