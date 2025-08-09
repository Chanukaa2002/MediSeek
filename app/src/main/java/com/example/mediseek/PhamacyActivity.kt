package com.example.mediseek

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mediseek.databinding.ActivityClientBinding
import com.example.mediseek.databinding.ActivityPhamacyBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PhamacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhamacyBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        if (userId != null) {
            val pharmacyRef = db.collection("pharmacy").document(userId)
            pharmacyRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val location = document.get("location") as? List<*>
                    if (location == null || location.size != 2) {
                        // Location not set, redirect to MapActivity
                        startActivity(Intent(this, AddLocationActivity::class.java))
                        showDashboard()
                    } else {
                        Toast.makeText(this, "Location already set", Toast.LENGTH_SHORT).show()
                        // Location already set, proceed to dashboard
                        showDashboard()
                    }
                } else {
                    Toast.makeText(this, "Pharmacy document does not exist", Toast.LENGTH_LONG).show()
                    // Optional: redirect to MapActivity or show an error
                    startActivity(Intent(this, MapActivity::class.java))
                    finish()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching document: ${exception.message}", Toast.LENGTH_LONG).show()
                // Optional: showDashboard() or redirect based on your logic
                showDashboard()
            }
        } else {
            // No user logged in
            showDashboard()
        }
    }

    private fun showDashboard() {
        binding = ActivityPhamacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_pharmacy) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationViewPharmacy.setupWithNavController(navController)

        val profileIcon = binding.toolbarPharmacy.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.profile_pic_p)

        // Set the click listener
        profileIcon.setOnClickListener {
            // Navigate to the PharmacyProfileFragment.
            // Ensure 'R.id.pharmacyProfileFragment' is the correct ID from your pharmacy_nav_graph.xml
            navController.navigate(R.id.pharmacyProfileFragment)
        }
    }

    override fun onBackPressed() {
        if (navController.currentDestination?.id == navController.graph.startDestinationId) {
            // If on the start destination, minimize the app
            moveTaskToBack(true)
        } else {
            // Otherwise, perform the default back navigation
            super.onBackPressed()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
