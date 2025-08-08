package com.example.mediseek.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mediseek.R
import com.example.mediseek.model.Medicine
import com.example.mediseek.service.GmailSender
import com.example.mediseek.service.PaymentHandler
import com.example.mediseek.service.QRCodeGenerator
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import lk.payhere.androidsdk.PHConstants
import lk.payhere.androidsdk.PHResponse
import lk.payhere.androidsdk.model.StatusResponse
import timber.log.Timber
import java.text.DecimalFormat

class OrderFragment : Fragment() {

    private lateinit var paymentHandler: PaymentHandler
    private lateinit var placeOrderButton: MaterialButton
    private lateinit var currentOrderId: String
    private lateinit var paymentLauncher: ActivityResultLauncher<Intent>
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Medicine selection views
    private lateinit var medicineDropdown: AutoCompleteTextView
    private lateinit var priceInput: TextInputEditText
    private lateinit var quantityInput: TextInputEditText
    private lateinit var totalPriceText: TextView

    // Medicine data
    private var selectedMedicine: Medicine? = null
    private var medicinesList = mutableListOf<Medicine>()
    private lateinit var pharmacyId: String
    private lateinit var pharmacyName: String
    private var totalAmount = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        paymentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handlePaymentResult(result.resultCode, result.data)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_order, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        paymentHandler = PaymentHandler(requireContext(), paymentLauncher)
        placeOrderButton = view.findViewById(R.id.place_order_button)

        pharmacyId = arguments?.getString("pharmacyId") ?: ""
        // FIXED: Use the correct argument key "pharmacyName"
        pharmacyName = arguments?.getString("pharmacyName") ?: "Pharmacy"
        view.findViewById<TextView>(R.id.pharmacy_title).text = pharmacyName

        medicineDropdown = view.findViewById(R.id.medicine_dropdown)
        priceInput = view.findViewById(R.id.price_input)
        quantityInput = view.findViewById(R.id.quantity_input)
        totalPriceText = view.findViewById(R.id.total_price_text)

        // FIXED: Added click listener for the chat button
        view.findViewById<FloatingActionButton>(R.id.pharmacy_chat).setOnClickListener {
            // Ensure this destination ID is correct in your nav_graph.xml
            findNavController().navigate(R.id.liveChatFragment)
        }

        loadMedicinesForPharmacy(pharmacyId)
        setupListeners()
    }

    private fun loadMedicinesForPharmacy(pharmacyId: String) {
        if (pharmacyId.isEmpty()) {
            Toast.makeText(context, "Pharmacy not selected", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("medicines")
            .whereEqualTo("pharmacyId", pharmacyId)
            .get()
            .addOnSuccessListener { documents ->
                medicinesList.clear()
                for (document in documents) {
                    val medicine = document.toObject(Medicine::class.java).apply {
                        id = document.id
                    }
                    medicinesList.add(medicine)
                }
                populateMedicineDropdown()
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Error loading medicines")
                Toast.makeText(context, "Failed to load medicines", Toast.LENGTH_SHORT).show()
            }
    }

    private fun populateMedicineDropdown() {
        if (medicinesList.isEmpty()) {
            Toast.makeText(context, "No medicines available", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            medicinesList.map { it.brand }
        )

        medicineDropdown.setAdapter(adapter)
        medicineDropdown.setOnItemClickListener { _, _, position, _ ->
            selectedMedicine = medicinesList[position]
            updateMedicineDetails()
        }

        val preselectedMedicineName = arguments?.getString("medicineName")
        if (!preselectedMedicineName.isNullOrEmpty()) {
            preselectMedicine(preselectedMedicineName)
        }
    }

    private fun preselectMedicine(medicineName: String) {
        val medicineToSelect = medicinesList.find { it.name.equals(medicineName, ignoreCase = true) }
        if (medicineToSelect != null) {
            selectedMedicine = medicineToSelect
            val adapter = medicineDropdown.adapter as ArrayAdapter<String>
            val position = adapter.getPosition(medicineToSelect.brand)
            if (position >= 0) {
                medicineDropdown.setText(adapter.getItem(position), false)
                updateMedicineDetails()
            }
        }
    }

    private fun setupListeners() {
        quantityInput.addTextChangedListener {
            updateTotalPrice()
        }
        placeOrderButton.setOnClickListener {
            placeOrder()
        }
    }

    private fun updateMedicineDetails() {
        selectedMedicine?.let { medicine ->
            priceInput.setText(DecimalFormat("#,##0.00").format(medicine.price))
            quantityInput.setText("1")
            updateTotalPrice()
        }
    }

    private fun updateTotalPrice() {
        try {
            val quantity = quantityInput.text.toString().toIntOrNull() ?: 0
            if (quantity < 1 && quantityInput.hasFocus()) return
            if (quantity < 1) {
                quantityInput.setText("1")
                return
            }
            selectedMedicine?.let { medicine ->
                totalAmount = medicine.price * quantity
                val formattedTotal = DecimalFormat("#,##0.00").format(totalAmount)
                totalPriceText.text = "Total: LKR $formattedTotal"
            }
        } catch (e: Exception) {
            Timber.e(e, "Error calculating total")
            totalPriceText.text = "Total: LKR 0.00"
        }
    }

    private fun placeOrder() {
        val patientName = view?.findViewById<TextInputEditText>(R.id.patient_name)?.text.toString()
        val patientAge = view?.findViewById<TextInputEditText>(R.id.patient_age)?.text.toString()
        val collectTime = view?.findViewById<TextInputEditText>(R.id.collect_time)?.text.toString()
        val collectDate = view?.findViewById<TextInputEditText>(R.id.collect_date)?.text.toString()
        val prescription = view?.findViewById<TextInputEditText>(R.id.prescription)?.text.toString()
        val note = view?.findViewById<TextInputEditText>(R.id.note)?.text.toString()

        if (selectedMedicine == null) {
            Toast.makeText(context, "Please select a medicine", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityInput.text.toString().toIntOrNull() ?: 0
        if (quantity < 1) {
            Toast.makeText(context, "Quantity must be at least 1", Toast.LENGTH_SHORT).show()
            return
        }

        if (patientName.isNullOrBlank() || patientAge.isNullOrBlank() || collectTime.isNullOrBlank() ||
            collectDate.isNullOrBlank() || prescription.isNullOrBlank()
        ) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        placeOrderButton.isEnabled = false
        placeOrderButton.text = "Processing..."
        currentOrderId = "ORDER_${System.currentTimeMillis()}"

        paymentHandler.initiateOneTimePayment(
            merchantId = "1231446",
            amount = totalAmount,
            orderId = currentOrderId,
            notifyUrl = "https://your-callback-url.com/notify"
        )
    }

    private fun handlePaymentResult(resultCode: Int, data: Intent?) {
        Timber.d("handlePaymentResult called with resultCode: $resultCode")
        when (resultCode) {
            Activity.RESULT_OK -> {
                val response = data?.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT) as? PHResponse<StatusResponse>
                if (response?.isSuccess == true) {
                    onPaymentSuccess()
                } else {
                    onPaymentFailure("Payment failed or was cancelled.")
                }
            }
            Activity.RESULT_CANCELED -> onPaymentCancel()
            else -> onPaymentFailure("Unknown payment result")
        }
    }

    private fun onPaymentSuccess() {
        requireActivity().runOnUiThread {
            Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()

            val patientName = view?.findViewById<TextInputEditText>(R.id.patient_name)?.text.toString()
            val patientAge = view?.findViewById<TextInputEditText>(R.id.patient_age)?.text.toString()
            val collectTime = view?.findViewById<TextInputEditText>(R.id.collect_time)?.text.toString()
            val collectDate = view?.findViewById<TextInputEditText>(R.id.collect_date)?.text.toString()
            val prescription = view?.findViewById<TextInputEditText>(R.id.prescription)?.text.toString()
            val note = view?.findViewById<TextInputEditText>(R.id.note)?.text.toString()
            val currentUser = firebaseAuth.currentUser
            val userId = currentUser?.uid ?: ""

            val orderData = hashMapOf(
                "orderId" to currentOrderId,
                "userId" to userId,
                "userEmail" to (currentUser?.email ?: ""),
                "pharmacyId" to pharmacyId,
                "pharmacyName" to pharmacyName,
                "medicineId" to selectedMedicine?.id,
                "medicineName" to selectedMedicine?.name,
                "quantity" to (quantityInput.text.toString().toIntOrNull() ?: 0),
                "unitPrice" to selectedMedicine?.price,
                "totalAmount" to totalAmount,
                "patientName" to patientName,
                "patientAge" to patientAge,
                "collectTime" to collectTime,
                "collectDate" to collectDate,
                "prescription" to prescription,
                "note" to note,
                "paymentStatus" to "completed",
                "collected" to false,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            db.collection("Orders").document(currentOrderId).set(orderData)
                .addOnSuccessListener {
                    Timber.d("Order document successfully written!")
                    sendOrderQrToEmail(currentOrderId)
                }
                .addOnFailureListener { e ->
                    Timber.w(e, "Error writing order document")
                }

            clearForm()
            placeOrderButton.postDelayed({
                placeOrderButton.isEnabled = true
                placeOrderButton.text = "Place Order"
            }, 3000)
        }
    }

    private fun onPaymentFailure(errorMessage: String) {
        requireActivity().runOnUiThread {
            placeOrderButton.isEnabled = true
            placeOrderButton.text = "Place Order"
            Toast.makeText(context, "Payment failed: $errorMessage", Toast.LENGTH_LONG).show()
        }
    }

    private fun onPaymentCancel() {
        requireActivity().runOnUiThread {
            placeOrderButton.isEnabled = true
            placeOrderButton.text = "Place Order"
            Toast.makeText(context, "Payment canceled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendOrderQrToEmail(orderId: String) {
        try {
            val currentUser = firebaseAuth.currentUser
            val userEmail = currentUser?.email
            if (userEmail.isNullOrBlank()) {
                Timber.e("No user email found")
                return
            }

            val qrBitmap: Bitmap = QRCodeGenerator.generateQRCodeBitmap(orderId)
            val qrFile = QRCodeGenerator.saveBitmapToFile(requireContext(), qrBitmap, "order_${orderId}.png")

            val emailBody = """
                Hello ${currentUser.displayName ?: "Customer"},
                Thank you for your order. Please find your order QR code attached.
                Order ID: $orderId
                Pharmacy: $pharmacyName
                Medicine: ${selectedMedicine?.name}
                Quantity: ${quantityInput.text.toString()}
                Total Amount: LKR ${DecimalFormat("#,##0.00").format(totalAmount)}
                Please present this QR code when collecting your medication.
                Regards,
                MediSeek Team
            """.trimIndent()

            GmailSender.sendEmailWithAttachment(
                context = requireContext(),
                recipient = userEmail,
                subject = "Your MediSeek Order QR Code - $orderId",
                body = emailBody,
                attachment = qrFile
            ) { success, message ->
                requireActivity().runOnUiThread {
                    if (success) {
                        Toast.makeText(context, "QR code sent to $userEmail!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to send email: $message", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error sending QR code email")
        }
    }

    private fun clearForm() {
        medicineDropdown.text?.clear()
        priceInput.setText("0.00")
        quantityInput.setText("1")
        totalPriceText.text = "Total: LKR 0.00"
        selectedMedicine = null
        totalAmount = 0.0

        view?.findViewById<TextInputEditText>(R.id.patient_name)?.text?.clear()
        view?.findViewById<TextInputEditText>(R.id.patient_age)?.text?.clear()
        view?.findViewById<TextInputEditText>(R.id.collect_time)?.text?.clear()
        view?.findViewById<TextInputEditText>(R.id.collect_date)?.text?.clear()
        view?.findViewById<TextInputEditText>(R.id.prescription)?.text?.clear()
        view?.findViewById<TextInputEditText>(R.id.note)?.text?.clear()
    }
}
