package com.example.mediseek

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class AdminActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PharmacyAdapter
    private val db = FirebaseFirestore.getInstance()
    private val pendingPharmacies = mutableListOf<Pharmacy>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        title = "Admin Interface"

        recyclerView = findViewById(R.id.pharmacyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = PharmacyAdapter()
        recyclerView.adapter = adapter

        loadPendingPharmacies()
    }

    private fun loadPendingPharmacies() {
        db.collection("pharmacies")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { documents ->
                pendingPharmacies.clear()
                for (document in documents) {
                    val pharmacy = document.toObject(Pharmacy::class.java)
                    pharmacy.id = document.id
                    pendingPharmacies.add(pharmacy)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading pharmacies: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updatePharmacyStatus(pharmacyId: String, status: String) {
        db.collection("pharmacies")
            .document(pharmacyId)
            .update("status", status)
            .addOnSuccessListener {
                Toast.makeText(this, "Pharmacy $status successfully", Toast.LENGTH_SHORT).show()
                loadPendingPharmacies() // Refresh the list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    inner class PharmacyAdapter : RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder>() {
        inner class PharmacyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(R.id.pharmacyName)
            val regNumTextView: TextView = itemView.findViewById(R.id.pharmacyRegNum)
            val approveBtn: Button = itemView.findViewById(R.id.approveBtn)
            val rejectBtn: Button = itemView.findViewById(R.id.rejectBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmacyViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pharmacy_approval, parent, false)
            return PharmacyViewHolder(view)
        }

        override fun onBindViewHolder(holder: PharmacyViewHolder, position: Int) {
            val pharmacy = pendingPharmacies[position]
            holder.nameTextView.text = pharmacy.name
            holder.regNumTextView.text = pharmacy.registrationNumber

            holder.approveBtn.setOnClickListener {
                updatePharmacyStatus(pharmacy.id, "approved")
            }

            holder.rejectBtn.setOnClickListener {
                updatePharmacyStatus(pharmacy.id, "rejected")
            }
        }

        override fun getItemCount(): Int = pendingPharmacies.size
    }
}

data class Pharmacy(
    var id: String = "",
    val name: String = "",
    val registrationNumber: String = "",
    val status: String = "pending" // pending, approved, rejected
)