package com.example.mediseek.model

// This data class now mirrors the structure needed to hold data from your Firestore 'Orders' collection.
data class Order(
    val id: String, // The document ID from Firestore
    val orderId: String, // Formatted Order ID (e.g., "OID : 12345")
    val itemsSummary: String, // From the 'prescription' field
    val totalPrice: String,   // Formatted total amount (e.g., "Rs. 1000.00")
    val orderDate: String,    // Formatted creation date
    val status: String,       // "Pending" or "Done"
    val statusBackgroundColor: Int // R.color.yellow or R.color.lightGreen
)
