package com.zayan.solareye

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class LiveStreamActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_stream)
        supportActionBar?.title = "Live Streaming"
    }
}
