package com.baser.apartman

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        setupNavigation() // BaseActivity'den geliyor
        setupRecyclerView()

        loadExpenses()

        fabAddExpense.setOnClickListener {
            if (userType == "admin" || userType == "manager") {
                showAddExpenseDialog()
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
            expensesList,
            userType,
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
                    editExpense(expense)
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

    // Basit bir harcama ekleme dialog'u
    private fun showAddExpenseDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Yeni Harcama Ekle")
            .setMessage("Yeni harcama ekleme özelliği yakında eklenecek. Web arayüzünü kullanabilirsiniz.")
            .setPositiveButton("Tamam", null)
            .show()
    }

    // Harcama detaylarını gösteren dialog
    private fun showExpenseDetailsDialog(expense: SharedExpense) {
        val details = """
            Başlık: ${expense.title}
            Kategori: ${expense.category}
            Tutar: ₺${NumberFormat.getInstance().format(expense.totalAmount)}
            Dağıtım: ${expense.distributionType}
            Tarih: ${formatDate(expense.expenseDate)}
            Durum: ${expense.status}
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

                // Web'deki gibi parametreleri gönder
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

                    // İstatistikleri güncelle
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

    private fun editExpense(expense: SharedExpense) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Harcama Düzenle")
            .setMessage("Harcama düzenleme özelliği yakında eklenecek.")
            .setPositiveButton("Tamam", null)
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

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
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
                    loadExpenses() // Listeyi yenile
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

                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = conn.inputStream
                    result = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
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
                    loadExpenses() // Listeyi yenile
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

// Adapter
class SharedExpensesAdapter(
    private val expenses: List<SharedExpense>,
    private val userType: String,
    private val onItemClick: (SharedExpense) -> Unit,
    private val onAddToDues: (SharedExpense) -> Unit,
    private val onEdit: (SharedExpense) -> Unit,
    private val onDelete: (SharedExpense) -> Unit
) : RecyclerView.Adapter<SharedExpensesAdapter.ViewHolder>() {

    private val categoryLabels = mapOf(
        "maintenance" to "Bakım",
        "cleaning" to "Temizlik",
        "security" to "Güvenlik",
        "improvement" to "İyileştirme",
        "other" to "Diğer"
    )

    private val distributionLabels = mapOf(
        "equal" to "Eşit Dağıtım",
        "by_floor" to "Kat Bazlı",
        "by_apartment_size" to "Metrekare Bazlı",
        "custom" to "Özel Dağıtım"
    )

    private val statusLabels = mapOf(
        "pending" to "Bekliyor",
        "approved" to "Onaylandı",
        "rejected" to "Reddedildi"
    )

    private val statusColors = mapOf(
        "pending" to "#ffc107",
        "approved" to "#28a745",
        "rejected" to "#dc3545"
    )

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
        val context = holder.itemView.context

        holder.tvTitle.text = expense.title
        holder.tvCategory.text = categoryLabels[expense.category] ?: expense.category
        holder.tvAmount.text = "₺${NumberFormat.getInstance().format(expense.totalAmount)}"
        holder.tvDistribution.text = distributionLabels[expense.distributionType] ?: expense.distributionType
        holder.tvDate.text = formatDate(expense.expenseDate)
        holder.tvStatus.text = statusLabels[expense.status] ?: expense.status
        holder.tvCreatedBy.text = "Oluşturan: ${expense.createdByName}"
        holder.tvDistributionInfo.text = "${expense.addedToDues}/${expense.totalUsers} aidata eklendi"

        // Durum rengini ayarla
        val statusColor = statusColors[expense.status] ?: "#6c757d"
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