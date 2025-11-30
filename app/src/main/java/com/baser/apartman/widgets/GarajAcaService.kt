package com.baser.apartman.widgets

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.baser.apartman.R

class GarajAcaService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Widget'a tıklandığında titreşim ve ses
        playSoundAndVibration()
        openGarageFromWidget()
        stopSelf() // İşlem bitince servisi durdur
        return START_NOT_STICKY
    }

    private fun playSoundAndVibration() {
        try {
            // Titreşim efekti
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(150)
                }
            }

            // Sistem click sesi
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun openGarageFromWidget() {
        val prefs = getSharedPreferences("ApartmanPrefs", MODE_PRIVATE)
        val blynkToken = prefs.getString("blynk_token", "") ?: ""
        val blynkPin = prefs.getString("blynk_pin", "V1") ?: "V1"
        val blynkUrl = "https://sgp1.blynk.cloud/external/api/update?token=$blynkToken&$blynkPin=1"

        val stringRequest = StringRequest(
            Request.Method.GET,
            blynkUrl,
            { response ->
                Toast.makeText(this, "✅ Garaj açıldı", Toast.LENGTH_LONG).show()
            },
            { error ->
                Toast.makeText(this, "❌ Hata: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )

        stringRequest.retryPolicy = DefaultRetryPolicy(
            10000,
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(stringRequest)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}