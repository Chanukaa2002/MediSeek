package com.example.mediseek.model

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pharmacy(
    // These property names now match your Firestore fields exactly
    var email: String = "",
    var registrationNumber: String = "",
    var username: String = "", // This holds the pharmacy's name

    // Use @PropertyName to map the 'userId' field in Firestore
    @get:PropertyName("userId") @set:PropertyName("userId")
    var pharmacyId: String = "",

    // Location is now correctly modeled as a list of strings
    var location: List<String> = emptyList()
) : Parcelable
