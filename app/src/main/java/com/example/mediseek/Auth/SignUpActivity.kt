package com.example.mediseek

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Login text click handler
        findViewById<TextView>(R.id.login_text).setOnClickListener {
            navigateToLogin()
        }

        // Sign up button click handler
        findViewById<MaterialButton>(R.id.sign_up_button).setOnClickListener {
            handleSignUp()
        }

        // Google sign up button click handler
        findViewById<MaterialButton>(R.id.google_sign_up_button).setOnClickListener {
            handleGoogleSignUp()
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun handleSignUp() {
        val fullName = findViewById<TextInputEditText>(R.id.fullname_input).text.toString()
        val email = findViewById<TextInputEditText>(R.id.email_input).text.toString()
        val password = findViewById<TextInputEditText>(R.id.password_input).text.toString()
        val confirmPassword = findViewById<TextInputEditText>(R.id.confirm_password_input).text.toString()

        val radioGroup = findViewById<RadioGroup>(R.id.user_type_radio_group)
        val selectedId = radioGroup.checkedRadioButtonId
        val userType = findViewById<MaterialRadioButton>(selectedId).text.toString()

        // Validation
        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showToast("Please fill all fields")
            return
        }

        if (password != confirmPassword) {
            showToast("Passwords don't match")
            return
        }

        // Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        saveUserData(userId, fullName, email, password, userType)
                    }
                } else {
                    showToast("Sign Up Failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserData(userId: String, fullName: String, email: String, password: String, userType: String) {
        val userMap = hashMapOf(
            "userId" to userId,
            "username" to fullName,
            "email" to email,
            "password" to password,  // Note: Storing raw password is NOT recommended in production!
            "role" to userType
        )

        firestore.collection("Users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                showToast("Sign Up Successful")
                when (userType) {
                    "Client" -> navigateTo(ClientActivity::class.java)
                    "Pharmacy" -> navigateTo(PhamacyActivity::class.java)
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed to save data: ${e.message}")
            }
    }

    private fun handleGoogleSignUp() {
        val intent = Intent(this, PharmacyRegisterActivity::class.java)
        startActivity(intent)
    }

    private fun navigateTo(activity: Class<*>) {
        val intent = Intent(this, activity)
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
