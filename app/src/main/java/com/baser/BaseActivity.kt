package com.baser.apartman

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView

open class BaseActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    protected lateinit var userEmail: String
    protected lateinit var userType: String
    protected lateinit var userName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kullanıcı bilgilerini yükle
        loadUserInfo()
    }

    protected open fun setupNavigation() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)

        // Null kontrolü yap - eğer navigation view yoksa işlem yapma
        if (navigationView == null) {
            println("🔍 NavigationView bulunamadı - bu sayfada gerekli değil")
            return
        }

        navigationView.setNavigationItemSelectedListener(this)

        // Header'daki kullanıcı bilgilerini güncelle
        val headerView = navigationView.getHeaderView(0)
        headerView?.let {
            val tvUserName = it.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = it.findViewById<TextView>(R.id.tvUserEmail)

            tvUserName.text = userName
            tvUserEmail.text = userEmail
        }

        // Toolbar'ı ayarla
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        if (toolbar != null) {
            setSupportActionBar(toolbar)

            // Navigation toggle
            val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
            drawerLayout?.let {
                val toggle = androidx.appcompat.app.ActionBarDrawerToggle(
                    this, it, toolbar,
                    R.string.navigation_drawer_open,
                    R.string.navigation_drawer_close
                )
                it.addDrawerListener(toggle)
                toggle.syncState()
            }
        }
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
            // YENİ: Kullanıcı Yönetimi navigasyonu
            R.id.nav_user_management -> {
                if (this::class.java.simpleName != "UserManagementActivity") {
                    startActivity(Intent(this, UserManagementActivity::class.java).apply {
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
            R.id.nav_shared_expenses -> {
                if (this::class.java.simpleName != "SharedExpensesActivity") {
                    startActivity(Intent(this, SharedExpensesActivity::class.java).apply {
                        putExtra("user_email", userEmail)
                        putExtra("user_type", userType)
                        putExtra("user_name", userName)
                    })
                } else {
                    // Zaten SharedExpensesActivity'deyiz, sadece yenile
                    Toast.makeText(this, "Toplu Harcamalar yenileniyor...", Toast.LENGTH_SHORT).show()
                    // Eğer SharedExpensesActivity'de loadExpenses() metodu varsa çağır
                    if (this is SharedExpensesActivity) {
                        this.loadExpenses()
                    }
                }
                return true
            }
            R.id.nav_gruplanmis_borclar -> {
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
            R.id.nav_yeni_sikayet -> {
                startActivity(Intent(this, YeniSikayetActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
                return true
            }
            R.id.nav_bakim -> {
                Toast.makeText(this, "Bakım talepleri yakında eklenecek", Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.nav_settings -> {
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

    private fun loadUserInfo() {
        userEmail = intent.getStringExtra("user_email") ?: getSavedUserEmail()
        userType = intent.getStringExtra("user_type") ?: getSavedUserType()
        userName = intent.getStringExtra("user_name") ?: getSavedUserName()

        println("🔍 KULLANICI BİLGİLERİ YÜKLENDİ: $userEmail, $userType, $userName")
    }

    // BU METODLARI PROTECTED YAPIN
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

    // YENİ METOD: Kullanıcı ID'sini al - property yerine method kullanıyoruz
    protected fun getCurrentUserId(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", "") ?: ""
    }

    private fun logoutUser() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("user_email")
            remove("user_type")
            remove("user_name")
            remove("user_id")
            apply()
        }

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        val drawerLayout = findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
        if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}