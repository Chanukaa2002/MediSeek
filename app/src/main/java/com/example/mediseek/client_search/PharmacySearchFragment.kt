import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.client_search.Pharmacy

class PharmacySearchFragment : Fragment() {

    private lateinit var pharmacyRecyclerView: RecyclerView
    private lateinit var pharmacyAdapter: PharmacyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_pharmacy_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pharmacyRecyclerView = view.findViewById(R.id.pharmacyRecyclerView)
        pharmacyRecyclerView.layoutManager = LinearLayoutManager(context)


        val pharmacies = listOf(
            Pharmacy("ABC Pharmacy"),
            Pharmacy("PQR Pharmacy"),
            Pharmacy("XYZ Pharmacy"),
            Pharmacy("XYZ Pharmacy"),
            Pharmacy("XYZ Pharmacy"),
            Pharmacy("XYZ Pharmacy")
        )


        pharmacyAdapter = PharmacyAdapter(pharmacies)
        pharmacyRecyclerView.adapter = pharmacyAdapter
    }
}