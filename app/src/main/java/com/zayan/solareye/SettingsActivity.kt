package com.zayan.solareye

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SettingsActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Logout Button
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Email Address Change Button - Navigate to EmailChangeActivity
        findViewById<Button>(R.id.emailChangeButton).setOnClickListener {
            val intent = Intent(this, EmailChangeActivity::class.java)
            startActivity(intent)
        }

        // FAQ Button - Navigate to FAQActivity
        findViewById<Button>(R.id.faqButton).setOnClickListener {
            val intent = Intent(this, FAQActivity::class.java)
            startActivity(intent)
        }
    }
}