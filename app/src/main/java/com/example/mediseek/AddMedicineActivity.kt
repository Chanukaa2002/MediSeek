package com.example.mediseek

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.mediseek.service.ImageService
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddMedicineActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var supplyDateEditText: EditText
    private lateinit var expireDateEditText: EditText
    private lateinit var nameEditText: TextInputEditText
    private lateinit var brandEditText: TextInputEditText
    private lateinit var priceEditText: TextInputEditText
    private lateinit var batchEditText: TextInputEditText
    private lateinit var productImageView: ImageView
    private lateinit var addProductButton: Button
    private lateinit var cancelButton: Button
    private lateinit var progressBar: ProgressBar // Added for loading indication

    // Services and Data
    private lateinit var imageService: ImageService
    private var selectedImageUri: Uri? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Activity Result Launcher for picking an image from the gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                productImageView.setImageURI(uri) // Display the selected image
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_medicine)

        // Initialize Firebase and ImageService
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        imageService = ImageService.getInstance(this)

        // Bind UI elements
        bindViews()

        // Set listeners for UI interactions
        setupListeners()
    }

    private fun bindViews() {
        supplyDateEditText = findViewById(R.id.editText_supply_date)
        expireDateEditText = findViewById(R.id.editText_expire_date)
        nameEditText = findViewById(R.id.editText_name)
        brandEditText = findViewById(R.id.editText_brand)
        priceEditText = findViewById(R.id.editText_price)
        batchEditText = findViewById(R.id.editText_batch)
        productImageView = findViewById(R.id.imageView_product)
        addProductButton = findViewById(R.id.button_add_product)
        cancelButton = findViewById(R.id.button_cancel)
        // You might need to add a ProgressBar to your XML layout
        // For now, we'll assume it exists or handle it programmatically.
        // progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        // Date Picker Listeners
        supplyDateEditText.setOnClickListener { showDatePickerDialog(supplyDateEditText) }
        expireDateEditText.setOnClickListener { showDatePickerDialog(expireDateEditText) }

        // Image Picker Listener
        productImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
            pickImageLauncher.launch(intent)
        }

        // Button Listeners
        cancelButton.setOnClickListener { finish() } // Close the activity
        addProductButton.setOnClickListener { saveProduct() }
    }

    private fun showDatePickerDialog(dateEditText: EditText) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateEditText.setText(dateFormat.format(selectedDate.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun validateInputs(): Boolean {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Please select a product image", Toast.LENGTH_SHORT).show()
            return false
        }
        if (nameEditText.text.isNullOrEmpty() ||
            brandEditText.text.isNullOrEmpty() ||
            priceEditText.text.isNullOrEmpty() ||
            batchEditText.text.isNullOrEmpty() ||
            supplyDateEditText.text.isNullOrEmpty() ||
            expireDateEditText.text.isNullOrEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun saveProduct() {
        if (!validateInputs()) {
            return
        }

        val pharmacyId = auth.currentUser?.uid
        if (pharmacyId == null) {
            Toast.makeText(this, "User not logged in. Cannot add product.", Toast.LENGTH_LONG).show()
            return
        }

        // Show loading indicator
        // progressBar.visibility = View.VISIBLE

        selectedImageUri?.let { uri ->
            imageService.uploadProfileImage(
                userId = pharmacyId, // Using pharmacyId for organization
                imageUri = uri,
                onSuccess = { imageUrl ->
                    // Image uploaded successfully, now save data to Firestore
                    saveProductDataToFirestore(pharmacyId, imageUrl)
                },
                onError = { errorMessage ->
                    // Handle image upload error
                    // progressBar.visibility = View.GONE
                    Toast.makeText(this, "Image Upload Failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    private fun saveProductDataToFirestore(pharmacyId: String, imageUrl: String) {
        val medicineData = hashMapOf(
            "name" to nameEditText.text.toString(),
            "brand" to brandEditText.text.toString(),
            "price" to priceEditText.text.toString().toDoubleOrNull(),
            "batchNo" to batchEditText.text.toString(),
            "imgURL" to imageUrl,
            "SupD" to supplyDateEditText.text.toString(),
            "EXD" to expireDateEditText.text.toString(),
            "pharmacyId" to pharmacyId,
            "status" to "In Stock", // Default status
            "qty" to 100 // Default quantity, you can add a field for this
        )

        db.collection("medicines").document("product").collection("items")
            .add(medicineData)
            .addOnSuccessListener {
                // progressBar.visibility = View.GONE
                Toast.makeText(this, "Medicine added successfully!", Toast.LENGTH_SHORT).show()
                finish() // Go back to the previous screen
            }
            .addOnFailureListener { e ->
                // progressBar.visibility = View.GONE
                Toast.makeText(this, "Error adding medicine: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}