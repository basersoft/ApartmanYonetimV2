package com.baser.apartman

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AidatAdapter(private var aidatList: List<Aidat> = emptyList()) :
    RecyclerView.Adapter<AidatAdapter.AidatViewHolder>() {

    class AidatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTarih: TextView = itemView.findViewById(R.id.tvTarih)
        val tvTutar: TextView = itemView.findViewById(R.id.tvTutar)
        val tvDurum: TextView = itemView.findViewById(R.id.tvDurum)
        val tvSonTarih: TextView = itemView.findViewById(R.id.tvSonTarih)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AidatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_aidat, parent, false)
        return AidatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AidatViewHolder, position: Int) {
        val aidat = aidatList[position]

        holder.tvTarih.text = aidat.ay
        holder.tvTutar.text = aidat.miktar
        holder.tvDurum.text = aidat.durum

        // Durum rengini ayarla
        try {
            holder.tvDurum.setBackgroundColor(Color.parseColor(aidat.durumRenk))
        } catch (e: Exception) {
            // Varsayılan renk
            holder.tvDurum.setBackgroundColor(Color.parseColor("#f39c12"))
        }

        // Son tarihi göster
        holder.tvSonTarih.text = if (!aidat.son_tarih.isNullOrEmpty()) {
            "Son Tarih: ${aidat.son_tarih}"
        } else {
            "Son Tarih: Belirtilmemiş"
        }
    }

    override fun getItemCount(): Int = aidatList.size

    fun setAidatList(newList: List<Aidat>) {
        aidatList = newList
        notifyDataSetChanged()
    }
}