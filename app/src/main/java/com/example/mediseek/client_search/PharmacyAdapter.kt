import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import com.example.mediseek.R
import com.example.mediseek.client_search.Pharmacy

class PharmacyAdapter(private val pharmacyList: List<Pharmacy>) :
    RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder>() {

    class PharmacyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pharmacyName: TextView = itemView.findViewById(R.id.pharmacyName)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PharmacyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pharmacy, parent, false)
        return PharmacyViewHolder(view)
    }

    override fun onBindViewHolder(holder: PharmacyViewHolder, position: Int) {
        val pharmacy = pharmacyList[position]
        holder.pharmacyName.text = pharmacy.name
    }

    override fun getItemCount() = pharmacyList.size
}