package com.baser.apartman

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class SikayetAdapter(
    private var sikayetList: List<Sikayet> = emptyList(),
    private val isAdmin: Boolean = false,
    private val onAdminActionClick: ((Sikayet) -> Unit)? = null
) : RecyclerView.Adapter<SikayetAdapter.SikayetViewHolder>() {

    private var expandedPosition = -1

    class SikayetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardSikayet: MaterialCardView = itemView.findViewById(R.id.cardSikayet)
        val layoutDetay: View = itemView.findViewById(R.id.layoutDetay)
        val imageExpand: ImageView = itemView.findViewById(R.id.imageExpand)
        val tvBaslik: TextView = itemView.findViewById(R.id.tvSikayetBaslik)
        val tvAciklama: TextView = itemView.findViewById(R.id.tvSikayetAciklama)
        val tvKategori: TextView = itemView.findViewById(R.id.tvSikayetKategori)
        val tvDurum: TextView = itemView.findViewById(R.id.tvSikayetDurum)
        val tvTarih: TextView = itemView.findViewById(R.id.tvSikayetTarih)
        val tvCevap: TextView = itemView.findViewById(R.id.tvSikayetCevap)
        val tvKullaniciAdi: TextView = itemView.findViewById(R.id.tvKullaniciAdi)
        val tvKonum: TextView = itemView.findViewById(R.id.tvKonum)
        val tvKullaniciTipi: TextView = itemView.findViewById(R.id.tvKullaniciTipi)
        val layoutCevap: View = itemView.findViewById(R.id.cevapLayout)
        val layoutKullanici: View = itemView.findViewById(R.id.userInfoLayout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SikayetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sikayet, parent, false)
        return SikayetViewHolder(view)
    }

    override fun onBindViewHolder(holder: SikayetViewHolder, position: Int) {
        val sikayet = sikayetList[position]
        val isExpanded = position == expandedPosition

        holder.layoutDetay.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.imageExpand.rotation = if (isExpanded) 180f else 0f

        holder.tvBaslik.text = sikayet.baslik
        holder.tvAciklama.text = sikayet.aciklama
        holder.tvKategori.text = sikayet.kategori
        holder.tvDurum.text = sikayet.durum
        holder.tvTarih.text = sikayet.tarih

        // Durum renkleri
        val (durumRengi, textRengi) = when (sikayet.durum) {
            "Beklemede" -> Pair("#fff3e0", "#e65100")
            "İşlemde" -> Pair("#e3f2fd", "#1565c0")
            "Çözüldü" -> Pair("#e8f5e8", "#2e7d32")
            "Reddedildi" -> Pair("#ffebee", "#c62828")
            else -> Pair("#f5f5f5", "#616161")
        }

        holder.tvDurum.setBackgroundColor(Color.parseColor(durumRengi))
        holder.tvDurum.setTextColor(Color.parseColor(textRengi))

        // Admin kontrolü
        if (isAdmin) {
            holder.layoutKullanici.visibility = View.VISIBLE
            holder.tvKullaniciAdi.text = sikayet.kullaniciAdi ?: "Bilinmeyen Kullanıcı"
            holder.tvKonum.text = sikayet.konum ?: "Konum bilgisi yok"
            holder.tvKullaniciTipi.text = sikayet.kullaniciTipi ?: "Site Sakini"

            // Admin için uzun tıklama
            holder.cardSikayet.setOnLongClickListener {
                onAdminActionClick?.invoke(sikayet)
                true
            }
        } else {
            holder.layoutKullanici.visibility = View.GONE
            holder.cardSikayet.setOnLongClickListener(null)
        }

        // Cevap varsa göster
        if (!sikayet.cevap.isNullOrEmpty()) {
            holder.layoutCevap.visibility = View.VISIBLE
            var cevapText = sikayet.cevap
            if (!sikayet.cevapTarihi.isNullOrEmpty()) {
                cevapText += "\n\n(Cevap Tarihi: ${sikayet.cevapTarihi})"
            }
            holder.tvCevap.text = cevapText
        } else {
            holder.layoutCevap.visibility = View.GONE
        }

        // Normal tıklama (expand/collapse)
        holder.cardSikayet.setOnClickListener {
            if (expandedPosition == position) {
                expandedPosition = -1
            } else {
                expandedPosition = position
            }
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int = sikayetList.size

    fun setSikayetList(newList: List<Sikayet>) {
        sikayetList = newList
        expandedPosition = -1
        notifyDataSetChanged()
    }
}