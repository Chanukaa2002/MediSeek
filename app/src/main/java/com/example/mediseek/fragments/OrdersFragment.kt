package com.example.mediseek.fragments // Or your correct package

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.commit
import androidx.fragment.app.setFragmentResultListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.OrdersAdapter // Make sure this path is correct
import com.example.mediseek.model.Order
import androidx.navigation.fragment.findNavController

// This class now handles launching the scanner and receiving the result.
class OrdersFragment : Fragment(R.layout.fragment_orders) { // Pass layout to constructor

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private var orderList: MutableList<Order> = mutableListOf()

    // Declare views for UI elements
    private lateinit var searchEditText: EditText
    private lateinit var searchIconImageView: ImageView
    private lateinit var qrScanButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- STEP 1: Set up the listener to receive the result from ScannerFragment ---
        setFragmentResultListener(ScannerFragment.REQUEST_KEY) { _, bundle ->
            // A result has been received from the scanner.
            val scannedValue = bundle.getString(ScannerFragment.BUNDLE_KEY)

            // Populate the EditText with the scanned value.
            searchEditText.setText(scannedValue)

            if (!scannedValue.isNullOrEmpty()) {
                // --- NEW: Show the scanned value in a pop-up dialog ---
                showResultDialog(scannedValue)
            }
            // You could also trigger a search automatically here if you wish.
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views from the inflated layout
        ordersRecyclerView = view.findViewById(R.id.order_recycler_view)
        searchEditText = view.findViewById(R.id.et_search_id)
        searchIconImageView = view.findViewById(R.id.iv_search_icon)
        qrScanButton = view.findViewById(R.id.btn_qr_scan)

        setupRecyclerView()
        loadOrders()
        setupQrScanButton() // Set up the listener for our button
    }

    private fun showResultDialog(scannedValue: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("QR Code Result")
            .setMessage("The scanned value is:\n\n$scannedValue")
            .setPositiveButton("OK") { dialog, _ ->
                // When the user clicks OK, populate the search bar
                searchEditText.setText(scannedValue)
                dialog.dismiss()
            }
            .setCancelable(false) // Optional: prevent dismissing by tapping outside
            .show()
    }

    private fun setupRecyclerView() {
        if (ordersRecyclerView.layoutManager == null) {
            ordersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        }
        ordersAdapter = OrdersAdapter(orderList)
        ordersRecyclerView.adapter = ordersAdapter
    }

    // --- STEP 2: Set up the QR Scan Button's click listener ---
    private fun setupQrScanButton() {
        qrScanButton.setOnClickListener {
            launchScanner()
        }
    }

    // --- STEP 3: Logic to launch the ScannerFragment ---
    private fun launchScanner() {
        // Navigate to the ScannerFragment destination using its ID from the nav graph
        findNavController().navigate(R.id.nav_scanner)
    }

    private fun loadOrders() {
        // This is your existing sample data logic.
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
                statusBackgroundColor = R.color.lightGreen
            ),
            Order(
                orderId = "OID : 00010",
                itemsSummary = "Bandages",
                totalPrice = "$2.00",
                orderDate = "1 June, 2022",
                status = "Cancelled",
                statusBackgroundColor = R.color.red
            )
        )
        orderList.clear()
        orderList.addAll(sampleOrders)
        ordersAdapter.updateData(orderList) // Assuming your adapter has this method
    }
}
