package com.baser.apartman

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

object ProfileImageManager {

    private var cachedBitmap: Bitmap? = null
    private var lastEmail: String = ""
    private var lastRefreshTime: Long = 0
    private val REFRESH_INTERVAL = 30 * 60 * 1000 // 30 dakika

    fun saveBitmapToCache(context: Context, bitmap: Bitmap, userEmail: String) {
        cachedBitmap = bitmap
        lastEmail = userEmail
        lastRefreshTime = System.currentTimeMillis()
        saveToSharedPrefs(context, bitmap, userEmail)
        println("✅ Profil resmi cache'e kaydedildi")
    }

    fun getCachedBitmap(): Bitmap? = cachedBitmap

    fun loadProfileImage(context: Context, userEmail: String, imageView: ImageView?, forceRefresh: Boolean = false) {
        if (userEmail.isEmpty()) return
        if (imageView == null && cachedBitmap == null && !forceRefresh) return

        println("🔄 ProfileImageManager: Kullanıcı: $userEmail, ForceRefresh: $forceRefresh")

        if (forceRefresh) {
            clearCacheForUser(context, userEmail)
        } else {
            if (lastEmail == userEmail && cachedBitmap != null) {
                val timeSinceLastRefresh = System.currentTimeMillis() - lastRefreshTime
                if (timeSinceLastRefresh < REFRESH_INTERVAL) {
                    println("✅ Cache'ten profil resmi gösteriliyor")
                    setImageWithCircleBackground(context, imageView, cachedBitmap!!)
                    return
                } else {
                    println("🔄 Cache süresi doldu, yenileniyor...")
                }
            }
        }

        val bitmap = loadFromSharedPrefs(context, userEmail)
        if (bitmap != null && !forceRefresh) {
            println("✅ SharedPreferences'tan profil resmi yüklendi")
            cachedBitmap = bitmap
            lastEmail = userEmail
            lastRefreshTime = System.currentTimeMillis()
            setImageWithCircleBackground(context, imageView, bitmap)
            loadFromApiInBackground(context, userEmail, imageView)
            return
        }

        setDefaultProfileImage(context, imageView)
        loadFromApiInBackground(context, userEmail, imageView)
    }

    private fun setImageWithCircleBackground(context: Context, imageView: ImageView?, bitmap: Bitmap) {
        imageView?.let { iv ->
            val roundedBitmap = getRoundedBitmap(bitmap)
            iv.setImageBitmap(roundedBitmap)
            try {
                val circleBackground = ContextCompat.getDrawable(context, R.drawable.circle_background)
                iv.background = circleBackground
            } catch (e: Exception) {
                println("⚠️ Circle background bulunamadı")
            }
        }
    }

    private fun setDefaultProfileImage(context: Context, imageView: ImageView?) {
        imageView?.let { iv ->
            try {
                iv.setImageResource(R.drawable.ic_profile_placeholder)
                val circleBackground = ContextCompat.getDrawable(context, R.drawable.circle_background)
                iv.background = circleBackground
            } catch (e: Exception) {
                println("⚠️ Varsayılan profil resmi bulunamadı")
            }
        }
    }

    private fun getRoundedBitmap(bitmap: Bitmap): Bitmap {
        val size = minOf(bitmap.width, bitmap.height)
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val rect = Rect(0, 0, size, size)

        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(bitmap, rect, rect, paint)

        return output
    }

    /**
     * *** GÜNCELLENMİŞ: API'den profil resmini yükle - ImageView parametresi eklendi ***
     */
    private fun loadFromApiInBackground(context: Context, userEmail: String, imageView: ImageView? = null) {
        Thread {
            try {
                println("🌐 API'den profil resmi yükleniyor: $userEmail")

                val sqlQuery = "SELECT profile_image FROM apartman_users WHERE email = '$userEmail'"

                // Senkron API çağrısı için latch kullan
                val latch = CountDownLatch(1)
                var resultImage: String? = null
                var errorMsg: String? = null

                val url = "http://baser.org/site/api/api_hepsi.php"
                val params = HashMap<String, String>()
                params["query"] = sqlQuery
                params["key"] = "randomkey"

                val stringRequest = object : StringRequest(
                    Request.Method.POST, url,
                    { response ->
                        resultImage = response
                        latch.countDown()
                    },
                    { error ->
                        errorMsg = error.message
                        latch.countDown()
                    }
                ) {
                    override fun getParams(): Map<String, String> = params
                }

                Volley.newRequestQueue(context).add(stringRequest)

                // 30 saniye bekle
                if (latch.await(30, TimeUnit.SECONDS) && resultImage != null) {
                    parseAndLoad(context, resultImage!!, userEmail, imageView)
                } else {
                    println("❌ Profil resmi API zaman aşımı: $errorMsg")
                }
            } catch (e: Exception) {
                println("❌ Profil resmi yükleme hatası: ${e.message}")
            }
        }.start()
    }

    /**
     * *** GÜNCELLENMİŞ: Parse et ve yükle - ImageView parametresi eklendi ***
     */
    private fun parseAndLoad(context: Context, csvResponse: String, userEmail: String, imageView: ImageView? = null) {
        try {
            val lines = csvResponse.trim().split("\n")
            if (lines.size > 1) {
                val headers = lines[0].split(",")
                val userData = lines[1].split(",")

                val profileImageIndex = headers.indexOfFirst { it == "profile_image" }
                if (profileImageIndex != -1 && profileImageIndex < userData.size) {
                    val profileImage = userData[profileImageIndex].replace("\"", "")

                    if (profileImage.isNotEmpty() && profileImage != "null" && profileImage != "NULL") {
                        val imageUrl = "http://baser.org/site/uploads/profile_images/$profileImage"
                        println("🖼️ Profil Resim URL: $imageUrl")

                        loadBitmapFromUrl(context, imageUrl, userEmail, imageView)
                    } else {
                        println("ℹ️ Profil resmi bulunamadı")
                    }
                }
            } else {
                println("ℹ️ Profil resmi bulunamadı")
            }
        } catch (e: Exception) {
            println("❌ Parse hatası: ${e.message}")
        }
    }

    /**
     * *** GÜNCELLENMİŞ: URL'den bitmap yükle - ImageView parametresi eklendi ***
     */
    private fun loadBitmapFromUrl(context: Context, imageUrl: String, userEmail: String, imageView: ImageView? = null) {
        Thread {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                connection.setRequestProperty("Referer", "http://baser.org/")
                connection.setRequestProperty("Accept-Language", "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7")
                connection.setRequestProperty("DNT", "1")
                connection.setRequestProperty("Connection", "keep-alive")

                connection.doInput = true
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.connect()

                val responseCode = connection.responseCode
                println("🌐 HTTP $responseCode: $imageUrl")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream

                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                        inPreferredConfig = Bitmap.Config.RGB_565
                    }

                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    inputStream.close()
                    connection.disconnect()

                    if (bitmap != null) {
                        saveBitmapToCache(context, bitmap, userEmail)

                        // ImageView varsa UI'da güncelle
                        imageView?.let { iv ->
                            android.os.Handler(context.mainLooper).post {
                                setImageWithCircleBackground(context, iv, bitmap)
                            }
                        }
                        println("✅ Profil resmi başarıyla yüklendi")
                    }
                } else {
                    println("❌ HTTP hatası: $responseCode")
                    connection.disconnect()

                    if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        val httpsUrl = imageUrl.replace("http://", "https://")
                        if (httpsUrl != imageUrl) {
                            println("🔄 HTTPS denemesi: $httpsUrl")
                            loadBitmapFromUrl(context, httpsUrl, userEmail, imageView)
                        }
                    }
                }
            } catch (e: Exception) {
                println("❌ Resim yükleme hatası: ${e.message}")
            }
        }.start()
    }

    private fun saveToSharedPrefs(context: Context, bitmap: Bitmap, userEmail: String) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val sharedPref = context.getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString("profile_$userEmail", byteArray.joinToString(","))
                apply()
            }
            println("💾 Profil resmi SharedPreferences'a kaydedildi")
        } catch (e: Exception) {
            println("❌ Cache kayıt hatası: ${e.message}")
        }
    }

    private fun loadFromSharedPrefs(context: Context, userEmail: String): Bitmap? {
        return try {
            val sharedPref = context.getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
            val imageString = sharedPref.getString("profile_$userEmail", null)

            if (!imageString.isNullOrEmpty()) {
                val byteArray = imageString.split(",").map { it.toByte() }.toByteArray()
                val bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                println("📂 SharedPreferences'tan profil resmi yüklendi")
                bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache() {
        cachedBitmap = null
        lastEmail = ""
        lastRefreshTime = 0
        println("🧹 Profil resmi cache temizlendi")
    }

    fun clearCacheForUser(context: Context, userEmail: String) {
        cachedBitmap = null
        lastEmail = ""
        lastRefreshTime = 0

        val profileSharedPref = context.getSharedPreferences("profile_cache", Context.MODE_PRIVATE)
        profileSharedPref.edit().remove("profile_$userEmail").apply()

        println("🧹 $userEmail için cache temizlendi")
    }

    fun forceRefresh(context: Context, userEmail: String, imageView: ImageView?) {
        println("🔄 Profil resmi zorla yenileniyor")
        clearCacheForUser(context, userEmail)
        loadProfileImage(context, userEmail, imageView, true)
    }

    fun isCachedForUser(userEmail: String): Boolean {
        return lastEmail == userEmail && cachedBitmap != null
    }

    fun getLastRefreshTime(): Long = lastRefreshTime

    /**
     * *** GÜNCELLENMİŞ: Sunucuya resim yükleme - Volley kullanarak ***
     */
    fun uploadProfileImage(context: Context, bitmap: Bitmap, userEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        Thread {
            try {
                println("📤 Sunucuya profil resmi yükleniyor...")

                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "profile_${userEmail}_$timeStamp.jpg"

                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val imageBytes = stream.toByteArray()

                val uploadUrl = "http://baser.org/site/upload_profile_image.php"
                val boundary = "Boundary-${System.currentTimeMillis()}"

                val url = URL(uploadUrl)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Connection", "Keep-Alive")
                connection.setRequestProperty("Cache-Control", "no-cache")
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")

                val outputStream = DataOutputStream(connection.outputStream)

                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"email\"\r\n\r\n")
                outputStream.writeBytes("$userEmail\r\n")

                outputStream.writeBytes("--$boundary\r\n")
                outputStream.writeBytes("Content-Disposition: form-data; name=\"profile_image\"; filename=\"$fileName\"\r\n")
                outputStream.writeBytes("Content-Type: image/jpeg\r\n\r\n")

                outputStream.write(imageBytes)
                outputStream.writeBytes("\r\n")
                outputStream.writeBytes("--$boundary--\r\n")
                outputStream.flush()
                outputStream.close()

                val responseCode = connection.responseCode
                val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val response = inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                println("📤 Upload yanıtı ($responseCode): $response")

                if (responseCode == HttpURLConnection.HTTP_OK && response.contains("success", ignoreCase = true)) {
                    updateProfileImageInDatabase(context, fileName, userEmail, onSuccess, onError)
                } else {
                    onError("Sunucu hatası: $responseCode")
                }

            } catch (e: Exception) {
                println("❌ Upload hatası: ${e.message}")
                onError(e.message ?: "Bilinmeyen hata")
            }
        }.start()
    }

    /**
     * *** GÜNCELLENMİŞ: Veritabanını güncelleme - Volley ile ***
     */
    private fun updateProfileImageInDatabase(context: Context, fileName: String, userEmail: String,
                                             onSuccess: () -> Unit, onError: (String) -> Unit) {
        val sqlQuery = "UPDATE apartman_users SET profile_image = '$fileName' WHERE email = '$userEmail'"
        val url = "http://baser.org/site/api/api_hepsi.php"

        val params = HashMap<String, String>()
        params["query"] = sqlQuery
        params["key"] = "randomkey"

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                println("✅ Veritabanı güncellendi: $response")
                clearCacheForUser(context, userEmail)
                onSuccess()
            },
            { error ->
                println("❌ Veritabanı güncelleme hatası: ${error.message}")
                onError("Veritabanı güncellenemedi: ${error.message}")
            }
        ) {
            override fun getParams(): Map<String, String> = params
        }

        Volley.newRequestQueue(context).add(stringRequest)
    }

    fun processSelectedImage(context: Context, bitmap: Bitmap, userEmail: String,
                             onSuccess: () -> Unit, onError: (String) -> Unit) {
        println("🖼️ Seçilen resim işleniyor...")

        saveBitmapToCache(context, bitmap, userEmail)
        uploadProfileImage(context, bitmap, userEmail, onSuccess, onError)
    }
}