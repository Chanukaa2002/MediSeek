package com.example.mediseek.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.AddMedicineActivity // Import the activity
import com.example.mediseek.R
import com.example.mediseek.adapter.ProductsAdapter
import com.example.mediseek.model.Product
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProductsFragment : Fragment(R.layout.fragment_products) {

    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private var productList: MutableList<Product> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView from the inflated view
        productsRecyclerView = view.findViewById(R.id.product_recycler_view)

        // --- MODIFIED NAVIGATION LOGIC ---
        // Find the FloatingActionButton
        val addDrugButton = view.findViewById<FloatingActionButton>(R.id.add_drug_button)

        // Set the click listener to start AddMedicineActivity
        addDrugButton.setOnClickListener {
            // Create an Intent to launch AddMedicineActivity
            val intent = Intent(requireActivity(), AddMedicineActivity::class.java)
            startActivity(intent)
        }
        // --- End of modification ---

        setupRecyclerView()
        loadProducts() // Call the method to load product data
    }

    private fun setupRecyclerView() {
        // The layout you provided uses a GridLayoutManager in the XML,
        // so we'll ensure it's set up correctly here as well.
        productsRecyclerView.layoutManager = GridLayoutManager(requireContext(), 3) // 3 columns as per your XML

        // Initialize the adapter with the (initially empty) product list
        productsAdapter = ProductsAdapter(productList)
        productsRecyclerView.adapter = productsAdapter
    }

    private fun loadProducts() {
        // This is your existing sample data logic.
        val sampleProducts = listOf(
            Product(
                id = "1",
                imageName = R.drawable.ordersimg,
                name = "Product Alpha",
                stockStatus = "IN STOCK",
                price = "Rs. 130.00"
            ),
            Product(
                id = "2",
                imageName = R.drawable.ordersimg,
                name = "Product Beta",
                stockStatus = "OUT OF STOCK",
                price = "Rs. 250.50"
            ),
            Product(
                id = "3",
                imageName = R.drawable.ordersimg,
                name = "Product Gamma",
                stockStatus = "IN STOCK",
                price = "Rs. 99.00"
            )
        )

        productList.clear()
        productList.addAll(sampleProducts)

        // Notify the adapter that the data has changed
        productsAdapter.updateData(productList)
    }
}