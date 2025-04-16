package com.zayan.solareye

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        val videoView = findViewById<VideoView>(R.id.videoView)
        val videoUrl = intent.getStringExtra("VIDEO_URL")

        if (videoUrl != null) {
            val uri = Uri.parse(videoUrl)
            videoView.setVideoURI(uri)

            val mediaController = MediaController(this)
            mediaController.setAnchorView(videoView)
            videoView.setMediaController(mediaController)

            videoView.setOnPreparedListener {
                videoView.start()
            }

            videoView.setOnErrorListener { _, _, _ ->
                Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show()
                true
            }

        } else {
            Toast.makeText(this, "No video URL provided", Toast.LENGTH_SHORT).show()
        }
    }
}
