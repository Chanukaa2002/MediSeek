package com.example.mediseek

import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var adminLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        title = "Admin Interface"
        adminLayout = findViewById(R.id.adminLayout)
        db = FirebaseFirestore.getInstance()

        loadPendingPharmacies()
    }

    private fun loadPendingPharmacies() {
        db.collection("Users")
            .whereEqualTo("role", "Pharmacy")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val username = doc.getString("username") ?: ""
                    val registrationNumber = doc.getString("registrationNumber") ?: ""
                    val userId = doc.id

                    // Create layout for each pharmacy entry
                    val container = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setPadding(10, 20, 10, 20)
                    }

                    // Pharmacy info text
                    val infoText = TextView(this).apply {
                        text = "$username - $registrationNumber - Approved?"
                        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                    }

                    // Approve button
                    val approveBtn = Button(this).apply {
                        text = "Approve"
                        setOnClickListener {
                            updateStatus(userId, "approved")
                        }
                    }

                    // No button
                    val rejectBtn = Button(this).apply {
                        text = "No"
                        setOnClickListener {
                            updateStatus(userId, "rejected")
                        }
                    }

                    container.addView(infoText)
                    container.addView(approveBtn)
                    container.addView(rejectBtn)

                    adminLayout.addView(container)
                }

                if (documents.isEmpty) {
                    val noData = TextView(this).apply {
                        text = "No pending pharmacies."
                        textSize = 18f
                        setPadding(0, 20, 0, 0)
                    }
                    adminLayout.addView(noData)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateStatus(userId: String, newStatus: String) {
        db.collection("Users").document(userId)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "Status updated to $newStatus", Toast.LENGTH_SHORT).show()
                recreate() // Refresh to remove the updated entry
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update status.", Toast.LENGTH_SHORT).show()
            }
    }
}