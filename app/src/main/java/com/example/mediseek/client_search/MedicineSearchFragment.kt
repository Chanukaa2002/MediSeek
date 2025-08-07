package com.example.mediseek.client_search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediseek.R
import com.example.mediseek.adapter.MedicineAdapter
import com.example.mediseek.databinding.FragmentMedicineSearchBinding
import com.example.mediseek.model.Medicine
import com.google.firebase.firestore.FirebaseFirestore
import timber.log.Timber // <-- Import Timber

class MedicineSearchFragment : Fragment() {

    private var _binding: FragmentMedicineSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MedicineAdapter
    private val db = FirebaseFirestore.getInstance()
    private var allMedicines: List<Medicine> = listOf() // Cache for all medicines

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
        setupRecyclerView()
        setupSearchView()
        setupSearchTextColors()
        fetchAllMedicines() // Fetch all medicines once when the view is created
    }

    private fun setupRecyclerView() {
        adapter = MedicineAdapter(emptyList()) { medicine ->
            val bundle = Bundle().apply {
                putString("medicineName", medicine.brand)
            }
            findNavController().navigate(R.id.action_medicine_to_pharmacy, bundle)
        }
        binding.medicineRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.medicineRecyclerView.adapter = adapter
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { filterLocalMedicines(it) }
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterLocalMedicines(newText ?: "")
                return true
            }
        })
    }

    // New function to fetch all data initially
    private fun fetchAllMedicines() {
        Timber.d("Fetching all medicines from Firestore...")
        db.collection("medicines")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Timber.w("No medicines found in the database.")
                    Toast.makeText(context, "No medicine data available", Toast.LENGTH_SHORT).show()
                } else {
                    allMedicines = documents.toObjects(Medicine::class.java)
                    Timber.d("Successfully fetched and cached ${allMedicines.size} medicines.")
                    // Initially display all medicines
                    adapter.updateData(allMedicines)
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to fetch medicines.")
                Toast.makeText(context, "Error: Could not load data.", Toast.LENGTH_SHORT).show()
            }
    }

    // Renamed and simplified search function to filter the cached list
    private fun filterLocalMedicines(query: String) {
        val searchQuery = query.trim()
        Timber.d("Filtering for: $searchQuery")

        if (searchQuery.isEmpty()) {
            adapter.updateData(allMedicines) // Show all if search is empty
            return
        }

        // Perform case-insensitive search on the local list
        val filteredList = allMedicines.filter { medicine ->
            medicine.brand.contains(searchQuery, ignoreCase = true) ||
                    medicine.name.contains(searchQuery, ignoreCase = true)
        }

        adapter.updateData(filteredList)

        if (filteredList.isEmpty()) {
            Toast.makeText(context, "No medicines found for '$searchQuery'", Toast.LENGTH_SHORT).show()
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
}
