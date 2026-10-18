package com.baser.apartman

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.appcompat.widget.SearchView

class ButonDurumActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvEmptyState: TextView
    private lateinit var searchView: SearchView

    private val butonList = mutableListOf<ButonDurum>()
    private lateinit var adapter: ButonDurumAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_buton_durum)

        setupToolbar()
        initViews()
        setupNavigation()
        setupRecyclerView()
        setupSearchView()

        loadButonDurum()

        fabAdd.setOnClickListener {
            showButonDurumFormDialog()
        }
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Buton Durum Yönetimi"

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        fabAdd = findViewById(R.id.fabAdd)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        searchView = findViewById(R.id.searchView)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                filterList(query)
                return true
            }

            override fun onQueryTextChange(newText: String): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    private fun filterList(query: String) {
        val filteredList = if (query.trim().isEmpty()) {
            butonList.toList()
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            butonList.filter {
                it.ad.lowercase().contains(lowerCaseQuery) ||
                        it.soyad.lowercase().contains(lowerCaseQuery) ||
                        it.telefon.contains(query.trim()) ||
                        it.email.lowercase().contains(lowerCaseQuery) ||
                        it.yer.lowercase().contains(lowerCaseQuery)
            }
        }

        adapter.updateList(filteredList)

        if (filteredList.isEmpty()) {
            tvEmptyState.text = if (query.trim().isEmpty())
                "Henüz kayıt bulunmuyor."
            else
                "Aranan kritere uygun kayıt bulunamadı."
            tvEmptyState.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvEmptyState.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        adapter = ButonDurumAdapter(
            butonList,
            onItemClick = { buton ->
                showButonDurumDetailsDialog(buton)
            },
            onEdit = { buton ->
                showButonDurumFormDialog(buton)
            },
            onDelete = { buton ->
                deleteButonDurum(buton)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showButonDurumFormDialog(existingButon: ButonDurum? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_buton_durum_form, null)

        val etTelefon = dialogView.findViewById<EditText>(R.id.etTelefon)
        val etAd = dialogView.findViewById<EditText>(R.id.etAd)
        val etSoyad = dialogView.findViewById<EditText>(R.id.etSoyad)
        val etYer = dialogView.findViewById<EditText>(R.id.etYer)
        val etEmail = dialogView.findViewById<EditText>(R.id.etEmail)

        existingButon?.let { buton ->
            etTelefon.setText(buton.telefon)
            etAd.setText(buton.ad)
            etSoyad.setText(buton.soyad)
            etYer.setText(buton.yer)
            etEmail.setText(buton.email)
        }

        val dialogTitle = if (existingButon == null) "Yeni Buton Durumu Ekle" else "Buton Durumu Düzenle"

        MaterialAlertDialogBuilder(this)
            .setTitle(dialogTitle)
            .setView(dialogView)
            .setPositiveButton("Kaydet") { _, _ ->
                val telefon = etTelefon.text.toString().trim()
                val ad = etAd.text.toString().trim()
                val soyad = etSoyad.text.toString().trim()
                val yer = etYer.text.toString().trim()
                val email = etEmail.text.toString().trim()

                when {
                    telefon.isEmpty() -> Toast.makeText(this, "Telefon alanı zorunludur", Toast.LENGTH_SHORT).show()
                    ad.isEmpty() -> Toast.makeText(this, "Ad alanı zorunludur", Toast.LENGTH_SHORT).show()
                    soyad.isEmpty() -> Toast.makeText(this, "Soyad alanı zorunludur", Toast.LENGTH_SHORT).show()
                    else -> {
                        if (existingButon == null) {
                            createButonDurum(telefon, ad, soyad, yer, email)
                        } else {
                            updateButonDurum(existingButon.id, telefon, ad, soyad, yer, email)
                        }
                    }
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showButonDurumDetailsDialog(buton: ButonDurum) {
        val details = """
            📞 Telefon: ${buton.telefon}
            👤 Ad: ${buton.ad}
            👤 Soyad: ${buton.soyad}
            📍 Yer: ${buton.yer}
            📧 Email: ${buton.email}
            📅 Tarih: ${formatDateTime(buton.tarih)}
            🔢 ID: ${buton.id}
        """.trimIndent()

        MaterialAlertDialogBuilder(this)
            .setTitle("Buton Durum Detayları")
            .setMessage(details)
            .setPositiveButton("Tamam", null)
            .setNeutralButton("Düzenle") { _, _ ->
                showButonDurumFormDialog(buton)
            }
            .show()
    }

    private fun createButonDurum(telefon: String, ad: String, soyad: String, yer: String, email: String) {
        progressBar.visibility = View.VISIBLE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_create_buton_durum.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("telefon=${URLEncoder.encode(telefon, "UTF-8")}")
                    append("&ad=${URLEncoder.encode(ad, "UTF-8")}")
                    append("&soyad=${URLEncoder.encode(soyad, "UTF-8")}")
                    append("&yer=${URLEncoder.encode(yer, "UTF-8")}")
                    append("&email=${URLEncoder.encode(email, "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 YENİ BUTON DURUM OLUŞTUR: $postData")

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
                handleCreateResult(result)
            }
        }.start()
    }

    fun loadButonDurum() {
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_get_buton_durum.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "GET"
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Accept", "application/json")

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
                handleLoadResult(result)
            }
        }.start()
    }

    private fun handleCreateResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Buton durumu başarıyla eklendi!", Toast.LENGTH_SHORT).show()
                    loadButonDurum()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleLoadResult(jsonResponse: String) {
        try {
            butonList.clear()

            if (jsonResponse.isBlank()) {
                showEmptyState()
                return
            }

            if (jsonResponse.startsWith("[")) {
                val jsonArray = JSONArray(jsonResponse)

                for (i in 0 until jsonArray.length()) {
                    val butonObj = jsonArray.getJSONObject(i)
                    val buton = ButonDurum(
                        id = butonObj.getInt("id"),
                        telefon = butonObj.getString("telefon"),
                        ad = butonObj.getString("ad"),
                        soyad = butonObj.getString("soyad"),
                        yer = butonObj.getString("yer"),
                        tarih = butonObj.getString("tarih"),
                        email = butonObj.getString("email")
                    )
                    butonList.add(buton)
                }

                if (butonList.isEmpty()) {
                    showEmptyState()
                } else {
                    adapter.notifyDataSetChanged()
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }

            } else if (jsonResponse.startsWith("{")) {
                val jsonObject = JSONObject(jsonResponse)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    val dataArray = jsonObject.getJSONArray("data")

                    for (i in 0 until dataArray.length()) {
                        val butonObj = dataArray.getJSONObject(i)
                        val buton = ButonDurum(
                            id = butonObj.getInt("id"),
                            telefon = butonObj.getString("telefon"),
                            ad = butonObj.getString("ad"),
                            soyad = butonObj.getString("soyad"),
                            yer = butonObj.getString("yer"),
                            tarih = butonObj.getString("tarih"),
                            email = butonObj.getString("email")
                        )
                        butonList.add(buton)
                    }

                    if (butonList.isEmpty()) {
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

    private fun updateButonDurum(id: Int, telefon: String, ad: String, soyad: String, yer: String, email: String) {
        progressBar.visibility = View.VISIBLE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_update_buton_durum.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("id=$id")
                    append("&telefon=${URLEncoder.encode(telefon, "UTF-8")}")
                    append("&ad=${URLEncoder.encode(ad, "UTF-8")}")
                    append("&soyad=${URLEncoder.encode(soyad, "UTF-8")}")
                    append("&yer=${URLEncoder.encode(yer, "UTF-8")}")
                    append("&email=${URLEncoder.encode(email, "UTF-8")}")
                    append("&api_key=apartman_secret_key_2024")
                }.toString()

                println("🔍 BUTON DURUM GÜNCELLE: $postData")

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
                handleUpdateResult(result)
            }
        }.start()
    }

    private fun handleUpdateResult(result: String) {
        try {
            if (result.isNotEmpty() && result.startsWith("{")) {
                val jsonObject = JSONObject(result)
                val success = jsonObject.getBoolean("success")

                if (success) {
                    Toast.makeText(this, "Buton durumu başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
                    loadButonDurum()
                } else {
                    val message = jsonObject.optString("message", "İşlem başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Sonuç işleme hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteButonDurum(buton: ButonDurum) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Buton Durumu Sil")
            .setMessage("'${buton.ad} ${buton.soyad}' isimli buton durumunu silmek istediğinizden emin misiniz?\n\nBu işlem geri alınamaz!")
            .setPositiveButton("Sil") { _, _ ->
                deleteButonDurumApiCall(buton.id)
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun deleteButonDurumApiCall(id: Int) {
        progressBar.visibility = View.VISIBLE

        Thread {
            var result = ""
            var responseCode = 0

            try {
                val url = URL("http://baser.org/apartman/api/api_delete_buton_durum.php")
                val conn = url.openConnection() as HttpURLConnection

                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.doInput = true
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

                val postData = StringBuilder().apply {
                    append("id=$id")
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
                    Toast.makeText(this, "Buton durumu başarıyla silindi!", Toast.LENGTH_SHORT).show()
                    loadButonDurum()
                } else {
                    val message = jsonObject.optString("message", "Silme işlemi başarısız")
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Silme işlemi hatası", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEmptyState() {
        tvEmptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun formatDateTime(dateTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateTime)
            if (date != null) {
                outputFormat.format(date)
            } else {
                dateTime
            }
        } catch (e: Exception) {
            dateTime
        }
    }
}

// Data Classes
data class ButonDurum(
    val id: Int,
    val telefon: String,
    val ad: String,
    val soyad: String,
    val yer: String,
    val tarih: String,
    val email: String
)

// Adapter
class ButonDurumAdapter(
    private var butonList: List<ButonDurum>,
    private val onItemClick: (ButonDurum) -> Unit,
    private val onEdit: (ButonDurum) -> Unit,
    private val onDelete: (ButonDurum) -> Unit
) : RecyclerView.Adapter<ButonDurumAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardView)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvPhone: TextView = itemView.findViewById(R.id.tvPhone)
        val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        val tvEmail: TextView = itemView.findViewById(R.id.tvEmail)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_buton_durum, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val buton = butonList[position]

        holder.tvName.text = "${buton.ad} ${buton.soyad}"
        holder.tvPhone.text = buton.telefon
        holder.tvLocation.text = buton.yer
        holder.tvEmail.text = buton.email
        holder.tvDate.text = formatDate(buton.tarih)

        holder.cardView.setOnClickListener {
            onItemClick(buton)
        }

        holder.cardView.setOnLongClickListener {
            showOptionsMenu(holder.itemView.context, buton)
            true
        }
    }

    override fun getItemCount() = butonList.size

    fun updateList(newList: List<ButonDurum>) {
        this.butonList = newList
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            if (date != null) {
                outputFormat.format(date)
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    private fun showOptionsMenu(context: Context, buton: ButonDurum) {
        val options = arrayOf("Detay Gör", "Düzenle", "Sil")

        MaterialAlertDialogBuilder(context)
            .setTitle("İşlem Seçin")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onItemClick(buton)
                    1 -> onEdit(buton)
                    2 -> onDelete(buton)
                }
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}