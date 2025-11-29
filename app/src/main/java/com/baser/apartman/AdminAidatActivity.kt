package com.baser.apartman

import android.os.Bundle
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AdminAidatActivity : BaseActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    // Public yapalım ki fragment'lar erişebilsin
    val fragmentUserEmail: String
        get() = userEmail

    val fragmentUserType: String
        get() = userType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_aidat)

        setupNavigation()
        setupTabs()
    }

    private fun setupTabs() {
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        val adapter = ViewPagerAdapter(this)
        adapter.addFragment(AidatListesiFragment(), "Aidat Listesi")
        adapter.addFragment(AidatEklemeFragment(), "Aidat Ekle")

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = adapter.getPageTitle(position)
        }.attach()
    }
}