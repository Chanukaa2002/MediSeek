package com.example.mediseek.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mediseek.R
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailsDialogFragment : DialogFragment() {

    private lateinit var db: FirebaseFirestore
    private var orderDocumentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        orderDocumentId = arguments?.getString(ARG_ORDER_ID)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_order_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadOrderDetails(view)
        setupButtons(view)
    }

    private fun loadOrderDetails(view: View) {
        val titleTextView = view.findViewById<TextView>(R.id.tv_dialog_order_id)
        val patientTextView = view.findViewById<TextView>(R.id.tv_dialog_patient_name)
        val prescriptionTextView = view.findViewById<TextView>(R.id.tv_dialog_prescription)
        val totalTextView = view.findViewById<TextView>(R.id.tv_dialog_total_amount)
        val dateTextView = view.findViewById<TextView>(R.id.tv_dialog_order_date)

        orderDocumentId?.let { id ->
            db.collection("Orders").document(id).get().addOnSuccessListener { doc ->
                if (doc != null) {
                    val orderIdFull = doc.getString("orderId") ?: ""
                    titleTextView.text = "Order ID: ${orderIdFull.takeLast(5)}"
                    patientTextView.text = "Patient: ${doc.getString("patientName") ?: "N/A"}"
                    prescriptionTextView.text = "Prescription: ${doc.getString("prescription") ?: "N/A"}"
                    totalTextView.text = "Total: Rs.${String.format("%.2f", doc.getDouble("totalAmount") ?: 0.0)}"

                    val createdAtTimestamp = doc.getTimestamp("createdAt") ?: Timestamp.now()
                    val formattedDate = SimpleDateFormat("d MMMM, yyyy, hh:mm a", Locale.getDefault()).format(createdAtTimestamp.toDate())
                    dateTextView.text = "Date: $formattedDate"
                }
            }
        }
    }

    private fun setupButtons(view: View) {
        view.findViewById<Button>(R.id.button_cancel_order).setOnClickListener {
            dismiss() // Close the dialog
        }

        view.findViewById<Button>(R.id.button_complete_order).setOnClickListener {
            completeOrder()
        }
    }

    private fun completeOrder() {
        orderDocumentId?.let { id ->
            val orderRef = db.collection("Orders").document(id)
            db.runTransaction { transaction ->
                // Update both fields in a single atomic operation
                transaction.update(orderRef, "collected", true)
                transaction.update(orderRef, "paymentStatus", "collected")
                null // Transaction must return null or a value
            }.addOnSuccessListener {
                Toast.makeText(context, "Order completed successfully!", Toast.LENGTH_SHORT).show()
                dismiss()
            }.addOnFailureListener { e ->
                Toast.makeText(context, "Failed to complete order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val TAG = "OrderDetailsDialog"
        private const val ARG_ORDER_ID = "order_id"

        fun newInstance(orderId: String): OrderDetailsDialogFragment {
            val args = Bundle()
            args.putString(ARG_ORDER_ID, orderId)
            val fragment = OrderDetailsDialogFragment()
            fragment.arguments = args
            return fragment
        }
    }
}
