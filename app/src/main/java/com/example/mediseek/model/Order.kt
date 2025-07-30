package com.example.mediseek.model

data class Order(
    val orderId: String,
    val itemsSummary: String, // e.g., "Panadol 02 cards, Vitamin C"
    val totalPrice: String,   // e.g., "$3.00"
    val orderDate: String,    // e.g., "5 June, 2022"
    val status: String,       // e.g., "Processing"
    val statusBackgroundColor: Int // To hold R.color.yellow or other color resource ID
)
