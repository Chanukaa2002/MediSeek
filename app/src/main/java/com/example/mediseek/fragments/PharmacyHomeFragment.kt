package com.example.mediseek.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mediseek.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class PharmacyHomeFragment:Fragment () {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pharmacy_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<CardView>(R.id.card_orders).setOnClickListener {
            findNavController().navigate(R.id.nav_orders)
        }

        view.findViewById<CardView>(R.id.card_store).setOnClickListener {
            findNavController().navigate(R.id.nav_products)
        }

        view.findViewById<CardView>(R.id.card_live_chat).setOnClickListener {
            findNavController().navigate(R.id.nav_chat)
        }
    }
}