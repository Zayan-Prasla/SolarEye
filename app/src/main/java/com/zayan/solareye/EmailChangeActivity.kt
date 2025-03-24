package com.zayan.solareye

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EmailChangeActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_change)

        val oldEmailEditText = findViewById<EditText>(R.id.oldEmailEditText)
        val newEmailEditText = findViewById<EditText>(R.id.newEmailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val submitButton = findViewById<Button>(R.id.submitEmailChangeButton)

        submitButton.setOnClickListener {
            val oldEmail = oldEmailEditText.text.toString().trim()
            val newEmail = newEmailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (oldEmail.isEmpty() || newEmail.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = auth.currentUser
            if (currentUser == null || currentUser.email != oldEmail) {
                Toast.makeText(this, R.string.user_not_logged_in, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val credential = EmailAuthProvider.getCredential(oldEmail, password)
            currentUser.reauthenticate(credential)
                .addOnSuccessListener {
                    currentUser.verifyBeforeUpdateEmail(newEmail)
                        .addOnSuccessListener {
                            // Update Firestore with the new email
                            val userId = currentUser.uid
                            val userRef = db.collection("users").document(userId)
                            userRef.update("email", newEmail)
                                .addOnSuccessListener {
                                    // Inform the user about the email change verification
                                    Toast.makeText(
                                        this,
                                        R.string.email_change_success,
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        getString(R.string.failed_to_update_firestore, e.message),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                getString(R.string.failed_to_update_email, e.message),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        getString(R.string.authentication_failed, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}
