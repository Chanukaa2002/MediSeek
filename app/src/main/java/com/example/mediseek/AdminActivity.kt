// File: AdminActivity.kt
package com.example.mediseek

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AdminActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        title = "Admin Interface"
    }
}
