package com.baser.apartman

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.min

class AidatListesiFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var adapter: AdminAidatAdapter
    private lateinit var etSearch: EditText
    private lateinit var spinnerStatusFilter: Spinner
    private lateinit var spinnerGroupBy: Spinner
    private lateinit var btnClearFilters: Button
    private lateinit var layoutPagination: LinearLayout
    private lateinit var btnPrevPage: Button
    private lateinit var btnNextPage: Button
    private lateinit var tvPageInfo: TextView
    private lateinit var cardStats: androidx.cardview.widget.CardView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvPaidAmount: TextView
    private lateinit var tvPendingAmount: TextView
    private lateinit var tvLateFeeTotal: TextView

    private var allAidatList: List<Aidat> = emptyList()
    private var filteredAidatList: List<Aidat> = emptyList()
    private var currentPage = 1
    private val pageSize = 15

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_aidat_listesi, container, false)
        setupViews(view)
        setupFilters()
        setupPagination()
        loadAdminAidatData()
        return view
    }

    private fun setupViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerViewAdminAidat)
        progressBar = view.findViewById(R.id.progressBar)
        etSearch = view.findViewById(R.id.etSearch)
        spinnerStatusFilter = view.findViewById(R.id.spinnerStatusFilter)
        spinnerGroupBy = view.findViewById(R.id.spinnerGroupBy)
        btnClearFilters = view.findViewById(R.id.btnClearFilters)
        layoutPagination = view.findViewById(R.id.layoutPagination)
        btnPrevPage = view.findViewById(R.id.btnPrevPage)
        btnNextPage = view.findViewById(R.id.btnNextPage)
        tvPageInfo = view.findViewById(R.id.tvPageInfo)
        cardStats = view.findViewById(R.id.cardStats)
        tvTotalAmount = view.findViewById(R.id.tvTotalAmount)
        tvPaidAmount = view.findViewById(R.id.tvPaidAmount)
        tvPendingAmount = view.findViewById(R.id.tvPendingAmount)
        tvLateFeeTotal = view.findViewById(R.id.tvLateFeeTotal)

        // Adapter'ı click listener ile oluştur
        adapter = AdminAidatAdapter(onItemClick = { aidat ->
            onAidatItemClick(aidat)
        })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupFilters() {
        // Durum filtreleme
        val statusOptions = arrayOf("Tümü", "Ödendi", "Bekliyor", "Gecikmiş")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatusFilter.adapter = statusAdapter

        // Gruplama seçenekleri
        val groupOptions = arrayOf("Gruplama Yok", "Kullanıcıya Göre", "Duruma Göre")
        val groupAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, groupOptions)
        groupAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGroupBy.adapter = groupAdapter

        // Arama dinleyicisi
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }
        })

        // Filtre dinleyicileri
        spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerGroupBy.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnClearFilters.setOnClickListener {
            etSearch.text.clear()
            spinnerStatusFilter.setSelection(0)
            spinnerGroupBy.setSelection(0)
            applyFilters()
        }
    }

    private fun setupPagination() {
        btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                updatePagedData()
            }
        }

        btnNextPage.setOnClickListener {
            val totalPages = getTotalPages()
            if (currentPage < totalPages) {
                currentPage++
                updatePagedData()
            }
        }
    }

    private fun applyFilters() {
        val searchText = etSearch.text.toString().lowercase()
        val selectedStatus = spinnerStatusFilter.selectedItem.toString()
        val groupBy = spinnerGroupBy.selectedItem.toString()

        filteredAidatList = allAidatList.filter { aidat ->
            val matchesSearch = searchText.isEmpty() ||
                    aidat.userName.lowercase().contains(searchText) ||
                    aidat.kullanici_email.lowercase().contains(searchText) ||
                    "${aidat.apartmentBlock}${aidat.apartmentNumber}".lowercase().contains(searchText) ||
                    aidat.description.lowercase().contains(searchText)

            val matchesStatus = when (selectedStatus) {
                "Tümü" -> true
                "Ödendi" -> aidat.durum.lowercase() == "paid"
                "Bekliyor" -> aidat.durum.lowercase() == "pending"
                "Gecikmiş" -> aidat.durum.lowercase() == "overdue"
                else -> true
            }

            matchesSearch && matchesStatus
        }

        // Gruplama uygula
        when (groupBy) {
            "Kullanıcıya Göre" -> {
                val grouped = filteredAidatList.groupBy { it.userId }
                val sortedList = mutableListOf<Aidat>()
                grouped.values.forEach { userAidat ->
                    sortedList.addAll(userAidat.sortedBy { it.son_tarih })
                }
                filteredAidatList = sortedList
            }
            "Duruma Göre" -> {
                filteredAidatList = filteredAidatList.sortedBy { it.durum }
            }
        }

        currentPage = 1
        updatePagedData()
        updatePaginationVisibility()

        // Seçimi temizle
        adapter.clearSelection()
    }

    private fun updatePagedData() {
        val startIndex = (currentPage - 1) * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredAidatList.size)

        val pagedList = if (filteredAidatList.isNotEmpty()) {
            filteredAidatList.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        adapter.setAidatList(pagedList)
        updatePaginationButtons()
    }

    private fun updatePaginationButtons() {
        val totalPages = getTotalPages()

        btnPrevPage.isEnabled = currentPage > 1
        btnNextPage.isEnabled = currentPage < totalPages

        tvPageInfo.text = "Sayfa $currentPage/$totalPages (Toplam: ${filteredAidatList.size})"
    }

    private fun getTotalPages(): Int {
        return ceil(filteredAidatList.size.toDouble() / pageSize).toInt()
    }

    private fun updatePaginationVisibility() {
        layoutPagination.visibility = if (filteredAidatList.size > pageSize) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun loadAdminAidatData() {
        progressBar.visibility = View.VISIBLE

        // Activity'den user bilgilerini al
        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        println("🔍 AidatListesiFragment - loadAdminAidatData")
        println("🔍 User Email from activity: $userEmail")
        println("🔍 User Type from activity: $userType")

        if (userEmail.isEmpty() || userType.isEmpty()) {
            Toast.makeText(requireContext(), "Kullanıcı bilgileri yüklenemedi. Lütfen tekrar giriş yapın.", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            return
        }

        AidatApiService.getAndroidDues(
            userEmail = userEmail,
            userType = userType,
            onSuccess = { data ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    handleAndroidAidatData(data)
                }
            },
            onError = { error ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Veri yükleme hatası: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun handleAndroidAidatData(data: JSONObject) {
        try {
            val duesArray = data.getJSONArray("dues")
            val aidatList = mutableListOf<Aidat>()

            var totalAmount = 0.0
            var paidAmount = 0.0
            var pendingAmount = 0.0
            var lateFeeTotal = 0.0

            for (i in 0 until duesArray.length()) {
                val item = duesArray.getJSONObject(i)

                val amount = item.getDouble("amount")
                val lateFee = item.getDouble("late_fee_amount")
                val status = item.getString("status")

                totalAmount += amount + lateFee
                lateFeeTotal += lateFee

                when (status) {
                    "paid" -> paidAmount += amount
                    "pending", "overdue" -> pendingAmount += amount
                }

                val aidat = Aidat(
                    ay = item.optString("description", ""),
                    miktar = "₺${"%.2f".format(amount)}",
                    durum = status,
                    durumRenk = when (status.lowercase()) {
                        "paid" -> "#2ecc71"
                        "pending" -> "#f39c12"
                        "overdue" -> "#e74c3c"
                        else -> "#95a5a6"
                    },
                    son_tarih = item.getString("due_date"),
                    odeme_tarihi = if (item.has("payment_date") && !item.isNull("payment_date"))
                        item.getString("payment_date") else null,
                    kullanici_adi = item.getString("user_name"),
                    kullanici_email = item.getString("user_email"),
                    id = item.getInt("id"),
                    userId = item.getInt("user_id"),
                    userName = item.getString("user_name"),
                    apartmentBlock = item.getString("apartment_block"),
                    apartmentNumber = item.getString("apartment_number"),
                    description = item.optString("description", ""),
                    lateFeeAmount = lateFee,
                    amount = amount
                )
                aidatList.add(aidat)
            }

            allAidatList = aidatList
            filteredAidatList = aidatList

            updateStatistics(totalAmount, paidAmount, pendingAmount, lateFeeTotal)
            applyFilters()

            Toast.makeText(requireContext(), "${aidatList.size} aidat kaydı yüklendi", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Veri işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatistics(total: Double, paid: Double, pending: Double, lateFee: Double) {
        tvTotalAmount.text = "₺${"%.2f".format(total)}"
        tvPaidAmount.text = "₺${"%.2f".format(paid)}"
        tvPendingAmount.text = "₺${"%.2f".format(pending)}"
        tvLateFeeTotal.text = "₺${"%.2f".format(lateFee)}"

        cardStats.visibility = View.VISIBLE
    }

    // Aidat item'ına tıklandığında
    private fun onAidatItemClick(aidat: Aidat) {
        showAidatDetailDialog(aidat)
    }

    // Detay dialog'u göster
    private fun showAidatDetailDialog(aidat: Aidat) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val totalAmount = aidat.amount + aidat.lateFeeAmount

        val message = """
            👤 Sakin: ${aidat.userName}
            📧 Email: ${aidat.kullanici_email}
            🏠 Daire: ${aidat.apartmentBlock} - ${aidat.apartmentNumber}
            
            💰 Orijinal Tutar: ${numberFormat.format(aidat.amount)}
            ⚡ Gecikme Zammı: ${numberFormat.format(aidat.lateFeeAmount)}
            💵 Toplam Tutar: ${numberFormat.format(totalAmount)}
            
            📅 Son Ödeme: ${formatDate(aidat.son_tarih)}
            ✅ Durum: ${getStatusText(aidat.durum)}
            
            ${if (!aidat.odeme_tarihi.isNullOrEmpty()) "🗓️ Ödeme Tarihi: ${formatDate(aidat.odeme_tarihi)}" else ""}
            
            📝 Açıklama: ${aidat.description}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Aidat Detayları")
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Ödeme Yap") { dialog, _ ->
                showPaymentDialog(aidat)
                dialog.dismiss()
            }
            .show()
    }

    // PROFESYONEL ÖDEME DİALOG'U
    private fun showPaymentDialog(aidat: Aidat) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val totalAmount = aidat.amount + aidat.lateFeeAmount

        // Dialog layout'u oluştur
        val layout = LinearLayout(requireContext())
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 30, 50, 30)

        // Borç bilgileri
        val tvDebtInfo = TextView(requireContext())
        tvDebtInfo.text = "📋 ${aidat.userName} - ${aidat.description}"
        tvDebtInfo.textSize = 16f
        tvDebtInfo.setPadding(0, 0, 0, 20)
        layout.addView(tvDebtInfo)

        // Tutar bilgileri
        val layoutAmounts = LinearLayout(requireContext())
        layoutAmounts.orientation = LinearLayout.VERTICAL

        val tvOriginal = TextView(requireContext())
        tvOriginal.text = "💰 Orijinal Tutar: ${numberFormat.format(aidat.amount)}"
        tvOriginal.textSize = 14f
        layoutAmounts.addView(tvOriginal)

        val tvLateFee = TextView(requireContext())
        tvLateFee.text = "⚡ Gecikme Zammı: ${numberFormat.format(aidat.lateFeeAmount)}"
        tvLateFee.textSize = 14f
        layoutAmounts.addView(tvLateFee)

        val tvTotal = TextView(requireContext())
        tvTotal.text = "💵 Toplam Borç: ${numberFormat.format(totalAmount)}"
        tvTotal.textSize = 16f
        tvTotal.setTypeface(tvTotal.typeface, android.graphics.Typeface.BOLD)
        tvTotal.setPadding(0, 10, 0, 20)
        layoutAmounts.addView(tvTotal)

        layout.addView(layoutAmounts)

        // Tam ödeme switch
        val switchFullPayment = Switch(requireContext())
        switchFullPayment.text = "Tam Ödeme"
        switchFullPayment.isChecked = true
        switchFullPayment.setPadding(0, 0, 0, 20)
        layout.addView(switchFullPayment)

        // Ödeme tutarı
        val tvPaymentAmountLabel = TextView(requireContext())
        tvPaymentAmountLabel.text = "Ödeme Tutarı (₺)"
        tvPaymentAmountLabel.textSize = 14f
        tvPaymentAmountLabel.setPadding(0, 0, 0, 5)
        layout.addView(tvPaymentAmountLabel)

        val etPaymentAmount = EditText(requireContext())
        etPaymentAmount.setText(totalAmount.toString())
        etPaymentAmount.isEnabled = false
        etPaymentAmount.inputType = android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        etPaymentAmount.setBackgroundResource(R.drawable.edittext_background)
        etPaymentAmount.setPadding(40, 20, 40, 20)
        layout.addView(etPaymentAmount)

        // Switch listener
        switchFullPayment.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                etPaymentAmount.setText(totalAmount.toString())
                etPaymentAmount.isEnabled = false
            } else {
                etPaymentAmount.setText("")
                etPaymentAmount.isEnabled = true
                etPaymentAmount.requestFocus()
            }
        }

        // Ödeme yöntemi
        val tvPaymentMethodLabel = TextView(requireContext())
        tvPaymentMethodLabel.text = "Ödeme Yöntemi"
        tvPaymentMethodLabel.textSize = 14f
        tvPaymentMethodLabel.setPadding(0, 20, 0, 5)
        layout.addView(tvPaymentMethodLabel)

        val spinnerPaymentMethod = Spinner(requireContext())
        val paymentMethods = arrayOf("Nakit", "Banka Havalesi", "Kredi Kartı", "Çek", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, paymentMethods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaymentMethod.adapter = adapter
        layout.addView(spinnerPaymentMethod)

        // Notlar
        val tvNotesLabel = TextView(requireContext())
        tvNotesLabel.text = "Notlar (Opsiyonel)"
        tvNotesLabel.textSize = 14f
        tvNotesLabel.setPadding(0, 20, 0, 5)
        layout.addView(tvNotesLabel)

        val etPaymentNotes = EditText(requireContext())
        etPaymentNotes.hint = "Ödeme notu ekleyin..."
        etPaymentNotes.setBackgroundResource(R.drawable.edittext_background)
        etPaymentNotes.setPadding(40, 20, 40, 20)
        layout.addView(etPaymentNotes)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Ödeme İşlemi")
            .setView(layout)
            .setPositiveButton("Ödemeyi Tamamla") { dialogInterface, _ ->
                val paymentAmount = etPaymentAmount.text.toString().toDoubleOrNull()
                val paymentMethod = spinnerPaymentMethod.selectedItem.toString()
                val notes = etPaymentNotes.text.toString().trim()

                if (paymentAmount == null || paymentAmount <= 0) {
                    Toast.makeText(requireContext(), "Lütfen geçerli bir ödeme tutarı girin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (paymentAmount > totalAmount) {
                    Toast.makeText(requireContext(), "Ödeme tutarı toplam borçtan fazla olamaz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (paymentAmount < totalAmount) {
                    // Kısmi ödeme onayı
                    showPartialPaymentConfirmation(aidat, paymentAmount, paymentMethod, notes, dialogInterface)
                } else {
                    // Tam ödeme onayı
                    showFullPaymentConfirmation(aidat, paymentAmount, paymentMethod, notes, dialogInterface)
                }
            }
            .setNegativeButton("İptal") { dialogInterface, _ ->
                dialogInterface.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showPartialPaymentConfirmation(aidat: Aidat, amount: Double, method: String, notes: String, dialog: android.content.DialogInterface) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val totalAmount = aidat.amount + aidat.lateFeeAmount
        val remaining = totalAmount - amount

        val message = """
            ⚠️ **KISMI ÖDEME**
            
            👤 Sakin: ${aidat.userName}
            💰 Toplam Borç: ${numberFormat.format(totalAmount)}
            💵 Ödeme Tutarı: ${numberFormat.format(amount)}
            📉 Kalan Borç: ${numberFormat.format(remaining)}
            
            **Kısmi ödeme yapmak istiyor musunuz?**
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Kısmi Ödeme Onayı")
            .setMessage(message)
            .setPositiveButton("Evet, Kısmi Ödeme Yap") { innerDialog, _ ->
                innerDialog.dismiss()
                dialog.dismiss()
                processPayment(aidat, amount, method, notes)
            }
            .setNegativeButton("İptal") { innerDialog, _ ->
                innerDialog.dismiss()
            }
            .setNeutralButton("Tam Ödeme Yap") { innerDialog, _ ->
                innerDialog.dismiss()
                // Dialog'u kapat ve tam ödeme yap
                dialog.dismiss()
                showPaymentDialog(aidat) // Yeniden aç ve tam ödeme yap
            }
            .show()
    }

    private fun showFullPaymentConfirmation(aidat: Aidat, amount: Double, method: String, notes: String, dialog: android.content.DialogInterface) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        val message = """
            ✅ **TAM ÖDEME**
            
            👤 Sakin: ${aidat.userName}
            💰 Ödeme Tutarı: ${numberFormat.format(amount)}
            💳 Ödeme Yöntemi: $method
            
            **Tam ödemeyi onaylıyor musunuz?**
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Tam Ödeme Onayı")
            .setMessage(message)
            .setPositiveButton("Evet, Ödemeyi Tamamla") { innerDialog, _ ->
                innerDialog.dismiss()
                dialog.dismiss()
                processPayment(aidat, amount, method, notes)
            }
            .setNegativeButton("İptal") { innerDialog, _ ->
                innerDialog.dismiss()
            }
            .show()
    }

    // Ödeme işlemini gerçekleştir
    private fun processPayment(aidat: Aidat, paymentAmount: Double, paymentMethod: String, notes: String) {
        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        println("🔍 =========== PROCESS PAYMENT DEBUG ===========")
        println("🔍 Fragment Activity: ${activity.javaClass.simpleName}")
        println("🔍 User Email from activity: $userEmail")
        println("🔍 User Type from activity: $userType")
        println("🔍 Aidat ID: ${aidat.id}")
        println("🔍 Payment Amount: $paymentAmount")
        println("🔍 Payment Method: $paymentMethod")
        println("🔍 Notes: $notes")
        println("🔍 ===========================================")

        if (userEmail.isEmpty() || userType.isEmpty()) {
            Toast.makeText(requireContext(),
                "Kullanıcı bilgileri eksik!\nEmail: $userEmail\nType: $userType\nLütfen tekrar giriş yapın.",
                Toast.LENGTH_LONG).show()
            return
        }

        // Progress göster
        val progressDialog = AlertDialog.Builder(requireContext())
            .setMessage("Ödeme işleniyor...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        AidatApiService.payDue(
            dueId = aidat.id,
            paymentAmount = paymentAmount,
            paymentMethod = paymentMethod,
            notes = notes,
            userEmail = userEmail,
            userType = userType,
            onSuccess = { message ->
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()

                    // Başarı mesajı göster
                    showPaymentSuccessDialog(aidat, paymentAmount, paymentMethod)

                    // Listeyi yenile
                    loadAdminAidatData()

                    // Seçimi temizle
                    adapter.clearSelection()
                }
            },
            onError = { error ->
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()

                    // DEBUG: Hata detayı
                    println("❌ processPayment - Error: $error")

                    // Hata mesajı göster
                    showPaymentErrorDialog(error, aidat)
                }
            }
        )
    }

    private fun showPaymentSuccessDialog(aidat: Aidat, amount: Double, method: String) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val currentDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

        val message = """
            ✅ **ÖDEME BAŞARILI**
            
            👤 Sakin: ${aidat.userName}
            💰 Ödenen Tutar: ${numberFormat.format(amount)}
            💳 Ödeme Yöntemi: $method
            📅 Tarih: $currentDate
            
            Fiş No: ${System.currentTimeMillis().toString().takeLast(8)}
            
            Aidat durumu güncellendi.
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Ödeme Tamamlandı")
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Fiş Yazdır") { dialog, _ ->
                Toast.makeText(requireContext(), "Fiş yazdırma işlemi başlatıldı", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showPaymentErrorDialog(error: String, aidat: Aidat) {
        val message = """
            ❌ **ÖDEME HATASI**
            
            Hata: $error
            
            Lütfen:
            1. İnternet bağlantınızı kontrol edin
            2. Bilgilerin doğruluğunu kontrol edin
            3. Daha sonra tekrar deneyin
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Ödeme Hatası")
            .setMessage(message)
            .setPositiveButton("Tekrar Dene") { dialog, _ ->
                dialog.dismiss()
                showPaymentDialog(aidat)
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Yardımcı fonksiyonlar
    private fun formatDate(dateString: String): String {
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                "${parts[2]}.${parts[1]}.${parts[0]}"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getStatusText(status: String): String {
        return when (status.lowercase()) {
            "paid" -> "Ödendi"
            "pending" -> "Bekliyor"
            "overdue" -> "Gecikmiş"
            else -> status
        }
    }
}