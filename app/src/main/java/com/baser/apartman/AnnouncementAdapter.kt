package com.baser.apartman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AnnouncementAdapter(private val announcements: List<Announcement>) :
    RecyclerView.Adapter<AnnouncementAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvContent: TextView = itemView.findViewById(R.id.tvContent)
        val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthor)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_announcement, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val announcement = announcements[position]

        holder.tvTitle.text = announcement.title
        holder.tvContent.text = announcement.content
        holder.tvAuthor.text = "Yazar: ${announcement.authorName}"
        holder.tvDate.text = announcement.createdAt
    }

    override fun getItemCount() = announcements.size
}