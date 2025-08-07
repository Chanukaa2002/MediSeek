package com.example.mediseek.model

import android.os.Parcelable
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pharmacy(
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("name_lowercase") @set:PropertyName("name_lowercase") var nameLowercase: String = "",
    @get:PropertyName("ph_id") @set:PropertyName("ph_id") var pharmacyId: String = "",
    @get:PropertyName("Reg_no") @set:PropertyName("Reg_no") var regNo: String = "",
    @get:PropertyName("location") @set:PropertyName("location") var location: String = "",
    @get:PropertyName("email") @set:PropertyName("email") var email: String = ""
) : Parcelable