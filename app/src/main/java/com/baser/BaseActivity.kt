package com.baser.apartman

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Değişkenleri nullable yap ve varsayılan değer ata
    protected var userEmail: String = ""
    protected var userType: String = ""
    protected var userName: String = ""


    // Profil resmi için - PROTECTED yap
    protected var ivProfileImage: ImageView? = null
    protected var ivCameraIcon: ImageView? = null
    protected var btnChangeProfileImage: android.widget.Button? = null

    // Kamera/Galeri için request kodları
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_GALLERY = 2
    private val REQUEST_CAMERA_PERMISSION = 100
    private val REQUEST_GALLERY_PERMISSION = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kullanıcı bilgilerini yükle
        loadUserInfo()
        // Widget'dan gelen email bilgisini kontrol et
        val widgetEmail = intent.getStringExtra("user_email")
        if (!widgetEmail.isNullOrEmpty()) {
            userEmail = widgetEmail
        }
    }

    protected open fun setupNavigation() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        if (navigationView == null) {
            println("🔍 NavigationView bulunamadı - bu sayfada gerekli değil")
            return
        }

        navigationView.setNavigationItemSelectedListener(this)

        val headerView = navigationView.getHeaderView(0)
        headerView?.let {
            val tvUserName = it.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = it.findViewById<TextView>(R.id.tvUserEmail)

            ivProfileImage = it.findViewById(R.id.ivProfileImage)
            ivCameraIcon = it.findViewById(R.id.ivCameraIcon)
            btnChangeProfileImage = it.findViewById(R.id.btnChangeProfileImage)

            tvUserName.text = userName
            tvUserEmail.text = userEmail

            ProfileImageManager.loadProfileImage(this, userEmail, ivProfileImage)
            setupProfileImageListeners()
        }

        // Toolbar'ı ayarla
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)

        if (toolbar != null && drawerLayout != null) {
            setSupportActionBar(toolbar)

            // Geri butonunu göster
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)

            // TOGGLE oluştur
            val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
            )

            // Drawer'a listener ekle
            drawerLayout.addDrawerListener(toggle)

            // SyncState çağır (ikonu gösterir)
            toggle.syncState()

            // İkon rengini beyaz yap
            toggle.drawerArrowDrawable.color = resources.getColor(android.R.color.white, null)

            // ===== EKSTRA GÜVENCE: Manuel tıklama işlevi =====
            toolbar.setNavigationOnClickListener {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
            }

            println("✅ Menü başarıyla kuruldu")
        } else {
            println("❌ Toolbar veya DrawerLayout bulunamadı")
        }
    }

    protected fun setupProfileImageListeners() {
        // Profil resmine tıklama
        ivProfileImage?.setOnClickListener {
            showImageSelectionDialog()
        }

        // Kamera ikonuna tıklama
        ivCameraIcon?.setOnClickListener {
            showImageSelectionDialog()
        }

        // Resim değiştir butonuna tıklama
        btnChangeProfileImage?.setOnClickListener {
            showImageSelectionDialog()
        }
    }
    private fun showImageSelectionDialog() {
        val options = arrayOf("Kameradan Çek", "Galeriden Seç", "İptal")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Profil Resmi Seç")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> takePhotoFromCamera()
                    1 -> choosePhotoFromGallery()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun takePhotoFromCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION)
        } else {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent.resolveActivity(packageManager) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            } else {
                Toast.makeText(this, "Kamera bulunamadı", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun choosePhotoFromGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_GALLERY_PERMISSION)
        } else {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(Intent.createChooser(intent, "Resim Seç"), REQUEST_IMAGE_GALLERY)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhotoFromCamera()
                } else {
                    Toast.makeText(this, "Kamera izni gerekiyor", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_GALLERY_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    choosePhotoFromGallery()
                } else {
                    Toast.makeText(this, "Galeri izni gerekiyor", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let { bitmap ->
                        handleSelectedImage(bitmap)
                    }
                }
                REQUEST_IMAGE_GALLERY -> {
                    val selectedImageUri = data?.data
                    selectedImageUri?.let { uri ->
                        try {
                            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                            handleSelectedImage(bitmap)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Resim yüklenemedi", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun handleSelectedImage(bitmap: Bitmap) {
        // 1. Önce ImageView'da göster (anında feedback)
        ivProfileImage?.setImageBitmap(bitmap)

        // 2. ProfileImageManager ile işle
        ProfileImageManager.processSelectedImage(
            context = this,
            bitmap = bitmap,
            userEmail = userEmail,
            onSuccess = {
                runOnUiThread {
                    Toast.makeText(this, "Profil resmi güncellendi", Toast.LENGTH_SHORT).show()

                    // Sol menüyü yenile
                    ProfileImageManager.loadProfileImage(this, userEmail, ivProfileImage, true)
                }
            },
            onError = { error ->
                runOnUiThread {
                    Toast.makeText(this, "Hata: $error", Toast.LENGTH_LONG).show()

                    // Hata durumunda eski resme geri dön
                    ProfileImageManager.loadProfileImage(this, userEmail, ivProfileImage)
                }
            }
        )
    }
    // Bu metodu SİLİYORUZ - ProfileImageManager bunu hallediyor
    protected open fun saveProfileImage(bitmap: Bitmap) {
        // ProfileImageManager.saveBitmapToCache() kullanılıyor
    }

    // Bu metodu SİLİYORUZ
    private fun loadProfileImage() {
        // ProfileImageManager.loadProfileImage() kullanılıyor
    }

    // Bu metodu SİLİYORUZ - ProfileImageManager bunu hallediyor
    protected fun loadProfileImageFromServer() {
        // Artık kullanılmıyor
    }

    // Bu metodu SİLİYORUZ
    private fun parseProfileImageFromResponse(csvResponse: String) {
        // Artık kullanılmıyor
    }

    // Bu metodu SİLİYORUZ
    private fun loadRemoteProfileImage(imageUrl: String) {
        // Artık kullanılmıyor
    }

    // Bu metodu SİLİYORUZ
    private fun tryAlternativeImageUrl(imageUrl: String) {
        // Artık kullanılmıyor
    }

    protected open fun setupBottomNavigation() {
        val bottomNav = findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottom_navigation)
        bottomNav?.setOnNavigationItemSelectedListener { item ->
            handleNavigation(item.itemId)
            true
        }

        // Mevcut sayfayı aktif göster
        bottomNav?.selectedItemId = getCurrentNavigationId()
    }

    private fun getCurrentNavigationId(): Int {
        return when (this::class.java.simpleName) {
            "DashboardActivity" -> R.id.nav_dashboard
            "DuyurularActivity" -> R.id.nav_duyurular
            "AidatActivity" -> R.id.nav_aidat
            "SikayetlerActivity" -> R.id.nav_sikayetler
            else -> R.id.nav_dashboard
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        drawerLayout?.closeDrawer(GravityCompat.START)

        return handleNavigation(item.itemId)
    }

    protected open fun handleNavigation(itemId: Int): Boolean {
        println("🔍 NAVIGATION: $itemId, Mevcut Activity: ${this::class.java.simpleName}")

        when (itemId) {
            R.id.nav_dashboard -> {
                if (this::class.java.simpleName != "DashboardActivity") {
                    startActivity(Intent(this, DashboardActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_profil -> {
                startActivity(Intent(this, ProfilActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_weather -> {  // Hava Durumu
                startActivity(Intent(this, WeatherActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_user_management -> {  // Kullanıcı Yönetimi
                startActivity(Intent(this, UserManagementActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_duyurular -> {
                if (this::class.java.simpleName != "DuyurularActivity") {
                    startActivity(Intent(this, DuyurularActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_uyku_modu -> {
                if (this::class.java.simpleName != "UykuModuActivity") {
                    startActivity(Intent(this, UykuModuActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_aidat -> {
                if (this::class.java.simpleName != "AidatActivity") {
                    startActivity(Intent(this, AidatActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_shared_expenses -> {  // Toplu Harcamalar
                if (this::class.java.simpleName != "SharedExpensesActivity") {
                    startActivity(Intent(this, SharedExpensesActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_gruplanmis_borclar -> {  // Gruplanmış Borçlar
                if (this::class.java.simpleName != "GroupedDuesActivity") {
                    startActivity(Intent(this, GroupedDuesActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_sikayetler -> {
                if (this::class.java.simpleName != "SikayetlerActivity") {
                    startActivity(Intent(this, SikayetlerActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_yeni_sikayet -> {  // Yeni Şikayet
                startActivity(Intent(this, YeniSikayetActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_messages -> {
                if (this::class.java.simpleName != "MessagesActivity") {
                    startActivity(Intent(this, MessagesActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_api_settings -> {  // API Ayarları
                startActivity(Intent(this, ApiSettingsActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_buton_durum -> {  // Buton Durum
                startActivity(Intent(this, ButonDurumActivity::class.java))
                return true
            }
            R.id.nav_kombi -> {  // Kombi Kontrol
                if (this::class.java.simpleName != "KombiActivity") {
                    startActivity(Intent(this, KombiActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_esp_settings -> {  // ESP Ayarlar
                startActivity(Intent(this, EspSettingsActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_rahatsiz_etme -> {
                if (this::class.java.simpleName != "RahatsizEtmeModuActivity") {
                    startActivity(Intent(this, RahatsizEtmeModuActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_namaz_vakitleri -> {
                if (this::class.java.simpleName != "NamazVakitleriActivity") {
                    startActivity(Intent(this, NamazVakitleriActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                }
                return true
            }
            R.id.nav_settings -> {  // Garaj Ayarlar
                startActivity(Intent(this, SettingsActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_logout -> {
                logoutUser()
                return true
            }
        }
        return false
    }
    // BaseActivity.kt'ye bu metodu ekleyin (handleNavigation'dan sonra):

    protected fun refreshProfileImage() {
        runOnUiThread {
            ivProfileImage?.let {
                ProfileImageManager.forceRefresh(this, userEmail, ivProfileImage)
            }
        }
    }
    protected open fun loadUserInfo() {
        userEmail = intent.getStringExtra("user_email") ?: getSavedUserEmail()
        userType = intent.getStringExtra("user_type") ?: getSavedUserType()
        userName = intent.getStringExtra("user_name") ?: getSavedUserName()

        println("🔍 KULLANICI BİLGİLERİ YÜKLENDİ: $userEmail, $userType, $userName")
    }

    protected fun getSavedUserEmail(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_email", "") ?: ""
    }

    protected fun getSavedUserType(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_type", "resident") ?: "resident"
    }

    protected fun getSavedUserName(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_name", "Kullanıcı") ?: "Kullanıcı"
    }

    protected fun getCurrentUserId(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", "") ?: ""
    }

    // Bu metodu SİLİYORUZ - ProfileImageManager bunu hallediyor
    protected fun getProfileImage(): Bitmap? {
        // ProfileImageManager.getCachedBitmap() kullanılacak
        return null
    }

    protected open fun logoutUser() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("user_email")
            remove("user_type")
            remove("user_name")
            remove("user_id")
            apply()
        }

        // ProfileImageManager cache'ini temizle
        ProfileImageManager.clearCache()

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
    protected open fun getActivityTitle(): String {
        return when (this::class.java.simpleName) {
            "DashboardActivity" -> "🏠 Ana Sayfa"
            "ProfilActivity" -> "👤 Profilim"
            "DuyurularActivity" -> "📢 Duyurular"
            "AidatActivity" -> "💰 Aidat Takibi"
            "SikayetlerActivity" -> "⚠️ Şikayetler"
            "NamazVakitleriActivity" -> "🕌 Namaz Vakitleri"
            "UykuModuActivity" -> "😴 Uyku Modu"
            "RahatsizEtmeModuActivity" -> "🔕 Rahatsız Etme"
            "MessagesActivity" -> "💬 Mesajlar"
            "ApiSettingsActivity" -> "⚙️ API Ayarları"
            "WeatherActivity" -> "☀️ HAVA DURUMU"  // Burayı güncelledik
            "UserManagementActivity" -> "👥 Kullanıcı Yönetimi"
            "SharedExpensesActivity" -> "📊 Toplu Harcamalar"
            "GroupedDuesActivity" -> "📋 Gruplanmış Borçlar"
            "YeniSikayetActivity" -> "📝 Yeni Şikayet"
            "ButonDurumActivity" -> "🔘 Buton Durum"
            "KombiActivity" -> "🔥 Kombi Kontrol"
            "EspSettingsActivity" -> "🔧 ESP Ayarlar"
            "SettingsActivity" -> "⚙️ Garaj Ayarlar"
            else -> "🏢 Apartman Yönetim"
        }
    }

}