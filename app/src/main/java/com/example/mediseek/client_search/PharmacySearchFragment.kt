package com.example.mediseek.client_search

import android.os.Bundle
import android.util.Log
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

class PharmacySearchFragment : Fragment() {

    private var _binding: FragmentPharmacySearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PharmacyAdapter
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "PharmacySearch"
    private var allPharmacies: List<Pharmacy> = emptyList()

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
        loadInitialData()
    }

    private fun setupRecyclerView() {
        adapter = PharmacyAdapter(emptyList()) { pharmacy ->
            Timber.tag(TAG).d("Selected pharmacy: ${pharmacy.name}")
            val bundle = Bundle().apply {
                putString("pharmacyId", pharmacy.pharmacyId)
                putString("pharmacyName", pharmacy.name)
            }
            findNavController().navigate(R.id.action_pharmacy_to_order, bundle)
        }
        binding.pharmacyRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.pharmacyRecyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    Timber.tag(TAG).d("Pharmacy search submitted: $it")
                    filterPharmacies(it)
                    binding.searchView.clearFocus()
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    Timber.tag(TAG).d("Pharmacy search text changed: $it")
                    filterPharmacies(it)
                }
                return true
            }
        })
    }

    private fun loadInitialData() {
        binding.progressBar.visibility = View.VISIBLE
        Log.d(TAG, "Loading initial data")

        val medicineName = arguments?.getString("medicineName") ?: ""
        // The pharmacyId is no longer sent from the medicine list,
        // but this logic is kept in case another part of the app sends it.
        val pharmacyId = arguments?.getString("pharmacyId") ?: ""

        binding.title.text = when {
            medicineName.isNotEmpty() -> "Pharmacies with: $medicineName"
            pharmacyId.isNotEmpty() -> "Pharmacy Details"
            else -> "All Pharmacies"
        }

        when {
            // This is kept for flexibility but won't be triggered from the medicine list anymore
            pharmacyId.isNotEmpty() -> loadSinglePharmacy(pharmacyId)
            // This will now be called when navigating from the medicine list
            medicineName.isNotEmpty() -> loadPharmaciesWithMedicine(medicineName)
            // This is called when searching pharmacies directly
            else -> loadAllPharmacies()
        }
    }

    private fun loadSinglePharmacy(pharmacyId: String) {
        Log.d(TAG, "Loading single pharmacy: $pharmacyId")
        db.collection("Pharmacy")
            .whereEqualTo("ph_id", pharmacyId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    Log.d(TAG, "No pharmacy found with ID: $pharmacyId")
                    Toast.makeText(context, "Pharmacy not found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                allPharmacies = documents.toObjects(Pharmacy::class.java)
                Log.d(TAG, "Loaded single pharmacy: ${allPharmacies[0].name}")
                adapter.updateData(allPharmacies)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading pharmacy", e)
                Toast.makeText(context, "Failed to load pharmacy: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPharmaciesWithMedicine(medicineName: String) {
        Log.d(TAG, "Loading pharmacies for medicine: $medicineName")
        db.collection("Medicine")
            .whereEqualTo("name", medicineName)
            .get()
            .addOnSuccessListener { medicineDocs ->
                if (medicineDocs.isEmpty) {
                    binding.progressBar.visibility = View.GONE
                    Log.d(TAG, "No medicines found with name: $medicineName")
                    Toast.makeText(context, "No pharmacies carry this medicine", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val pharmacyIds = medicineDocs.mapNotNull { it.getString("ph_id") }.filter { it.isNotEmpty() }.distinct()

                Log.d(TAG, "Found ${pharmacyIds.size} pharmacy IDs: $pharmacyIds")

                if (pharmacyIds.isEmpty()) {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(context, "No pharmacies found", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                db.collection("Pharmacy")
                    .whereIn("ph_id", pharmacyIds)
                    .get()
                    .addOnSuccessListener { pharmacyDocs ->
                        binding.progressBar.visibility = View.GONE
                        if (pharmacyDocs.isEmpty) {
                            Log.d(TAG, "No pharmacies found for IDs: $pharmacyIds")
                            Toast.makeText(context, "No pharmacy details available", Toast.LENGTH_SHORT).show()
                            return@addOnSuccessListener
                        }

                        allPharmacies = pharmacyDocs.toObjects(Pharmacy::class.java)
                        Log.d(TAG, "Loaded ${allPharmacies.size} pharmacies")
                        adapter.updateData(allPharmacies)
                    }
                    .addOnFailureListener { e ->
                        binding.progressBar.visibility = View.GONE
                        Log.e(TAG, "Error loading pharmacies", e)
                        Toast.makeText(context, "Failed to load pharmacies: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error finding medicine", e)
                Toast.makeText(context, "Medicine search failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadAllPharmacies() {
        Log.d(TAG, "Loading all pharmacies")
        db.collection("Pharmacy")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    Log.d(TAG, "No pharmacies found in database")
                    Toast.makeText(context, "No pharmacies available", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                allPharmacies = documents.toObjects(Pharmacy::class.java)
                Log.d(TAG, "Loaded ${allPharmacies.size} pharmacies")
                adapter.updateData(allPharmacies)
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Log.e(TAG, "Error loading pharmacies", e)
                Toast.makeText(context, "Failed to load pharmacies: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterPharmacies(query: String) {
        val filtered = if (query.isEmpty()) {
            allPharmacies
        } else {
            allPharmacies.filter { pharmacy ->
                pharmacy.name.contains(query, true) ||
                        pharmacy.location.contains(query, true) ||
                        (pharmacy.regNo?.contains(query, true) ?: false)
            }
        }

        Log.d(TAG, "Filtered ${filtered.size} pharmacies for query: $query")
        adapter.updateData(filtered)

        if (filtered.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(context, "No matching pharmacies found", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}