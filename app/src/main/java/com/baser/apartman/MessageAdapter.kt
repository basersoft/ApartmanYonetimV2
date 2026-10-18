package com.baser.apartman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import android.util.Log
class MessageAdapter(
    private var messages: MutableList<Message> = mutableListOf(),
    private val isAdmin: Boolean = false,
    private val currentUserId: Int = 0,
    private val onMessageClick: (Message) -> Unit = {},
    private val onReplyClick: (Message) -> Unit = {},
    private val onDeleteClick: (Message) -> Unit = {},
    private val onMarkReadClick: (Message) -> Unit = {}
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardMessage)
        val tvSenderReceiver: TextView = itemView.findViewById(R.id.tvSenderReceiver)
        val tvSubject: TextView = itemView.findViewById(R.id.tvSubject)
        val tvPreview: TextView = itemView.findViewById(R.id.tvPreview)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val btnReply: MaterialButton = itemView.findViewById(R.id.btnReply)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        // Liste sınırlarını kontrol et
        if (position !in 0 until messages.size) {
            Log.w("MessageAdapter", "Geçersiz pozisyon: $position, liste boyutu: ${messages.size}")
            return
        }

        val message = messages[position]
        Log.d("MessageAdapter", "Pozisyon $position mesajı gösteriliyor: ID=${message.id}, Konu=${message.subject}")

        try {
            // Gönderen/Alıcı bilgisi
            val senderReceiverText = if (message.isOwnMessage) {
                "Ben → ${message.receiverName}"
            } else {
                "${message.senderName} → Ben"
            }
            holder.tvSenderReceiver.text = senderReceiverText

            holder.tvSubject.text = message.subject

            // Mesaj önizleme - basit şekilde
            val preview = message.message.take(80)
            holder.tvPreview.text = preview

            // Tarih formatla
            holder.tvDate.text = formatDate(message.createdAt)

            // Buton görünürlükleri
            holder.btnReply.visibility = if (message.canReply) View.VISIBLE else View.GONE
            holder.btnDelete.visibility = if (message.isOwnMessage || isAdmin) View.VISIBLE else View.GONE

            // Arka plan rengi
            if (message.status == "unread" && !message.isOwnMessage) {
                holder.cardView.setCardBackgroundColor(0xFFE3F2FD.toInt()) // Açık mavi
            } else {
                holder.cardView.setCardBackgroundColor(0xFFFFFFFF.toInt()) // Beyaz
            }

            // Tıklama dinleyicileri
            holder.cardView.setOnClickListener {
                Log.d("MessageAdapter", "Mesaj tıklandı: ${message.id}")
                onMessageClick(message)
            }

            holder.btnReply.setOnClickListener {
                Log.d("MessageAdapter", "Cevapla butonuna tıklandı: ${message.id}")
                onReplyClick(message)
            }

            holder.btnDelete.setOnClickListener {
                Log.d("MessageAdapter", "Sil butonuna tıklandı: ${message.id}")
                onDeleteClick(message)
            }

            // Uzun tıklama (okundu olarak işaretle)
            holder.cardView.setOnLongClickListener {
                if (isAdmin && message.status == "unread" && !message.isOwnMessage) {
                    Log.d("MessageAdapter", "Uzun tıklandı - okundu işaretle: ${message.id}")
                    onMarkReadClick(message)
                    true
                } else {
                    false
                }
            }

            Log.d("MessageAdapter", "Pozisyon $position başarıyla bağlandı")

        } catch (e: Exception) {
            Log.e("MessageAdapter", "onBindViewHolder hatası: ${e.message}", e)
            // Hata durumunda minimum bilgi göster
            holder.tvSubject.text = "Hata"
            holder.tvPreview.text = "Mesaj yüklenemedi"
            holder.tvDate.text = ""
        }
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun clearMessages() {
        messages.clear()
        notifyDataSetChanged()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date)
        } catch (e: Exception) {
            dateString
        }
    }
}