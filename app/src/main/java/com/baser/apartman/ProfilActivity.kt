package com.baser.apartman

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.HashMap

class ProfilActivity : BaseActivity() {

    private lateinit var ivProfilResmi: ImageView
    private lateinit var tvAdSoyad: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvTelefon: TextView
    private lateinit var tvBlok: TextView
    private lateinit var tvDaireNo: TextView
    private lateinit var tvKullaniciTipi: TextView
    private lateinit var tvSakinTipi: TextView
    private lateinit var tvAidatSorumlu: TextView
    private lateinit var tvIkametDurumu: TextView
    private lateinit var tvKayitTarihi: TextView
    private lateinit var tvTarihFormatli: TextView
    private lateinit var tvHatirlatici: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        setupNavigation()
        setupViews()
        loadUserProfile()
    }

    // Artık override edebiliriz çünkü BaseActivity'de open yaptık
    override fun setupNavigation() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        if (toolbar == null || drawerLayout == null) {
            println("❌ Toolbar veya DrawerLayout bulunamadı")
            return
        }

        // Toolbar'ı ayarla
        setSupportActionBar(toolbar)

        // HAMBURGER İKONU İÇİN ÖNEMLİ: Display options'ı sıfırla
        supportActionBar?.setDisplayHomeAsUpEnabled(false) // Önce kapat
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // ActionBarDrawerToggle oluştur - HAMBURGER İÇİN
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState() // BU ÇOK ÖNEMLİ!

        // Hamburger ikonunu zorla göster
        toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.white, null)

        // Başlık ayarla
        supportActionBar?.title = "Profilim"

        println("✅ Hamburger ikonu ayarlandı")

        // Navigation listener
        navigationView?.setNavigationItemSelectedListener(this)

        // Header bilgilerini güncelle
        val headerView = navigationView?.getHeaderView(0)
        headerView?.let {
            val tvUserName = it.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = it.findViewById<TextView>(R.id.tvUserEmail)

            tvUserName.text = userName
            tvUserEmail.text = userEmail
        }
    }
    private fun setupViews() {
        ivProfilResmi = findViewById(R.id.ivProfilResmi)
        tvAdSoyad = findViewById(R.id.tvAdSoyad)
        tvEmail = findViewById(R.id.tvEmail)
        tvTelefon = findViewById(R.id.tvTelefon)
        tvBlok = findViewById(R.id.tvBlok)
        tvDaireNo = findViewById(R.id.tvDaireNo)
        tvKullaniciTipi = findViewById(R.id.tvKullaniciTipi)
        tvSakinTipi = findViewById(R.id.tvSakinTipi)
        tvAidatSorumlu = findViewById(R.id.tvAidatSorumlu)
        tvIkametDurumu = findViewById(R.id.tvIkametDurumu)
        tvKayitTarihi = findViewById(R.id.tvKayitTarihi)
        tvTarihFormatli = findViewById(R.id.tvTarihFormatli)
        tvHatirlatici = findViewById(R.id.tvHatirlatici)

        // Başlangıç değerlerini ayarla
        tvAdSoyad.text = userName
        tvEmail.text = userEmail

        // Varsayılan profil resmini yükle
        loadDefaultProfileImage()
    }

    // ... diğer metodlar (loadUserProfile, parseUserProfile, vb.) aynı kalacak
    private fun loadUserProfile() {
        println("🔍 Profil yükleniyor - Email: $userEmail")

        val apiUrl = "http://baser.org/apartman/api/api_hepsi.php"
        val SQLKEY = "randomkey"

        val sqlQuery = "SELECT * FROM apartman_users WHERE email = '$userEmail'"
        println("🔍 SQL Sorgusu: $sqlQuery")

        val params = HashMap<String, String>()
        params["query"] = sqlQuery
        params["key"] = SQLKEY

        val stringRequest = object : StringRequest(
            Request.Method.POST,
            apiUrl,
            { response ->
                println("🔍 API Yanıtı: $response")
                parseUserProfile(response)
            },
            { error ->
                val errorMsg = error.message ?: "Bilinmeyen hata"
                println("❌ Profil yükleme hatası: $errorMsg")
                Toast.makeText(this, "Profil bilgileri yüklenemedi", Toast.LENGTH_SHORT).show()
                showDefaultProfile()
            }
        ) {
            override fun getParams(): Map<String, String> {
                return params
            }

            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/x-www-form-urlencoded"
                return headers
            }
        }

        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun parseUserProfile(csvResponse: String) {
        try {
            println("🔍 CSV Yanıtı işleniyor...")

            val lines = csvResponse.trim().split("\n")
            println("🔍 Satır sayısı: ${lines.size}")

            if (lines.size > 1) {
                val headers = lines[0].split(",")
                val userData = lines[1].split(",")

                println("🔍 Başlıklar: $headers")
                println("🔍 Veriler: $userData")

                // CSV'den verileri al
                val userMap = mutableMapOf<String, String>()
                for (i in headers.indices) {
                    if (i < userData.size) {
                        userMap[headers[i]] = userData[i]
                    }
                }

                println("🔍 User Map: $userMap")

                // PROFİL RESMİ DEBUG
                val profileImage = userMap["profile_image"]
                println("🔍 Profil Resmi Alanı: $profileImage")

                // UI'ı güncelle
                runOnUiThread {
                    // Temel bilgiler
                    tvAdSoyad.text = userMap["name"] ?: userName
                    tvEmail.text = userMap["email"] ?: userEmail
                    tvTelefon.text = userMap["phone"] ?: "Belirtilmemiş"
                    tvBlok.text = userMap["apartment_block"] ?: "Belirtilmemiş"
                    tvDaireNo.text = userMap["apartment_number"] ?: "Belirtilmemiş"

                    // Kullanıcı tipi
                    val userTypeFromDB = userMap["user_type"] ?: userType
                    tvKullaniciTipi.text = when (userTypeFromDB) {
                        "admin" -> "Yönetici"
                        "manager" -> "Site Sorumlusu"
                        "resident" -> "Sakin"
                        else -> userTypeFromDB
                    }

                    // Sakin tipi
                    val residentType = userMap["resident_type"] ?: "owner"
                    tvSakinTipi.text = when (residentType) {
                        "owner" -> "Ev Sahibi"
                        "tenant" -> "Kiracı"
                        else -> residentType
                    }

                    // Aidat sorumluluğu
                    val aidatSorumlu = userMap["is_responsible_for_dues"] ?: "yes"
                    tvAidatSorumlu.text = when (aidatSorumlu) {
                        "yes" -> "Evet"
                        "no" -> "Hayır"
                        else -> aidatSorumlu
                    }

                    // İkamet durumu
                    val ikametDurumu = userMap["is_residing"] ?: "yes"
                    tvIkametDurumu.text = when (ikametDurumu) {
                        "yes" -> "Evet"
                        "no" -> "Hayır"
                        else -> ikametDurumu
                    }

                    // Hatırlatıcılar
                    val hatirlatici = userMap["receive_reminders"] ?: "yes"
                    tvHatirlatici.text = when (hatirlatici) {
                        "yes" -> "Evet"
                        "no" -> "Hayır"
                        else -> hatirlatici
                    }

                    // Tarih bilgileri
                    val kayitTarihi = userMap["created_at"] ?: ""
                    tvKayitTarihi.text = kayitTarihi.ifEmpty { "Belirtilmemiş" }

                    // Formatlı tarih
                    tvTarihFormatli.text = formatDate(kayitTarihi)

                    // Profil resmini yükle
                    loadProfileImageSimple(profileImage)

                    Toast.makeText(this, "Profil bilgileri güncellendi", Toast.LENGTH_SHORT).show()
                }

                println("✅ Profil bilgileri yüklendi")
            } else {
                println("❌ Kullanıcı bulunamadı veya yetersiz veri")
                showDefaultProfile()
            }
        } catch (e: Exception) {
            println("❌ Profil ayrıştırma hatası: ${e.message}")
            e.printStackTrace()
            showDefaultProfile()
        }
    }

    // BASİT RESİM YÜKLEME FONKSİYONU
    private fun loadProfileImageSimple(imageUrl: String?) {
        try {
            if (!imageUrl.isNullOrEmpty() && imageUrl != "null") {
                println("🔍 Basit resim yükleme: $imageUrl")

                val fullImageUrl = "http://baser.org/apartman/uploads/profile_images/$imageUrl"
                println("🔍 Tam resim URL: $fullImageUrl")

                Thread {
                    try {
                        val url = URL(fullImageUrl)
                        val connection = url.openConnection() as HttpURLConnection
                        connection.doInput = true
                        connection.connectTimeout = 20000 // 20 saniye
                        connection.readTimeout = 20000 // 20 saniye
                        connection.connect()

                        val inputStream = connection.inputStream

                        // Direkt küçük boyutta decode et
                        val options = BitmapFactory.Options().apply {
                            inSampleSize = 4 // 1/4 boyutunda
                            inPreferredConfig = Bitmap.Config.RGB_565
                        }

                        val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                        inputStream.close()
                        connection.disconnect()

                        runOnUiThread {
                            if (bitmap != null) {
                                // Daha da küçült
                                val finalBitmap = if (bitmap.width > 400 || bitmap.height > 400) {
                                    val scaleFactor = minOf(400f / bitmap.width, 400f / bitmap.height)
                                    val newWidth = (bitmap.width * scaleFactor).toInt()
                                    val newHeight = (bitmap.height * scaleFactor).toInt()
                                    Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
                                } else {
                                    bitmap
                                }

                                val roundedBitmap = getRoundedBitmap(finalBitmap)
                                ivProfilResmi.setImageBitmap(roundedBitmap)
                                ivProfilResmi.background = resources.getDrawable(R.drawable.circle_background, null)
                                println("✅ Basit yükleme başarılı: ${finalBitmap.width}x${finalBitmap.height}")
                            } else {
                                loadDefaultProfileImage()
                            }
                        }
                    } catch (e: Exception) {
                        println("❌ Basit resim yükleme hatası: ${e.message}")
                        runOnUiThread {
                            loadDefaultProfileImage()
                        }
                    }
                }.start()
            } else {
                loadDefaultProfileImage()
            }
        } catch (e: Exception) {
            println("❌ Resim yükleme hatası: ${e.message}")
            loadDefaultProfileImage()
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

    private fun loadDefaultProfileImage() {
        runOnUiThread {
            ivProfilResmi.setImageResource(R.drawable.ic_profile_placeholder)
            // Yuvarlak arkaplan ekle
            ivProfilResmi.background = resources.getDrawable(R.drawable.circle_background, null)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            if (dateString.isNotEmpty()) {
                // MySQL timestamp formatını Türkçe tarihe çevir
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR"))
                val date = inputFormat.parse(dateString)
                outputFormat.format(date ?: Date())
            } else {
                "Belirtilmemiş"
            }
        } catch (e: Exception) {
            "Tarih formatlanamadı"
        }
    }

    private fun showDefaultProfile() {
        runOnUiThread {
            tvAdSoyad.text = userName
            tvEmail.text = userEmail
            tvTelefon.text = "Bulunamadı"
            tvBlok.text = "Bulunamadı"
            tvDaireNo.text = "Bulunamadı"

            tvKullaniciTipi.text = when (userType) {
                "admin" -> "Yönetici"
                "manager" -> "Site Sorumlusu"
                "resident" -> "Sakin"
                else -> userType
            }

            tvSakinTipi.text = "Bulunamadı"
            tvAidatSorumlu.text = "Bulunamadı"
            tvIkametDurumu.text = "Bulunamadı"
            tvKayitTarihi.text = "Bulunamadı"
            tvTarihFormatli.text = "Bulunamadı"
            tvHatirlatici.text = "Bulunamadı"

            loadDefaultProfileImage()

            Toast.makeText(this, "Varsayılan profil gösteriliyor", Toast.LENGTH_SHORT).show()
        }
    }

    // Memory optimizasyonu
    override fun onDestroy() {
        super.onDestroy()
        // Bitmap'leri temizle (memory leak önleme)
        ivProfilResmi.setImageBitmap(null)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ivProfilResmi.setImageBitmap(null)
        System.gc()
    }
}