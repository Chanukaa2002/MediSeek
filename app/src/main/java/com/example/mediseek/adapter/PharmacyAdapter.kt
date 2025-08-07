package com.example.mediseek.adapter

import android.annotation.SuppressLint
import android.util.Log
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
        private val nameText: TextView = itemView.findViewById(R.id.pharmacyName)
        private val regNoText: TextView = itemView.findViewById(R.id.pharmacyRegNo)
        private val locationText: TextView = itemView.findViewById(R.id.pharmacyStore)

        @SuppressLint("SetTextI18n")
        fun bind(pharmacy: Pharmacy) {
            nameText.text = pharmacy.name
            regNoText.text = "Reg: ${pharmacy.regNo}"
            locationText.text = "Location: ${pharmacy.location}"

            Log.d("PharmacyAdapter", "Binding: ${pharmacy.name} (ID: ${pharmacy.pharmacyId})")
            itemView.setOnClickListener {
                Log.d("PharmacyAdapter", "Clicked: ${pharmacy.name}")
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