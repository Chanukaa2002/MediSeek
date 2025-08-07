package com.example.mediseek.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mediseek.R
import com.example.mediseek.model.Medicine

class MedicineAdapter(
    private var medicines: List<Medicine>,
    private val onItemClick: (Medicine) -> Unit
) : RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder>() {

    inner class MedicineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // Ensure these IDs exist in your item_medicine.xml
        private val nameText: TextView = itemView.findViewById(R.id.medicineNameText)
        private val priceText: TextView = itemView.findViewById(R.id.medicinePriceText)
        private val stockText: TextView = itemView.findViewById(R.id.medicineStockText)
        private val imageView: ImageView = itemView.findViewById(R.id.medicineImage)
        private val batchText: TextView = itemView.findViewById(R.id.batchText)
        private val expiryText: TextView = itemView.findViewById(R.id.expiryText)

        @SuppressLint("SetTextI18n")
        fun bind(medicine: Medicine) {
            // Bind data from the Medicine object to the views
            nameText.text = medicine.brand // Displaying the brand name
            priceText.text = "Rs. ${"%.2f".format(medicine.price)}"
            stockText.text = "Stock: ${medicine.qty}" // Using 'qty' from your database
            batchText.text = "Batch: ${medicine.batchNo}"
            expiryText.text = "Expiry: ${medicine.expiryDate}"

            // Load image using Glide
            if (medicine.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(medicine.imageUrl)
                    .placeholder(R.drawable.logo) // Optional: change to your placeholder
                    .error(R.drawable.logo)       // Optional: change to your error image
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.logo)
            }

            itemView.setOnClickListener {
                onItemClick(medicine)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine, parent, false)
        return MedicineViewHolder(view)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(medicines[position])
    }

    override fun getItemCount(): Int = medicines.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newMedicines: List<Medicine>) {
        this.medicines = newMedicines
        notifyDataSetChanged()
    }
}
