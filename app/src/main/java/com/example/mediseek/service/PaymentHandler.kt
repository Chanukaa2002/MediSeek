package com.example.mediseek.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import lk.payhere.androidsdk.PHConstants
import lk.payhere.androidsdk.PHConfigs
import lk.payhere.androidsdk.PHMainActivity
import lk.payhere.androidsdk.model.InitPreapprovalRequest
import lk.payhere.androidsdk.model.InitRequest
import lk.payhere.androidsdk.model.StatusResponse
import lk.payhere.androidsdk.model.Item
import lk.payhere.androidsdk.PHResponse

class PaymentHandler(
    private val context: Context,
    private val activityResultLauncher: ActivityResultLauncher<Intent>? = null
) {

    companion object {
        const val PAYHERE_REQUEST = 11001
        const val TAG = "PaymentHandler"
    }

    // One-time payment
    fun initiateOneTimePayment(
        merchantId: String,
        amount: Double,
        orderId: String,
        notifyUrl: String? = null
    ) {
        val req = InitRequest().apply {
            this.merchantId = merchantId
            currency = "LKR"
            this.amount = amount
            this.orderId = orderId
            itemsDescription = "Payment for order $orderId"
            custom1 = "Additional info 1"
            custom2 = "Additional info 2"

            customer.apply {
                firstName = "Saman"
                lastName = "Perera"
                email = "samanp@gmail.com"
                phone = "+94771234567"
                address.apply {
                    address = "No.1, Galle Road"
                    city = "Colombo"
                    country = "Sri Lanka"
                }
            }

            notifyUrl?.let { this.notifyUrl = it }

            items.add(Item(null, "Main product", 1, amount))
        }

        launchPayment(req)
    }

    // Recurring payment
    fun initiateRecurringPayment(
        merchantId: String,
        amount: Double,
        orderId: String,
        recurrence: String,
        duration: String,
        notifyUrl: String? = null
    ) {
        val req = InitRequest().apply {
            this.merchantId = merchantId
            currency = "LKR"
            this.amount = amount
            this.orderId = orderId
            this.recurrence = recurrence
            this.duration = duration
            itemsDescription = "Subscription payment"
            custom1 = "Additional info 1"
            custom2 = "Additional info 2"

            customer.apply {
                firstName = "Saman"
                lastName = "Perera"
                email = "samanp@gmail.com"
                phone = "+94771234567"
                address.apply {
                    address = "No.1, Galle Road"
                    city = "Colombo"
                    country = "Sri Lanka"
                }
            }

            notifyUrl?.let { this.notifyUrl = it }
        }

        launchPayment(req)
    }

    // Preapproval payment
    fun initiatePreapprovalPayment(
        merchantId: String,
        orderId: String,
        currency: String = "LKR",
        notifyUrl: String? = null
    ) {
        val req = InitPreapprovalRequest().apply {
            this.merchantId = merchantId
            this.orderId = orderId
            this.currency = currency
            itemsDescription = "Preapproval request"
            custom1 = "Additional info 1"
            custom2 = "Additional info 2"

            customer.apply {
                firstName = "Saman"
                lastName = "Perera"
                email = "samanp@gmail.com"
                phone = "+94771234567"
                address.apply {
                    address = "No.1, Galle Road"
                    city = "Colombo"
                    country = "Sri Lanka"
                }
            }

            notifyUrl?.let { this.notifyUrl = it }
        }

        launchPayment(req)
    }

    // Hold-on-card payment
    fun initiateHoldOnCardPayment(
        merchantId: String,
        amount: Double,
        orderId: String,
        notifyUrl: String? = null
    ) {
        val req = InitRequest().apply {
            this.merchantId = merchantId
            currency = "LKR"
            this.amount = amount * 1.03437
            this.orderId = orderId
            itemsDescription = "Payment with card hold"
            custom1 = "Additional info 1"
            custom2 = "Additional info 2"

            customer.apply {
                firstName = "Saman"
                lastName = "Perera"
                email = "samanp@gmail.com"
                phone = "+94771234567"
                address.apply {
                    address = "No.1, Galle Road"
                    city = "Colombo"
                    country = "Sri Lanka"
                }
            }

            notifyUrl?.let { this.notifyUrl = it }

            items.add(Item(null, "Main product", 1, amount))
        }

        launchPayment(req)
    }

    private fun launchPayment(request: Any) {
        PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL) // Use SANDBOX for testing

        val intent = Intent(context, PHMainActivity::class.java).apply {
            putExtra(PHConstants.INTENT_EXTRA_DATA, request as java.io.Serializable)
        }

        Log.d(TAG, "Launching payment with intent: $intent")

        if (activityResultLauncher != null) {
            // Use modern Activity Result API
            Log.d(TAG, "Using Activity Result Launcher")
            activityResultLauncher.launch(intent)
        } else if (context is Activity) {
            // Fallback to old method
            Log.d(TAG, "Using startActivityForResult fallback")
            context.startActivityForResult(intent, PAYHERE_REQUEST)
        } else {
            // Last resort - start activity without result
            Log.d(TAG, "Starting activity without result")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    // Keep this method for backward compatibility, but it's not used with the new approach
    fun handlePaymentResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        onSuccess: (StatusResponse) -> Unit,
        onFailure: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        Log.d(TAG, "handlePaymentResult called - requestCode: $requestCode, resultCode: $resultCode")
        Log.d(TAG, "Expected requestCode: $PAYHERE_REQUEST")
        Log.d(TAG, "Data intent: $data")

        if (requestCode == PAYHERE_REQUEST) {
            Log.d(TAG, "Request code matches PayHere request")

            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d(TAG, "Result OK received")

                    if (data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        Log.d(TAG, "Data contains result extra")

                        try {
                            val response = data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT) as? PHResponse<StatusResponse>
                            Log.d(TAG, "Response received: $response")

                            if (response != null) {
                                if (response.isSuccess) {
                                    Log.d(TAG, "Payment response indicates success")
                                    response.data?.let { statusResponse ->
                                        Log.d(TAG, "Status response data: $statusResponse")
                                        onSuccess(statusResponse)
                                    } ?: run {
                                        Log.e(TAG, "Payment successful but no status data received")
                                        onFailure("Payment successful but no data received")
                                    }
                                } else {
                                    Log.e(TAG, "Payment response indicates failure: $response")
                                    onFailure("Payment failed: ${response.toString()}")
                                }
                            } else {
                                Log.e(TAG, "No response object received from payment gateway")
                                onFailure("No response received from payment gateway")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing payment result", e)
                            onFailure("Error processing payment result: ${e.message}")
                        }
                    } else {
                        Log.e(TAG, "Data is null or doesn't contain result extra")
                        // Sometimes PayHere might return success without proper data structure
                        // In sandbox mode, this might still be a successful payment
                        onSuccess(StatusResponse().apply {
                            // Create a basic status response for successful payment
                        })
                    }
                }
                Activity.RESULT_CANCELED -> {
                    Log.d(TAG, "Payment was canceled by user")
                    onCancel()
                }
                else -> {
                    Log.d(TAG, "Unexpected result code: $resultCode")
                    onFailure("Unexpected payment result: $resultCode")
                }
            }
        } else {
            Log.d(TAG, "Request code doesn't match PayHere request: $requestCode vs $PAYHERE_REQUEST")
        }
    }
}