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

class ProductsFragment : Fragment(R.layout.fragment_products) {

    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private lateinit var db: FirebaseFirestore
    private lateinit var searchEditText: EditText

    // This list holds all products fetched from Firestore, unfiltered.
    private var fullProductList: MutableList<Product> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Bind UI elements
        productsRecyclerView = view.findViewById(R.id.product_recycler_view)
        searchEditText = view.findViewById(R.id.et_search_medicine)
        val addDrugButton = view.findViewById<FloatingActionButton>(R.id.add_drug_button)

        // Set the click listener to start AddMedicineActivity
        addDrugButton.setOnClickListener {
            val intent = Intent(requireActivity(), AddMedicineActivity::class.java)
            startActivity(intent)
        }

        // Listener for the search bar
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filter(s.toString())
            }
        })

        setupRecyclerView()
        loadProductsFromFirestore() // Load initial data
    }

    private fun setupRecyclerView() {
        productsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        productsAdapter = ProductsAdapter(mutableListOf())
        productsRecyclerView.adapter = productsAdapter
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
                        val product = Product(
                            id = doc.id,
                            name = doc.getString("name") ?: "No Name",
                            imgURL = doc.getString("imgURL") ?: "",
                            stockStatus = doc.getString("status") ?: "N/A",
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
