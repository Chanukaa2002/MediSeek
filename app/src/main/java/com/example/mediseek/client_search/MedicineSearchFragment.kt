import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.client_search.Pharmacy

class MedicineSearchFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_medicine_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val medicineRecyclerView: RecyclerView = view.findViewById(R.id.medicineRecyclerView)


        val pharmacyResults = listOf(
            Pharmacy("ABC Pharmacy"),
            Pharmacy("PQR Pharmacy"),
            Pharmacy("XYZ Pharmacy"),
            Pharmacy("XYZ Pharmacy"),
            Pharmacy("XYZ Pharmacy"),
            Pharmacy("XYZ Pharmacy")
        )


        medicineRecyclerView.adapter = PharmacyResultAdapter(pharmacyResults)
    }
}