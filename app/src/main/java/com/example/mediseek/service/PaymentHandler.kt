package com.example.mediseek.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import lk.payhere.androidsdk.PHConstants
import lk.payhere.androidsdk.PHConfigs
import lk.payhere.androidsdk.PHMainActivity
import lk.payhere.androidsdk.model.InitPreapprovalRequest
import lk.payhere.androidsdk.model.InitRequest
import lk.payhere.androidsdk.model.StatusResponse
import lk.payhere.androidsdk.model.Item
import lk.payhere.androidsdk.PHResponse



class PaymentHandler(private val context: Context) {

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
            this.amount = amount *1.03437
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

        if (context is Activity) {
            context.startActivityForResult(intent, PAYHERE_REQUEST)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun handlePaymentResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        onSuccess: (StatusResponse) -> Unit,
        onFailure: (String) -> Unit,
        onCancel: () -> Unit
    ) {
        if (requestCode == PAYHERE_REQUEST) {
            if (data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                val response = data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT) as? PHResponse<StatusResponse>

                when (resultCode) {
                    Activity.RESULT_OK -> {
                        if (response != null) {
                            if (response.isSuccess) {
                                response.data?.let(onSuccess) ?: run {
                                    onFailure("Payment successful but no data received")
                                }
                            } else {
                                onFailure("Payment failed: ${response.toString()}")
                            }
                        } else {
                            onFailure("No response received from payment gateway")
                        }
                    }
                    Activity.RESULT_CANCELED -> {
                        onCancel()
                    }
                }
            } else {
                onCancel()
            }
        }
    }
}