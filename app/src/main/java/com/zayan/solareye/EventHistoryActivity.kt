package com.zayan.solareye

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import java.text.SimpleDateFormat
import java.util.Locale

class EventHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var enlargedThumbnail: ImageView
    private lateinit var adapter: EventHistoryAdapter
    private val events = mutableListOf<EventItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_event_history)

        recyclerView = findViewById(R.id.eventHistoryRecyclerView)
        enlargedThumbnail = findViewById(R.id.enlargedThumbnail)

        adapter = EventHistoryAdapter(events) { videoUrl ->
            playVideo(videoUrl)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        enlargedThumbnail.setOnClickListener {
            enlargedThumbnail.visibility = View.GONE
        }

        // Initialize Live Stream Button
        val liveStreamButton = findViewById<Button>(R.id.liveStreamButton)
        liveStreamButton.setOnClickListener {
            // Placeholder for live stream functionality
            Toast.makeText(this, "Live Stream Button Clicked", Toast.LENGTH_SHORT).show()
        }

        // Ensure the user is authenticated before fetching events
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            fetchEventsFromFirestore()
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchEventsFromFirestore() {
        val db = FirebaseFirestore.getInstance()
        db.collection("video_sensor_data")
            .get()
            .addOnSuccessListener { result ->
                processEvents(result)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error getting documents: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processEvents(result: Iterable<QueryDocumentSnapshot>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val eventsMap = mutableMapOf<String, MutableList<EventItem>>()

        for (document in result) {
            val timestamp = document.getString("timestamp")
            val video_url = document.getString("video_url")
            val date = timestamp?.let { dateFormat.parse(it) }

            if (date != null && video_url != null) {
                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                val event = EventItem(detectionType = getString(R.string.event_type_motion_detected), time = time, thumbnail = video_url)

                if (eventsMap.containsKey(dateKey)) {
                    eventsMap[dateKey]?.add(event)
                } else {
                    eventsMap[dateKey] = mutableListOf(event)
                }
            }
        }

        val sortedDates = eventsMap.keys.sortedDescending()
        var position = 0
        for (date in sortedDates) {
            events.add(position, EventItem(headerTitle = date))
            position++
            val eventList = eventsMap[date] ?: emptyList()
            events.addAll(position, eventList)
            position += eventList.size
        }

        adapter.notifyItemRangeInserted(0, events.size)
    }

    private fun playVideo(videoUrl: String) {
        val intent = Intent(this, VideoPlayerActivity::class.java)
        intent.putExtra("VIDEO_URL", videoUrl)
        startActivity(intent)
    }
}