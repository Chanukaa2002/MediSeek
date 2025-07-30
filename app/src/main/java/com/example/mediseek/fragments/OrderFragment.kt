package com.example.mediseek.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mediseek.R
import com.example.mediseek.service.PaymentHandler
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class OrderFragment : Fragment() {

    private lateinit var paymentHandler: PaymentHandler
    private lateinit var placeOrderButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        paymentHandler = PaymentHandler(requireContext())
        placeOrderButton = view.findViewById(R.id.place_order_button)

        val name = arguments?.getString("pharmacy_name") ?: "Pharmacy"
        view.findViewById<TextView>(R.id.pharmacy_title).text = name

        // Handle chat button click
        view.findViewById<FloatingActionButton>(R.id.pharmacy_chat).setOnClickListener {
            findNavController().navigate(R.id.nav_livechat)
        }

        // Handle place order button click
        placeOrderButton.setOnClickListener {
            // Get all the form data
            val patientName = view.findViewById<EditText>(R.id.patient_name).text.toString()
            val patientAge = view.findViewById<EditText>(R.id.patient_age).text.toString()
            val collectTime = view.findViewById<EditText>(R.id.collect_time).text.toString()
            val collectDate = view.findViewById<EditText>(R.id.collect_date).text.toString()
            val prescription = view.findViewById<EditText>(R.id.prescription).text.toString()
            val note = view.findViewById<EditText>(R.id.note).text.toString()

            // Validate form
            if (patientName.isBlank() || patientAge.isBlank() || collectTime.isBlank() ||
                collectDate.isBlank() || prescription.isBlank()) {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Disable button during payment processing
            placeOrderButton.isEnabled = false
            placeOrderButton.text = "Processing..."

            // Create order summary
            val orderDescription = "Medicine order for $patientName (Age: $patientAge)"

            // Initiate payment
            initiatePayment(orderDescription)
        }
    }

    private fun initiatePayment(orderDescription: String) {
        paymentHandler.initiateOneTimePayment(
            merchantId = "1231446", // Replace with your merchant ID
            amount = 1000.00, // Calculate based on order
            orderId = "ORDER_${System.currentTimeMillis()}",
            notifyUrl = "https://your-callback-url.com/notify",
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        paymentHandler.handlePaymentResult(
            requestCode = requestCode,
            resultCode = resultCode,
            data = data,
            onSuccess = { statusResponse ->
                // Payment successful - show success UI
                placeOrderButton.isEnabled = true
                placeOrderButton.text = "Order Placed Successfully!"
                placeOrderButton.setBackgroundColor(resources.getColor(R.color.green, null))

                // Show success message
                Toast.makeText(context, "Payment successful! Your order is confirmed.", Toast.LENGTH_LONG).show()

                // Optionally clear form or show order details
                clearForm()
            },
            onFailure = { errorMessage ->
                // Payment failed - reset button
                placeOrderButton.isEnabled = true
                placeOrderButton.text = "Place Order"
                Toast.makeText(context, "Payment failed: $errorMessage", Toast.LENGTH_LONG).show()
            },
            onCancel = {
                // Payment canceled - reset button
                placeOrderButton.isEnabled = true
                placeOrderButton.text = "Place Order"
                Toast.makeText(context, "Payment canceled", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun clearForm() {
        view?.findViewById<EditText>(R.id.patient_name)?.text?.clear()
        view?.findViewById<EditText>(R.id.patient_age)?.text?.clear()
        view?.findViewById<EditText>(R.id.collect_time)?.text?.clear()
        view?.findViewById<EditText>(R.id.collect_date)?.text?.clear()
        view?.findViewById<EditText>(R.id.prescription)?.text?.clear()
        view?.findViewById<EditText>(R.id.note)?.text?.clear()
    }
}