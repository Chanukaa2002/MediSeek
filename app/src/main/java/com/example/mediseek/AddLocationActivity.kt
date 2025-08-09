package com.example.mediseek

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mediseek.databinding.AddLocationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddLocationActivity : AppCompatActivity() {

    private lateinit var binding: AddLocationBinding
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    companion object {
        const val MAP_REQUEST_CODE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddLocationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonAddLocation.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivityForResult(intent, MAP_REQUEST_CODE)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == MAP_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            selectedLat = data?.getDoubleExtra("latitude", 0.0)
            selectedLng = data?.getDoubleExtra("longitude", 0.0)

            if (selectedLat != null && selectedLng != null) {
                showConfirmationDialog(selectedLat!!, selectedLng!!)
            } else {
                Toast.makeText(this, "Location not received!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showConfirmationDialog(lat: Double, lng: Double) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Confirm Location")
        builder.setMessage("Do you want to save this location?\n\nLatitude: $lat\nLongitude: $lng")

        builder.setPositiveButton("OK") { _: DialogInterface, _: Int ->
            saveLocationToFirestore(lat, lng)
        }

        builder.setNegativeButton("Cancel") { dialog: DialogInterface, _: Int ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    private fun saveLocationToFirestore(lat: Double, lng: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid

        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            val locationData = listOf(lat.toString(), lng.toString())

            db.collection("pharmacy").document(userId)
                .update("location", locationData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Location saved successfully", Toast.LENGTH_SHORT).show()
                    // Redirect to Pharmacy Dashboard
                    startActivity(Intent(this, PhamacyActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save location", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }
}