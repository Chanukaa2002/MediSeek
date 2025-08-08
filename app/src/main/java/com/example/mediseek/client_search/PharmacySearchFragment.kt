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

class PharmacySearchFragment : Fragment() {

    private var _binding: FragmentPharmacySearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PharmacyAdapter
    private val db = FirebaseFirestore.getInstance()
    private var allPharmacies: List<Pharmacy> = emptyList()
    private var hasShownNoResults = false

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
                        adapter.updateData(allPharmacies)
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
                                adapter.updateData(allPharmacies)
                            }
                        }
                        .addOnFailureListener { e ->
                            if (!isAdded) return@addOnFailureListener
                            chunksProcessed++
                            Timber.e(e, "Error loading a chunk of pharmacies by ID")
                            if (chunksProcessed == pharmacyChunks.size) {
                                binding.progressBar.visibility = View.GONE
                                adapter.updateData(pharmaciesFound)
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
                    adapter.updateData(allPharmacies)
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
        adapter.updateData(filtered)

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
}
