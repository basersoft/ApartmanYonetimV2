package com.baser.apartman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class DuyuruAdapter(private var duyuruList: List<Duyuru> = emptyList()) :
    RecyclerView.Adapter<DuyuruAdapter.DuyuruViewHolder>() {

    class DuyuruViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBaslik: TextView = itemView.findViewById(R.id.tvBaslik)
        val tvIcerik: TextView = itemView.findViewById(R.id.tvIcerik)
        val tvTarih: TextView = itemView.findViewById(R.id.tvTarih)
        val ivResim: ImageView = itemView.findViewById(R.id.ivResim)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DuyuruViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_duyuru, parent, false)
        return DuyuruViewHolder(view)
    }

    override fun onBindViewHolder(holder: DuyuruViewHolder, position: Int) {
        val duyuru = duyuruList[position]
        val context = holder.itemView.context

        // HTML formatlı metinleri düzgün şekilde göster
        holder.tvBaslik.text = HtmlCompat.fromHtml(duyuru.baslik, HtmlCompat.FROM_HTML_MODE_COMPACT)
        holder.tvIcerik.text = HtmlCompat.fromHtml(duyuru.icerik, HtmlCompat.FROM_HTML_MODE_COMPACT)
        holder.tvTarih.text = HtmlCompat.fromHtml(duyuru.tarih, HtmlCompat.FROM_HTML_MODE_COMPACT)

        // Resim URL'si varsa göster
        if (duyuru.resimUrl.isNotBlank()) {
            holder.ivResim.visibility = View.VISIBLE

            println("🔍 RESİM YÜKLENİYOR: ${duyuru.resimUrl}")

            Glide.with(context)
                .load(duyuru.resimUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.ivResim)
        } else {
            holder.ivResim.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = duyuruList.size

    fun setDuyuruList(newList: List<Duyuru>) {
        duyuruList = newList
        notifyDataSetChanged()
    }
}