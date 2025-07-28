package com.example.mediseek

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val usernameInput = findViewById<TextInputEditText>(R.id.username_input)
        val passwordInput = findViewById<TextInputEditText>(R.id.password_input)
        val loginButton = findViewById<MaterialButton>(R.id.login_button)
        val signUpText = findViewById<TextView>(R.id.sign_up_text)

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showToast("Please enter username and password")
                return@setOnClickListener
            }

            // This now calls the secure login function
            loginWithUsername(username, password)
        }

        signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun loginWithUsername(username: String, password: String) {
        // Step 1: Find the user document in Firestore to get their email
        firestore.collection("Users")
            .whereEqualTo("fullName", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showToast("Username not found")
                    return@addOnSuccessListener
                }

                // Get the email from the document
                val email = documents.documents[0].getString("email")

                if (email == null) {
                    showToast("Error: Email not found for this user.")
                    return@addOnSuccessListener
                }

                // Step 2: Use the email to sign in with Firebase Authentication
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Login successful, currentUser is now correctly set.
                            showToast("Login Successful")

                            // You can still use the role from the document to navigate
                            val role = documents.documents[0].getString("role")
                            when (role) {
                                "Client" -> navigateTo(ClientActivity::class.java)
                                "Pharmacy" -> navigateTo(PhamacyActivity::class.java)
                                else -> showToast("Role not found")
                            }
                        } else {
                            // This handles wrong password, etc.
                            showToast("Login Failed: ${task.exception?.message}")
                        }
                    }
            }
            .addOnFailureListener {
                showToast("Error finding user: ${it.message}")
            }
    }

    private fun navigateTo(activity: Class<*>) {
        val intent = Intent(this, activity)
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}