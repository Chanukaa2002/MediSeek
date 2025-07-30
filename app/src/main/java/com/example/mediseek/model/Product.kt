package com.example.mediseek.model

data class Product(
    val id: String, // Or Int, unique identifier for the product
    val imageName: Int, // To hold R.drawable.ordersimg or other drawable resource IDs
    // OR: val imageUrl: String? = null, // If you plan to load images from the internet
    val name: String,
    val stockStatus: String,
    val price: String // Storing price as a formatted string as in your XML
)
