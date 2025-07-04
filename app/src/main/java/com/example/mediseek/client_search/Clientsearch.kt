package com.example.mediseek.client_search

import MedicineSearchFragment
import PharmacySearchFragment
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mediseek.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Clientsearch : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_client_search, container, false)


    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<CardView>(R.id.card_pharmacy).setOnClickListener {
            findNavController().navigate(R.id.nav_search_pharmacy)
        }

        view.findViewById<CardView>(R.id.card_medicine).setOnClickListener {
            findNavController().navigate(R.id.nav_search_medicine)
        }

        view.findViewById<FloatingActionButton>(R.id.pharmacy_chat)?.setOnClickListener {
            findNavController().navigate(R.id.nav_livechat)
        }
    }

}
