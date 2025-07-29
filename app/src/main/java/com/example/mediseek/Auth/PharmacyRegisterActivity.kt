package com.example.mediseek

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PharmacyRegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pharmacy_register)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        val registerButton = findViewById<MaterialButton>(R.id.pharmacy_register_button)
        registerButton.setOnClickListener {
            registerPharmacy()
        }
    }

    private fun registerPharmacy() {
        val username = findViewById<TextInputEditText>(R.id.username_input).text.toString()
        val email = findViewById<TextInputEditText>(R.id.email_input).text.toString()
        val password = findViewById<TextInputEditText>(R.id.password_input).text.toString()
        val regNumber = findViewById<TextInputEditText>(R.id.reg_number_input).text.toString()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || regNumber.isEmpty()) {
            showToast("Please fill all fields")
            return
        }

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val userMap = hashMapOf(
                            "userId" to userId,
                            "username" to username,
                            "email" to email,
                            "password" to password, // ⚠️ Don't store plain passwords in production
                            "registrationNumber" to regNumber,
                            "role" to "Pharmacy",
                            "status" to "pending"  // <-- Added Status Field
                        )
                        firestore.collection("Users").document(userId)
                            .set(userMap)
                            .addOnSuccessListener {
                                showToast("Pharmacy Registered Successfully")
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                showToast("Failed to save data: ${e.message}")
                            }
                    }
                } else {
                    showToast("Registration Failed: ${task.exception?.message}")
                }
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
