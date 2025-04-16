package com.zayan.solareye

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.util.Util
import java.security.MessageDigest

class EventHistoryAdapter(
    private val events: List<EventItem>,
    private val onThumbnailClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

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

    override fun getItemCount(): Int = events.size

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
            val context = itemView.context

            Glide.with(context)
                .load(event.thumbnail)
                .apply(
                    RequestOptions().transform(
                        MultiTransformation(
                            CenterCrop(),
                            Rotate180Transformation()
                        )
                    )
                )
                .into(eventThumbnail)

            detectionType.text = event.detectionType
            eventTime.text = event.time

            eventThumbnail.setOnClickListener {
                event.thumbnail?.let { videoUrl ->
                    onThumbnailClick(videoUrl)
                }
            }
        }
    }

    // Custom Glide Transformation to rotate bitmap by 180 degrees
    class Rotate180Transformation : Transformation<Bitmap> {

        override fun updateDiskCacheKey(messageDigest: MessageDigest) {
            messageDigest.update("rotate180Transformation".toByteArray())
        }

        override fun equals(other: Any?): Boolean = other is Rotate180Transformation

        override fun hashCode(): Int = Util.hashCode(javaClass.name.hashCode())

        override fun transform(
            context: Context,
            resource: Resource<Bitmap>,
            outWidth: Int,
            outHeight: Int
        ): Resource<Bitmap> {
            val source = resource.get()
            val matrix = Matrix().apply {
                postRotate(180f)
            }
            val rotatedBitmap = Bitmap.createBitmap(
                source, 0, 0,
                source.width, source.height,
                matrix, true
            )
            return BitmapResource.obtain(rotatedBitmap, Glide.get(context).bitmapPool)!!
        }
    }
}
