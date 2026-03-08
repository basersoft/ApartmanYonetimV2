package com.baser.apartman

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import com.google.android.material.button.MaterialButton
class SikayetlerActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddComplaint: FloatingActionButton
    private lateinit var tvEmptyState: LinearLayout
    private lateinit var tvEmptyStateText: TextView
    private lateinit var tvTotalComplaints: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvInProgressCount: TextView
    private lateinit var tvResolvedCount: TextView
    private lateinit var spinnerFilter: Spinner
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    private val sikayetList = mutableListOf<Sikayet>()
    private lateinit var adapter: SikayetAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sikayetler)

        initViews()
        setupNavigation()
        setupRecyclerView()
        setupFilterSpinner()
        setupSwipeRefresh()

        loadComplaints()

        fabAddComplaint.setOnClickListener {
            showAddComplaintDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val navigationView = findViewById<com.google.android.material.navigation.NavigationView>(R.id.navigation_view)
            navigationView.setCheckedItem(R.id.nav_sikayetler)
        } catch (e: Exception) {}
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        fabAddComplaint = findViewById(R.id.fabAddComplaint)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvEmptyStateText = tvEmptyState.findViewById<TextView>(R.id.textView2)
        tvTotalComplaints = findViewById(R.id.tvTotalComplaints)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvInProgressCount = findViewById(R.id.tvInProgressCount)
        tvResolvedCount = findViewById(R.id.tvResolvedCount)
        spinnerFilter = findViewById(R.id.spinnerFilter)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeColors(
            resources.getColor(R.color.purple_500, theme),
            resources.getColor(android.R.color.holo_blue_bright, theme),
            resources.getColor(android.R.color.holo_green_light, theme),
            resources.getColor(android.R.color.holo_orange_light, theme)
        )

        swipeRefreshLayout.setOnRefreshListener {
            loadComplaints()
        }
    }

    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("Tümü", "Bekleyen", "İncelenen", "Çözülen", "Reddedilen")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter

        spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                filterSikayetler(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        val isAdmin = userType == "admin" || userType == "manager"
        adapter = SikayetAdapter(
            sikayetList = sikayetList,
            isAdmin = isAdmin,
            onAdminActionClick = { sikayet ->
                showAdminActionsDialog(sikayet)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showAddComplaintDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_complaint, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        val categories = arrayOf("Bakım/Arıza", "Temizlik", "Gürültü", "Güvenlik", "Diğer")
        val categoryKeys = arrayOf("maintenance", "cleaning", "noise", "security", "other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Yeni Şikayet")
            .setView(dialogView)
            .setPositiveButton("Gönder") { dialogInterface, which ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val categoryIndex = spinnerCategory.selectedItemPosition

                if (title.isEmpty()) {
                    Toast.makeText(this, "Başlık giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (description.isEmpty()) {
                    Toast.makeText(this, "Açıklama giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val category = categoryKeys.getOrElse(categoryIndex) { "other" }
                createComplaint(title, description, category)
            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.show()
    }

    private fun showAdminActionsDialog(sikayet: Sikayet) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_admin_actions, null)

        val btnInProgress = dialogView.findViewById<Button>(R.id.btnInProgress)
        val btnResolved = dialogView.findViewById<Button>(R.id.btnResolved)
        val btnRejected = dialogView.findViewById<Button>(R.id.btnRejected)
        val btnDelete = dialogView.findViewById<Button>(R.id.btnDelete)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("${sikayet.baslik} - Yönetici İşlemleri")
            .setView(dialogView)
            .setNegativeButton("İptal", null)
            .create()

        btnInProgress.setOnClickListener {
            showUpdateStatusDialog(sikayet.id, "in_progress")
            dialog.dismiss()
        }

        btnResolved.setOnClickListener {
            showUpdateStatusDialog(sikayet.id, "resolved")
            dialog.dismiss()
        }

        btnRejected.setOnClickListener {
            showUpdateStatusDialog(sikayet.id, "rejected")
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            deleteComplaint(sikayet.id)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showUpdateStatusDialog(complaintId: Int, status: String? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_update_status, null)

        val etAdminNotes = dialogView.findViewById<EditText>(R.id.etAdminNotes)
        val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerStatus)

        val statusOptions = arrayOf("İnceleniyor", "Çözüldü", "Reddedildi")
        val statusKeys = arrayOf("in_progress", "resolved", "rejected")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter

        status?.let {
            val index = statusKeys.indexOf(it)
            if (index != -1) {
                spinnerStatus.setSelection(index)
                spinnerStatus.isEnabled = false
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Şikayet Durumu Güncelle")
            .setView(dialogView)
            .setPositiveButton("Güncelle") { dialogInterface, which ->
                val selectedIndex = spinnerStatus.selectedItemPosition
                val newStatus = statusKeys.getOrElse(selectedIndex) { "in_progress" }
                val adminNotes = etAdminNotes.text.toString().trim()

                updateComplaintStatus(complaintId, newStatus, adminNotes)
            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.show()
    }

    // ==================== API İŞLEMLERİ ====================

    private fun createComplaint(title: String, description: String, category: String) {
        progressBar.visibility = View.VISIBLE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_create_complaint.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("title=${URLEncoder.encode(title, "UTF-8")}")
                    append("&description=${URLEncoder.encode(description, "UTF-8")}")
                    append("&category=${URLEncoder.encode(category, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getCurrentUserId(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 YENİ ŞİKAYET OLUŞTUR: $postData")

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                responseCode = conn.responseCode
                println("🔍 HTTP YANIT KODU: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                    println("🔍 BAŞARILI YANIT: $result")
                } else {
                    val errorStream = conn.errorStream
                    result = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    println("🔍 HATA YANITI: $result")
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
                println("🔍 EXCEPTION: ${e.message}")
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handleCreateComplaintResult(result)
            }
        }.start()
    }

    private fun loadComplaints() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_get_complaints.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("user_email=${URLEncoder.encode(userEmail, "UTF-8")}")
                    append("&user_type=${URLEncoder.encode(userType, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getCurrentUserId(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 ŞİKAYETLERİ GETİR: $postData")

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                responseCode = conn.responseCode
                println("🔍 HTTP YANIT KODU: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                    println("🔍 BAŞARILI YANIT: $result")
                } else {
                    val errorStream = conn.errorStream
                    result = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    println("🔍 HATA YANITI: $result")
                }

            } catch (e: Exception) {
                result = "Bağlantı Hatası: ${e.message}"
                println("🔍 EXCEPTION: ${e.message}")
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false
                handleComplaintsResult(result)
            }
        }.start()
    }

    private fun handleCreateComplaintResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Şikayetiniz başarıyla gönderildi!", Toast.LENGTH_SHORT).show()
                    loadComplaints()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleComplaintsResult(jsonResponse: String) {
        try {
            sikayetList.clear()

            if (jsonResponse.isBlank()) {
                showEmptyState("Veri alınamadı")
                return
            }

            if (jsonResponse.startsWith("{")) {
                val jsonObject = JSONObject(jsonResponse)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    val complaintsArray = jsonObject.getJSONArray("complaints")

                    for (i in 0 until complaintsArray.length()) {
                        val complaintObj = complaintsArray.getJSONObject(i)

                        val sikayet = Sikayet(
                            id = complaintObj.getInt("id"),
                            baslik = complaintObj.getString("title"),
                            aciklama = complaintObj.getString("description"),
                            kategori = getCategoryLabel(complaintObj.getString("category")),
                            durum = getStatusLabel(complaintObj.getString("status")),
                            tarih = formatDateTime(complaintObj.getString("created_at")),
                            kullaniciAdi = complaintObj.getString("user_name"),
                            konum = "${complaintObj.getString("apartment_block")} ${complaintObj.getString("apartment_number")}",
                            cevap = complaintObj.optString("admin_notes", ""),
                            cevapTarihi = complaintObj.optString("updated_at", "").takeIf { it.isNotEmpty() }?.let {
                                formatDateTime(it)
                            },
                            ikametTipi = "",
                            kullaniciTipi = userType
                        )
                        sikayetList.add(sikayet)
                    }

                    updateStatistics(sikayetList)

                    if (sikayetList.isEmpty()) {
                        showEmptyState("Henüz şikayet bulunmuyor")
                    } else {
                        adapter.setSikayetList(sikayetList)
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }

                } else {
                    val message = jsonObject.optString("message", "Bilinmeyen hata")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    showEmptyState(message)
                }
            } else {
                Toast.makeText(this, "Geçersiz yanıt formatı", Toast.LENGTH_LONG).show()
                showEmptyState("Geçersiz yanıt")
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Veri işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            showEmptyState("Veri işleme hatası")
        }
    }

    private fun updateComplaintStatus(complaintId: Int, status: String, adminNotes: String) {
        progressBar.visibility = View.VISIBLE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_update_complaint_status.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("complaint_id=$complaintId")
                    append("&status=${URLEncoder.encode(status, "UTF-8")}")
                    append("&admin_notes=${URLEncoder.encode(adminNotes, "UTF-8")}")
                    append("&admin_name=${URLEncoder.encode(userName, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getCurrentUserId(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 ŞİKAYET DURUMU GÜNCELLE: $postData")

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                responseCode = conn.responseCode
                println("🔍 HTTP YANIT KODU: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                    println("🔍 BAŞARILI YANIT: $result")
                } else {
                    val errorStream = conn.errorStream
                    result = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                    println("🔍 HATA YANITI: $result")
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
                println("🔍 EXCEPTION: ${e.message}")
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handleUpdateStatusResult(result)
            }
        }.start()
    }

    private fun handleUpdateStatusResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Şikayet durumu başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
                    loadComplaints()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteComplaint(complaintId: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Şikayeti Sil")
            .setMessage("Şikayeti silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz!")
            .setPositiveButton("Sil") { dialog, which ->
                deleteComplaintApiCall(complaintId)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deleteComplaintApiCall(complaintId: Int) {
        progressBar.visibility = View.VISIBLE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_delete_complaint.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("complaint_id=$complaintId")
                    append("&user_id=${URLEncoder.encode(getCurrentUserId(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                } else {
                    val errorStream = conn.errorStream
                    result = BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "Hata: ${e.message}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handleDeleteResult(result)
            }
        }.start()
    }

    private fun handleDeleteResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Şikayet başarıyla silindi!", Toast.LENGTH_SHORT).show()
                    loadComplaints()
                } else {
                    val message = jsonObject.optString("message", "Silme işlemi başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Silme işlemi hatası", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== YARDIMCI FONKSİYONLAR ====================

    private fun updateStatistics(complaints: List<Sikayet>) {
        val totalComplaints = complaints.size
        val pendingCount = complaints.count { it.durum == "Beklemede" }
        val inProgressCount = complaints.count { it.durum == "İnceleniyor" }
        val resolvedCount = complaints.count { it.durum == "Çözüldü" }

        tvTotalComplaints.text = totalComplaints.toString()
        tvPendingCount.text = pendingCount.toString()
        tvInProgressCount.text = inProgressCount.toString()
        tvResolvedCount.text = resolvedCount.toString()
    }

    private fun filterSikayetler(filterIndex: Int) {
        val filteredList = when (filterIndex) {
            0 -> sikayetList // Tümü
            1 -> sikayetList.filter { it.durum == "Beklemede" } // Bekleyen
            2 -> sikayetList.filter { it.durum == "İnceleniyor" } // İncelenen
            3 -> sikayetList.filter { it.durum == "Çözüldü" } // Çözülen
            4 -> sikayetList.filter { it.durum == "Reddedildi" } // Reddedilen
            else -> sikayetList
        }

        adapter.setSikayetList(filteredList)

        if (filteredList.isEmpty()) {
            showEmptyState("Bu filtreye uygun şikayet bulunmuyor")
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun showEmptyState(message: String = "Henüz şikayet bulunmuyor") {
        tvEmptyStateText.text = message
        tvEmptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }



    private fun getCategoryLabel(category: String): String {
        return when (category) {
            "maintenance" -> "Bakım/Arıza"
            "cleaning" -> "Temizlik"
            "noise" -> "Gürültü"
            "security" -> "Güvenlik"
            "other" -> "Diğer"
            else -> category
        }
    }

    private fun getStatusLabel(status: String): String {
        return when (status) {
            "pending" -> "Beklemede"
            "in_progress" -> "İnceleniyor"
            "resolved" -> "Çözüldü"
            "rejected" -> "Reddedildi"
            else -> status
        }
    }

    private fun formatDateTime(dateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateTime
        }
    }
}