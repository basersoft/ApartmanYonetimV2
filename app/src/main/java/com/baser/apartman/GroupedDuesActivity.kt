package com.baser.apartman

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.baser.apartman.models.DuesGroup
import com.google.android.material.navigation.NavigationView
import org.json.JSONArray
import org.json.JSONObject

class GroupedDuesActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var filterSpinner: Spinner
    private lateinit var groupBySpinner: Spinner
    private lateinit var tvTotalDebt: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalDues: TextView

    // Navigation için
    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var navigationView: NavigationView

    // Paging için
    private var currentPage = 1
    private val pageSize = 10
    private var isLoading = false
    private var hasMoreData = true

    private var groupedDuesList = mutableListOf<DuesGroup>()
    private var currentGroupBy = "block"
    private var currentFilterBlock = "all"
    private var availableBlocks = mutableListOf<String>()

    // Spinner kontrolü için
    private var isSpinnerInitialized = false

    // View type constants
    private companion object ViewTypes {
        const val TYPE_ITEM = 0
        const val TYPE_LOADING = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grouped_dues)

        initViews()

        // BU SATIRI EKLE: BaseActivity'den gelen navigation'ı kullan
        super.setupNavigation()

        setupToolbarAndNavigation()
        setupSpinners()
        setupRecyclerView()
        loadGroupedDues(true)
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        emptyState = findViewById(R.id.emptyState)
        filterSpinner = findViewById(R.id.filterSpinner)
        groupBySpinner = findViewById(R.id.groupBySpinner)
        tvTotalDebt = findViewById(R.id.tvTotalDebt)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvTotalDues = findViewById(R.id.tvTotalDues)

        // Navigation views
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        println("🔍 Navigation View bulundu: ${navigationView != null}")
        println("🔍 Drawer Layout bulundu: ${drawerLayout != null}")
    }

    private fun setupToolbarAndNavigation() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        // Toolbar'ı ayarla
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Gruplanmış Borçlar"

        // HAMBURGER İKONU İÇİN - Bu kısmı BaseActivity zaten hallediyor
        // ActionBarDrawerToggle
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

        // Navigation listener - BaseActivity'den geliyor zaten
        // navigationView.setNavigationItemSelectedListener(this) // BU SATIR GEREKSİZ

        // Header bilgilerini güncelle - BU KISIM ARTIK GEREKSİZ, BaseActivity hallediyor
        // updateNavigationHeader()

        println("✅ GroupedDuesActivity: Toolbar ve Navigation ayarlandı")
    }

    // BU METODU SİLİYORUZ - BaseActivity zaten yapıyor
    /*
    private fun updateNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        headerView?.let {
            val tvUserName = it.findViewById<TextView>(R.id.tvUserName)
            val tvUserEmail = it.findViewById<TextView>(R.id.tvUserEmail)

            tvUserName.text = userName
            tvUserEmail.text = userEmail
            println("✅ Navigation header güncellendi: $userName, $userEmail")
        } ?: run {
            println("❌ Navigation header bulunamadı!")
        }
    }
    */

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    drawerLayout.openDrawer(GravityCompat.START)
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                startActivity(Intent(this, DashboardActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }
            R.id.nav_duyurular -> {
                startActivity(Intent(this, DuyurularActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }
            R.id.nav_aidat -> {
                startActivity(Intent(this, AidatActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }
            R.id.nav_sikayetler -> {
                startActivity(Intent(this, SikayetlerActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }
            R.id.nav_gruplanmis_borclar -> {
                // Zaten bu sayfadayız
                Toast.makeText(this, "Zaten Gruplanmış Borçlar sayfasındasınız", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_profil -> {
                startActivity(Intent(this, ProfilActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java).apply {
                    putExtra("user_email", userEmail)
                    putExtra("user_type", userType)
                    putExtra("user_name", userName)
                })
            }
            R.id.nav_logout -> {
                super.logoutUser()
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }


    private fun setupSpinners() {
        // Gruplama türü spinner'ı
        val groupByOptions = arrayOf("Blok Bazında", "Kullanıcı Bazında", "Durum Bazında")
        val groupByAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, groupByOptions)
        groupByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groupBySpinner.adapter = groupByAdapter

        groupBySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newGroupBy = when (position) {
                    0 -> "block"
                    1 -> "user"
                    2 -> "status"
                    else -> "block"
                }

                if (newGroupBy != currentGroupBy) {
                    currentGroupBy = newGroupBy
                    resetPaging()
                    loadGroupedDues(true)
                    println("🔄 Gruplama değişti: $currentGroupBy")
                }

                if (!isSpinnerInitialized) {
                    isSpinnerInitialized = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Blok filtresi spinner'ı - başlangıçta sadece "Tüm Bloklar"
        val filterOptions = mutableListOf("Tüm Bloklar")
        val filterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = filterAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newFilterBlock = if (position == 0) "all" else {
                    if (availableBlocks.size > position - 1) availableBlocks[position - 1] else "all"
                }

                if (newFilterBlock != currentFilterBlock) {
                    currentFilterBlock = newFilterBlock
                    resetPaging()
                    loadGroupedDues(true)
                    println("🔄 Filtre değişti: $currentFilterBlock")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = GroupedDuesAdapter()

        // Scroll listener for paging
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy <= 0 || isLoading || !hasMoreData) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                // Son 3 öğe görünürse yeni veri yükle
                if (lastVisibleItemPosition + 3 >= totalItemCount) {
                    loadGroupedDues(false)
                }
            }
        })
    }

    private fun resetPaging() {
        currentPage = 1
        hasMoreData = true
        groupedDuesList.clear()
        (recyclerView.adapter as? GroupedDuesAdapter)?.updateData(groupedDuesList)
        println("🔄 Paging sıfırlandı - Sayfa: $currentPage")
    }

    private fun loadGroupedDues(isFirstLoad: Boolean) {
        if (isLoading) return

        isLoading = true

        if (isFirstLoad) {
            loadingIndicator.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
            println("🔄 İlk yükleme başlatılıyor - Sayfa: $currentPage")
        } else {
            println("🔄 Sayfa yükleme başlatılıyor - Sayfa: $currentPage")
            (recyclerView.adapter as? GroupedDuesAdapter)?.showLoading(true)
        }

        val apiUrl = "http://baser.org/apartman/api/api_grouped_dues_v2.php"
        val apiKey = "apartman_secret_key_2024"
        val url = "$apiUrl?api_key=$apiKey&group_by=$currentGroupBy&filter_block=$currentFilterBlock&page=$currentPage&limit=$pageSize"

        println("🔍 API URL: $url")

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            { response ->
                isLoading = false
                loadingIndicator.visibility = View.GONE
                (recyclerView.adapter as? GroupedDuesAdapter)?.showLoading(false)

                try {
                    if (response.getBoolean("success")) {
                        val data = response.getJSONObject("data")
                        val groupsArray = data.getJSONArray("groups")
                        val stats = data.getJSONObject("statistics")
                        val pagination = data.getJSONObject("pagination")

                        parseJsonResponse(groupsArray, stats, pagination)

                        if (isFirstLoad && currentPage == 1) {
                            updateBlockFilter(groupsArray)
                        }

                        val loadedCount = groupsArray.length()
                        println("✅ Veri yüklendi: $loadedCount kayıt - Sayfa: $currentPage")

                        if (isFirstLoad && currentPage == 1) {
                            Toast.makeText(this, "$loadedCount grup yüklendi", Toast.LENGTH_SHORT).show()
                        }

                    } else {
                        val error = response.optString("error", "Bilinmeyen hata")
                        showErrorState("API Hatası: $error")
                        println("❌ API Hatası: $error")
                    }
                } catch (e: Exception) {
                    isLoading = false
                    println("❌ JSON parsing hatası: ${e.message}")
                    showErrorState("Veri işleme hatası")
                }
            },
            { error ->
                isLoading = false
                loadingIndicator.visibility = View.GONE
                (recyclerView.adapter as? GroupedDuesAdapter)?.showLoading(false)

                val errorMsg = error.message ?: "Bilinmeyen hata"
                println("❌ API bağlantı hatası: $errorMsg")
                showErrorState("Bağlantı hatası: $errorMsg")
            }
        )

        Volley.newRequestQueue(this).add(jsonObjectRequest)
    }

    private fun parseJsonResponse(groupsArray: JSONArray, stats: JSONObject, pagination: JSONObject) {
        try {
            val newGroups = mutableListOf<DuesGroup>()

            for (i in 0 until groupsArray.length()) {
                val groupJson = groupsArray.getJSONObject(i)
                val duesGroup = DuesGroup.fromJson(groupJson)

                // Gruplama türüne göre özel alanları ayarla
                when (currentGroupBy) {
                    "user" -> {
                        duesGroup.userName = groupJson.optString("user_name", "")
                        duesGroup.apartmentBlock = groupJson.optString("apartment_block", "")
                        duesGroup.apartmentNumber = groupJson.optString("apartment_number", "")
                    }
                    "status" -> {
                        duesGroup.groupName = groupJson.optString("group_name", duesGroup.groupKey)
                    }
                }

                newGroups.add(duesGroup)
                println("✅ Grup eklendi: ${duesGroup.groupName} - ${duesGroup.userCount} kullanıcı")
            }

            // Paging kontrolü
            hasMoreData = pagination.optBoolean("has_more", false)
            val loadedPage = pagination.optInt("current_page", currentPage)

            if (hasMoreData) {
                currentPage = loadedPage + 1
            }

            println("📊 Paging bilgisi: HasMore: $hasMoreData, CurrentPage: $currentPage")

            if (loadedPage == 1) {
                groupedDuesList.clear()
            }

            groupedDuesList.addAll(newGroups)
            updateStatisticsWithJson(stats)
            updateRecyclerView()

            if (groupedDuesList.isEmpty() && loadedPage == 1) {
                showEmptyState()
            }

        } catch (e: Exception) {
            println("❌ JSON ayrıştırma hatası: ${e.message}")
            showErrorState("Veri işleme hatası")
        }
    }

    private fun updateBlockFilter(groupsArray: JSONArray) {
        try {
            availableBlocks.clear()

            for (i in 0 until groupsArray.length()) {
                val groupJson = groupsArray.getJSONObject(i)
                val block = groupJson.optString("apartment_block", "")
                if (block.isNotEmpty() && !availableBlocks.contains(block)) {
                    availableBlocks.add(block)
                }
            }

            // Spinner'ı güncelle
            val filterOptions = mutableListOf("Tüm Bloklar")
            filterOptions.addAll(availableBlocks.sorted())

            val filterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
            filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            // Spinner'ı güncellerken listener'ı geçici olarak devre dışı bırak
            val currentSelection = filterSpinner.selectedItemPosition
            filterSpinner.onItemSelectedListener = null
            filterSpinner.adapter = filterAdapter
            if (currentSelection >= 0 && currentSelection < filterOptions.size) {
                filterSpinner.setSelection(currentSelection)
            }
            // Listener'ı tekrar ekle
            setupFilterSpinnerListener()

            println("✅ Blok filtresi güncellendi: ${availableBlocks.size} blok")

        } catch (e: Exception) {
            println("❌ Blok filtresi güncelleme hatası: ${e.message}")
        }
    }

    private fun setupFilterSpinnerListener() {
        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val newFilterBlock = if (position == 0) "all" else {
                    if (availableBlocks.size > position - 1) availableBlocks[position - 1] else "all"
                }

                if (newFilterBlock != currentFilterBlock) {
                    currentFilterBlock = newFilterBlock
                    resetPaging()
                    loadGroupedDues(true)
                    println("🔄 Filtre değişti: $currentFilterBlock")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateStatisticsWithJson(stats: JSONObject) {
        try {
            val totalDebt = stats.optDouble("total_pending", 0.0) + stats.optDouble("total_overdue", 0.0)
            val totalUsers = stats.optInt("total_users", groupedDuesList.sumBy { it.userCount })
            val totalDues = stats.optInt("total_dues", groupedDuesList.sumBy { it.totalDues })

            tvTotalDebt.text = "₺${String.format("%.2f", totalDebt)}"
            tvTotalUsers.text = totalUsers.toString()
            tvTotalDues.text = totalDues.toString()

        } catch (e: Exception) {
            println("❌ İstatistik güncelleme hatası: ${e.message}")
        }
    }

    private fun updateRecyclerView() {
        (recyclerView.adapter as? GroupedDuesAdapter)?.updateData(groupedDuesList)
        println("✅ RecyclerView güncellendi: ${groupedDuesList.size} öğe")
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        println("📭 Boş state gösteriliyor")
    }

    private fun showErrorState(message: String) {
        emptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE

        val tvErrorMessage = findViewById<TextView>(R.id.tvErrorMessage)
        tvErrorMessage?.text = message
        println("❌ Hata state: $message")
    }

    // Refresh butonu için
    fun onRefreshClicked(view: View) {
        resetPaging()
        loadGroupedDues(true)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // RecyclerView Adapter
    inner class GroupedDuesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private var items = mutableListOf<Any>()
        private var isLoading = false

        fun updateData(newItems: List<DuesGroup>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        fun showLoading(show: Boolean) {
            if (isLoading != show) {
                isLoading = show
                if (show) {
                    items.add("loading")
                    notifyItemInserted(items.size - 1)
                } else {
                    if (items.isNotEmpty() && items.last() == "loading") {
                        items.removeAt(items.size - 1)
                        notifyItemRemoved(items.size)
                    }
                }
            }
        }

        override fun getItemViewType(position: Int): Int {
            return when (items[position]) {
                is DuesGroup -> ViewTypes.TYPE_ITEM
                else -> ViewTypes.TYPE_LOADING
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return when (viewType) {
                ViewTypes.TYPE_ITEM -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_grouped_dues, parent, false)
                    ItemViewHolder(view)
                }
                ViewTypes.TYPE_LOADING -> {
                    val view = LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_loading, parent, false)
                    LoadingViewHolder(view)
                }
                else -> throw IllegalArgumentException("Unknown view type")
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (holder) {
                is ItemViewHolder -> {
                    val item = items[position] as DuesGroup
                    holder.bind(item)
                }
                is LoadingViewHolder -> holder.bind()
            }
        }

        override fun getItemCount(): Int = items.size

        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardView)
            private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
            private val tvUserCount: TextView = itemView.findViewById(R.id.tvUserCount)
            private val tvTotalDebt: TextView = itemView.findViewById(R.id.tvTotalDebt)
            private val tvDebtStatus: TextView = itemView.findViewById(R.id.tvDebtStatus)
            private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
            private val tvProgressText: TextView = itemView.findViewById(R.id.tvProgressText)
            private val layoutStats: LinearLayout = itemView.findViewById(R.id.layoutStats)
            private val tvPaidCount: TextView = itemView.findViewById(R.id.tvPaidCount)
            private val tvPendingCount: TextView = itemView.findViewById(R.id.tvPendingCount)
            private val tvOverdueCount: TextView = itemView.findViewById(R.id.tvOverdueCount)
            private val tvPaidAmount: TextView = itemView.findViewById(R.id.tvPaidAmount)
            private val tvPendingAmount: TextView = itemView.findViewById(R.id.tvPendingAmount)
            private val tvOverdueAmount: TextView = itemView.findViewById(R.id.tvOverdueAmount)

            fun bind(duesGroup: DuesGroup) {
                // Grup bilgileri - gruplama türüne göre formatla
                when (currentGroupBy) {
                    "block" -> {
                        tvGroupName.text = "${duesGroup.groupName} Blok"
                        tvUserCount.text = "${duesGroup.userCount} kullanıcı"
                    }
                    "user" -> {
                        tvGroupName.text = "${duesGroup.userName}\n${duesGroup.apartmentBlock}-${duesGroup.apartmentNumber}"
                        tvUserCount.text = "${duesGroup.totalDues} aidat"
                    }
                    "status" -> {
                        tvGroupName.text = duesGroup.groupName
                        tvUserCount.text = "${duesGroup.userCount} kullanıcı"
                    }
                }

                tvTotalDebt.text = duesGroup.formatCurrency(duesGroup.totalPending + duesGroup.totalOverdue)
                tvDebtStatus.text = duesGroup.debtStatusText
                tvDebtStatus.setTextColor(android.graphics.Color.parseColor(duesGroup.debtStatusColor))

                progressBar.progress = duesGroup.paidPercentage.toInt()
                tvProgressText.text = "${String.format("%.1f", duesGroup.paidPercentage)}% ödendi"

                tvPaidCount.text = duesGroup.paidCount.toString()
                tvPendingCount.text = duesGroup.pendingCount.toString()
                tvOverdueCount.text = duesGroup.overdueCount.toString()
                tvPaidAmount.text = duesGroup.formatCurrency(duesGroup.totalPaid)
                tvPendingAmount.text = duesGroup.formatCurrency(duesGroup.totalPending)
                tvOverdueAmount.text = duesGroup.formatCurrency(duesGroup.totalOverdue)

                cardView.setOnClickListener {
                    toggleExpandedState()
                }
            }

            private fun toggleExpandedState() {
                val isExpanded = layoutStats.visibility == View.VISIBLE
                layoutStats.visibility = if (isExpanded) View.GONE else View.VISIBLE

                if (!isExpanded) {
                    layoutStats.alpha = 0f
                    layoutStats.animate().alpha(1f).setDuration(300).start()
                }
            }
        }

        inner class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)

            fun bind() {
                progressBar.visibility = View.VISIBLE
            }
        }
    }
}