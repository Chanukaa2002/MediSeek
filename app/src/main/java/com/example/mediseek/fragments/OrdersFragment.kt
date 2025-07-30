package com.example.mediseek.fragments // Or your correct package

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// Import other necessary UI elements if you plan to interact with them
// import android.widget.EditText
// import android.widget.ImageButton
// import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.OrdersAdapter // Make sure this path is correct
import com.example.mediseek.model.Order         // Make sure this path is correct

// Rename this class to OrderListFragment if your tools:context is .OrderListFragment
class OrdersFragment : Fragment() {

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private var orderList: MutableList<Order> = mutableListOf()

    // Optional: Declare views for other UI elements if you need to interact with them
    // private lateinit var searchEditText: EditText
    // private lateinit var searchIconImageView: ImageView
    // private lateinit var qrScanButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // This should be R.layout.fragment_orders (the XML you just provided)
        val view = inflater.inflate(R.layout.fragment_orders, container, false)

        // Initialize RecyclerView from the inflated view
        ordersRecyclerView = view.findViewById(R.id.order_recycler_view) // Use the ID from your XML

        // Optional: Initialize other views
        // searchEditText = view.findViewById(R.id.et_search_id)
        // searchIconImageView = view.findViewById(R.id.iv_search_icon)
        // qrScanButton = view.findViewById(R.id.btn_qr_scan)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadOrders()

        // Optional: Set up listeners or logic for other UI elements
        // setupSearchFunctionality()
        // setupQrScanButton()
    }

    private fun setupRecyclerView() {
        // Check if layoutManager is already set in XML (it is in your case)
        if (ordersRecyclerView.layoutManager == null) {
            ordersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        ordersAdapter = OrdersAdapter(orderList)
        ordersRecyclerView.adapter = ordersAdapter
    }

    private fun loadOrders() {
        // Sample data - Replace with your actual data source
        // Ensure your color resources (R.color.yellow, R.color.green_status, etc.) are defined
        val sampleOrders = listOf(
            Order(
                orderId = "OID : 00012",
                itemsSummary = "Panadol 02 cards, Vitamin C",
                totalPrice = "$3.00",
                orderDate = "5 June, 2022",
                status = "Processing",
                statusBackgroundColor = R.color.yellow
            ),
            Order(
                orderId = "OID : 00015",
                itemsSummary = "Antibiotics, Painkillers",
                totalPrice = "$15.50",
                orderDate = "10 June, 2022",
                status = "Delivered",
                statusBackgroundColor = R.color.lightGreen // Define R.color.green_status
            ),
            Order(
                orderId = "OID : 00010",
                itemsSummary = "Bandages",
                totalPrice = "$2.00",
                orderDate = "1 June, 2022",
                status = "Cancelled",
                statusBackgroundColor = R.color.red // Define R.color.red_status
            )
        )

        orderList.clear()
        orderList.addAll(sampleOrders)
        ordersAdapter.updateData(orderList)
    }

    // Optional: Placeholder for search functionality
    // private fun setupSearchFunctionality() {
    //    searchIconImageView.setOnClickListener {
    //        val searchTerm = searchEditText.text.toString()
    //        // Implement search logic
    //    }
    //    // Add TextWatcher to et_search_id for real-time search, etc.
    // }

    // Optional: Placeholder for QR scan button functionality
    // private fun setupQrScanButton() {
    //    qrScanButton.setOnClickListener {
    //        // Implement QR code scanning logic
    //    }
    // }
}
