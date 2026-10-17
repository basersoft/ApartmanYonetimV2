package com.baser.apartman



import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MessagesActivity : AppCompatActivity() {

    // UI Components
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvEmptyState: TextView
    private lateinit var btnNewMessage: MaterialButton
    private lateinit var btnDebtReminder: MaterialButton
    private lateinit var btnBulkMessage: MaterialButton
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnFilterAll: MaterialButton
    private lateinit var btnFilterUnread: MaterialButton
    private lateinit var btnFilterSent: MaterialButton
    private lateinit var btnFilterReceived: MaterialButton
    private lateinit var btnFilterAnswered: MaterialButton
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: MaterialButton
    private lateinit var btnClearSearch: MaterialButton

    private lateinit var messageAdapter: MessageAdapter
    private val messages = mutableListOf<Message>()
    private var filterType = "all"
    private var searchQuery = ""
    private var currentUserId = 0
    private var isAdmin = false
    private var userType = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        // Kullanıcı bilgilerini al
        userType = getSavedUserType()
        currentUserId = getCurrentUserId().toIntOrNull() ?: 0
        isAdmin = userType == "admin"

        Log.d("MessagesActivity", "Kullanıcı ID: $currentUserId, Admin: $isAdmin, Tür: $userType")

        initViews()
        setupToolbar()
        setupClickListeners()
        loadMessages()
    }

    private fun getSavedUserType(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_type", "resident") ?: "resident"
    }

    private fun getCurrentUserId(): String {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("user_id", "") ?: ""
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.rvMessages)
        progressBar = findViewById(R.id.progressBar)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        btnNewMessage = findViewById(R.id.btnNewMessage)
        btnDebtReminder = findViewById(R.id.btnDebtReminder)
        btnBulkMessage = findViewById(R.id.btnBulkMessage)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnFilterAll = findViewById(R.id.btnFilterAll)
        btnFilterUnread = findViewById(R.id.btnFilterUnread)
        btnFilterSent = findViewById(R.id.btnFilterSent)
        btnFilterReceived = findViewById(R.id.btnFilterReceived)
        btnFilterAnswered = findViewById(R.id.btnFilterAnswered)
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)

        // Admin kontrollerini göster/gizle
        if (isAdmin) {
            btnDebtReminder.visibility = View.VISIBLE
            btnBulkMessage.visibility = View.VISIBLE
            btnFilterAnswered.visibility = View.VISIBLE
        } else {
            btnDebtReminder.visibility = View.GONE
            btnBulkMessage.visibility = View.GONE
            btnFilterAnswered.visibility = View.GONE
        }

        // Adapter'ı ayarla
        messageAdapter = MessageAdapter(
            messages = messages,
            isAdmin = isAdmin,
            currentUserId = currentUserId,
            onMessageClick = { message ->
                showMessageDetailDialog(message)
            },
            onReplyClick = { message ->
                showReplyDialog(message)
            },
            onDeleteClick = { message ->
                deleteMessage(message)
            },
            onMarkReadClick = { message ->
                markMessageAsRead(message)
            }
        )

        // RecyclerView ayarları
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = messageAdapter

        updateFilterButtons()
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun setupClickListeners() {
        btnNewMessage.setOnClickListener {
            showNewMessageDialog()
        }

        btnDebtReminder.setOnClickListener {
            Toast.makeText(this, "Borç hatırlatma özelliği geliştiriliyor...", Toast.LENGTH_SHORT).show()
        }

        btnBulkMessage.setOnClickListener {
            Toast.makeText(this, "Toplu mesaj özelliği geliştiriliyor...", Toast.LENGTH_SHORT).show()
        }

        btnRefresh.setOnClickListener {
            refreshMessages()
        }

        btnFilterAll.setOnClickListener { setFilter("all") }
        btnFilterUnread.setOnClickListener { setFilter("unread") }
        btnFilterSent.setOnClickListener { setFilter("sent") }
        btnFilterReceived.setOnClickListener { setFilter("received") }
        btnFilterAnswered.setOnClickListener { setFilter("answered") }

        btnSearch.setOnClickListener {
            searchQuery = etSearch.text.toString().trim()
            filterAndSearchMessages()
        }

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            searchQuery = ""
            filterAndSearchMessages()
        }

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                searchQuery = etSearch.text.toString().trim()
                filterAndSearchMessages()
                true
            } else {
                false
            }
        }
    }

    private fun setFilter(type: String) {
        filterType = type
        updateFilterButtons()
        filterAndSearchMessages()
    }

    private fun updateFilterButtons() {
        val filterButtons = mapOf(
            "all" to btnFilterAll,
            "unread" to btnFilterUnread,
            "sent" to btnFilterSent,
            "received" to btnFilterReceived,
            "answered" to btnFilterAnswered
        )

        filterButtons.forEach { (filterName, button) ->
            if (button?.visibility == View.VISIBLE) {
                if (filterType == filterName) {
                    // Aktif buton
                    button.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
                    button.setTextColor(getColor(android.R.color.white))
                    button.strokeWidth = 0
                } else {
                    // Pasif buton
                    button.setBackgroundColor(getColor(android.R.color.transparent))
                    button.setTextColor(getColor(android.R.color.holo_blue_dark))
                    button.strokeWidth = 1
                }
            }
        }
    }

    private fun loadMessages() {
        Log.d("MessagesActivity", "Mesajlar yükleniyor...")
        progressBar.visibility = View.VISIBLE
        tvEmptyState.visibility = View.GONE

        ApiManager.getMessages(
            context = this,
            userId = currentUserId,
            siteId = 1,
            onSuccess = { csvData ->
                Log.d("MessagesActivity", "API'den CSV verisi alındı: ${csvData.length} karakter")
                parseMessagesFromCSV(csvData)
                progressBar.visibility = View.GONE
            },
            onError = { error ->
                Log.e("MessagesActivity", "API error: $error")
                progressBar.visibility = View.GONE
                tvEmptyState.visibility = View.VISIBLE
                tvEmptyState.text = "Mesajlar yüklenemedi"
                Toast.makeText(this, "Hata: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun parseMessagesFromCSV(csvData: String) {
        Log.d("MessagesActivity", "CSV verisi işleniyor...")

        // TEMP: Mesajları elle oluşturarak test et
        createTestMessages()
        filterAndSearchMessages()
        return

        // Aşağıdaki kodu daha sonra aktif edin
        try {
            if (csvData.isNotEmpty()) {
                val lines = csvData.trim().split("\n")
                Log.d("MessagesActivity", "CSV satır sayısı: ${lines.size}")

                messages.clear() // Listeyi temizle

                if (lines.size > 1) {
                    for (i in 1 until lines.size) {
                        val line = lines[i].trim()
                        if (line.isNotEmpty()) {
                            Log.d("MessagesActivity", "Satır $i: $line")
                            val fields = parseCSVLine(line)
                            Log.d("MessagesActivity", "Alan sayısı: ${fields.size}")

                            if (fields.size >= 16) {
                                val isOwnMessage = fields[12].toBooleanStrictOrNull() ?: false
                                val isReply = fields[13].toBooleanStrictOrNull() ?: false
                                val isReminder = fields[14].toBooleanStrictOrNull() ?: false
                                val canReply = fields[15].toBooleanStrictOrNull() ?: false

                                val message = Message(
                                    id = fields[0].toIntOrNull() ?: 0,
                                    siteId = fields[1].toIntOrNull() ?: 1,
                                    senderId = fields[2].toIntOrNull() ?: 0,
                                    receiverId = fields[3].toIntOrNull() ?: 0,
                                    senderName = fields[10],
                                    receiverName = fields[11],
                                    subject = fields[4],
                                    message = fields[5],
                                    status = fields[6],
                                    parentId = fields[7].takeIf { it.isNotBlank() }?.toIntOrNull(),
                                    createdAt = fields[8],
                                    updatedAt = fields[9],
                                    isOwnMessage = isOwnMessage,
                                    isReply = isReply,
                                    isReminder = isReminder,
                                    canReply = canReply
                                )

                                Log.d("MessagesActivity", "Mesaj oluşturuldu: ID=${message.id}, Konu='${message.subject}', Gönderen='${message.senderName}' -> '${message.receiverName}'")
                                messages.add(message)
                            } else {
                                Log.w("MessagesActivity", "Satır $i yeterli alana sahip değil: ${fields.size} alan")
                            }
                        }
                    }
                }
            }

            Log.d("MessagesActivity", "Toplam ${messages.size} mesaj işlendi")

            // Adapter'ı güncelle
            runOnUiThread {
                filterAndSearchMessages()
            }

        } catch (e: Exception) {
            Log.e("MessagesActivity", "Parse error: ${e.message}", e)
            Toast.makeText(this, "Mesajlar işlenirken hata oluştu", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createTestMessages() {
        messages.clear()

        // Test mesajları oluştur
        messages.addAll(listOf(
            Message(
                id = 1,
                siteId = 1,
                senderId = 3,
                receiverId = 1,
                senderName = "Ahmet Candan",
                receiverName = "Site Yöneticisi",
                subject = "Selam Hatırlatma",
                message = "Selam Nasılsınız",
                status = "unread",
                parentId = null,
                createdAt = "2026-01-03 17:22:14",
                updatedAt = "2026-01-03 17:22:14",
                isOwnMessage = false,
                isReply = false,
                isReminder = false,
                canReply = true
            ),
            Message(
                id = 2,
                siteId = 1,
                senderId = 1,
                receiverId = 5,
                senderName = "Site Yöneticisi",
                receiverName = "Ayşe Demir",
                subject = "Deneme",
                message = "sdfsdfsdfsd",
                status = "unread",
                parentId = null,
                createdAt = "2026-01-03 17:20:57",
                updatedAt = "2026-01-03 17:20:57",
                isOwnMessage = true,
                isReply = false,
                isReminder = false,
                canReply = false
            ),
            Message(
                id = 3,
                siteId = 1,
                senderId = 1,
                receiverId = 3,
                senderName = "Site Yöneticisi",
                receiverName = "Ahmet Candan",
                subject = "Deneme",
                message = "sgsgsdfgs",
                status = "unread",
                parentId = null,
                createdAt = "2026-01-03 17:14:37",
                updatedAt = "2026-01-03 17:14:37",
                isOwnMessage = true,
                isReply = false,
                isReminder = false,
                canReply = false
            )
        ))

        Log.d("MessagesActivity", "${messages.size} test mesajı oluşturuldu")
    }

    private fun filterAndSearchMessages() {
        Log.d("MessagesActivity", "filterAndSearchMessages çağrıldı, Toplam mesaj: ${messages.size}")

        val filtered = messages.filter { message ->
            // Filtreleme
            val passesFilter = when (filterType) {
                "unread" -> message.status == "unread" && !message.isOwnMessage
                "sent" -> message.isOwnMessage
                "received" -> !message.isOwnMessage
                "answered" -> message.status == "answered"
                else -> true // "all"
            }

            // Arama
            val passesSearch = if (searchQuery.isEmpty()) true
            else {
                message.subject.contains(searchQuery, true) ||
                        message.message.contains(searchQuery, true) ||
                        message.senderName.contains(searchQuery, true) ||
                        message.receiverName.contains(searchQuery, true)
            }

            passesFilter && passesSearch
        }

        Log.d("MessagesActivity", "Filtrelenmiş mesaj sayısı: ${filtered.size}")

        // Adapter'ı güncelle
        messageAdapter.updateMessages(filtered)

        // Boş durumu kontrol et
        if (filtered.isEmpty()) {
            tvEmptyState.visibility = View.VISIBLE
            tvEmptyState.text = if (searchQuery.isNotEmpty()) {
                "'$searchQuery' ile ilgili mesaj bulunamadı"
            } else {
                "Henüz mesaj bulunmuyor"
            }
            Log.d("MessagesActivity", "Boş durum gösterildi")
        } else {
            tvEmptyState.visibility = View.GONE
            Log.d("MessagesActivity", "Boş durum gizlendi, ${filtered.size} mesaj gösteriliyor")
        }
    }

    private fun showMessageDetailDialog(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Mesaj Detayı")
            .setMessage("""
                📩 **Mesaj Detayı**
                
                **Gönderen:** ${message.senderName}
                **Alıcı:** ${message.receiverName}
                **Tarih:** ${formatDate(message.createdAt)}
                **Durum:** ${message.status}
                **Konu:** ${message.subject}
                
                **Mesaj:**
                ${message.message}
            """.trimIndent())
            .setPositiveButton("Tamam", null)
            .setNeutralButton("Cevapla") { _, _ ->
                showReplyDialog(message)
            }
            .create()
            .show()
    }

    private fun showReplyDialog(message: Message) {
        val editText = EditText(this)
        editText.hint = "Cevabınızı buraya yazın..."
        editText.minLines = 4
        editText.setPadding(16, 16, 16, 16)

        AlertDialog.Builder(this)
            .setTitle("Mesajı Cevapla")
            .setMessage("${message.senderName} için cevap yazın")
            .setView(editText)
            .setPositiveButton("Gönder") { _, _ ->
                val reply = editText.text.toString().trim()
                if (reply.isNotEmpty()) {
                    sendReply(message.id, "Re: ${message.subject}", reply, message.senderId)
                } else {
                    Toast.makeText(this, "Lütfen bir mesaj yazın", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("İptal", null)
            .show()
            .apply {
                // Her iki butonu da turkuaz yap
                val positiveButton = getButton(AlertDialog.BUTTON_POSITIVE)
                val negativeButton = getButton(AlertDialog.BUTTON_NEGATIVE)

                positiveButton.setBackgroundColor(Color.parseColor("#00BCD4"))
                positiveButton.setTextColor(Color.WHITE)

                negativeButton.setBackgroundColor(Color.parseColor("#FFC0CB"))
                negativeButton.setTextColor(Color.parseColor("#666666"))
            }
    }

    private fun showNewMessageDialog() {
        val editText = EditText(this)
        editText.hint = "Kime göndermek istiyorsunuz? (Email)"
        editText.setPadding(16, 16, 16, 16)

        AlertDialog.Builder(this)
            .setTitle("Yeni Mesaj")
            .setMessage("Bu özellik yakında eklenecek. Şimdilik sadece mevcut mesajlara cevap verebilirsiniz.")
            .setView(editText)
            .setPositiveButton("Tamam", null)
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun sendReply(messageId: Int, subject: String, message: String, receiverId: Int) {
        progressBar.visibility = View.VISIBLE

        ApiManager.sendReply(
            context = this,
            originalMessageId = messageId,
            senderId = currentUserId,
            subject = subject,
            message = message,
            receiverId = receiverId,
            siteId = 1,
            onSuccess = {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "✓ Cevabınız gönderildi", Toast.LENGTH_SHORT).show()
                refreshMessages()
            },
            onError = { error ->
                progressBar.visibility = View.GONE
                Toast.makeText(this, "✗ Cevap gönderilemedi: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun markMessageAsRead(message: Message) {
        if (isAdmin && message.status == "unread" && !message.isOwnMessage) {
            ApiManager.markMessageAsRead(
                context = this,
                messageId = message.id,
                userId = currentUserId,
                siteId = 1,
                onSuccess = {
                    Toast.makeText(this, "✓ Mesaj okundu olarak işaretlendi", Toast.LENGTH_SHORT).show()
                    refreshMessages()
                },
                onError = { error ->
                    Toast.makeText(this, "✗ İşlem başarısız: $error", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun deleteMessage(message: Message) {
        AlertDialog.Builder(this)
            .setTitle("Mesajı Sil")
            .setMessage("Bu mesajı silmek istediğinizden emin misiniz?\n\nKonu: ${message.subject}")
            .setPositiveButton("Sil") { _, _ ->
                ApiManager.deleteMessage(
                    context = this,
                    messageId = message.id,
                    userId = currentUserId,
                    isAdmin = isAdmin,
                    siteId = 1,
                    onSuccess = {
                        Toast.makeText(this, "✓ Mesaj silindi", Toast.LENGTH_SHORT).show()
                        refreshMessages()
                    },
                    onError = { error ->
                        Toast.makeText(this, "✗ Mesaj silinemedi: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun refreshMessages() {
        loadMessages()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = ArrayList<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (i in line.indices) {
            when {
                line[i] == '"' -> inQuotes = !inQuotes
                line[i] == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current = StringBuilder()
                }
                else -> current.append(line[i])
            }
        }
        result.add(current.toString())
        return result
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}