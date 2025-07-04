import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.client_search.Pharmacy

class PharmacyAdapter(private val pharmacyList: List<Pharmacy>) :
    RecyclerView.Adapter<PharmacyAdapter.PharmacyViewHolder>() {

    inner class PharmacyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val pharmacyName: TextView = itemView.findViewById(R.id.pharmacyName)

        init {
            itemView.setOnClickListener {
                val pharmacy = pharmacyList[adapterPosition]
                // Navigate to OrderFragment with pharmacy name
                itemView.findNavController().navigate(
                    R.id.action_pharmacySearchFragment_to_orderFragment,
                    Bundle().apply {
                        putString("pharmacy_name", pharmacy.name)
                    }
                )
            }
        }
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