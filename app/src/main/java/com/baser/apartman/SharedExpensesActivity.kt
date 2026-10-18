package com.baser.apartman

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class SharedExpensesActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAddExpense: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var tvApprovedCount: TextView

    private val expensesList = mutableListOf<SharedExpense>()
    private lateinit var adapter: SharedExpensesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_expenses)

        initViews()
      //  setupNavigation()
        setupRecyclerView()

        loadExpenses()

        fabAddExpense.setOnClickListener {
            if (userType == "admin" || userType == "manager") {
                showExpenseFormDialog()
            } else {
                Toast.makeText(this, "Bu işlem için yetkiniz bulunmuyor", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        fabAddExpense = findViewById(R.id.fabAddExpense)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        tvApprovedCount = findViewById(R.id.tvApprovedCount)
    }

    private fun setupRecyclerView() {
        adapter = SharedExpensesAdapter(
            context = this,
            expenses = expensesList,
            userType = userType,
            onItemClick = { expense ->
                showExpenseDetailsDialog(expense)
            },
            onAddToDues = { expense ->
                if (userType == "admin" || userType == "manager") {
                    addExpenseToDues(expense)
                }
            },
            onEdit = { expense ->
                if (userType == "admin" || userType == "manager") {
                    showExpenseFormDialog(expense)
                }
            },
            onDelete = { expense ->
                if (userType == "admin" || userType == "manager") {
                    deleteExpense(expense)
                }
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showExpenseFormDialog(existingExpense: SharedExpense? = null) {
        // Dialog layout'u oluştur
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_expense, null)

        // View'ları bul
        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val spinnerDistribution = dialogView.findViewById<Spinner>(R.id.spinnerDistribution)
        val etExpenseDate = dialogView.findViewById<EditText>(R.id.etExpenseDate)

        // Kategorileri ayarla
        val categories = arrayOf("Bakım", "Temizlik", "Güvenlik", "İyileştirme", "Diğer")
        val categoryKeys = arrayOf("maintenance", "cleaning", "security", "improvement", "other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter

        // Dağıtım türlerini ayarla
        val distributions = arrayOf("Eşit Dağıtım", "Kat Bazlı", "Metrekare Bazlı", "Özel Dağıtım")
        val distributionKeys = arrayOf("equal", "by_floor", "by_apartment_size", "custom")
        val distributionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, distributions)
        distributionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDistribution.adapter = distributionAdapter

        // Tarih seçici
        etExpenseDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = String.format("%02d.%02d.%04d", day, month + 1, year)
                    etExpenseDate.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Bugünün tarihini varsayılan olarak ayarla
        val today = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())
        etExpenseDate.setText(today)

        // Mevcut harcama varsa formu doldur
        existingExpense?.let { expense ->
            etTitle.setText(expense.title)
            etDescription.setText(expense.description)
            etAmount.setText(expense.totalAmount.toString())

            val categoryIndex = categoryKeys.indexOf(expense.category)
            if (categoryIndex != -1) {
                spinnerCategory.setSelection(categoryIndex)
            }

            val distributionIndex = distributionKeys.indexOf(expense.distributionType)
            if (distributionIndex != -1) {
                spinnerDistribution.setSelection(distributionIndex)
            }

            etExpenseDate.setText(formatDate(expense.expenseDate))
        }

        // Dialog oluştur
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (existingExpense == null) "Yeni Harcama Ekle" else "Harcama Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { dialogInterface, which ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                val amountStr = etAmount.text.toString().trim()
                val categoryIndex = spinnerCategory.selectedItemPosition
                val distributionIndex = spinnerDistribution.selectedItemPosition
                val expenseDate = etExpenseDate.text.toString().trim()

                // Validasyon
                if (title.isEmpty()) {
                    Toast.makeText(this, "Başlık giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (amountStr.isEmpty()) {
                    Toast.makeText(this, "Tutar giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = try {
                    amountStr.toDouble()
                } catch (e: Exception) {
                    Toast.makeText(this, "Geçerli bir tutar giriniz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (amount <= 0) {
                    Toast.makeText(this, "Tutar 0'dan büyük olmalı", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // API çağrısı
                if (existingExpense == null) {
                    createExpense(
                        title = title,
                        description = description,
                        amount = amount,
                        categoryIndex = categoryIndex,
                        distributionIndex = distributionIndex,
                        expenseDate = expenseDate
                    )
                } else {
                    updateExpense(
                        expenseId = existingExpense.id,
                        title = title,
                        description = description,
                        amount = amount,
                        categoryIndex = categoryIndex,
                        distributionIndex = distributionIndex,
                        expenseDate = expenseDate
                    )
                }
            }
            .setNegativeButton("İptal") { dialogInterface, which ->
                dialogInterface.dismiss()
            }
            .create()

        // Dialog penceresini büyüt
        dialog.window?.let { window ->
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
            window.attributes = layoutParams
        }

        dialog.show()
    }

    private fun createExpense(
        title: String,
        description: String,
        amount: Double,
        categoryIndex: Int,
        distributionIndex: Int,
        expenseDate: String
    ) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_create_shared_expense.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                // Kategori ve dağıtım türünü API formatına çevir
                val categoryKeys = arrayOf("maintenance", "cleaning", "security", "improvement", "other")
                val distributionKeys = arrayOf("equal", "by_floor", "by_apartment_size", "custom")

                val category = categoryKeys.getOrElse(categoryIndex) { "other" }
                val distribution = distributionKeys.getOrElse(distributionIndex) { "equal" }

                // Tarihi API formatına çevir (yyyy-MM-dd)
                val formattedDate = try {
                    val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = inputFormat.parse(expenseDate)
                    outputFormat.format(date)
                } catch (e: Exception) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }

                // Kullanıcı ID'sini al
                val userId = getUserIdFromPrefs()

                val postData = StringBuilder().apply {
                    append("title=${URLEncoder.encode(title, "UTF-8")}")
                    append("&description=${URLEncoder.encode(description, "UTF-8")}")
                    append("&total_amount=$amount")
                    append("&category=${URLEncoder.encode(category, "UTF-8")}")
                    append("&distribution_type=${URLEncoder.encode(distribution, "UTF-8")}")
                    append("&expense_date=${URLEncoder.encode(formattedDate, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(userId, "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 YENİ HARCAMA OLUŞTUR: $postData")

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
                handleCreateExpenseResult(result)
            }
        }
    }

    private fun handleCreateExpenseResult(result: String) {
        println("🔍 CREATE EXPENSE RESULT: $result")

        try {
            if (result.isNotEmpty()) {
                if (result.startsWith("{")) {
                    val jsonObject = JSONObject(result)
                    val success = jsonObject.getBoolean("success")

                    if (success) {
                        Toast.makeText(this, "Harcama başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                        loadExpenses() // Listeyi yenile
                    } else {
                        val message = jsonObject.optString("message", "İşlem başarısız")
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                } else {
                    // JSON olmayan yanıtları işle
                    if (result.contains("success")) {
                        // JSON parse edilmeye çalış
                        val cleanResult = result.replace("\\", "")
                        val jsonObject = JSONObject(cleanResult)
                        val success = jsonObject.getBoolean("success")

                        if (success) {
                            Toast.makeText(this, "Harcama başarıyla oluşturuldu!", Toast.LENGTH_SHORT).show()
                            loadExpenses()
                        } else {
                            val message = jsonObject.optString("message", "İşlem başarısız")
                            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "API yanıtı: $result", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Boş yanıt alındı", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası: ${e.message}\nYanıt: $result", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateExpense(
        expenseId: Int,
        title: String,
        description: String,
        amount: Double,
        categoryIndex: Int,
        distributionIndex: Int,
        expenseDate: String
    ) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_update_shared_expense.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                // Kategori ve dağıtım türünü API formatına çevir
                val categoryKeys = arrayOf("maintenance", "cleaning", "security", "improvement", "other")
                val distributionKeys = arrayOf("equal", "by_floor", "by_apartment_size", "custom")

                val category = categoryKeys.getOrElse(categoryIndex) { "other" }
                val distribution = distributionKeys.getOrElse(distributionIndex) { "custom" }

                // Tarihi API formatına çevir
                val formattedDate = try {
                    val inputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                    val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = inputFormat.parse(expenseDate)
                    outputFormat.format(date)
                } catch (e: Exception) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                }

                val postData = StringBuilder().apply {
                    append("expense_id=$expenseId")
                    append("&title=${URLEncoder.encode(title, "UTF-8")}")
                    append("&description=${URLEncoder.encode(description, "UTF-8")}")
                    append("&total_amount=$amount")
                    append("&category=${URLEncoder.encode(category, "UTF-8")}")
                    append("&distribution_type=${URLEncoder.encode(distribution, "UTF-8")}")
                    append("&expense_date=${URLEncoder.encode(formattedDate, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 HARCAMA GÜNCELLE: $postData")

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
                result = "Hata: ${e.message}"
                println("🔍 EXCEPTION: ${e.message}")
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handleUpdateExpenseResult(result)
            }
        }
    }

    private fun handleUpdateExpenseResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Harcama başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
                    loadExpenses() // Listeyi yenile
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Geçersiz yanıt formatı", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showExpenseDetailsDialog(expense: SharedExpense) {
        val details = """
            Başlık: ${expense.title}
            Kategori: ${getCategoryLabel(expense.category)}
            Tutar: ₺${NumberFormat.getInstance().format(expense.totalAmount)}
            Dağıtım: ${getDistributionLabel(expense.distributionType)}
            Tarih: ${formatDate(expense.expenseDate)}
            Durum: ${getStatusLabel(expense.status)}
            Oluşturan: ${expense.createdByName}
            Aidata Eklenen: ${expense.addedToDues}/${expense.totalUsers}
            
            ${if (expense.description.isNotEmpty()) "Açıklama: ${expense.description}" else ""}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Harcama Detayları")
            .setMessage(details)
            .setPositiveButton("Tamam", null)
            .show()
    }

    fun loadExpenses() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_get_shared_expenses.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("Accept", "application/json")

                val postData = StringBuilder().apply {
                    append("user_email=${URLEncoder.encode(userEmail, "UTF-8")}")
                    append("&user_type=${URLEncoder.encode(userType, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 HARCAMALARI GETİR: $postData")

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
                handleExpensesResult(result)
            }
        }
    }

    private fun handleExpensesResult(jsonResponse: String) {
        try {
            expensesList.clear()

            if (jsonResponse.isBlank()) {
                showEmptyState()
                return
            }

            if (jsonResponse.startsWith("{")) {
                val jsonObject = JSONObject(jsonResponse)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    val expensesArray = jsonObject.getJSONArray("expenses")

                    for (i in 0 until expensesArray.length()) {
                        val expenseObj = expensesArray.getJSONObject(i)
                        val expense = SharedExpense(
                            id = expenseObj.getInt("id"),
                            title = expenseObj.getString("title"),
                            description = expenseObj.optString("description", ""),
                            totalAmount = expenseObj.getDouble("total_amount"),
                            category = expenseObj.getString("category"),
                            distributionType = expenseObj.getString("distribution_type"),
                            expenseDate = expenseObj.getString("expense_date"),
                            status = expenseObj.optString("status", "pending"),
                            createdByName = expenseObj.optString("created_by_name", ""),
                            totalUsers = expenseObj.optInt("total_users", 0),
                            addedToDues = expenseObj.optInt("added_to_dues", 0),
                            createdBy = expenseObj.optInt("created_by", 0)
                        )
                        expensesList.add(expense)
                    }

                    updateStatistics(expensesList)

                    if (expensesList.isEmpty()) {
                        showEmptyState()
                    } else {
                        adapter.notifyDataSetChanged()
                        tvEmptyState.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                    }

                } else {
                    val message = jsonObject.optString("message", "Bilinmeyen hata")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    showEmptyState()
                }
            } else {
                Toast.makeText(this, "Geçersiz yanıt formatı", Toast.LENGTH_LONG).show()
                showEmptyState()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Veri işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
            showEmptyState()
        }
    }

    private fun updateStatistics(expenses: List<SharedExpense>) {
        val totalExpenses = expenses.size
        val totalAmount = expenses.sumOf { it.totalAmount }
        val pendingCount = expenses.count { it.status == "pending" }
        val approvedCount = expenses.count { it.status == "approved" }

        tvTotalExpenses.text = totalExpenses.toString()
        tvTotalAmount.text = "₺${NumberFormat.getInstance().format(totalAmount)}"
        tvPendingCount.text = pendingCount.toString()
        tvApprovedCount.text = approvedCount.toString()
    }

    private fun showEmptyState() {
        tvEmptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun getUserIdFromPrefs(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", "") ?: ""
    }

    // Admin/Manager işlemleri
    private fun addExpenseToDues(expense: SharedExpense) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Aidata Ekle")
            .setMessage("Bu harcamayı aidatlara eklemek istediğinizden emin misiniz?")
            .setPositiveButton("Evet") { dialog, which ->
                addToDuesApiCall(expense.id)
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun deleteExpense(expense: SharedExpense) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Harcamayı Sil")
            .setMessage("${expense.title} başlıklı harcamayı silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz!")
            .setPositiveButton("Sil") { dialog, which ->
                deleteExpenseApiCall(expense.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun addToDuesApiCall(expenseId: Int) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_add_expense_to_dues.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("expense_id=$expenseId")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
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
                handleAddToDuesResult(result, expenseId)
            }
        }
    }

    private fun handleAddToDuesResult(result: String, expenseId: Int) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Harcama başarıyla aidatlara eklendi!", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteExpenseApiCall(expenseId: Int) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_delete_shared_expense.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("expense_id=$expenseId")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
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
        }
    }

    private fun handleDeleteResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Harcama başarıyla silindi!", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    val message = jsonObject.optString("message", "Silme işlemi başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Silme işlemi hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getCategoryLabel(category: String): String {
        return when (category) {
            "maintenance" -> "Bakım"
            "cleaning" -> "Temizlik"
            "security" -> "Güvenlik"
            "improvement" -> "İyileştirme"
            "other" -> "Diğer"
            else -> category
        }
    }

    private fun getDistributionLabel(distribution: String): String {
        return when (distribution) {
            "equal" -> "Eşit Dağıtım"
            "by_floor" -> "Kat Bazlı"
            "by_apartment_size" -> "Metrekare Bazlı"
            "custom" -> "Özel Dağıtım"
            else -> distribution
        }
    }

    private fun getStatusLabel(status: String): String {
        return when (status) {
            "pending" -> "Bekliyor"
            "approved" -> "Onaylandı"
            "rejected" -> "Reddedildi"
            else -> status
        }
    }

    // ==================== YENİ EKLENEN FONKSİYONLAR ====================

    // 1. DAĞITIM DETAYLARINI GÖRÜNTÜLEME
    private fun showDistributionDetails(expense: SharedExpense) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            try {
                val url = URL("http://baser.org/apartman/api/api_get_expense_distribution.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("expense_id=${expense.id}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                showDistributionDetailsDialog(expense, result)
            }
        }
    }

    private fun showDistributionDetailsDialog(expense: SharedExpense, jsonResponse: String) {
        try {
            if (jsonResponse.startsWith("{")) {
                val jsonObject = JSONObject(jsonResponse)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    val distributionsArray = jsonObject.getJSONArray("distributions")
                    val distributionDetails = parseDistributionDetails(distributionsArray)

                    showDistributionListDialog(expense, distributionDetails)
                } else {
                    val message = jsonObject.optString("message", "Dağıtım bilgisi alınamadı")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Dağıtım detayları yüklenemedi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseDistributionDetails(distributionsArray: JSONArray): List<DistributionDetail> {
        val distributionList = mutableListOf<DistributionDetail>()

        for (i in 0 until distributionsArray.length()) {
            val distObj = distributionsArray.getJSONObject(i)
            val distribution = DistributionDetail(
                id = distObj.getInt("id"),
                userId = distObj.getInt("user_id"),
                userName = distObj.optString("user_name", ""),
                apartmentCode = distObj.optString("apartment_code", ""),
                shareAmount = distObj.getDouble("share_amount"),
                sharePercentage = distObj.optDouble("share_percentage", 0.0),
                status = distObj.optString("status", "pending"),
                addedToDuesDate = distObj.optString("added_to_dues_date", null),
                dueStatus = distObj.optString("due_status", "")
            )
            distributionList.add(distribution)
        }

        return distributionList
    }

    private fun showDistributionListDialog(expense: SharedExpense, distributions: List<DistributionDetail>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_distribution_details, null)

        val tvExpenseInfo = dialogView.findViewById<TextView>(R.id.tvExpenseInfo)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val tvTotalAmount = dialogView.findViewById<TextView>(R.id.tvTotalAmount)
        val tvDistributedAmount = dialogView.findViewById<TextView>(R.id.tvDistributedAmount)
        val tvPendingCount = dialogView.findViewById<TextView>(R.id.tvPendingCount)
        val tvAddedCount = dialogView.findViewById<TextView>(R.id.tvAddedCount)
        val tvPaidCount = dialogView.findViewById<TextView>(R.id.tvPaidCount)

        // Gider bilgilerini göster
        tvExpenseInfo.text = "${expense.title} - ₺${NumberFormat.getInstance().format(expense.totalAmount)}"

        // İstatistikleri hesapla
        val totalAmount = expense.totalAmount
        val distributedAmount = distributions.sumOf { it.shareAmount }
        val pendingCount = distributions.count { it.status == "pending" }
        val addedCount = distributions.count { it.status == "added_to_dues" }
        val paidCount = distributions.count { it.status == "paid" }

        tvTotalAmount.text = "Toplam: ₺${NumberFormat.getInstance().format(totalAmount)}"
        tvDistributedAmount.text = "Dağıtılan: ₺${NumberFormat.getInstance().format(distributedAmount)}"
        tvPendingCount.text = "Bekleyen: $pendingCount"
        tvAddedCount.text = "Aidata Eklenen: $addedCount"
        tvPaidCount.text = "Ödenen: $paidCount"

        // RecyclerView için adapter
        val adapter = DistributionDetailsAdapter(this, distributions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Dağıtım Detayları")
            .setView(dialogView)
            .setPositiveButton("Tamam", null)

        // Admin/Manager için ek butonlar
        if (userType == "admin" || userType == "manager") {
            dialog.setNegativeButton("İşlemler") { _, _ ->
                showDistributionActionsDialog(expense, distributions)
            }
        }

        dialog.show()
    }

    // 2. DAĞITIM İŞLEMLERİ DIALOG'U
    private fun showDistributionActionsDialog(expense: SharedExpense, distributions: List<DistributionDetail>) {
        val actions = mutableListOf<String>()

        // Duruma göre işlemleri belirle
        if (expense.status == "pending") {
            actions.add("Dağıtımı Yeniden Hesapla")
            actions.add("Dağıtımı Düzenle")
            actions.add("Dağıtımı Sil")
        }

        if (distributions.any { it.status == "pending" }) {
            actions.add("Bekleyenleri Aidata Aktar")
        }

        if (distributions.any { it.status == "added_to_dues" }) {
            actions.add("Aidata Aktarılanları Görüntüle")
        }

        if (actions.isEmpty()) {
            Toast.makeText(this, "Bu gider için uygulanabilir işlem bulunmuyor", Toast.LENGTH_SHORT).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Dağıtım İşlemleri")
            .setItems(actions.toTypedArray()) { dialog, which ->
                when (actions[which]) {
                    "Dağıtımı Yeniden Hesapla" -> recalculateDistribution(expense)
                    "Dağıtımı Düzenle" -> editDistribution(expense, distributions)
                    "Dağıtımı Sil" -> deleteDistribution(expense)
                    "Bekleyenleri Aidata Aktar" -> addPendingToDues(expense, distributions)
                    "Aidata Aktarılanları Görüntüle" -> showAddedToDuesDetails(expense, distributions)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // 3. DAĞITIMI YENİDEN HESAPLA
    private fun recalculateDistribution(expense: SharedExpense) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Dağıtımı Yeniden Hesapla")
            .setMessage("Mevcut dağıtımı silip yeniden hesaplamak istediğinize emin misiniz?")
            .setPositiveButton("Evet") { dialog, which ->
                recalculateDistributionApi(expense)
            }
            .setNegativeButton("Hayır", null)
            .show()
    }

    private fun recalculateDistributionApi(expense: SharedExpense) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            try {
                val url = URL("http://baser.org/apartman/api/api_recalculate_distribution.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("expense_id=${expense.id}")
                    append("&distribution_type=${URLEncoder.encode(expense.distributionType, "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handleRecalculateResult(result)
            }
        }
    }

    private fun handleRecalculateResult(result: String) {
        try {
            if (result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Dağıtım yeniden hesaplandı", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "İşlem hatası", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. DAĞITIMI DÜZENLE (MANUEL DAĞITIM)
    private fun editDistribution(expense: SharedExpense, distributions: List<DistributionDetail>) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_distribution, null)

        val tvExpenseInfo = dialogView.findViewById<TextView>(R.id.tvExpenseInfo)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerView)
        val tvTotalAmount = dialogView.findViewById<TextView>(R.id.tvTotalAmount)
        val tvRemainingAmount = dialogView.findViewById<TextView>(R.id.tvRemainingAmount)

        tvExpenseInfo.text = "${expense.title} - ₺${NumberFormat.getInstance().format(expense.totalAmount)}"
        tvTotalAmount.text = "Toplam: ₺${NumberFormat.getInstance().format(expense.totalAmount)}"

        // Adapter'ı oluştur
        val adapter = EditDistributionAdapter(this, expense.totalAmount, distributions)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Toplam tutarı güncelle
        adapter.onAmountChanged = { remainingAmount ->
            tvRemainingAmount.text = "Kalan: ₺${NumberFormat.getInstance().format(remainingAmount)}"
            tvRemainingAmount.setTextColor(
                if (remainingAmount >= 0)
                    resources.getColor(android.R.color.holo_green_dark, null)
                else
                    resources.getColor(android.R.color.holo_red_dark, null)
            )
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Dağıtımı Düzenle")
            .setView(dialogView)
            .setPositiveButton("Kaydet") { dialogInterface, which ->
                val updatedDistributions = adapter.getUpdatedDistributions()
                saveUpdatedDistribution(expense.id, updatedDistributions)
            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.window?.let { window ->
            val layoutParams = WindowManager.LayoutParams()
            layoutParams.copyFrom(window.attributes)
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            window.attributes = layoutParams
        }

        dialog.show()
    }

    private fun saveUpdatedDistribution(expenseId: Int, distributions: List<DistributionDetail>) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            try {
                val url = URL("http://baser.org/apartman/api/api_update_distribution.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                // Dağıtım verilerini JSON formatında gönder
                val distributionsJson = JSONArray()
                distributions.forEach { dist ->
                    val distJson = JSONObject()
                    distJson.put("id", dist.id)
                    distJson.put("share_amount", dist.shareAmount)
                    distributionsJson.put(distJson)
                }

                val postData = StringBuilder().apply {
                    append("expense_id=$expenseId")
                    append("&distributions=${URLEncoder.encode(distributionsJson.toString(), "UTF-8")}")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handleUpdateDistributionResult(result)
            }
        }
    }

    private fun handleUpdateDistributionResult(result: String) {
        try {
            if (result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Dağıtım başarıyla güncellendi", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "İşlem hatası", Toast.LENGTH_SHORT).show()
        }
    }

    // 5. BEKLEYEN DAĞITIMLARI AİDATA AKTAR
    private fun addPendingToDues(expense: SharedExpense, distributions: List<DistributionDetail>) {
        val pendingDistributions = distributions.filter { it.status == "pending" }

        if (pendingDistributions.isEmpty()) {
            Toast.makeText(this, "Aktarılacak bekleyen dağıtım bulunmuyor", Toast.LENGTH_SHORT).show()
            return
        }

        val totalAmount = pendingDistributions.sumOf { it.shareAmount }

        MaterialAlertDialogBuilder(this)
            .setTitle("Aidata Aktar")
            .setMessage("${pendingDistributions.size} dağıtım (Toplam: ₺${NumberFormat.getInstance().format(totalAmount)}) aidata aktarılacak.\n\nDevam etmek istiyor musunuz?")
            .setPositiveButton("Aktar") { dialog, which ->
                addPendingDistributionsToDues(expense.id, pendingDistributions)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // 6. DAĞITIMI SİL
    private fun deleteDistribution(expense: SharedExpense) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Dağıtımı Sil")
            .setMessage("Bu harcamanın tüm dağıtım kayıtlarını silmek istediğinize emin misiniz?\n\nBu işlem geri alınamaz!")
            .setPositiveButton("Sil") { dialog, which ->
                deleteDistributionApi(expense.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    // Helper functions
    private fun addPendingDistributionsToDues(expenseId: Int, distributions: List<DistributionDetail>) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            try {
                val url = URL("http://baser.org/apartman/api/api_add_pending_to_dues.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("expense_id=$expenseId")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handlePendingToDuesResult(result)
            }
        }
    }
    // 1. handlePendingToDuesResult metodu
    private fun handlePendingToDuesResult(result: String) {
        try {
            if (result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    val message = jsonObject.getString("message")
                    val addedCount = jsonObject.optInt("added_count", 0)
                    val totalAmount = jsonObject.optDouble("total_amount", 0.0)

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    loadExpenses() // Listeyi yenile
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Geçersiz yanıt formatı", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteDistributionApi(expenseId: Int) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            try {
                val url = URL("http://baser.org/apartman/api/api_delete_distribution.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("expense_id=$expenseId")
                    append("&user_id=${URLEncoder.encode(getUserIdFromPrefs(), "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                handleDeleteDistributionResult(result)
            }
        }
    }

    private fun showAddedToDuesDetails(expense: SharedExpense, distributions: List<DistributionDetail>) {
        progressBar.visibility = View.VISIBLE

        thread {
            var result = ""
            try {
                val url = URL("http://baser.org/apartman/api/api_get_added_to_dues_details.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("expense_id=${expense.id}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                val outputStream = conn.outputStream
                OutputStreamWriter(outputStream, "UTF-8").use { writer ->
                    writer.write(postData)
                    writer.flush()
                }

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                }

            } catch (e: Exception) {
                result = "{\"success\": false, \"message\": \"${e.message}\"}"
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                showAddedToDuesDialog(expense, result)
            }
        }
    }

    private fun showAddedToDuesDialog(expense: SharedExpense, result: String) {
        try {
            if (result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    val summary = jsonObject.getJSONObject("summary")
                    val distributionsArray = jsonObject.getJSONArray("distributions")

                    val message = """
                        Aidata Aktarılan Detaylar:
                        
                        Toplam Dağıtım: ${summary.getInt("total_distributions")}
                        Toplam Tutar: ₺${NumberFormat.getInstance().format(summary.getDouble("total_amount"))}
                        Ödenen: ₺${NumberFormat.getInstance().format(summary.getDouble("paid_amount"))}
                        Bekleyen: ₺${NumberFormat.getInstance().format(summary.getDouble("pending_amount"))}
                        Gecikmiş: ₺${NumberFormat.getInstance().format(summary.getDouble("overdue_amount"))}
                    """.trimIndent()

                    MaterialAlertDialogBuilder(this)
                        .setTitle("Aidata Aktarılan Detayları")
                        .setMessage(message)
                        .setPositiveButton("Tamam", null)
                        .show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Detaylar yüklenemedi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleDeleteDistributionResult(result: String) {
        try {
            if (result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Dağıtım başarıyla silindi", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "İşlem hatası", Toast.LENGTH_SHORT).show()
        }
    }
}

// Data Classes
data class SharedExpense(
    val id: Int,
    val title: String,
    val description: String,
    val totalAmount: Double,
    val category: String,
    val distributionType: String,
    val expenseDate: String,
    val status: String,
    val createdByName: String,
    val totalUsers: Int,
    val addedToDues: Int,
    val createdBy: Int
)

data class DistributionDetail(
    val id: Int,
    val userId: Int,
    val userName: String,
    val apartmentCode: String,
    val shareAmount: Double,
    val sharePercentage: Double,
    val status: String,
    val addedToDuesDate: String?,
    val dueStatus: String
)

// ==================== ADAPTER'LAR ====================

// SharedExpensesAdapter
class SharedExpensesAdapter(
    private val context: Context,
    private val expenses: List<SharedExpense>,
    private val userType: String,
    private val onItemClick: (SharedExpense) -> Unit,
    private val onAddToDues: (SharedExpense) -> Unit,
    private val onEdit: (SharedExpense) -> Unit,
    private val onDelete: (SharedExpense) -> Unit
) : RecyclerView.Adapter<SharedExpensesAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardView)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDistribution: TextView = itemView.findViewById(R.id.tvDistribution)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvCreatedBy: TextView = itemView.findViewById(R.id.tvCreatedBy)
        val tvDistributionInfo: TextView = itemView.findViewById(R.id.tvDistributionInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_expense_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val expense = expenses[position]

        holder.tvTitle.text = expense.title
        holder.tvCategory.text = getCategoryLabel(expense.category)
        holder.tvAmount.text = "₺${NumberFormat.getInstance().format(expense.totalAmount)}"
        holder.tvDistribution.text = getDistributionLabel(expense.distributionType)
        holder.tvDate.text = formatDate(expense.expenseDate)
        holder.tvStatus.text = getStatusLabel(expense.status)
        holder.tvCreatedBy.text = "Oluşturan: ${expense.createdByName}"
        holder.tvDistributionInfo.text = "${expense.addedToDues}/${expense.totalUsers} aidata eklendi"

        // Durum rengini ayarla
        val statusColor = getStatusColor(expense.status)
        holder.tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor))

        // CardView tıklama olayı
        holder.cardView.setOnClickListener {
            onItemClick(expense)
        }

        // Uzun tıklama ile admin işlemleri
        if (userType == "admin" || userType == "manager") {
            holder.cardView.setOnLongClickListener {
                showAdminOptions(context, expense)
                true
            }
        }

        // Açıklama varsa göster
        if (expense.description.isNotEmpty()) {
            val shortDescription = if (expense.description.length > 50) {
                "${expense.description.substring(0, 50)}..."
            } else {
                expense.description
            }
            holder.tvTitle.text = "${expense.title}\n${shortDescription}"
        }
    }

    override fun getItemCount() = expenses.size

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getCategoryLabel(category: String): String {
        return when (category) {
            "maintenance" -> "Bakım"
            "cleaning" -> "Temizlik"
            "security" -> "Güvenlik"
            "improvement" -> "İyileştirme"
            "other" -> "Diğer"
            else -> category
        }
    }

    private fun getDistributionLabel(distribution: String): String {
        return when (distribution) {
            "equal" -> "Eşit Dağıtım"
            "by_floor" -> "Kat Bazlı"
            "by_apartment_size" -> "Metrekare Bazlı"
            "custom" -> "Özel Dağıtım"
            else -> distribution
        }
    }

    private fun getStatusLabel(status: String): String {
        return when (status) {
            "pending" -> "Bekliyor"
            "approved" -> "Onaylandı"
            "rejected" -> "Reddedildi"
            else -> status
        }
    }

    private fun getStatusColor(status: String): String {
        return when (status) {
            "pending" -> "#ffc107"
            "approved" -> "#28a745"
            "rejected" -> "#dc3545"
            else -> "#6c757d"
        }
    }

    private fun showAdminOptions(context: Context, expense: SharedExpense) {
        val options = arrayOf("Aidata Ekle", "Düzenle", "Sil")

        MaterialAlertDialogBuilder(context)
            .setTitle("Harcama İşlemleri")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> onAddToDues(expense)
                    1 -> onEdit(expense)
                    2 -> onDelete(expense)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}

// Distribution Details Adapter
class DistributionDetailsAdapter(
    private val context: Context,
    private val distributions: List<DistributionDetail>
) : RecyclerView.Adapter<DistributionDetailsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvApartment: TextView = itemView.findViewById(R.id.tvApartment)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_distribution_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val distribution = distributions[position]

        holder.tvUserName.text = distribution.userName
        holder.tvApartment.text = distribution.apartmentCode
        holder.tvAmount.text = "₺${NumberFormat.getInstance().format(distribution.shareAmount)}"
        holder.tvPercentage.text = "${String.format("%.1f", distribution.sharePercentage)}%"
        holder.tvStatus.text = getStatusLabel(distribution.status)

        // Duruma göre renk
        val statusColor = when (distribution.status) {
            "pending" -> "#ffc107"
            "added_to_dues" -> "#17a2b8"
            "paid" -> "#28a745"
            else -> "#6c757d"
        }
        holder.tvStatus.setTextColor(android.graphics.Color.parseColor(statusColor))
    }

    override fun getItemCount() = distributions.size

    private fun getStatusLabel(status: String): String {
        return when (status) {
            "pending" -> "Bekliyor"
            "added_to_dues" -> "Aidata Eklendi"
            "paid" -> "Ödendi"
            else -> status
        }
    }
}

// Edit Distribution Adapter
class EditDistributionAdapter(
    private val context: Context,
    private val totalAmount: Double,
    private val distributions: List<DistributionDetail>
) : RecyclerView.Adapter<EditDistributionAdapter.ViewHolder>() {

    var onAmountChanged: ((Double) -> Unit)? = null
    private val editedAmounts = mutableMapOf<Int, Double>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvApartment: TextView = itemView.findViewById(R.id.tvApartment)
        val etAmount: EditText = itemView.findViewById(R.id.etAmount)
        val tvPercentage: TextView = itemView.findViewById(R.id.tvPercentage)
        val btnSetEqual: ImageButton = itemView.findViewById(R.id.btnSetEqual)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_distribution, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val distribution = distributions[position]
        val currentAmount = editedAmounts[distribution.id] ?: distribution.shareAmount

        holder.tvUserName.text = distribution.userName
        holder.tvApartment.text = distribution.apartmentCode
        holder.etAmount.setText(NumberFormat.getInstance().format(currentAmount))

        // Yüzdeyi hesapla
        val percentage = (currentAmount / totalAmount) * 100
        holder.tvPercentage.text = "${String.format("%.1f", percentage)}%"

        // Eşit dağıtım butonu
        holder.btnSetEqual.setOnClickListener {
            val equalAmount = totalAmount / distributions.size
            holder.etAmount.setText(NumberFormat.getInstance().format(equalAmount))
            editedAmounts[distribution.id] = equalAmount
            updateRemainingAmount()
        }

        // Tutar değişikliği listener'ı
        holder.etAmount.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val newAmount = holder.etAmount.text.toString()
                        .replace(",", ".")
                        .toDouble()

                    if (newAmount >= 0) {
                        editedAmounts[distribution.id] = newAmount
                        updateRemainingAmount()
                    }
                } catch (e: Exception) {
                    // Geçersiz giriş
                }
            }
        }
    }

    override fun getItemCount() = distributions.size

    fun getUpdatedDistributions(): List<DistributionDetail> {
        return distributions.map { dist ->
            val updatedAmount = editedAmounts[dist.id] ?: dist.shareAmount
            dist.copy(shareAmount = updatedAmount)
        }
    }

    private fun updateRemainingAmount() {
        val totalDistributed = distributions.sumOf { dist ->
            editedAmounts[dist.id] ?: dist.shareAmount
        }
        val remainingAmount = totalAmount - totalDistributed
        onAmountChanged?.invoke(remainingAmount)
    }
}