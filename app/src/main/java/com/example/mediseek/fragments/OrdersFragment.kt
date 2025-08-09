package com.example.mediseek.fragments // Or your correct package

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.google.android.material.tabs.TabLayout
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

// This class now handles launching the scanner and receiving the result.
class OrdersFragment : Fragment(R.layout.fragment_orders), OrdersAdapter.OnItemClickListener { // Pass layout to constructor

    private lateinit var ordersRecyclerView: RecyclerView
    private lateinit var ordersAdapter: OrdersAdapter
    private lateinit var tabLayout: TabLayout
    private var currentFilterStatus = "Pending"

    // Declare views for UI elements
    private lateinit var searchEditText: EditText
    private lateinit var searchIconImageView: ImageView
    private lateinit var qrScanButton: ImageButton

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var fullOrderList: MutableList<Order> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(ScannerFragment.REQUEST_KEY) { _, bundle ->
            val scannedValue = bundle.getString(ScannerFragment.BUNDLE_KEY)
            searchEditText.setText(scannedValue)
            if (!scannedValue.isNullOrEmpty()) {
                showResultDialog(scannedValue)
            }
        }
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

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views from the inflated layout
        ordersRecyclerView = view.findViewById(R.id.order_recycler_view)
        searchEditText = view.findViewById(R.id.et_search_id)
        searchIconImageView = view.findViewById(R.id.iv_search_icon)
        qrScanButton = view.findViewById(R.id.btn_qr_scan)
        tabLayout = view.findViewById(R.id.tab_layout_orders)

        setupRecyclerView()
        setupSearchListener()
        loadOrdersFromFirestore()
        setupTabLayoutListener()
        setupQrScanButton() // Set up the listener for our button
    }

    private fun setupTabLayoutListener() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilterStatus = tab?.text.toString()
                filterOrders(searchEditText.text.toString()) // Re-filter list when tab changes
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterOrders(s.toString())
            }
        })
    }

    override fun onItemClick(orderId: String) {
        // Show the details dialog when an order card is clicked
        OrderDetailsDialogFragment.newInstance(orderId)
            .show(parentFragmentManager, OrderDetailsDialogFragment.TAG)
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
        ordersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        ordersAdapter = OrdersAdapter(mutableListOf(),this)
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

    private fun filterOrders(query: String) {
        // First, filter by the selected tab status ("Pending" or "Done")
        val statusFilteredList = fullOrderList.filter {
            it.status.equals(currentFilterStatus, ignoreCase = true)
        }

        // Then, filter by the search query
        val finalList = if (query.isEmpty()) {
            statusFilteredList
        } else {
            statusFilteredList.filter {
                it.orderId.contains(query, ignoreCase = true)
            }
        }
        ordersAdapter.updateData(finalList)
    }


    private fun loadOrdersFromFirestore() {
        val currentPharmacyId = auth.currentUser?.uid
        if (currentPharmacyId == null) {
            Log.w("OrdersFragment", "User not logged in.")
            return
        }

        db.collection("Orders")
            .whereEqualTo("pharmacyId", currentPharmacyId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("OrdersFragment", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val newOrderList = mutableListOf<Order>()
                if (snapshots != null) {
                    for (doc in snapshots) {
                        val paymentStatus = doc.getString("paymentStatus") ?: ""
                        val status = if (paymentStatus == "completed") "Pending" else "Done"
                        val statusColor = if (status == "Pending") R.color.yellow else R.color.lightGreen

                        val orderIdFull = doc.getString("orderId") ?: ""
                        val formattedOrderId = "OID : ${orderIdFull}"

                        val createdAtTimestamp = doc.getTimestamp("createdAt") ?: Timestamp.now()
                        val formattedDate = SimpleDateFormat("d MMMM, yyyy", Locale.getDefault()).format(createdAtTimestamp.toDate())

                        val order = Order(
                            id = doc.id,
                            orderId = formattedOrderId,
                            itemsSummary = doc.getString("prescription") ?: "No description",
                            totalPrice = "Rs.${String.format("%.2f", doc.getDouble("totalAmount") ?: 0.0)}",
                            orderDate = formattedDate,
                            status = status,
                            statusBackgroundColor = statusColor
                        )
                        newOrderList.add(order)
                    }
                }
                fullOrderList.clear()
                fullOrderList.addAll(newOrderList)
                // Apply initial filter based on the default selected tab
                filterOrders(searchEditText.text.toString())
            }
    }
}
