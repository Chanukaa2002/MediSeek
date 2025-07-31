package com.example.mediseek.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController // Ensure this import is present
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.ProductsAdapter
import com.example.mediseek.model.Product
import com.google.android.material.floatingactionbutton.FloatingActionButton // Import FloatingActionButton

class ProductsFragment : Fragment() {

    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var productsAdapter: ProductsAdapter
    private var productList: MutableList<Product> = mutableListOf()
    private lateinit var addProductFab: FloatingActionButton // Declare the FAB

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_products, container, false)

        productsRecyclerView = view.findViewById(R.id.product_recycler_view)
        addProductFab = view.findViewById(R.id.fab_add_product) // Initialize the FAB

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadProducts()

        // Set OnClickListener for the FAB
        addProductFab.setOnClickListener {
            // Navigate to AddDrugFragment using its ID from the navigation graph
            findNavController().navigate(R.id.nav_add_pro)
        }
    }

    private fun setupRecyclerView() {
        if (productsRecyclerView.layoutManager == null) {
            productsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
        productsAdapter = ProductsAdapter(productList)
        productsRecyclerView.adapter = productsAdapter
    }

    private fun loadProducts() {
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
        productsAdapter.updateData(productList) // Assuming ProductsAdapter has this method
        // If not, use: productsAdapter.notifyDataSetChanged() after modifying productList
    }
}
