package com.zayan.solareye

import android.graphics.Matrix
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class VideoPlayerActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private lateinit var textureView: TextureView
    private var videoUrl: String? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        videoUrl = intent.getStringExtra("VIDEO_URL")
        textureView = findViewById(R.id.textureView)
        textureView.surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surfaceTexture: android.graphics.SurfaceTexture, width: Int, height: Int) {
        if (videoUrl != null) {
            val surface = Surface(surfaceTexture)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@VideoPlayerActivity, Uri.parse(videoUrl))
                setSurface(surface)
                setOnPreparedListener {
                    it.videoWidth
                    it.videoHeight
                    applyRotation(textureView, it.videoWidth, it.videoHeight)
                    it.start()
                }
                setOnErrorListener { _, _, _ ->
                    Toast.makeText(this@VideoPlayerActivity, "Error playing video", Toast.LENGTH_SHORT).show()
                    true
                }
                prepareAsync()
            }
        } else {
            Toast.makeText(this, "No video URL", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyRotation(view: TextureView, videoWidth: Int, videoHeight: Int) {
        val viewWidth = view.width.toFloat()
        val viewHeight = view.height.toFloat()

        val aspectRatioView = viewWidth / viewHeight
        val aspectRatioVideo = videoWidth.toFloat() / videoHeight.toFloat()

        var scaleX = 1f
        var scaleY = 1f

        if (aspectRatioVideo > aspectRatioView) {
            // Video is wider than view
            scaleY = aspectRatioView / aspectRatioVideo
        } else {
            // Video is taller than view
            scaleX = aspectRatioVideo / aspectRatioView
        }

        val pivotX = viewWidth / 2f
        val pivotY = viewHeight / 2f

        val matrix = Matrix().apply {
            // First rotate 180 degrees around center
            postRotate(180f, pivotX, pivotY)

            // Then scale to fit while maintaining aspect ratio
            postScale(scaleX, scaleY, pivotX, pivotY)
        }

        view.setTransform(matrix)
    }


    override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
        mediaPlayer?.release()
        return true
    }
    override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {}
}
