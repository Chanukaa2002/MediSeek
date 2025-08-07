package com.example.mediseek.model

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Medicine(
    // These property names match your Firestore fields exactly
    var brand: String = "",
    var name: String = "",
    var pharmacyId: String = "",
    var price: Double = 0.0, // Use Double for price
    var qty: Int = 0,        // Use Int for quantity
    var status: String = "",

    // Use @PropertyName for fields with different names or capitalization
    @get:PropertyName("EXD") @set:PropertyName("EXD")
    var expiryDate: String = "",

    @get:PropertyName("SupD") @set:PropertyName("SupD")
    var supplierDate: String = "",

    @get:PropertyName("batchNo") @set:PropertyName("batchNo")
    var batchNo: String = "",

    @get:PropertyName("imgURL") @set:PropertyName("imgURL")
    var imageUrl: String = "",

    // IMPORTANT: Add this field to your database for searching
    @get:PropertyName("brand_lowercase") @set:PropertyName("brand_lowercase")
    var brandLowercase: String = ""
) : Parcelable
