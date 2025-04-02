package com.zayan.solareye

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class EventHistoryAdapter(private val events: List<EventItem>, private val onThumbnailClick: (String) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_EVENT = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (events[position].isHeader) TYPE_HEADER else TYPE_EVENT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.section_header, parent, false)
            SectionHeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.event_item, parent, false)
            EventViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val event = events[position]
        if (holder is SectionHeaderViewHolder) {
            holder.bind(event.headerTitle ?: "")
        } else if (holder is EventViewHolder) {
            holder.bind(event)
        }
    }

    override fun getItemCount(): Int {
        return events.size
    }

    inner class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val headerTitle: TextView = itemView.findViewById(R.id.sectionHeader)

        fun bind(title: String) {
            headerTitle.text = title
        }
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val eventThumbnail: ImageView = itemView.findViewById(R.id.eventThumbnail)
        private val detectionType: TextView = itemView.findViewById(R.id.detectionType)
        private val eventTime: TextView = itemView.findViewById(R.id.eventTime)

        fun bind(event: EventItem) {
            Glide.with(itemView.context).load(event.thumbnail).into(eventThumbnail)
            detectionType.text = event.detectionType
            eventTime.text = event.time

            eventThumbnail.setOnClickListener {
                event.thumbnail?.let { videoUrl ->
                    onThumbnailClick(videoUrl)
                }
            }
        }
    }
}