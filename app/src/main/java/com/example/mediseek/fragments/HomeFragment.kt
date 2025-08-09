package com.example.mediseek.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.mediseek.R
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    private lateinit var usersCountTextView: TextView
    private lateinit var pharmaciesCountTextView: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Initialize TextViews
        usersCountTextView = view.findViewById(R.id.users_count)
        pharmaciesCountTextView = view.findViewById(R.id.pharmacies_count)

        getUsersCount()
        getPharmaciesCount()

        return view
    }

    private fun getUsersCount() {
        db.collection("Users")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                usersCountTextView.text = count.toString()
            }
            .addOnFailureListener {
                usersCountTextView.text = "0"
            }
    }

    private fun getPharmaciesCount() {
        db.collection("pharmacy")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                pharmaciesCountTextView.text = count.toString()
            }
            .addOnFailureListener {
                pharmaciesCountTextView.text = "0"
            }
    }
}
