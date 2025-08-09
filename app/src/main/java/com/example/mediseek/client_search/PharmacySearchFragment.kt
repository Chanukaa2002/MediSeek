package com.example.mediseek.client_search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediseek.R
import com.example.mediseek.adapter.PharmacyAdapter
import com.example.mediseek.databinding.FragmentPharmacySearchBinding
import com.example.mediseek.model.Pharmacy
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class PharmacySearchFragment : Fragment() {

    private var _binding: FragmentPharmacySearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PharmacyAdapter
    private val db = FirebaseFirestore.getInstance()
    private var allPharmacies: List<Pharmacy> = emptyList()
    private var hasShownNoResults = false
    private var userLat = 0.0
    private var userLng = 0.0
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPharmacySearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        requestLocationAndLoadData()
    }

    private fun setupRecyclerView() {
        adapter = PharmacyAdapter(emptyList()) { pharmacy ->
            val bundle = Bundle().apply {
                putString("pharmacyId", pharmacy.pharmacyId)
                putString("pharmacyName", pharmacy.username)
                putString("medicineName", arguments?.getString("medicineName"))
            }
            findNavController().navigate(R.id.action_pharmacy_to_order, bundle)
        }
        binding.pharmacyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.pharmacyRecyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterPharmacies(query ?: "")
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterPharmacies(newText ?: "")
                return true
            }
        })
    }

    private fun loadInitialData() {
        binding.progressBar.visibility = View.VISIBLE
        val medicineName = arguments?.getString("medicineName") ?: ""
        val pharmacyIdFromArgs = arguments?.getString("pharmacyId") ?: ""

        binding.title.text = when {
            medicineName.isNotEmpty() -> "Pharmacies with: $medicineName"
            pharmacyIdFromArgs.isNotEmpty() -> "Pharmacy Details"
            else -> "All Pharmacies"
        }

        when {
            pharmacyIdFromArgs.isNotEmpty() -> loadSinglePharmacy(pharmacyIdFromArgs)
            medicineName.isNotEmpty() -> loadPharmaciesWithMedicine(medicineName)
            else -> loadAllPharmacies()
        }
    }

    private fun loadSinglePharmacy(pharmacyId: String) {
        Timber.d("Loading single pharmacy: $pharmacyId")
        db.collection("pharmacy").document(pharmacyId).get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                if (document.exists()) {
                    val pharmacy = document.toObject(Pharmacy::class.java)
                    if (pharmacy != null) {
                        allPharmacies = listOf(pharmacy)
                        val sorted = sortPharmaciesByDistance(allPharmacies)
                        adapter.updateData(sorted)
                    }
                } else {
                    Toast.makeText(context, "Pharmacy not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Timber.e(e, "Error loading pharmacy")
                Toast.makeText(context, "Failed to load pharmacy details.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPharmaciesWithMedicine(medicineName: String) {
        binding.progressBar.visibility = View.VISIBLE
        Timber.d("Loading pharmacies for medicine: $medicineName")
        db.collection("medicines")
            .whereEqualTo("name", medicineName)
            .get()
            .addOnSuccessListener { medicineDocs ->
                if (!isAdded) return@addOnSuccessListener
                if (medicineDocs.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "No pharmacies carry this medicine.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val pharmacyIds = medicineDocs.mapNotNull { it.getString("pharmacyId") }.distinct()

                if (pharmacyIds.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "No associated pharmacies found.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val pharmacyChunks = pharmacyIds.chunked(30)
                val pharmaciesFound = mutableListOf<Pharmacy>()
                var chunksProcessed = 0

                for (chunk in pharmacyChunks) {
                    db.collection("pharmacy").whereIn("userId", chunk).get()
                        .addOnSuccessListener { pharmacyDocs ->
                            if (!isAdded) return@addOnSuccessListener
                            pharmaciesFound.addAll(pharmacyDocs.toObjects(Pharmacy::class.java))
                            chunksProcessed++
                            if (chunksProcessed == pharmacyChunks.size) {
                                binding.progressBar.visibility = View.GONE
                                allPharmacies = pharmaciesFound
                                val sorted = sortPharmaciesByDistance(allPharmacies)
                                adapter.updateData(sorted)
                            }
                        }
                        .addOnFailureListener { e ->
                            if (!isAdded) return@addOnFailureListener
                            chunksProcessed++
                            Timber.e(e, "Error loading a chunk of pharmacies by ID")
                            if (chunksProcessed == pharmacyChunks.size) {
                                binding.progressBar.visibility = View.GONE
                                val sorted = sortPharmaciesByDistance(pharmaciesFound)
                                adapter.updateData(sorted)
                                Toast.makeText(context, "Could not load all pharmacy details.", Toast.LENGTH_SHORT).show()
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Timber.e(e, "Error finding medicine")
                Toast.makeText(context, "An error occurred while searching.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAllPharmacies() {
        binding.progressBar.visibility = View.VISIBLE
        Timber.d("Loading all pharmacies")
        db.collection("pharmacy")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener
                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    Toast.makeText(context, "No pharmacies available.", Toast.LENGTH_SHORT).show()
                } else {
                    allPharmacies = documents.toObjects(Pharmacy::class.java)
                    val sorted = sortPharmaciesByDistance(allPharmacies)
                    adapter.updateData(sorted)
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                binding.progressBar.visibility = View.GONE
                Timber.e(e, "Error loading all pharmacies")
                Toast.makeText(context, "Failed to load pharmacies.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterPharmacies(query: String) {
        val filtered = if (query.isEmpty()) {
            hasShownNoResults = false
            allPharmacies
        } else {
            allPharmacies.filter { pharmacy ->
                pharmacy.username.contains(query, ignoreCase = true)
            }
        }
        val sorted = sortPharmaciesByDistance(filtered)
        adapter.updateData(sorted)

        if (filtered.isEmpty() && query.isNotEmpty() && !hasShownNoResults) {
            Toast.makeText(context, "No pharmacies match your search.", Toast.LENGTH_SHORT).show()
            hasShownNoResults = true
        } else if (filtered.isNotEmpty()) {
            hasShownNoResults = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun requestLocationAndLoadData() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                200
            )
        } else {
            getUserLocationAndLoad()
        }
    }

    private fun getUserLocationAndLoad() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLat = location.latitude
                userLng = location.longitude
            }
            loadInitialData()
        }.addOnFailureListener {
            loadInitialData()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getUserLocationAndLoad()
        } else {
            loadInitialData()
        }
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

    private fun sortPharmaciesByDistance(pharmacies: List<Pharmacy>): List<Pharmacy> {
        return pharmacies.sortedBy { pharmacy ->
            val locationList = pharmacy.location
            if (locationList != null && locationList.size >= 2) {
                val lat = locationList[0].toDoubleOrNull() ?: 0.0
                val lng = locationList[1].toDoubleOrNull() ?: 0.0
                distanceTo(userLat, userLng, lat, lng)
            } else {
                Double.MAX_VALUE // if no location, put at the end
            }
        }
    }
}