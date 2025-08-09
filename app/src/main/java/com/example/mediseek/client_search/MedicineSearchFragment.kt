package com.example.mediseek.client_search

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediseek.R
import com.example.mediseek.adapter.MedicineAdapter
import com.example.mediseek.databinding.FragmentMedicineSearchBinding
import com.example.mediseek.model.Medicine
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber

class MedicineSearchFragment : Fragment() {

    private var _binding: FragmentMedicineSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MedicineAdapter
    private val db = FirebaseFirestore.getInstance()
    private var allMedicines: List<Medicine> = listOf()
    private var hasShownNoResults = false
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMedicineSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupRecyclerView()
        setupSearchView()
        setupSearchTextColors()
        requestLocationAndFetchMedicines()
    }

    private fun requestLocationAndFetchMedicines() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            getUserLocation()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocation()
        } else {
            Toast.makeText(context, "Location permission is required to sort medicines by distance.", Toast.LENGTH_SHORT).show()
            fetchAllMedicines() // fallback: fetch without location
        }
    }

    private fun getUserLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLat = location.latitude
                userLng = location.longitude
            }
            fetchAllMedicines()
        }.addOnFailureListener {
            fetchAllMedicines() // fallback: fetch without location
        }
    }

    private fun setupRecyclerView() {
        adapter = MedicineAdapter(emptyList()) { medicine ->
            val bundle = Bundle().apply {
                putString("medicineName", medicine.name)
            }
            findNavController().navigate(R.id.action_medicine_to_pharmacy, bundle)
        }
        binding.medicineRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.medicineRecyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterLocalMedicines(query ?: "")
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterLocalMedicines(newText ?: "")
                return true
            }
        })
    }

    private fun fetchAllMedicines() {
        binding.progressBar.visibility = View.VISIBLE
        db.collection("medicines")
            .get()
            .addOnSuccessListener { medicineDocs ->
                if (!isAdded) return@addOnSuccessListener
                if (medicineDocs.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "No medicine data available.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val medicinesList = mutableListOf<Medicine>()
                val pharmacyFetchTasks = mutableListOf<Task<DocumentSnapshot>>()

                for (doc in medicineDocs) {
                    val medicine = doc.toObject(Medicine::class.java).apply {
                        id = doc.id
                    }
                    // Prepare pharmacy fetch
                    val pharmacyTask = db.collection("pharmacy").document(medicine.pharmacyId).get()
                        .addOnSuccessListener { pharmacyDoc ->
                            if (pharmacyDoc.exists()) {
                                val locationList = pharmacyDoc.get("location") as? List<*>
                                if (locationList != null && locationList.size >= 2) {
                                    medicine.latitude = (locationList[0] as? String)?.toDoubleOrNull() ?: 0.0
                                    medicine.longitude = (locationList[1] as? String)?.toDoubleOrNull() ?: 0.0
                                }
                            }
                        }
                    pharmacyFetchTasks.add(pharmacyTask)
                    medicinesList.add(medicine)
                }

                // Wait until all pharmacy locations are fetched
                Tasks.whenAllSuccess<DocumentSnapshot>(pharmacyFetchTasks)
                    .addOnSuccessListener {
                        allMedicines = medicinesList
                        val sortedList = sortMedicinesByLocation(allMedicines, userLat, userLng)
                        adapter.updateData(sortedList)
                        binding.progressBar.visibility = View.GONE
                    }
                    .addOnFailureListener {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(context, "Error fetching pharmacy locations.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Could not load medicine data.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun filterLocalMedicines(query: String) {
        val searchQuery = query.trim()
        Timber.d("Filtering for: $searchQuery")

        if (searchQuery.isEmpty()) {
            val sortedList = sortMedicinesByLocation(allMedicines, userLat, userLng)
            adapter.updateData(sortedList)
            hasShownNoResults = false
            return
        }

        val filteredList = allMedicines.filter { medicine ->
            medicine.name.contains(searchQuery, ignoreCase = true)
        }
        val sortedFilteredList = sortMedicinesByLocation(filteredList, userLat, userLng)
        adapter.updateData(sortedFilteredList)

        if (filteredList.isEmpty() && !hasShownNoResults) {
            Toast.makeText(context, "No medicines found for '$searchQuery'.", Toast.LENGTH_SHORT).show()
            hasShownNoResults = true
        } else if (filteredList.isNotEmpty()) {
            hasShownNoResults = false
        }
    }

    private fun setupSearchTextColors() {
        try {
            val searchText = binding.searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            searchText.setHintTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
            searchText.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } catch (e: Exception) {
            Timber.e(e, "Error setting search text colors")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun distanceTo(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371e3
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaPhi = Math.toRadians(lat2 - lat1)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                Math.cos(phi1) * Math.cos(phi2) *
                Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    private fun sortMedicinesByLocation(medicines: List<Medicine>, userLat: Double, userLng: Double): List<Medicine> {
        return medicines.sortedBy { medicine ->
            distanceTo(userLat, userLng, medicine.latitude, medicine.longitude)
        }
    }
}