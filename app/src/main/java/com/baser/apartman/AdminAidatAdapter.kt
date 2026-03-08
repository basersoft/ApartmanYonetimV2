package com.baser.apartman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AdminAidatAdapter(
    private var aidatList: List<Aidat> = emptyList(),
    private val onItemClick: (Aidat) -> Unit = {},
    private val onPaymentHistoryClick: (Aidat) -> Unit = {}
) : RecyclerView.Adapter<AdminAidatAdapter.AdminAidatViewHolder>() {

    private var selectedPosition = -1

    class AdminAidatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        val tvApartment: TextView = itemView.findViewById(R.id.tvApartment)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val tvLateFee: TextView = itemView.findViewById(R.id.tvLateFee)
        val tvPaymentDate: TextView = itemView.findViewById(R.id.tvPaymentDate)
        val layoutDetails: View = itemView.findViewById(R.id.layoutDetails)
        val tvDetailDescription: TextView = itemView.findViewById(R.id.tvDetailDescription)
        val tvDetailOriginalAmount: TextView = itemView.findViewById(R.id.tvDetailOriginalAmount)
        val tvDetailLateFee: TextView = itemView.findViewById(R.id.tvDetailLateFee)
        val tvDetailTotalAmount: TextView = itemView.findViewById(R.id.tvDetailTotalAmount)
        val tvDetailDaysLate: TextView = itemView.findViewById(R.id.tvDetailDaysLate)
        val tvPaymentInfo: TextView = itemView.findViewById(R.id.tvPaymentInfo)
        val tvPaymentHistoryBtn: TextView? = itemView.tryFindViewById(R.id.tvPaymentHistoryBtn)
        val tvAccountingInfo: TextView? = itemView.tryFindViewById(R.id.tvAccountingInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminAidatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_aidat, parent, false)
        return AdminAidatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminAidatViewHolder, position: Int) {
        val aidat = aidatList[position]
        val context = holder.itemView.context
        val isSelected = position == selectedPosition

        // 1. Temel bilgiler
        holder.tvUserName.text = aidat.userName
        holder.tvUserEmail.text = aidat.kullanici_email
        holder.tvApartment.text = "${aidat.apartmentBlock} - ${aidat.apartmentNumber}"

        // 2. Tutar formatı - GELİŞMİŞ
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))

        // Ana tutar
        holder.tvAmount.text = buildString {
            append("Toplam: ${numberFormat.format(aidat.calculatedTotalAmount)}\n")
            if (aidat.isPartiallyPaid || aidat.isFullyPaid) {
                append("Ödenen: ${numberFormat.format(aidat.paidAmount)}")
                if (aidat.isPartiallyPaid) {
                    append(" (%${aidat.realPaymentPercentage})")
                }
            }
        }

        // 3. Ödeme bilgileri
        if (aidat.paymentCount > 0) {
            holder.tvPaymentInfo?.text = buildString {
                append("${aidat.paymentCount} ödeme")
                if (aidat.paymentDetails.isNotEmpty()) {
                    append("\nDetaylar için tıklayın")
                }
            }
            holder.tvPaymentInfo?.visibility = View.VISIBLE
        } else {
            holder.tvPaymentInfo?.visibility = View.GONE
        }

        // 4. Muhasebe bilgisi
        holder.tvAccountingInfo?.text = aidat.accountingInfo
        holder.tvAccountingInfo?.setTextColor(
            ContextCompat.getColor(context,
                if (aidat.isAccounted) R.color.green else R.color.red)
        )

        // 5. Son tarih
        holder.tvDueDate.text = "Son Tarih: ${formatDate(aidat.son_tarih)}"

        // 6. Durum - KISMI ÖDEME DURUMUNU GÖSTER
        val statusText = aidat.formattedPaymentStatus
        holder.tvStatus.text = statusText
        setStatusBackground(holder.tvStatus, aidat.durum, context, aidat.isPartiallyPaid)

        // 7. Gecikme zammı
        if (aidat.lateFeeAmount > 0) {
            holder.tvLateFee.text = "Gecikme Zammı: ${numberFormat.format(aidat.lateFeeAmount)}"
            holder.tvLateFee.visibility = View.VISIBLE
        } else {
            holder.tvLateFee.visibility = View.GONE
        }

        // 8. Ödeme tarihi
        if (!aidat.odeme_tarihi.isNullOrEmpty()) {
            holder.tvPaymentDate.text = "Ödeme: ${formatDate(aidat.odeme_tarihi)}"
            holder.tvPaymentDate.visibility = View.VISIBLE
        } else {
            holder.tvPaymentDate.visibility = View.GONE
        }

        // 9. Detay bilgileri (sadece seçiliyse göster)
        if (isSelected) {
            holder.layoutDetails.visibility = View.VISIBLE

            // Detay alanlarını doldur
            holder.tvDetailDescription.text = aidat.description
            holder.tvDetailOriginalAmount.text = numberFormat.format(aidat.amount)
            holder.tvDetailLateFee.text = numberFormat.format(aidat.lateFeeAmount)
            holder.tvDetailTotalAmount.text = numberFormat.format(aidat.calculatedTotalAmount)

            // Gecikme gün sayısı
            val daysLate = calculateDaysLate(aidat.son_tarih, aidat.durum)
            holder.tvDetailDaysLate.text = if (daysLate > 0) "$daysLate gün gecikme" else "Gecikme yok"

            // Ödeme geçmişi butonu
            holder.tvPaymentHistoryBtn?.setOnClickListener {
                onPaymentHistoryClick(aidat)
            }

            // Seçili arka plan
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item_background))
        } else {
            holder.layoutDetails.visibility = View.GONE
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.white))
        }

        // 10. Tıklama olayı
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = if (selectedPosition == position) -1 else position

            if (previousSelected != -1) notifyItemChanged(previousSelected)
            if (selectedPosition != -1) notifyItemChanged(selectedPosition)

            onItemClick(aidat)
        }
    }

    override fun getItemCount(): Int = aidatList.size

    fun setAidatList(newList: List<Aidat>) {
        aidatList = newList
        selectedPosition = -1
        notifyDataSetChanged()
    }

    fun clearSelection() {
        val previousSelected = selectedPosition
        selectedPosition = -1
        if (previousSelected != -1) notifyItemChanged(previousSelected)
    }

    fun getSelectedItem(): Aidat? {
        return if (selectedPosition != -1 && selectedPosition < aidatList.size) {
            aidatList[selectedPosition]
        } else {
            null
        }
    }

    private fun formatDate(dateString: String): String {
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) "${parts[2]}.${parts[1]}.${parts[0]}"
            else dateString
        } catch (e: Exception) {
            dateString
        }
    }

    private fun setStatusBackground(textView: TextView, status: String, context: android.content.Context, isPartial: Boolean = false) {
        val (colorRes, textColorRes) = when {
            isPartial -> Pair(R.color.status_partial, R.color.white)
            status.lowercase() == "paid" -> Pair(R.color.status_paid, R.color.white)
            status.lowercase() == "pending" -> Pair(R.color.status_pending, R.color.white)
            status.lowercase() == "overdue" -> Pair(R.color.status_overdue, R.color.white)
            else -> Pair(R.color.status_default, R.color.white)
        }

        textView.setBackgroundColor(ContextCompat.getColor(context, colorRes))
        textView.setTextColor(ContextCompat.getColor(context, textColorRes))
    }

    private fun calculateDaysLate(dueDate: String, status: String): Int {
        if (status.lowercase() == "paid") return 0
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dueDateObj = dateFormat.parse(dueDate)
            val today = Date()
            if (dueDateObj != null && dueDateObj.before(today)) {
                ((today.time - dueDateObj.time) / (24 * 60 * 60 * 1000)).toInt()
            } else 0
        } catch (e: Exception) {
            0
        }
    }
}

// View bulma helper
fun View.tryFindViewById(id: Int): TextView? {
    return try {
        findViewById(id)
    } catch (e: Exception) {
        null
    }
}