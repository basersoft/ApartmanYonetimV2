package com.baser.apartman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AidatAdapter(
    private var aidatList: List<Aidat> = emptyList(),
    private val onItemClick: (Aidat) -> Unit = {}
) : RecyclerView.Adapter<AidatAdapter.AidatViewHolder>() {

    class AidatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // item_aidat.xml'deki ID'ler
        val tvKullanici: TextView = itemView.findViewById(R.id.tvKullanici)
        val tvTarih: TextView = itemView.findViewById(R.id.tvTarih)
        val tvTutar: TextView = itemView.findViewById(R.id.tvTutar)
        val tvSonTarih: TextView = itemView.findViewById(R.id.tvSonTarih)
        val tvDurum: TextView = itemView.findViewById(R.id.tvDurum)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AidatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_aidat, parent, false)
        return AidatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AidatViewHolder, position: Int) {
        val aidat = aidatList[position]
        val context = holder.itemView.context

        // Kullanıcı bilgisi - sadece admin ise göster
        holder.tvKullanici.text = "${aidat.userName} (${aidat.kullanici_email})"
        // Normal kullanıcılar için gizle
        holder.tvKullanici.visibility = View.GONE

        // Tarih/Ay bilgisi
        holder.tvTarih.text = aidat.description.ifEmpty { aidat.ay }

        // Tutar
        holder.tvTutar.text = formatAmount(aidat.amount)

        // Son tarih
        holder.tvSonTarih.text = "Son Tarih: ${formatDateForDisplay(aidat.son_tarih)}"

        // Durum
        holder.tvDurum.text = getStatusText(aidat.durum)
        setStatusBackground(holder.tvDurum, aidat.durum, context)

        // Tıklama
        holder.itemView.setOnClickListener {
            onItemClick(aidat)
        }
    }

    override fun getItemCount(): Int = aidatList.size

    fun setAidatList(newList: List<Aidat>) {
        aidatList = newList
        notifyDataSetChanged()
    }

    private fun formatAmount(amount: Double): String {
        return String.format("₺%.2f", amount)
    }

    private fun formatDateForDisplay(dateString: String): String {
        return try {
            val parts = dateString.split("-")
            if (parts.size == 3) {
                val day = parts[2]
                val month = getMonthName(parts[1].toInt())
                val year = parts[0]
                "$day $month $year"
            } else {
                dateString
            }
        } catch (e: Exception) {
            dateString
        }
    }

    private fun getMonthName(month: Int): String {
        return when (month) {
            1 -> "Ocak"
            2 -> "Şubat"
            3 -> "Mart"
            4 -> "Nisan"
            5 -> "Mayıs"
            6 -> "Haziran"
            7 -> "Temmuz"
            8 -> "Ağustos"
            9 -> "Eylül"
            10 -> "Ekim"
            11 -> "Kasım"
            12 -> "Aralık"
            else -> ""
        }
    }

    private fun getStatusText(status: String): String {
        return when (status.lowercase()) {
            "paid" -> "ÖDENDİ"
            "pending" -> "BEKLİYOR"
            "overdue" -> "GECİKMİŞ"
            else -> status.uppercase()
        }
    }

    private fun setStatusBackground(textView: TextView, status: String, context: android.content.Context) {
        val colorRes = when (status.lowercase()) {
            "paid" -> R.color.status_paid
            "pending" -> R.color.status_pending
            "overdue" -> R.color.status_overdue
            else -> R.color.status_default
        }
        textView.setBackgroundColor(androidx.core.content.ContextCompat.getColor(context, colorRes))
    }
}