package com.zayan.solareye

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class CreateAccountActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_account)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val confirmPasswordEditText = findViewById<EditText>(R.id.confirmPasswordEditText)
        val createAccountButton = findViewById<Button>(R.id.createAccountButton)
        val backButton = findViewById<Button>(R.id.backButton)

        createAccountButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, R.string.passwords_do_not_match, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Password policy: 6 characters, 1 uppercase, 1 special character
            val passwordRegex = Regex("^(?=.*[A-Z])(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{6,}\$")
            if (!password.matches(passwordRegex)) {
                Toast.makeText(this, R.string.invalid_password_policy, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            createFirebaseAccount(email, password)
        }

        backButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Handle the physical back button using OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing to disable the physical back button
            }
        })
    }

    private fun createFirebaseAccount(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                        if (verificationTask.isSuccessful) {
                            Toast.makeText(this, R.string.verification_email_sent, Toast.LENGTH_LONG).show()

                            // Redirect to login screen after sending verification email
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, R.string.failed_to_send_verification_email, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, getString(R.string.account_creation_failed, task.exception?.message), Toast.LENGTH_SHORT).show()
                }
            }
    }
}