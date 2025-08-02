package com.example.mediseek.fragments

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mediseek.R
import com.example.mediseek.service.GmailSender
import com.example.mediseek.service.PaymentHandler
import com.example.mediseek.service.QRCodeGenerator
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import lk.payhere.androidsdk.PHConstants
import lk.payhere.androidsdk.PHResponse
import lk.payhere.androidsdk.model.StatusResponse

class OrderFragment : Fragment() {

    private lateinit var paymentHandler: PaymentHandler
    private lateinit var placeOrderButton: MaterialButton
    private lateinit var currentOrderId: String
    private lateinit var paymentLauncher: ActivityResultLauncher<Intent>
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the activity result launcher
        paymentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            Log.d("OrderFragment", "Payment result received - resultCode: ${result.resultCode}")
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

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        paymentHandler = PaymentHandler(requireContext(), paymentLauncher)
        placeOrderButton = view.findViewById(R.id.place_order_button)

        val name = arguments?.getString("pharmacy_name") ?: "Pharmacy"
        view.findViewById<TextView>(R.id.pharmacy_title).text = name

        view.findViewById<FloatingActionButton>(R.id.pharmacy_chat).setOnClickListener {
            findNavController().navigate(R.id.nav_livechat)
        }

        placeOrderButton.setOnClickListener {
            val patientName = view.findViewById<EditText>(R.id.patient_name).text.toString()
            val patientAge = view.findViewById<EditText>(R.id.patient_age).text.toString()
            val collectTime = view.findViewById<EditText>(R.id.collect_time).text.toString()
            val collectDate = view.findViewById<EditText>(R.id.collect_date).text.toString()
            val prescription = view.findViewById<EditText>(R.id.prescription).text.toString()
            val note = view.findViewById<EditText>(R.id.note).text.toString()

            if (patientName.isBlank() || patientAge.isBlank() || collectTime.isBlank() ||
                collectDate.isBlank() || prescription.isBlank()) {
                Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            placeOrderButton.isEnabled = false
            placeOrderButton.text = "Processing..."

            currentOrderId = "ORDER_${System.currentTimeMillis()}"

            paymentHandler.initiateOneTimePayment(
                merchantId = "1231446",
                amount = 1000.0,
                orderId = currentOrderId,
                notifyUrl = "https://your-callback-url.com/notify"
            )
        }
    }

    private fun handlePaymentResult(resultCode: Int, data: Intent?) {
        Log.d("OrderFragment", "handlePaymentResult called with resultCode: $resultCode")

        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d("OrderFragment", "Payment result OK")

                if (data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                    try {
                        val response = data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT) as? PHResponse<StatusResponse>
                        Log.d("OrderFragment", "Payment response: $response")

                        if (response != null && response.isSuccess) {
                            Log.d("OrderFragment", "Payment successful")
                            onPaymentSuccess()
                        } else {
                            Log.d("OrderFragment", "Payment failed with response: $response")
                            onPaymentFailure("Payment failed")
                        }
                    } catch (e: Exception) {
                        Log.e("OrderFragment", "Error processing payment result", e)
                        onPaymentFailure("Error processing payment: ${e.message}")
                    }
                } else {
                    // Sometimes PayHere returns OK without proper data in sandbox mode
                    Log.d("OrderFragment", "Payment OK but no data - assuming success for sandbox")
                    onPaymentSuccess()
                }
            }
            Activity.RESULT_CANCELED -> {
                Log.d("OrderFragment", "Payment canceled")
                onPaymentCancel()
            }
            else -> {
                Log.d("OrderFragment", "Unknown payment result: $resultCode")
                onPaymentFailure("Unknown payment result")
            }
        }
    }

    private fun onPaymentSuccess() {
        requireActivity().runOnUiThread {
            Log.d("OrderFragment", "Processing payment success")

            placeOrderButton.text = "Order Placed Successfully!"
            Toast.makeText(context, "Payment successful!", Toast.LENGTH_SHORT).show()

            // Get form data
            val patientName = view?.findViewById<EditText>(R.id.patient_name)?.text.toString()
            val patientAge = view?.findViewById<EditText>(R.id.patient_age)?.text.toString()
            val collectTime = view?.findViewById<EditText>(R.id.collect_time)?.text.toString()
            val collectDate = view?.findViewById<EditText>(R.id.collect_date)?.text.toString()
            val prescription = view?.findViewById<EditText>(R.id.prescription)?.text.toString()
            val note = view?.findViewById<EditText>(R.id.note)?.text.toString()
            val pharmacyName = arguments?.getString("pharmacy_name") ?: "Pharmacy"

            // Get current user info
            val currentUser = firebaseAuth.currentUser
            val userId = currentUser?.uid ?: ""
            val userEmail = currentUser?.email ?: ""
            val userName = currentUser?.displayName ?: ""

            // Create order data
            val orderData = hashMapOf(
                "orderId" to currentOrderId,
                "userId" to userId,
                "userEmail" to userEmail,
                "pharmacyName" to pharmacyName,
                "patientName" to patientName,
                "patientAge" to patientAge,
                "collectTime" to collectTime,
                "collectDate" to collectDate,
                "prescription" to prescription,
                "note" to note,
                "amount" to 1000.0,
                "paymentStatus" to "completed",
                "collected" to false,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            // Add to Firestore
            FirebaseFirestore.getInstance().collection("Orders")
                .document(currentOrderId)
                .set(orderData)
                .addOnSuccessListener {
                    Log.d("OrderFragment", "Order document successfully written!")
                    sendOrderQrToEmail(currentOrderId)
                }
                .addOnFailureListener { e ->
                    Log.w("OrderFragment", "Error writing order document", e)
                    Toast.makeText(context, "Order placed but failed to save details", Toast.LENGTH_SHORT).show()
                    sendOrderQrToEmail(currentOrderId) // Still send email even if Firestore fails
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
            // Get current user's email from Firebase Auth
            val currentUser = firebaseAuth.currentUser
            val userEmail = currentUser?.email

            if (userEmail.isNullOrBlank()) {
                Log.e("OrderFragment", "No user email found, user might not be logged in")
                Toast.makeText(context, "Unable to send email - please check your login status", Toast.LENGTH_LONG).show()
                return
            }

            Log.d("OrderFragment", "Generating QR code for order: $orderId")
            val qrBitmap: Bitmap = QRCodeGenerator.generateQRCodeBitmap(orderId)
            val qrFile = QRCodeGenerator.saveBitmapToFile(requireContext(), qrBitmap, "order_${orderId}.png")

            val emailBody = """
                Hello ${currentUser.displayName ?: "Customer"},

                Thank you for your order. Please find your order QR code attached.

                Order ID: $orderId

                Please present this QR code when collecting your medication.

                Regards,
                MediSeek Team
            """.trimIndent()

            Log.d("OrderFragment", "Sending email with QR code to: $userEmail")
            GmailSender.sendEmailWithAttachment(
                context = requireContext(),
                recipient = userEmail,
                subject = "Your MediSeek Order QR Code - $orderId",
                body = emailBody,
                attachment = qrFile
            ) { success, message ->
                requireActivity().runOnUiThread {
                    if (success) {
                        Toast.makeText(context, "QR code sent to $userEmail successfully!", Toast.LENGTH_SHORT).show()
                        Log.d("OrderFragment", "QR code email sent successfully for order: $orderId to $userEmail")
                    } else {
                        Toast.makeText(context, "Failed to send email: $message", Toast.LENGTH_LONG).show()
                        Log.e("OrderFragment", "Failed to send QR code email: $message")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OrderFragment", "Error generating or sending QR code: ${e.message}", e)
            Toast.makeText(context, "Error generating QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearForm() {
        Log.d("OrderFragment", "Clearing form")
        view?.findViewById<EditText>(R.id.patient_name)?.text?.clear()
        view?.findViewById<EditText>(R.id.patient_age)?.text?.clear()
        view?.findViewById<EditText>(R.id.collect_time)?.text?.clear()
        view?.findViewById<EditText>(R.id.collect_date)?.text?.clear()
        view?.findViewById<EditText>(R.id.prescription)?.text?.clear()
        view?.findViewById<EditText>(R.id.note)?.text?.clear()
    }
}