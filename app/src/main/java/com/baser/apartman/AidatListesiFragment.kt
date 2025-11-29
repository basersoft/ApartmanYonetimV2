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
        // Burada ek işlemler yapabilirsiniz
        // Örneğin: Toast mesajı gösterme, log yazdırma vb.
        println("🔍 Seçilen aidat: ${aidat.userName} - ${aidat.description}")
        
        // İsterseniz burada detay dialog'u gösterebilirsiniz
        showAidatDetailDialog(aidat)
    }

    // Detay dialog'u göster (opsiyonel)
    private fun showAidatDetailDialog(aidat: Aidat) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        
        val message = """
            👤 Sakin: ${aidat.userName}
            📧 Email: ${aidat.kullanici_email}
            🏠 Daire: ${aidat.apartmentBlock} - ${aidat.apartmentNumber}
            
            💰 Orijinal Tutar: ${numberFormat.format(aidat.amount)}
            ⚡ Gecikme Zammı: ${numberFormat.format(aidat.lateFeeAmount)}
            💵 Toplam Tutar: ${numberFormat.format(aidat.amount + aidat.lateFeeAmount)}
            
            📅 Son Ödeme: ${formatDate(aidat.son_tarih)}
            ✅ Durum: ${getStatusText(aidat.durum)}
            
            📝 Açıklama: ${aidat.description}
        """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Aidat Detayları")
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
            }
            .setNeutralButton("Ödeme Yap") { dialog, _ ->
                // Ödeme yapma işlemi buraya eklenebilir
                showPaymentDialog(aidat)
                dialog.dismiss()
            }
            .show()
    }

    // Ödeme dialog'u (opsiyonel)
    // Ödeme dialog'u
    private fun showPaymentDialog(aidat: Aidat) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        val totalAmount = aidat.amount + aidat.lateFeeAmount

        val view = layoutInflater.inflate(R.layout.dialog_payment, null)
        val etPaymentAmount = view.findViewById<EditText>(R.id.etPaymentAmount)
        val spinnerPaymentMethod = view.findViewById<Spinner>(R.id.spinnerPaymentMethod)
        val etPaymentNotes = view.findViewById<EditText>(R.id.etPaymentNotes)

        // Ödeme yöntemleri
        val paymentMethods = arrayOf("Nakit", "Banka Havalesi", "Kredi Kartı", "Diğer")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, paymentMethods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaymentMethod.adapter = adapter

        // Varsayılan değerleri ayarla
        etPaymentAmount.setText(totalAmount.toString())

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Ödeme İşlemi")
            .setView(view)
            .setPositiveButton("Ödemeyi Tamamla") { dialog, _ ->
                val paymentAmount = etPaymentAmount.text.toString().toDoubleOrNull()
                val paymentMethod = spinnerPaymentMethod.selectedItem.toString()
                val notes = etPaymentNotes.text.toString()

                if (paymentAmount == null || paymentAmount <= 0) {
                    Toast.makeText(requireContext(), "Lütfen geçerli bir ödeme tutarı girin", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (paymentAmount < totalAmount) {
                    // Kısmi ödeme uyarısı
                    AlertDialog.Builder(requireContext())
                        .setTitle("Kısmi Ödeme")
                        .setMessage("Ödeme tutarı toplam borçtan az. Kısmi ödeme yapmak istiyor musunuz?")
                        .setPositiveButton("Evet, Kısmi Ödeme Yap") { _, _ ->
                            processPayment(aidat, paymentAmount, paymentMethod, notes)
                            dialog.dismiss()
                        }
                        .setNegativeButton("İptal", null)
                        .show()
                } else {
                    processPayment(aidat, paymentAmount, paymentMethod, notes)
                    dialog.dismiss()
                }
            }
            .setNegativeButton("İptal") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    // Ödeme işlemini gerçekleştir
    private fun processPayment(aidat: Aidat, paymentAmount: Double, paymentMethod: String, notes: String) {
        val activity = requireActivity() as AdminAidatActivity
        val userEmail = activity.fragmentUserEmail
        val userType = activity.fragmentUserType

        // Progress göster
        val progressDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
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
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()

                    // Listeyi yenile
                    loadAdminAidatData()

                    // Seçimi temizle
                    adapter.clearSelection()

                    // Başarı mesajı göster
                    showPaymentSuccessDialog(aidat, paymentAmount)
                }
            },
            onError = { error ->
                requireActivity().runOnUiThread {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Ödeme hatası: $error", Toast.LENGTH_LONG).show()

                    // Hata dialog'u göster
                    showPaymentErrorDialog(error)
                }
            }
        )
    }

    // Ödeme başarı dialog'u
    private fun showPaymentSuccessDialog(aidat: Aidat, paymentAmount: Double) {
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        val message = """
        ✅ Ödeme Başarılı!
        
        👤 Sakin: ${aidat.userName}
        💰 Ödenen Tutar: ${numberFormat.format(paymentAmount)}
        📅 Tarih: ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date())}
        
        Aidat durumu 'Ödendi' olarak güncellendi.
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Ödeme Tamamlandı")
            .setMessage(message)
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // Ödeme hata dialog'u
    private fun showPaymentErrorDialog(error: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Ödeme Hatası")
            .setMessage("Ödeme işlemi sırasında bir hata oluştu:\n\n$error\n\nLütfen daha sonra tekrar deneyin.")
            .setPositiveButton("Tamam") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    /*
    private fun makePayment(aidatId: Int, amount: Double) {
        // Ödeme işlemi burada yapılacak
        Toast.makeText(requireContext(), "Ödeme işlemi başlatılıyor...", Toast.LENGTH_SHORT).show()
    }
*/
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