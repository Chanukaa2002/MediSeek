package com.example.mediseek.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.model.Pharmacy
import timber.log.Timber

class PharmacyAdapter(
    private var pharmacies: List<Pharmacy>,
    private val onItemClick: (Pharmacy) -> Unit
) : RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder>() {

    inner class PharmacyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ensure these IDs exist in your item_pharmacy.xml
        private val nameText: TextView = itemView.findViewById(R.id.pharmacyName)
        private val regNoText: TextView = itemView.findViewById(R.id.pharmacyRegNo)
        private val locationText: TextView = itemView.findViewById(R.id.pharmacyStore)

        @SuppressLint("SetTextI18n")
        fun bind(pharmacy: Pharmacy) {
            // FIXED: Using 'username' as the pharmacy name
            nameText.text = pharmacy.username
            regNoText.text = "Reg: ${pharmacy.registrationNumber}"

            // FIXED: Displaying location from the list
            if (pharmacy.location.isNotEmpty()) {
                // You can format this however you like
                locationText.text = "Location: Lat ${pharmacy.location.getOrNull(0)}, Lon ${pharmacy.location.getOrNull(1)}"
            } else {
                locationText.text = "Location: Not Available"
            }

            itemView.setOnClickListener {
                Timber.d("Clicked: ${pharmacy.username}")
                onItemClick(pharmacy)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmacyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pharmacy, parent, false)
        return PharmacyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PharmacyViewHolder, position: Int) {
        holder.bind(pharmacies[position])
    }

    override fun getItemCount(): Int = pharmacies.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPharmacies: List<Pharmacy>) {
        Timber.tag("PharmacyAdapter").d("Updating data with ${newPharmacies.size} items")
        this.pharmacies = newPharmacies
        notifyDataSetChanged()
    }
}
