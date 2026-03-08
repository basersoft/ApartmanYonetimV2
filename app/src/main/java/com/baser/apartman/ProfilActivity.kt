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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.*

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

        // ApiManager'ı başlat
        ApiManager.initialize(this)

        // Önce BaseActivity'deki setupNavigation'ı çağır
        super.setupNavigation()

        // Sonra kendi toolbar ayarlarımızı yap
        setupToolbar()

        // View'ları bağla
        setupViews()

        // Kullanıcı profilini yükle
        loadUserProfile()
    }

    private fun setupToolbar() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        if (toolbar == null || drawerLayout == null) {
            println("❌ Toolbar veya DrawerLayout bulunamadı")
            return
        }

        // Toolbar'ı ayarla
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Profilim"

        // Hamburger ikonu için ActionBarDrawerToggle oluştur
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Hamburger ikonunu beyaz yap
        toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.white, null)

        println("✅ ProfilActivity: Toolbar ayarlandı")
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

        // SADECE ProfileImageManager kullan
        ProfileImageManager.loadProfileImage(this, userEmail, ivProfilResmi)
    }

    private fun loadUserProfile() {
        val sqlQuery = "SELECT * FROM apartman_users WHERE email = '$userEmail'"

        ApiManager.executeSQLQuery(
            context = this,
            query = sqlQuery,
            onSuccess = { response ->
                parseUserProfile(response)
            },
            onError = { error ->
                Toast.makeText(this, "Profil yüklenemedi: $error", Toast.LENGTH_SHORT).show()
                showDefaultProfile()
            }
        )
    }

    private fun parseUserProfile(csvResponse: String) {
        try {
            println("🔍 CSV Yanıtı işleniyor...")

            val lines = csvResponse.trim().split("\n")
            println("🔍 Satır sayısı: ${lines.size}")

            if (lines.size > 1) {
                val headers = lines[0].split(",")
                val userData = lines[1].split(",")

                // CSV'den verileri al
                val userMap = mutableMapOf<String, String>()
                for (i in headers.indices) {
                    if (i < userData.size) {
                        userMap[headers[i]] = userData[i]
                    }
                }

                println("🔍 User Map: $userMap")

                // Profil resmi URL'sini al
                val profileImage = userMap["profile_image"]
                println("🔍 Profil Resmi URL: $profileImage")

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

                    // ProfileImageManager ile resmi yükle (yeniden yükleme yapmaz, sadece cache'te yoksa yükler)
                    ProfileImageManager.loadProfileImage(this@ProfilActivity, userEmail, ivProfilResmi)

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
            ivProfilResmi.background = resources.getDrawable(R.drawable.circle_background, null)
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            if (dateString.isNotEmpty()) {
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
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ivProfilResmi.setImageBitmap(null)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        ivProfilResmi.setImageBitmap(null)
        System.gc()
    }
}