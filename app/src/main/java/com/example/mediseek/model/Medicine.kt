package com.example.mediseek.model

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Medicine(
    // This ID is set locally after fetching from Firestore. It is NOT a field in your database.
    var id: String = "",

    // --- Fields from your latest database structure ---
    var brand: String = "",
    var name: String = "",
    var pharmacyId: String = "",
    var price: Double = 0.0,
    var qty: Int = 0,
    var status: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,

    // Use @PropertyName for fields with different names, capitalization, or special characters
    @get:PropertyName("EXD") @set:PropertyName("EXD")
    var expiryDate: String = "",

    @get:PropertyName("SupD") @set:PropertyName("SupD")
    var supplierDate: String = "",

    @get:PropertyName("batchNo") @set:PropertyName("batchNo")
    var batchNo: String = "",

    @get:PropertyName("imgURL") @set:PropertyName("imgURL")
    var imageUrl: String = "",

    // This field is for making search case-insensitive.
    // You should add this field to your Firestore documents.
    @get:PropertyName("brand_lowercase") @set:PropertyName("brand_lowercase")
    var brandLowercase: String = ""
) : Parcelable
