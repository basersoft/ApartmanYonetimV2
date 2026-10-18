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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.cardview.widget.CardView

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
    private lateinit var cardStats: CardView
    private lateinit var tvTotalAmount: TextView
    private lateinit var tvPaidAmount: TextView
    private lateinit var tvPendingAmount: TextView
    private lateinit var tvLateFeeTotal: TextView
    private lateinit var tvPartialCount: TextView

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
        tvPartialCount = view.findViewById(R.id.tvPartialCount)

        adapter = AdminAidatAdapter(
            onItemClick = { aidat ->
                onAidatItemClick(aidat)
            },
            onPaymentHistoryClick = { aidat ->
                showPaymentHistoryDialog(aidat)
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun setupFilters() {
        // Durum filtreleme
        val statusOptions = arrayOf("Tümü", "Ödendi", "Bekliyor", "Gecikmiş", "Kısmi Ödenmiş")
        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatusFilter.adapter = statusAdapter

        // Gruplama seçenekleri
        val groupOptions = arrayOf("Gruplama Yok", "Kullanıcıya Göre", "Duruma Göre", "Ödeme Durumuna Göre")
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
                    (aidat.apartmentBlock + aidat.apartmentNumber).lowercase().contains(searchText) ||
                    aidat.description.lowercase().contains(searchText) ||
                    aidat.receiptNumber.lowercase().contains(searchText)

            val matchesStatus = when (selectedStatus) {
                "Tümü" -> true
                "Ödendi" -> aidat.isFullyPaid
                "Bekliyor" -> aidat.isPending
                "Gecikmiş" -> aidat.isReallyOverdue  // DEĞİŞTİRİLDİ: isOverdue yerine isReallyOverdue
                "Kısmi Ödenmiş" -> aidat.isPartiallyPaid
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
            "Ödeme Durumuna Göre" -> {
                filteredAidatList = filteredAidatList.sortedWith(compareBy(
                    { !it.isFullyPaid },
                    { !it.isPartiallyPaid },
                    { it.durum }
                ))
            }
        }

        currentPage = 1
        updatePagedData()
        updatePaginationVisibility()
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

        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        if (userEmail.isEmpty() || userType.isEmpty()) {
            Toast.makeText(requireContext(), "Kullanıcı bilgileri yüklenemedi. Lütfen tekrar giriş yapın.", Toast.LENGTH_LONG).show()
            progressBar.visibility = View.GONE
            return
        }

        AidatApiService.getAndroidDues(
            userEmail = userEmail,
            userType = userType,
            onSuccess = { aidatList: List<Aidat> ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    handleAndroidAidatData(aidatList)
                }
            },
            onError = { error: String ->
                requireActivity().runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Veri yükleme hatası: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun handleAndroidAidatData(aidatList: List<Aidat>) {
        try {
            allAidatList = aidatList
            filteredAidatList = aidatList
            updateStatistics(aidatList)
            applyFilters()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Veri işleme hatası: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun updateStatistics(aidatList: List<Aidat>) {
        var totalAmount = 0.0
        var paidAmount = 0.0
        var pendingAmount = 0.0
        var lateFeeTotal = 0.0
        var partialCount = 0

        for (aidat in aidatList) {
            totalAmount += aidat.calculatedTotalAmount
            paidAmount += aidat.paidAmount
            pendingAmount += aidat.calculatedRemainingAmount
            lateFeeTotal += aidat.lateFeeAmount

            if (aidat.isPartiallyPaid) {
                partialCount++
            }
        }

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        tvTotalAmount.text = numberFormat.format(totalAmount)
        tvPaidAmount.text = numberFormat.format(paidAmount)
        tvPendingAmount.text = numberFormat.format(pendingAmount)
        tvLateFeeTotal.text = numberFormat.format(lateFeeTotal)
        tvPartialCount.text = "$partialCount kısmi ödeme"

        cardStats.visibility = View.VISIBLE
    }

    // Aidat item'ına tıklandığında
    private fun onAidatItemClick(aidat: Aidat) {
        showAidatDetailDialog(aidat)
    }

    // GÜNCELLENMİŞ: Detay dialog'u
    private fun showAidatDetailDialog(aidat: Aidat) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_aidat_details, null)

        // Bilgileri doldur
        val tvDetailUserName = dialogView.findViewById<TextView>(R.id.tvDetailUserName)
        val tvDetailUserEmail = dialogView.findViewById<TextView>(R.id.tvDetailUserEmail)
        val tvDetailApartment = dialogView.findViewById<TextView>(R.id.tvDetailApartment)
        val tvDetailDescription = dialogView.findViewById<TextView>(R.id.tvDetailDescription)
        val tvDetailAmount = dialogView.findViewById<TextView>(R.id.tvDetailAmount)
        val tvDetailLateFee = dialogView.findViewById<TextView>(R.id.tvDetailLateFee)
        val tvDetailTotalAmount = dialogView.findViewById<TextView>(R.id.tvDetailTotalAmount)
        val tvDetailPaidAmount = dialogView.findViewById<TextView>(R.id.tvDetailPaidAmount)
        val tvDetailRemainingAmount = dialogView.findViewById<TextView>(R.id.tvDetailRemainingAmount)
        val tvDetailDueDate = dialogView.findViewById<TextView>(R.id.tvDetailDueDate)
        val tvDetailPaymentDate = dialogView.findViewById<TextView>(R.id.tvDetailPaymentDate)
        val tvDetailPaymentStatus = dialogView.findViewById<TextView>(R.id.tvDetailPaymentStatus)
        val tvDetailReceiptNumber = dialogView.findViewById<TextView>(R.id.tvDetailReceiptNumber)
        val tvDetailTransactionId = dialogView.findViewById<TextView>(R.id.tvDetailTransactionId)
        val tvDetailPaymentCount = dialogView.findViewById<TextView>(R.id.tvDetailPaymentCount)
        val tvDetailAccountStatus = dialogView.findViewById<TextView>(R.id.tvDetailAccountStatus)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        // Temel bilgiler
        tvDetailUserName.text = aidat.userName
        tvDetailUserEmail.text = aidat.kullanici_email
        tvDetailApartment.text = "${aidat.apartmentBlock} Blok - ${aidat.apartmentNumber}"
        tvDetailDescription.text = aidat.description

        // Finansal bilgiler
        tvDetailAmount.text = numberFormat.format(aidat.amount)
        tvDetailLateFee.text = numberFormat.format(aidat.lateFeeAmount)
        tvDetailTotalAmount.text = numberFormat.format(aidat.calculatedTotalAmount)
        tvDetailPaidAmount.text = numberFormat.format(aidat.paidAmount)
        tvDetailRemainingAmount.text = numberFormat.format(aidat.calculatedRemainingAmount)

        // Tarih bilgileri
        tvDetailDueDate.text = formatDate(aidat.son_tarih)
        tvDetailPaymentDate.text = if (aidat.odeme_tarihi.isNullOrEmpty()) "-" else formatDate(aidat.odeme_tarihi)

        // Durum bilgileri
        tvDetailPaymentStatus.text = aidat.formattedPaymentStatus
        tvDetailReceiptNumber.text = if (aidat.receiptNumber.isNotEmpty()) aidat.receiptNumber else "-"
        tvDetailTransactionId.text = if (aidat.transactionId.isNotEmpty()) aidat.transactionId else "-"
        tvDetailPaymentCount.text = aidat.paymentCount.toString()
        tvDetailAccountStatus.text = if (aidat.isAccounted) "Aktarıldı" else "Bekliyor"

        // Duruma göre renkler
        val statusColor = when {
            aidat.isFullyPaid -> R.color.green
            aidat.isPartiallyPaid -> R.color.orange
            aidat.isReallyOverdue -> R.color.red  // DEĞİŞTİRİLDİ: isOverdue yerine isReallyOverdue
            else -> R.color.blue
        }
        tvDetailPaymentStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Buton tıklamaları
        dialogView.findViewById<Button>(R.id.btnPaymentHistory).setOnClickListener {
            dialog.dismiss()
            showPaymentHistoryDialog(aidat)
        }

        dialogView.findViewById<Button>(R.id.btnMakePayment).setOnClickListener {
            dialog.dismiss()
            showPaymentDialog(aidat)
        }

        dialogView.findViewById<Button>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
        }

        // Dialog arkaplanını şeffaf yap
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // Ödeme geçmişi dialog'u
    private fun showPaymentHistoryDialog(aidat: Aidat) {
        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        val progressDialog = AlertDialog.Builder(requireContext())
            .setMessage("Ödeme geçmişi yükleniyor...")
            .setCancelable(false)
            .create()
        progressDialog.show()

        AidatApiService.getPaymentHistory(
            userEmail = userEmail,
            userType = userType,
            dueId = aidat.id,
            onSuccess = { paymentList: List<AidatApiService.PaymentHistory> ->
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    displayPaymentHistoryDialog(paymentList, aidat)
                }
            },
            onError = { error: String ->
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Ödeme geçmişi yüklenemedi: $error", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun displayPaymentHistoryDialog(paymentList: List<AidatApiService.PaymentHistory>, aidat: Aidat) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_history, null)

        // Üst bilgiler
        val tvHistoryUserName = dialogView.findViewById<TextView>(R.id.tvHistoryUserName)
        val tvHistoryApartment = dialogView.findViewById<TextView>(R.id.tvHistoryApartment)
        val tvHistoryTotalDebt = dialogView.findViewById<TextView>(R.id.tvHistoryTotalDebt)
        val tvHistoryTotalPaid = dialogView.findViewById<TextView>(R.id.tvHistoryTotalPaid)
        val tvHistoryRemaining = dialogView.findViewById<TextView>(R.id.tvHistoryRemaining)
        val tvRecordCount = dialogView.findViewById<TextView>(R.id.tvRecordCount)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        tvHistoryUserName.text = aidat.userName
        tvHistoryApartment.text = "${aidat.apartmentBlock} - ${aidat.apartmentNumber}"
        tvHistoryTotalDebt.text = numberFormat.format(aidat.calculatedTotalAmount)
        tvHistoryTotalPaid.text = numberFormat.format(aidat.paidAmount)
        tvHistoryRemaining.text = numberFormat.format(aidat.calculatedRemainingAmount)
        tvRecordCount.text = "${paymentList.size} kayıt"

        // ListView için adapter
        val listView = dialogView.findViewById<ListView>(R.id.listViewPayments)
        val adapter = PaymentHistoryAdapter(paymentList)
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<Button>(R.id.btnCloseHistory).setOnClickListener {
            dialog.dismiss()
        }

        // Dialog arkaplanını şeffaf yap
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // Ödeme dialog'u
    private fun showPaymentDialog(aidat: Aidat) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_make_payment, null)

        val tvDialogTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvPaymentInfo = dialogView.findViewById<TextView>(R.id.tvPaymentInfo)
        val tvOriginalAmount = dialogView.findViewById<TextView>(R.id.tvOriginalAmount)
        val tvLateFeeAmount = dialogView.findViewById<TextView>(R.id.tvLateFeeAmount)
        val tvTotalAmount = dialogView.findViewById<TextView>(R.id.tvTotalAmount)
        val tvPaidAmount = dialogView.findViewById<TextView>(R.id.tvPaidAmount)
        val tvRemainingAmount = dialogView.findViewById<TextView>(R.id.tvRemainingAmount)
        val etPaymentAmount = dialogView.findViewById<EditText>(R.id.etPaymentAmount)
        val spPaymentMethod = dialogView.findViewById<Spinner>(R.id.spPaymentMethod)
        val etPaymentNotes = dialogView.findViewById<EditText>(R.id.etPaymentNotes)
        val rgPaymentType = dialogView.findViewById<RadioGroup>(R.id.rgPaymentType)
        val rbFullPayment = dialogView.findViewById<RadioButton>(R.id.rbFullPayment)
        val rbPartialPayment = dialogView.findViewById<RadioButton>(R.id.rbPartialPayment)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        // Başlık ve bilgiler
        tvDialogTitle.text = "💳 ${aidat.userName} - Ödeme İşlemi"
        tvPaymentInfo.text = "${aidat.apartmentBlock} - ${aidat.apartmentNumber} • ${aidat.description}"

        // Finansal bilgiler
        tvOriginalAmount.text = "Orijinal Tutar: ${numberFormat.format(aidat.amount)}"

        // Gecikme cezası bilgisi
        if (aidat.lateFeeAmount > 0) {
            tvLateFeeAmount.visibility = View.VISIBLE
            tvLateFeeAmount.text = "Gecikme Cezası: ${numberFormat.format(aidat.lateFeeAmount)} (${aidat.daysLate} gün)"
            tvLateFeeAmount.setTextColor(ContextCompat.getColor(requireContext(), R.color.red))
        } else {
            tvLateFeeAmount.visibility = View.GONE
        }

        tvTotalAmount.text = "Toplam Borç: ${numberFormat.format(aidat.calculatedTotalAmount)}"
        tvPaidAmount.text = "Ödenen: ${numberFormat.format(aidat.paidAmount)}"
        tvRemainingAmount.text = "Kalan Borç: ${numberFormat.format(aidat.calculatedRemainingAmount)}"

        // Varsayılan ödeme tutarı
        etPaymentAmount.setText(aidat.calculatedRemainingAmount.toString())

        // Ödeme yöntemi spinner'ı
        val paymentMethods = arrayOf("Nakit", "Banka Havalesi", "Kredi Kartı", "Çek", "EFT", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, paymentMethods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spPaymentMethod.adapter = adapter

        // Radio button listener
        rgPaymentType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbFullPayment -> {
                    etPaymentAmount.setText(aidat.calculatedRemainingAmount.toString())
                    etPaymentAmount.isEnabled = false
                }
                R.id.rbPartialPayment -> {
                    etPaymentAmount.setText("")
                    etPaymentAmount.isEnabled = true
                    etPaymentAmount.requestFocus()
                }
            }
        }

        // Varsayılan olarak tam ödeme seçili
        rbFullPayment.isChecked = true

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Buton tıklamaları
        dialogView.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.btnMakePayment).setOnClickListener {
            val paymentAmount = etPaymentAmount.text.toString().toDoubleOrNull()
            val paymentMethodText = spPaymentMethod.selectedItem.toString()
            val notes = etPaymentNotes.text.toString().trim()
            val isFullPayment = rgPaymentType.checkedRadioButtonId == R.id.rbFullPayment

            // Ödeme yöntemi kodunu belirle
            val paymentMethod = when (paymentMethodText) {
                "Nakit" -> "cash"
                "Banka Havalesi" -> "bank_transfer"
                "Kredi Kartı" -> "credit_card"
                "Çek" -> "check"
                "EFT" -> "eft"
                else -> "other"
            }

            // Validasyon
            if (paymentAmount == null || paymentAmount <= 0) {
                Toast.makeText(requireContext(), "Lütfen geçerli bir ödeme tutarı girin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val maxPaymentAmount = aidat.calculatedRemainingAmount

            if (paymentAmount > maxPaymentAmount) {
                Toast.makeText(requireContext(),
                    "Ödeme tutarı kalan borçtan fazla olamaz. Maksimum: ${numberFormat.format(maxPaymentAmount)}",
                    Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isFullPayment && paymentAmount >= maxPaymentAmount) {
                Toast.makeText(requireContext(),
                    "Bu tutar için tam ödeme seçmelisiniz",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()

            // Ödeme onay dialog'u göster
            if (isFullPayment) {
                showFullPaymentConfirmationDialog(aidat, paymentAmount, paymentMethodText, notes)
            } else {
                showPartialPaymentConfirmationDialog(aidat, paymentAmount, paymentMethodText, notes)
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showPartialPaymentConfirmationDialog(aidat: Aidat, amount: Double, method: String, notes: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_confirmation, null)

        val tvConfirmationTitle = dialogView.findViewById<TextView>(R.id.tvConfirmationTitle)
        val tvConfirmationMessage = dialogView.findViewById<TextView>(R.id.tvConfirmationMessage)
        val tvPaymentDetails = dialogView.findViewById<TextView>(R.id.tvPaymentDetails)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnEdit = dialogView.findViewById<Button>(R.id.btnEdit)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val remaining = aidat.calculatedRemainingAmount - amount
        val newPercentage = ((aidat.paidAmount + amount) / aidat.calculatedTotalAmount * 100).toInt()

        tvConfirmationTitle.text = "⚡ Kısmi Ödeme Onayı"

        tvConfirmationMessage.text = buildString {
            append("${aidat.userName} kullanıcısı için kısmi ödeme yapmak üzeresiniz.\n\n")
            append("⚠️ Kısmi ödeme sonrası mevcut aidat güncellenecek.")
        }

        tvPaymentDetails.text = buildString {
            append("👤 **Sakin:** ${aidat.userName}\n")
            append("🏠 **Daire:** ${aidat.apartmentBlock} - ${aidat.apartmentNumber}\n\n")

            append("📊 **Mevcut Durum:**\n")
            append("   💰 Toplam Borç: ${numberFormat.format(aidat.calculatedTotalAmount)}\n")
            append("   💳 Ödenmiş: ${numberFormat.format(aidat.paidAmount)} (%${aidat.realPaymentPercentage})\n\n")

            append("📈 **Yeni Ödeme:**\n")
            append("   💵 Ödeme Tutarı: ${numberFormat.format(amount)}\n")
            append("   📉 Yeni Kalan Borç: ${numberFormat.format(remaining)}\n")
            append("   📊 Yeni Ödeme Oranı: %$newPercentage\n\n")

            append("💳 **Ödeme Yöntemi:** $method")

            if (notes.isNotEmpty()) {
                append("\n📝 **Not:** $notes")
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            processPayment(aidat, amount, method, notes, false)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showPaymentDialog(aidat)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showFullPaymentConfirmationDialog(aidat: Aidat, amount: Double, method: String, notes: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_confirmation, null)

        val tvConfirmationTitle = dialogView.findViewById<TextView>(R.id.tvConfirmationTitle)
        val tvConfirmationMessage = dialogView.findViewById<TextView>(R.id.tvConfirmationMessage)
        val tvPaymentDetails = dialogView.findViewById<TextView>(R.id.tvPaymentDetails)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val btnEdit = dialogView.findViewById<Button>(R.id.btnEdit)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        tvConfirmationTitle.text = "✅ Tam Ödeme Onayı"

        tvConfirmationMessage.text = buildString {
            append("${aidat.userName} kullanıcısı için tam ödeme yapmak üzeresiniz.\n\n")
            append("✅ Bu işlem ile aidat tamamen kapanacak ve muhasebeye aktarılacak.")
        }

        tvPaymentDetails.text = buildString {
            append("👤 **Sakin:** ${aidat.userName}\n")
            append("🏠 **Daire:** ${aidat.apartmentBlock} - ${aidat.apartmentNumber}\n\n")

            append("💰 **Ödeme Bilgileri:**\n")
            append("   💵 Ödeme Tutarı: ${numberFormat.format(amount)}\n")
            append("   💳 Ödeme Yöntemi: $method\n\n")

            append("📋 **Son Durum:**\n")
            append("   ✅ Aidat tamamen ödenecek\n")
            append("   🎉 Borç kapanacak\n")
            append("   ✅ Muhasebeye aktarılacak")

            if (notes.isNotEmpty()) {
                append("\n📝 **Not:** $notes")
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            processPayment(aidat, amount, method, notes, true)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnEdit.setOnClickListener {
            dialog.dismiss()
            showPaymentDialog(aidat)
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun processPayment(aidat: Aidat, paymentAmount: Double, paymentMethodText: String, notes: String, isFullPayment: Boolean) {
        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        // Ödeme yöntemi kodunu belirle
        val paymentMethod = when (paymentMethodText) {
            "Nakit" -> "cash"
            "Banka Havalesi" -> "bank_transfer"
            "Kredi Kartı" -> "credit_card"
            "Çek" -> "check"
            "EFT" -> "eft"
            else -> "other"
        }

        // Progress dialog
        val progressDialog = AlertDialog.Builder(requireContext())
            .setView(layoutInflater.inflate(R.layout.dialog_loading, null))
            .setCancelable(false)
            .create()
        progressDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        progressDialog.show()

        AidatApiService.payDue(
            dueId = aidat.id,
            paymentAmount = paymentAmount,
            paymentMethod = paymentMethod,
            notes = notes,
            userEmail = userEmail,
            userType = userType,
            onSuccess = { result: AidatApiService.PaymentResult ->
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    if (result.success) {
                        showPaymentSuccessDialog(aidat, result)
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadAdminAidatData()
                        }, 1500)
                    } else {
                        showPaymentErrorDialog("Ödeme işlemi başarısız: ${result.message}", aidat)
                    }
                }
            },
            onError = { error: String ->
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    showPaymentErrorDialog(error, aidat)
                }
            }
        )
    }

    private fun showPaymentSuccessDialog(aidat: Aidat, result: AidatApiService.PaymentResult) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_success, null)

        val tvSuccessTitle = dialogView.findViewById<TextView>(R.id.tvSuccessTitle)
        val tvSuccessMessage = dialogView.findViewById<TextView>(R.id.tvSuccessMessage)
        val tvPaymentDetails = dialogView.findViewById<TextView>(R.id.tvPaymentDetails)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)

        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        val isPartial = result.payment_type == "partial"

        tvSuccessTitle.text = if (isPartial) "⚡ Kısmi Ödeme Başarılı" else "✅ Ödeme Tamamlandı"

        tvSuccessMessage.text = if (isPartial) {
            "Kısmi ödeme başarıyla kaydedildi. Kalan borç güncellendi."
        } else {
            "Ödeme başarıyla tamamlandı. Aidat kapatıldı ve muhasebeye aktarıldı."
        }

        tvPaymentDetails.text = buildString {
            append("👤 **Sakin:** ${aidat.userName}\n")
            append("🏠 **Daire:** ${aidat.apartmentBlock} - ${aidat.apartmentNumber}\n\n")

            append("💰 **Ödeme Bilgileri:**\n")
            append("   💵 Ödeme Tutarı: ${numberFormat.format(result.payment_amount)}\n")
            append("   🧾 Fiş No: ${result.receipt_number}\n")

            if (!result.transaction_id.isNullOrEmpty()) {
                append("   🔢 İşlem No: ${result.transaction_id}\n")
            }

            if (isPartial) {
                append("\n📊 **Yeni Durum:**\n")
                append("   📋 Toplam Ödenen: ${numberFormat.format(result.paid_amount)}\n")
                append("   🔄 Kalan Borç: ${numberFormat.format(result.remaining_amount)}\n")
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun showPaymentErrorDialog(error: String, aidat: Aidat) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_error, null)

        val tvErrorTitle = dialogView.findViewById<TextView>(R.id.tvErrorTitle)
        val tvErrorMessage = dialogView.findViewById<TextView>(R.id.tvErrorMessage)
        val btnRetry = dialogView.findViewById<Button>(R.id.btnRetry)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        tvErrorTitle.text = "❌ Ödeme Hatası"
        tvErrorMessage.text = buildString {
            append("Ödeme işlemi sırasında bir hata oluştu:\n\n")
            append("**Hata:** $error\n\n")
            append("Lütfen:\n")
            append("1. İnternet bağlantınızı kontrol edin\n")
            append("2. Bilgilerin doğruluğunu kontrol edin\n")
            append("3. Sunucu bağlantısını kontrol edin\n")
            append("4. Daha sonra tekrar deneyin")
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnRetry.setOnClickListener {
            dialog.dismiss()
            showPaymentDialog(aidat)
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // Yardımcı fonksiyonlar
    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "-"
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}" else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    // Payment History Adapter
    private inner class PaymentHistoryAdapter(private val payments: List<AidatApiService.PaymentHistory>) : BaseAdapter() {
        override fun getCount(): Int = payments.size
        override fun getItem(position: Int): AidatApiService.PaymentHistory = payments[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.item_payment_history, parent, false)
            val payment = getItem(position)

            val tvPaymentNumber = view.findViewById<TextView>(R.id.tvPaymentNumber)
            val tvPaymentAmount = view.findViewById<TextView>(R.id.tvPaymentAmount)
            val tvPaymentDate = view.findViewById<TextView>(R.id.tvPaymentDate)
            val tvPaymentMethod = view.findViewById<TextView>(R.id.tvPaymentMethod)
            val tvPaymentType = view.findViewById<TextView>(R.id.tvPaymentType)
            val tvReceiptNumber = view.findViewById<TextView>(R.id.tvReceiptNumber)

            val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

            tvPaymentNumber.text = "#${position + 1}"
            tvPaymentAmount.text = numberFormat.format(payment.payment_amount)
            tvPaymentDate.text = formatDate(payment.payment_date)

            // Ödeme yöntemi görselleştirme
            val methodText = when (payment.payment_method) {
                "cash" -> "Nakit"
                "bank_transfer" -> "Banka Havalesi"
                "credit_card" -> "Kredi Kartı"
                "check" -> "Çek"
                "eft" -> "EFT"
                else -> payment.payment_method
            }
            tvPaymentMethod.text = methodText

            tvPaymentType.text = if (payment.is_partial == 1) "Kısmi" else "Tam"
            tvReceiptNumber.text = payment.receipt_number

            return view
        }
    }
}