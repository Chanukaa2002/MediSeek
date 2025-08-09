package com.example.mediseek.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import com.example.mediseek.MainActivity
import com.example.mediseek.MapActivity // Import your MapActivity
import com.example.mediseek.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint

class PharmacyProfileFragment : Fragment(R.layout.fragment_pharmacy_profile) {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // UI Elements
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var regNumberTextView: TextView
    private lateinit var userIdTextView: TextView
    private lateinit var emailEditText: TextInputEditText
    private lateinit var locationTextView: TextView
    private lateinit var editLocationButton: ImageButton
    private lateinit var saveChangesButton: Button
    private lateinit var logoutButton: Button

    private var currentUserId: String? = null
    private var newLocation: GeoPoint? = null

    // --- MODIFIED: Modern way to handle activity results ---
    private val mapLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val lat = result.data?.getDoubleExtra("latitude", 0.0)
            val lng = result.data?.getDoubleExtra("longitude", 0.0)

            if (lat != null && lng != null && lat != 0.0) {
                // A new location was successfully received from MapActivity
                newLocation = GeoPoint(lat, lng)
                locationTextView.text = "Lat: ${String.format("%.4f", lat)}, Lng: ${String.format("%.4f", lng)}"
                Toast.makeText(context, "Location selected. Tap 'Save Changes' to confirm.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize services
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid

        // Bind views from the layout
        bindViews(view)
        setupListeners()
        loadPharmacyData()
    }

    private fun bindViews(view: View) {
        usernameEditText = view.findViewById(R.id.et_pharmacy_username)
        regNumberTextView = view.findViewById(R.id.tv_reg_number)
        userIdTextView = view.findViewById(R.id.tv_user_id)
        emailEditText = view.findViewById(R.id.et_pharmacy_email)
        locationTextView = view.findViewById(R.id.tv_current_location)
        editLocationButton = view.findViewById(R.id.btn_edit_location)
        saveChangesButton = view.findViewById(R.id.btn_save_changes)
        logoutButton = view.findViewById(R.id.btn_logout)
    }

    private fun setupListeners() {
        editLocationButton.setOnClickListener {
            // --- MODIFIED: Launch your existing MapActivity ---
            val intent = Intent(activity, MapActivity::class.java)
            mapLauncher.launch(intent)
        }
        saveChangesButton.setOnClickListener { saveProfileChanges() }
        logoutButton.setOnClickListener { logoutUser() }
    }

    private fun loadPharmacyData() {
        if (currentUserId == null) {
            Log.e("ProfileFragment", "User not logged in.")
            return
        }

        db.collection("pharmacy").document(currentUserId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    usernameEditText.setText(document.getString("username"))
                    regNumberTextView.text = document.getString("registrationNumber")
                    userIdTextView.text = document.getString("userId")
                    emailEditText.setText(document.getString("email"))

                    val locationArray = document.get("location") as? List<String>
                    if (locationArray != null && locationArray.size == 2) {
                        locationTextView.text = "Lat: ${locationArray[0]}, Lng: ${locationArray[1]}"
                    } else {
                        locationTextView.text = "No location set"
                    }
                } else {
                    Log.d("ProfileFragment", "No such document")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ProfileFragment", "Error loading profile", e)
                Toast.makeText(context, "Failed to load profile.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfileChanges() {
        if (currentUserId == null) return

        val pharmacyRef = db.collection("pharmacy").document(currentUserId!!)

        val updates = mutableMapOf<String, Any>(
            "username" to usernameEditText.text.toString(),
            "email" to emailEditText.text.toString()
        )

        // If a new location was selected from the map, add it to the update
        newLocation?.let {
            // Your location is stored as an array of strings, so we convert it
            updates["location"] = listOf(it.latitude.toString(), it.longitude.toString())
        }

        pharmacyRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(activity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
    }
}
