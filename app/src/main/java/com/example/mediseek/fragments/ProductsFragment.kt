package com.example.mediseek.fragments

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.AddMedicineActivity
import com.example.mediseek.R
import com.example.mediseek.adapter.ProductsAdapter
import com.example.mediseek.model.Product
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProductsFragment : Fragment(R.layout.fragment_products), ProductsAdapter.OnItemClickListener {

    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var searchEditText: EditText
    private var fullProductList: MutableList<Product> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        productsRecyclerView = view.findViewById(R.id.product_recycler_view)
        searchEditText = view.findViewById(R.id.et_search_medicine)
        val addDrugButton = view.findViewById<FloatingActionButton>(R.id.add_drug_button)

        addDrugButton.setOnClickListener {
            val intent = Intent(requireActivity(), AddMedicineActivity::class.java)
            startActivity(intent)
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }
        })

        setupRecyclerView()
        loadProductsFromFirestore()
    }

    private fun setupRecyclerView() {
        productsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        productsAdapter = ProductsAdapter(mutableListOf(), this)
        productsRecyclerView.adapter = productsAdapter
    }

    override fun onItemClick(productId: String) {
        val dialog = EditProductDialogFragment.newInstance(productId)
        dialog.show(parentFragmentManager, EditProductDialogFragment.TAG)
    }

    private fun filter(query: String) {
        val filteredList = mutableListOf<Product>()
        for (product in fullProductList) {
            if (product.name.lowercase().contains(query.lowercase())) {
                filteredList.add(product)
            }
        }
        productsAdapter.updateData(filteredList)
    }

    /**
     * Determines the product status based on its expiration date and quantity.
     * This function now lives inside the fragment.
     */
    private fun determineStatus(expiryDateString: String, quantity: Long): String {
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val expiryDate = dateFormat.parse(expiryDateString)
            val currentDate = Date()
            if (expiryDate != null && expiryDate.before(currentDate)) {
                return "Expired"
            }
        } catch (e: Exception) {
            // Log error or handle invalid date format
        }

        return if (quantity < 10) "Out of Stock" else "In Stock"
    }

    private fun loadProductsFromFirestore() {
        db.collection("medicines")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("ProductsFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val newProductList = mutableListOf<Product>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        // Get the required fields from the document
                        val expiryDate = doc.getString("EXD") ?: ""
                        val quantity = doc.getLong("qty") ?: 0L

                        // Calculate the status dynamically
                        val status = determineStatus(expiryDate, quantity)

                        // Create the Product object with the calculated status
                        val product = Product(
                            id = doc.id,
                            name = doc.getString("name") ?: "No Name",
                            imgURL = doc.getString("imgURL") ?: "", // Correctly maps imgURL to imageName
                            stockStatus = status, // Use the dynamically calculated status
                            price = "Rs. ${String.format("%.2f", doc.getDouble("price") ?: 0.0)}"
                        )
                        newProductList.add(product)
                    }
                }
                fullProductList.clear()
                fullProductList.addAll(newProductList)
                filter(searchEditText.text.toString())
            }
    }
}
