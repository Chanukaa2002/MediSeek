package com.example.mediseek.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.ProductsAdapter // Import your adapter
import com.example.mediseek.model.Product         // Import your Product model
import com.google.android.material.floatingactionbutton.FloatingActionButton

// This is the corrected fragment that handles both the product list and navigation.
class ProductsFragment : Fragment(R.layout.fragment_products) {

    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private var productList: MutableList<Product> = mutableListOf()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView from the inflated view
        productsRecyclerView = view.findViewById(R.id.product_recycler_view)

        // --- FIX: Added navigation logic ---
        // Find the FloatingActionButton
        val addDrugButton = view.findViewById<FloatingActionButton>(R.id.add_drug_button)

        // Set the click listener to navigate to the AddDrugFragment
        addDrugButton.setOnClickListener {
            parentFragmentManager.commit {
                setReorderingAllowed(true)
                // Replace the current fragment with AddDrugFragment
                // R.id.fragment_container should be the ID of the FragmentContainerView in your host activity
                replace(R.id.nav_add_pro, AddDrugFragment::class.java, null)
                // Add to back stack so the user can press 'back' to return to the product list
                addToBackStack("add_drug")
            }
        }
        // --- End of fix ---

        setupRecyclerView()
        loadProducts() // Call the method to load product data
    }

    private fun setupRecyclerView() {
        // You can set the LayoutManager in XML (app:layoutManager) or programmatically here.
        // If not set in XML for productsRecyclerView:
        if (productsRecyclerView.layoutManager == null) {
            // The layout you provided uses a GridLayoutManager in the XML,
            // so this check might not be necessary, but it's good practice.
            productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        // Initialize the adapter with the (initially empty) product list
        productsAdapter = ProductsAdapter(productList)
        productsRecyclerView.adapter = productsAdapter
    }

    private fun loadProducts() {
        // This is your existing sample data logic.
        val sampleProducts = listOf(
            Product(
                id = "1",
                imageName = R.drawable.ordersimg, // Example image, ensure it exists in res/drawable
                name = "Product Alpha",
                stockStatus = "IN STOCK",
                price = "Rs. 130.00"
            ),
            Product(
                id = "2",
                imageName = R.drawable.ordersimg, // Replace with actual different images if available
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
