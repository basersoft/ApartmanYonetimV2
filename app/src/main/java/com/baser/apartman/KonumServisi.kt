package com.baser.apartman

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import java.util.*

class KonumServisi(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val TAG = "KonumServisi"

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(onResult: (il: String?, ilce: String?) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            onResult(null, null)
            return
        }

        try {
            val locationTask: Task<Location> = fusedLocationClient.lastLocation
            locationTask.addOnSuccessListener { location ->
                if (location != null) {
                    getIlIlceFromLocation(location.latitude, location.longitude, onResult)
                } else {
                    onResult(null, null)
                }
            }.addOnFailureListener {
                onResult(null, null)
            }
        } catch (e: SecurityException) {
            onResult(null, null)
        }
    }

    private fun getIlIlceFromLocation(latitude: Double, longitude: Double, onResult: (String?, String?) -> Unit) {
        try {
            val geocoder = Geocoder(context, Locale("tr"))
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val il = address.adminArea ?: "MERSİN"
                val ilce = address.locality ?: "Yenişehir"

                val duzeltilmisIl = if (il.uppercase(Locale("tr")) == "İÇEL" || il.uppercase(Locale("tr")) == "ICEL")
                    "MERSİN" else il.uppercase(Locale("tr"))

                onResult(duzeltilmisIl, ilce)
            } else {
                onResult("MERSİN", "Yenişehir")
            }
        } catch (e: Exception) {
            onResult("MERSİN", "Yenişehir")
        }
    }
}