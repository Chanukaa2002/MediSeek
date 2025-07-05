package com.example.mediseek

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        val loginText = findViewById<TextView>(R.id.login_text)
        loginText.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        val signUpButton = findViewById<MaterialButton>(R.id.sign_up_button)
        signUpButton.setOnClickListener {
            // Handle sign up logic here
            val intent = Intent(this, PhamacyActivity::class.java)
            startActivity(intent)
        }

        val googleSignUpButton = findViewById<MaterialButton>(R.id.google_sign_up_button)
        googleSignUpButton.setOnClickListener {
            // Handle Google sign up here
        }
    }
}