package com.baser.apartman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class AidatAdapter(
    private var aidatList: List<Aidat> = emptyList(),
    private val onItemClick: (Aidat) -> Unit = {},
    private val onOdemeClick: (Aidat) -> Unit = {} // YENİ: Ödeme butonu callback
) : RecyclerView.Adapter<AidatAdapter.AidatViewHolder>() {

    class AidatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvKullanici: TextView = itemView.findViewById(R.id.tvKullanici)
        val tvTarih: TextView = itemView.findViewById(R.id.tvTarih)
        val tvTutar: TextView = itemView.findViewById(R.id.tvTutar)
        val tvSonTarih: TextView = itemView.findViewById(R.id.tvSonTarih)
        val tvDurum: TextView = itemView.findViewById(R.id.tvDurum)
        val tvKalanBorc: TextView = itemView.findViewById(R.id.tvKalanBorc)
        val tvOdenenMiktar: TextView = itemView.findViewById(R.id.tvOdenenMiktar)
        val tvPaymentStatus: TextView = itemView.findViewById(R.id.tvPaymentStatus)
        val tvLateFee: TextView = itemView.findViewById(R.id.tvLateFee)
        val tvPaymentDate: TextView = itemView.findViewById(R.id.tvPaymentDate)
        val layoutPaymentInfo: View = itemView.findViewById(R.id.layoutPaymentInfo)
        val btnOdemeYap: TextView? = itemView.findViewById(R.id.btnOdemeYap) // YENİ: Ödeme butonu
        val tvSiteInfo: TextView? = itemView.findViewById(R.id.tvSiteInfo) // YENİ: Site bilgisi
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AidatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_aidat, parent, false)
        return AidatViewHolder(view)
    }

    override fun onBindViewHolder(holder: AidatViewHolder, position: Int) {
        val aidat = aidatList[position]
        val context = holder.itemView.context

        // 1. Kullanıcı bilgisi (sadece admin/manager görür)
        holder.tvKullanici.text = "${aidat.userName} (${aidat.kullanici_email})"

        // 2. Tarih/Ay bilgisi
        holder.tvTarih.text = aidat.description.ifEmpty { aidat.ay }

        // 3. Site bilgisi - YENİ
        holder.tvSiteInfo?.text = aidat.siteInfo
        holder.tvSiteInfo?.visibility = if (aidat.siteId > 1) View.VISIBLE else View.GONE

        // 4. Tutar bilgisi - TOPLAM TUTARI GÖSTER
        val totalAmountText = "Toplam: ${aidat.formattedTotalAmount}"
        holder.tvTutar.text = totalAmountText

        // 5. Son tarih
        holder.tvSonTarih.text = "Son Tarih: ${formatDateForDisplay(aidat.son_tarih)}"

        // 6. Durum - KISMI ÖDEME DURUMUNU DA GÖSTER
        val statusText = buildString {
            append(aidat.formattedStatus)

            if (aidat.isPartiallyPaid) {
                append(" (Kısmi Ödendi)")
            }

            if (aidat.paymentStatus.isNotEmpty() && aidat.paymentStatus != "none") {
                append(" - ${aidat.formattedPaymentStatus}")
            }
        }
        holder.tvDurum.text = statusText

        // 7. DURUM ARKA PLAN RENGİ
        setStatusBackground(holder.tvDurum, aidat.durum, context, aidat.isReallyOverdue)

        // 8. KALAN BORÇ BİLGİSİ
        if (aidat.calculatedRemainingAmount > 0) {
            holder.tvKalanBorc.visibility = View.VISIBLE
            holder.tvKalanBorc.text = "Kalan Borç: ${aidat.formattedRemainingAmount}"

            // Renk: kısmi ödeme yeşil, hiç ödenmemiş kırmızı
            val colorRes = if (aidat.isPartiallyPaid) {
                R.color.colorGreen
            } else {
                R.color.colorRed
            }
            holder.tvKalanBorc.setTextColor(ContextCompat.getColor(context, colorRes))
        } else {
            holder.tvKalanBorc.visibility = View.GONE
        }

        // 9. ÖDENEN MİKTAR (kısmi ödemeler için)
        if (aidat.paidAmount > 0) {
            holder.tvOdenenMiktar.visibility = View.VISIBLE
            holder.tvOdenenMiktar.text = "Ödenen: ${aidat.formattedPaidAmount}"

            // Yüzdeyi göster
            if (aidat.isPartiallyPaid) {
                holder.tvPaymentStatus.visibility = View.VISIBLE
                holder.tvPaymentStatus.text = "Tamamlanma: %${aidat.paymentPercentage}"

                // Yüzdeye göre renk
                val percentageColor = when {
                    aidat.paymentPercentage >= 75 -> R.color.colorGreen
                    aidat.paymentPercentage >= 50 -> R.color.colorOrange
                    else -> R.color.colorYellow
                }
                holder.tvPaymentStatus.setTextColor(ContextCompat.getColor(context, percentageColor))
            } else {
                holder.tvPaymentStatus.visibility = View.GONE
            }
        } else {
            holder.tvOdenenMiktar.visibility = View.GONE
            holder.tvPaymentStatus.visibility = View.GONE
        }

        // 10. GECİKME CEZASI
        if (aidat.lateFeeAmount > 0 || aidat.hasLateFee) {
            holder.tvLateFee.visibility = View.VISIBLE

            val lateFeeText = buildString {
                append("⚠️ ")
                if (aidat.lateFeeAmount > 0) {
                    append("Gecikme Cezası: ${aidat.formattedLateFee}")
                } else {
                    append("Gecikme cezası hesaplanacak")
                }

                if (aidat.daysLate > 0) {
                    append(" (${aidat.daysLate} gün)")
                }
            }

            holder.tvLateFee.text = lateFeeText
            holder.tvLateFee.setTextColor(ContextCompat.getColor(context, R.color.colorRed))

            // Gecikme cezası ikonu
            val warningDrawable = ContextCompat.getDrawable(context, R.drawable.ic_warning)
            if (warningDrawable != null) {
                warningDrawable.setTint(ContextCompat.getColor(context, R.color.colorRed))
                holder.tvLateFee.setCompoundDrawablesWithIntrinsicBounds(
                    warningDrawable,
                    null, null, null
                )
                holder.tvLateFee.compoundDrawablePadding = 8
            }
        } else {
            holder.tvLateFee.visibility = View.GONE
        }

        // 11. ÖDEME TARİHİ (eğer ödendiyse)
        if (!aidat.odeme_tarihi.isNullOrEmpty()) {
            holder.tvPaymentDate.visibility = View.VISIBLE
            holder.tvPaymentDate.text = "Ödeme Tarihi: ${formatDateForDisplay(aidat.odeme_tarihi)}"
            holder.tvPaymentDate.setTextColor(ContextCompat.getColor(context, R.color.colorGreen))
        } else {
            holder.tvPaymentDate.visibility = View.GONE
        }

        // 12. ÖDEME BİLGİLERİ LAYOUT'UNU GÖSTER/GİZLE
        val hasPaymentInfo = aidat.paidAmount > 0 || aidat.lateFeeAmount > 0 || !aidat.odeme_tarihi.isNullOrEmpty()
        holder.layoutPaymentInfo.visibility = if (hasPaymentInfo) View.VISIBLE else View.GONE

        // 13. ÖDEME YAP BUTONU - YENİ
        holder.btnOdemeYap?.let { btn ->
            // Sadece ödenmemiş aidatlar için göster
            if (!aidat.isFullyPaid && aidat.calculatedRemainingAmount > 0) {
                btn.visibility = View.VISIBLE
                btn.text = "Ödeme Yap (${aidat.formattedRemainingAmount})"
                btn.setOnClickListener {
                    onOdemeClick(aidat)
                }
            } else {
                btn.visibility = View.GONE
            }
        }

        // 14. ITEM ARKA PLAN RENGİ
        setItemBackground(holder.itemView, aidat, context)

        // 15. TIKLAMA OLAYI
        holder.itemView.setOnClickListener {
            onItemClick(aidat)
        }

        // Debug için log
        println("🔍 Adapter - Aidat ID: ${aidat.id}, Site: ${aidat.siteId}")
        println("   - Amount: ${aidat.amount}, Paid: ${aidat.paidAmount}, Remaining: ${aidat.calculatedRemainingAmount}")
        println("   - Late Fee: ${aidat.lateFeeAmount}, Has Late Fee: ${aidat.hasLateFee}")
        println("   - Status: ${aidat.durum}, Is Overdue: ${aidat.isReallyOverdue}")
    }

    override fun getItemCount(): Int = aidatList.size

    fun setAidatList(newList: List<Aidat>) {
        aidatList = newList
        notifyDataSetChanged()

        // Debug: liste boyutu
        println("🔍 Adapter - Yeni liste: ${aidatList.size} öğe")
        if (aidatList.isNotEmpty()) {
            val first = aidatList.first()
            println("🔍 İlk öğe - ID: ${first.id}, Site: ${first.siteId}, Remaining: ${first.calculatedRemainingAmount}")
        }
    }

    // ============ YARDIMCI FONKSİYONLAR ============

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

    private fun setStatusBackground(textView: TextView, status: String, context: android.content.Context, isOverdue: Boolean = false) {
        val colorRes = when {
            isOverdue -> R.color.status_overdue
            status.lowercase() == "paid" -> R.color.status_paid
            status.lowercase() == "pending" -> R.color.status_pending
            else -> R.color.status_default
        }
        textView.setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    private fun setItemBackground(view: View, aidat: Aidat, context: android.content.Context) {
        val colorRes = when {
            aidat.isFullyPaid -> R.color.item_background_paid
            aidat.isPartiallyPaid -> R.color.item_background_partial
            aidat.isReallyOverdue -> R.color.item_background_overdue
            else -> R.color.item_background_default
        }
        view.setBackgroundColor(ContextCompat.getColor(context, colorRes))
    }

    // Liste filtreleme fonksiyonu
    fun filter(query: String, statusFilter: String = "Tümü"): List<Aidat> {
        return aidatList.filter { aidat ->
            val matchesSearch = query.isEmpty() ||
                    aidat.userName.contains(query, ignoreCase = true) ||
                    aidat.description.contains(query, ignoreCase = true) ||
                    aidat.apartmentFullInfo.contains(query, ignoreCase = true)

            val matchesStatus = when (statusFilter) {
                "Tümü" -> true
                "Ödendi" -> aidat.isFullyPaid
                "Bekliyor" -> aidat.isPending && !aidat.isPartiallyPaid
                "Gecikmiş" -> aidat.isReallyOverdue
                "Kısmi Ödendi" -> aidat.isPartiallyPaid
                else -> {
                    // Site filtreleme için (örn: "Site 1")
                    if (statusFilter.startsWith("Site ")) {
                        val siteNum = statusFilter.removePrefix("Site ").toIntOrNull()
                        siteNum == aidat.siteId
                    } else {
                        true
                    }
                }
            }

            matchesSearch && matchesStatus
        }
    }

    // Özet istatistikleri
    fun getSummary(): Map<String, Any> {
        var totalAmount = 0.0
        var totalPaid = 0.0
        var totalRemaining = 0.0
        var partialCount = 0
        var pendingCount = 0
        var paidCount = 0
        var overdueCount = 0

        // Site bazlı istatistikler - YENİ
        val siteStats = mutableMapOf<Int, MutableMap<String, Any>>()

        aidatList.forEach { aidat ->
            totalAmount += aidat.calculatedTotalAmount
            totalPaid += aidat.paidAmount
            totalRemaining += aidat.calculatedRemainingAmount

            when {
                aidat.isFullyPaid -> paidCount++
                aidat.isPartiallyPaid -> partialCount++
                aidat.isReallyOverdue -> overdueCount++
                aidat.isPending -> pendingCount++
            }

            // Site istatistikleri
            if (!siteStats.containsKey(aidat.siteId)) {
                siteStats[aidat.siteId] = mutableMapOf(
                    "count" to 0,
                    "total" to 0.0,
                    "paid" to 0.0,
                    "remaining" to 0.0
                )
            }

            val siteStat = siteStats[aidat.siteId]!!
            siteStat["count"] = siteStat.getOrElse("count") { 0 } as Int + 1
            siteStat["total"] = (siteStat.getOrElse("total") { 0.0 } as Double) + aidat.calculatedTotalAmount
            siteStat["paid"] = (siteStat.getOrElse("paid") { 0.0 } as Double) + aidat.paidAmount
            siteStat["remaining"] = (siteStat.getOrElse("remaining") { 0.0 } as Double) + aidat.calculatedRemainingAmount
        }

        return mapOf(
            "totalAmount" to totalAmount,
            "totalPaid" to totalPaid,
            "totalRemaining" to totalRemaining,
            "partialCount" to partialCount,
            "pendingCount" to pendingCount,
            "paidCount" to paidCount,
            "overdueCount" to overdueCount,
            "totalCount" to aidatList.size,
            "siteStats" to siteStats // YENİ: Site bazlı istatistikler
        )
    }
}