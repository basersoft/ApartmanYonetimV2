package com.baser.apartman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DuyuruAdapter(private var duyuruList: List<Duyuru> = emptyList()) :
    RecyclerView.Adapter<DuyuruAdapter.DuyuruViewHolder>() {

    class DuyuruViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBaslik: TextView = itemView.findViewById(R.id.tvBaslik)
        val tvIcerik: TextView = itemView.findViewById(R.id.tvIcerik)
        val tvTarih: TextView = itemView.findViewById(R.id.tvTarih)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DuyuruViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_duyuru, parent, false)
        return DuyuruViewHolder(view)
    }

    override fun onBindViewHolder(holder: DuyuruViewHolder, position: Int) {
        val duyuru = duyuruList[position]

        holder.tvBaslik.text = duyuru.baslik
        holder.tvIcerik.text = duyuru.icerik
        holder.tvTarih.text = duyuru.tarih
    }

    override fun getItemCount(): Int = duyuruList.size

    fun setDuyuruList(newList: List<Duyuru>) {
        duyuruList = newList
        notifyDataSetChanged()
    }
}