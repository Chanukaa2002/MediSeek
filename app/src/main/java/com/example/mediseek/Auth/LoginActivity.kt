// LoginActivity.kt
package com.example.mediseek

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val signUpText = findViewById<TextView>(R.id.sign_up_text)
        signUpText.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        val loginButton = findViewById<MaterialButton>(R.id.login_button)
        loginButton.setOnClickListener {
            // Handle login logic here
            val intent = Intent(this,ClientActivity::class.java)
            startActivity(intent)
        }

        val googleLoginButton = findViewById<MaterialButton>(R.id.google_login_button)
        googleLoginButton.setOnClickListener {
            // Handle Google login here
        }

        val forgotPassword = findViewById<TextView>(R.id.forgot_password)
        forgotPassword.setOnClickListener {
            // Handle forgot password flow
        }
        // Update the signUpText click listener in LoginActivity.kt
        signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}