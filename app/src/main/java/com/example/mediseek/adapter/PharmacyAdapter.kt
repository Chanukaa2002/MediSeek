package com.example.mediseek.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.model.Pharmacy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.Locale

class PharmacyAdapter(
    private var pharmacies: List<Pharmacy>,
    private val onItemClick: (Pharmacy) -> Unit
) : RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder>() {

    inner class PharmacyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ensure these IDs exist in your item_pharmacy.xml
        private val nameText: TextView = itemView.findViewById(R.id.pharmacyName)
        private val regNoText: TextView = itemView.findViewById(R.id.pharmacyRegNo)
        private val locationText: TextView = itemView.findViewById(R.id.pharmacyStore)
        private var geocodeJob: Job? = null

        @SuppressLint("SetTextI18n")
        fun bind(pharmacy: Pharmacy) {
            // FIXED: Using 'username' as the pharmacy name
            geocodeJob?.cancel()
            nameText.text = pharmacy.username
            regNoText.text = "Reg: ${pharmacy.registrationNumber}"

            // FIXED: Displaying location from the list
            val latString = pharmacy.location.getOrNull(0)
            val lonString = pharmacy.location.getOrNull(1)

            if (!latString.isNullOrEmpty() && !lonString.isNullOrEmpty()) {
                // Set a temporary loading text
                locationText.text = "Location: Loading..."
                // Launch a new coroutine and store its job
                geocodeJob = CoroutineScope(Dispatchers.Main).launch {
                    val cityName = getCityName(itemView.context, latString, lonString)
                    locationText.text = "Location: $cityName"
                }
            } else {
                locationText.text = "Location: Not Available"
            }



            itemView.setOnClickListener {
                Timber.d("Clicked: ${pharmacy.username}")
                onItemClick(pharmacy)
            }
        }
    }

    private suspend fun getCityName(context: Context, latString: String, lonString: String): String {
        return withContext(Dispatchers.IO) {
            val lat = latString.toDoubleOrNull()
            val lon = lonString.toDoubleOrNull()

            if (lat == null || lon == null) {
                return@withContext "Invalid Coordinates"
            }

            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    addresses[0].locality ?: "Unknown Area"
                } else {
                    "City not found"
                }
            } catch (e: Exception) {
                "Could not determine city"
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
