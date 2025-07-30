package com.example.mediseek.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.ProductsAdapter // Import your adapter
import com.example.mediseek.model.Product         // Import your Product model

class ProductsFragment : Fragment() {

    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private var productList: MutableList<Product> = mutableListOf() // Initialize an empty list

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_products, container, false)

        // Initialize RecyclerView from the inflated view
        productsRecyclerView = view.findViewById(R.id.product_recycler_view)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadProducts() // Call the method to load product data
    }

    private fun setupRecyclerView() {
        // You can set the LayoutManager in XML (app:layoutManager) or programmatically here.
        // If not set in XML for productsRecyclerView:
        if (productsRecyclerView.layoutManager == null) {
            productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        // Initialize the adapter with the (initially empty) product list
        productsAdapter = ProductsAdapter(productList)
        productsRecyclerView.adapter = productsAdapter
    }

    private fun loadProducts() {
        // --- THIS IS WHERE YOU GET YOUR PRODUCT DATA ---
        // This is sample data. Replace this with your actual data source
        // (e.g., fetching from a database, network API, ViewModel, etc.)

        // Make sure your Product model and R.drawable.ordersimg exist
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
            // Add more products as needed
        )

        productList.clear() // Clear existing data if any
        productList.addAll(sampleProducts) // Add new sample data

        // Notify the adapter that the data has changed
        // If your adapter has an updateData method like the one we discussed:
        productsAdapter.updateData(productList)
        // Or, if you directly modified the list passed to the adapter during construction
        // and your adapter doesn't have an updateData function, you might do:
        // productsAdapter.notifyDataSetChanged()
    }
}

