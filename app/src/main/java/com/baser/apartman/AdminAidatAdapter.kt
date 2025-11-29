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
    private val onItemClick: (Aidat) -> Unit = {}
) : RecyclerView.Adapter<AdminAidatAdapter.AdminAidatViewHolder>() {

    private var selectedPosition = -1

    class AdminAidatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUserName)
        val tvUserEmail: TextView = itemView.findViewById(R.id.tvUserEmail)
        val tvApartment: TextView = itemView.findViewById(R.id.tvApartment)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvDueDate: TextView = itemView.findViewById(R.id.tvDueDate)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvPaymentDate: TextView = itemView.findViewById(R.id.tvPaymentDate)
        val tvLateFee: TextView = itemView.findViewById(R.id.tvLateFee)
        val layoutDetails: View = itemView.findViewById(R.id.layoutDetails)
        
        // Detay alanları
        val tvDetailDescription: TextView = itemView.findViewById(R.id.tvDetailDescription)
        val tvDetailOriginalAmount: TextView = itemView.findViewById(R.id.tvDetailOriginalAmount)
        val tvDetailLateFee: TextView = itemView.findViewById(R.id.tvDetailLateFee)
        val tvDetailTotalAmount: TextView = itemView.findViewById(R.id.tvDetailTotalAmount)
        val tvDetailDaysLate: TextView = itemView.findViewById(R.id.tvDetailDaysLate)
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

        holder.tvUserName.text = aidat.userName
        holder.tvUserEmail.text = aidat.kullanici_email
        holder.tvApartment.text = "${aidat.apartmentBlock} - ${aidat.apartmentNumber}"

        // Tutarı formatla
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("tr", "TR"))
        holder.tvAmount.text = numberFormat.format(aidat.amount)

        holder.tvDueDate.text = "Son Tarih: ${formatDate(aidat.son_tarih)}"
        holder.tvStatus.text = getStatusText(aidat.durum)

        // Gecikme zammı
        if (aidat.lateFeeAmount > 0) {
            holder.tvLateFee.text = "Gecikme Zammı: ${numberFormat.format(aidat.lateFeeAmount)}"
            holder.tvLateFee.visibility = View.VISIBLE
        } else {
            holder.tvLateFee.visibility = View.GONE
        }

        // Ödeme tarihi
        if (!aidat.odeme_tarihi.isNullOrEmpty()) {
            holder.tvPaymentDate.text = "Ödeme: ${formatDate(aidat.odeme_tarihi)}"
            holder.tvPaymentDate.visibility = View.VISIBLE
        } else {
            holder.tvPaymentDate.visibility = View.GONE
        }

        // Duruma göre renk ayarla
        setStatusBackground(holder.tvStatus, aidat.durum, context)

        // Detayları doldur
        holder.tvDetailDescription.text = aidat.description
        holder.tvDetailOriginalAmount.text = numberFormat.format(aidat.amount)
        holder.tvDetailLateFee.text = numberFormat.format(aidat.lateFeeAmount)
        holder.tvDetailTotalAmount.text = numberFormat.format(aidat.amount + aidat.lateFeeAmount)
        
        // Gecikme gün sayısını hesapla
        val daysLate = calculateDaysLate(aidat.son_tarih, aidat.durum)
        holder.tvDetailDaysLate.text = if (daysLate > 0) "$daysLate gün" else "Gecikme yok"

        // Seçili durumu ayarla
        if (isSelected) {
            holder.layoutDetails.visibility = View.VISIBLE
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.selected_item_background))
        } else {
            holder.layoutDetails.visibility = View.GONE
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, android.R.color.transparent))
        }

        // Tıklama olayı
        holder.itemView.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = if (selectedPosition == position) -1 else position
            
            notifyItemChanged(previousSelected)
            if (selectedPosition != -1) {
                notifyItemChanged(selectedPosition)
            }
            
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
        if (previousSelected != -1) {
            notifyItemChanged(previousSelected)
        }
    }

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

    private fun setStatusBackground(textView: TextView, status: String, context: android.content.Context) {
        val colorRes = when (status.lowercase()) {
            "paid", "ödendi" -> R.color.status_paid
            "pending", "bekliyor" -> R.color.status_pending
            "overdue", "gecikmiş" -> R.color.status_overdue
            else -> R.color.status_default
        }
        textView.setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    private fun calculateDaysLate(dueDate: String, status: String): Int {
        if (status.lowercase() == "paid") return 0
        
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dueDateObj = dateFormat.parse(dueDate)
            val today = Date()
            
            if (dueDateObj != null && dueDateObj.before(today)) {
                val diff = today.time - dueDateObj.time
                (diff / (24 * 60 * 60 * 1000)).toInt()
            } else {
                0
            }
        } catch (e: Exception) {
            0
        }
    }
}