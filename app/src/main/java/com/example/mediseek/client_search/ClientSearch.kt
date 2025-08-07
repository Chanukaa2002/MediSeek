package com.example.mediseek.client_search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mediseek.R
import com.example.mediseek.databinding.FragmentClientSearchBinding

// Renamed class to follow Kotlin conventions (PascalCase)
class ClientSearch : Fragment() {

    private var _binding: FragmentClientSearchBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Corrected the binding to match the XML file name `fragment_client_search.xml`
        _binding = FragmentClientSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardPharmacy.setOnClickListener {
            // Use the CORRECT navigation action ID from client_nav_graph.xml
            findNavController().navigate(R.id.action_search_to_pharmacy)
        }

        binding.cardMedicine.setOnClickListener {
            // Use the CORRECT navigation action ID from client_nav_graph.xml
            findNavController().navigate(R.id.action_search_to_medicine)
        }

        // Your FAB for chat is not in fragment_client_search.xml,
        // so I will comment it out. If you add it, you can uncomment this.
        /*
        binding.fabChat.setOnClickListener {
            findNavController().navigate(R.id.action_global_liveChatFragment)
        }
        */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}