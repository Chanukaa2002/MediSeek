package com.example.mediseek.fragments

import android.os.Bundle
import android.text.TextUtils.replace
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.navigation.fragment.findNavController
import com.example.mediseek.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class OrderFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState:Bundle?
    ):View? {
        return inflater.inflate(R.layout.fragment_order,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val name = arguments?.getString("pharmacy_name") ?: "Pharmacy"
        view.findViewById<FloatingActionButton>(R.id.pharmacy_chat).setOnClickListener{

            findNavController().navigate(R.id.nav_livechat)
        }
    }
}