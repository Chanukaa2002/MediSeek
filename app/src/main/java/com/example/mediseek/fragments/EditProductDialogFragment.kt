package com.example.mediseek.fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.example.mediseek.R
import com.example.mediseek.service.ImageService
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class EditProductDialogFragment : DialogFragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var imageService: ImageService
    private var productId: String? = null
    private var selectedImageUri: Uri? = null
    private var isNewImageSelected = false

    // UI Views
    private lateinit var nameEditText: TextInputEditText
    private lateinit var brandEditText: TextInputEditText
    private lateinit var priceEditText: TextInputEditText
    private lateinit var batchEditText: TextInputEditText
    private lateinit var supDateEditText: EditText
    private lateinit var expDateEditText: EditText
    private lateinit var currentQtyTextView: TextView
    private lateinit var productImageView: ImageView

    // Activity Result Launcher for picking an image
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                isNewImageSelected = true
                productImageView.setImageURI(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        productId = arguments?.getString(ARG_PRODUCT_ID)
        db = FirebaseFirestore.getInstance()
        imageService = ImageService.getInstance(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_edit_product, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupListeners(view)
        loadProductData()
    }

    private fun bindViews(view: View) {
        nameEditText = view.findViewById(R.id.editText_edit_name)
        brandEditText = view.findViewById(R.id.editText_edit_brand)
        priceEditText = view.findViewById(R.id.editText_edit_price)
        batchEditText = view.findViewById(R.id.editText_edit_batch)
        supDateEditText = view.findViewById(R.id.editText_edit_sup_date)
        expDateEditText = view.findViewById(R.id.editText_edit_exp_date)
        currentQtyTextView = view.findViewById(R.id.textView_current_qty)
        productImageView = view.findViewById(R.id.imageView_edit_product)
    }

    private fun setupListeners(view: View) {
        view.findViewById<Button>(R.id.button_cancel_edit).setOnClickListener { dismiss() }
        view.findViewById<Button>(R.id.button_add_stock).setOnClickListener { addStock(view) }
        view.findViewById<Button>(R.id.button_save_changes).setOnClickListener { saveChanges() }
        productImageView.setOnClickListener { openGallery() }
        supDateEditText.setOnClickListener { showDatePickerDialog(supDateEditText) }
        expDateEditText.setOnClickListener { showDatePickerDialog(expDateEditText) }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply { type = "image/*" }
        pickImageLauncher.launch(intent)
    }

    private fun loadProductData() {
        productId?.let { id ->
            db.collection("medicines").document(id)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc != null) {
                        nameEditText.setText(doc.getString("name"))
                        brandEditText.setText(doc.getString("brand"))
                        priceEditText.setText(doc.getDouble("price")?.toString())
                        batchEditText.setText(doc.getString("batchNo"))
                        supDateEditText.setText(doc.getString("SupD"))
                        expDateEditText.setText(doc.getString("EXD"))
                        currentQtyTextView.text = "Current Quantity: ${doc.getLong("qty") ?: 0}"

                        Glide.with(this).load(doc.getString("imgURL")).into(productImageView)
                    }
                }
        }
    }

    private fun addStock(view: View) {
        val stockToAdd = view.findViewById<TextInputEditText>(R.id.editText_add_stock).text.toString().toLongOrNull()
        if (stockToAdd == null || stockToAdd <= 0) {
            Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        productId?.let { id ->
            db.collection("medicines").document(id)
                .update("qty", FieldValue.increment(stockToAdd))
                .addOnSuccessListener {
                    Toast.makeText(context, "Stock added successfully!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error updating stock: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveChanges() {
        if (isNewImageSelected) {
            selectedImageUri?.let { uri ->
                imageService.uploadProfileImage(productId!!, uri,
                    onSuccess = { imageUrl ->
                        updateProductData(imageUrl)
                    },
                    onError = { error ->
                        Toast.makeText(context, "Image upload failed: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        } else {
            updateProductData(null) // No new image, pass null
        }
    }

    private fun updateProductData(newImageUrl: String?) {
        val productRef = db.collection("medicines").document(productId!!)

        val updates = mutableMapOf<String, Any>(
            "name" to nameEditText.text.toString(),
            "brand" to brandEditText.text.toString(),
            "price" to (priceEditText.text.toString().toDoubleOrNull() ?: 0.0),
            "batchNo" to batchEditText.text.toString(),
            "SupD" to supDateEditText.text.toString(),
            "EXD" to expDateEditText.text.toString()
        )

        newImageUrl?.let {
            updates["imgURL"] = it
        }

        productRef.update(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error updating product: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePickerDialog(dateEditText: EditText) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val selectedDate = Calendar.getInstance().apply { set(year, month, day) }
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateEditText.setText(dateFormat.format(selectedDate.time))
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    companion object {
        const val TAG = "EditProductDialog"
        private const val ARG_PRODUCT_ID = "product_id"

        fun newInstance(productId: String): EditProductDialogFragment {
            val args = Bundle()
            args.putString(ARG_PRODUCT_ID, productId)
            val fragment = EditProductDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}