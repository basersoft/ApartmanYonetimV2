package com.baser.apartman

import android.os.Bundle
import android.util.Log
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AdminAidatActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    // Fragment'lar için kullanıcı bilgileri
    val fragmentUserEmail: String
        get() {
            Log.d("AdminAidatActivity", "fragmentUserEmail getter called")
            val email = userEmail
            Log.d("AdminAidatActivity", "Returning email: $email")
            return email
        }

    val fragmentUserType: String
        get() {
            Log.d("AdminAidatActivity", "fragmentUserType getter called")
            val type = userType
            Log.d("AdminAidatActivity", "Returning type: $type")
            return type
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_aidat)

        Log.d("AdminAidatActivity", "========== onCreate START ==========")
        Log.d("AdminAidatActivity", "Base userEmail: $userEmail")
        Log.d("AdminAidatActivity", "Base userType: $userType")
        Log.d("AdminAidatActivity", "Base userName: $userName")

        // Intent'ten gelen değerleri kontrol et
        val intentEmail = intent.getStringExtra("user_email")
        val intentType = intent.getStringExtra("user_type")
        val intentName = intent.getStringExtra("user_name")

        Log.d("AdminAidatActivity", "Intent Email: $intentEmail")
        Log.d("AdminAidatActivity", "Intent Type: $intentType")
        Log.d("AdminAidatActivity", "Intent Name: $intentName")

        // Eğer intent'ten değer geldiyse, base değerleri güncelle
        if (!intentEmail.isNullOrEmpty()) {
            userEmail = intentEmail
            Log.d("AdminAidatActivity", "Updated userEmail from intent: $userEmail")
        }

        if (!intentType.isNullOrEmpty()) {
            userType = intentType
            Log.d("AdminAidatActivity", "Updated userType from intent: $userType")
        }

        if (!intentName.isNullOrEmpty()) {
            userName = intentName
            Log.d("AdminAidatActivity", "Updated userName from intent: $userName")
        }

        Log.d("AdminAidatActivity", "Final userEmail: $userEmail")
        Log.d("AdminAidatActivity", "Final userType: $userType")
        Log.d("AdminAidatActivity", "Final userName: $userName")
        Log.d("AdminAidatActivity", "========== onCreate END ==========")

        setupNavigation()
        setupTabs()
    }

    private fun setupTabs() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        adapter.addFragment(AidatListesiFragment(), "Aidat Listesi")
        // AidatEklemeFragment kaldırıldı - Web'den yönetilecek

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }
}