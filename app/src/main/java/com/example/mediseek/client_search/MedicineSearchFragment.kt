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
import timber.log.Timber

class MedicineSearchFragment : Fragment() {

    private var _binding: FragmentMedicineSearchBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: MedicineAdapter
    private val db = FirebaseFirestore.getInstance()
    private var allMedicines: List<Medicine> = listOf()
    private var hasShownNoResults = false

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
        fetchAllMedicines()
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
        Timber.d("Fetching all medicines from Firestore...")
        db.collection("medicines")
            .get()
            .addOnSuccessListener { documents ->
                // FIXED: Safety check to ensure the fragment is still active before showing alerts or updating UI.
                if (!isAdded) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                if (documents.isEmpty) {
                    Timber.w("No medicines found in the database.")
                    Toast.makeText(context, "No medicine data available.", Toast.LENGTH_SHORT).show()
                } else {
                    allMedicines = documents.map { document ->
                        document.toObject(Medicine::class.java).apply {
                            id = document.id
                        }
                    }
                    Timber.d("Successfully fetched and cached ${allMedicines.size} medicines.")
                    adapter.updateData(allMedicines)
                }
            }
            .addOnFailureListener { exception ->
                // FIXED: Safety check to ensure the fragment is still active before showing alerts.
                if (!isAdded) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Timber.e(exception, "Failed to fetch medicines.")
                Toast.makeText(context, "Could not load medicine data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterLocalMedicines(query: String) {
        val searchQuery = query.trim()
        Timber.d("Filtering for: $searchQuery")

        if (searchQuery.isEmpty()) {
            adapter.updateData(allMedicines)
            hasShownNoResults = false
            return
        }

        val filteredList = allMedicines.filter { medicine ->
            medicine.name.contains(searchQuery, ignoreCase = true)
        }

        adapter.updateData(filteredList)

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
}
