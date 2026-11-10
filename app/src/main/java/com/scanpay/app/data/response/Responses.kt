package com.scanpay.app.data.response

import com.google.gson.annotations.SerializedName
import com.scanpay.app.data.model.QRCode
import com.scanpay.app.data.model.User
import com.scanpay.app.data.model.Vendor

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("user_type") val userType: String? = null,
    val user: User? = null,
    val vendor: Vendor? = null,
    val message: String? = null
)

data class VendorAuthResponse(
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("user_type") val userType: String = "vendor",
    val vendor: Vendor,
    val message: String? = null
)

data class QRScanResponse(
    val valid: Boolean = false,
    val message: String? = null,
    val vendor: Vendor? = null,
    @SerializedName("qr_code") val qrCode: QRCode? = null,
    val amount: Double? = null,
    @SerializedName("is_dynamic") val isDynamic: Boolean = false,
    @SerializedName("shortcode_type") val shortcodeType: String = "TILL",
    val error: String? = null
)

data class GenerateQRResponse(
    val message: String,
    @SerializedName("qr_code") val qrCode: QRCode,
    val payload: String
)

data class TransactionResponse(
    val id: Long,
    val amount: Double,
    val currency: String = "404",
    val type: String,
    val status: String,
    val phone: String? = null,
    @SerializedName("account_reference") val accountReference: String? = null,
    @SerializedName("checkout_request_id") val checkoutRequestId: String? = null,
    @SerializedName("mpesa_receipt") val mpesaReceipt: String? = null,
    @SerializedName("initiated_at") val initiatedAt: String,
    @SerializedName("completed_at") val completedAt: String? = null,
    @SerializedName("merchant_name") val merchantName: String? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    val message: String? = null,
    @SerializedName("transaction_id") val transactionId: Long? = null
)

data class PaymentStatusResponse(
    val status: String,
    @SerializedName("transaction_id") val transactionId: Long,
    @SerializedName("mpesa_receipt") val mpesaReceipt: String? = null,
    val amount: Double,
    val message: String
)

data class DailyAnalytics(
    val date: String,
    val total: Double,
    val count: Int
)

data class CategorySpending(
    val category: String,
    val amount: Double,
    val count: Int,
    val percentage: Double = 0.0
)

data class AnalyticsResponse(
    @SerializedName("total_amount") val totalAmount: Double = 0.0,
    @SerializedName("total_count") val totalCount: Int = 0,
    @SerializedName("today_amount") val todayAmount: Double = 0.0,
    @SerializedName("today_count") val todayCount: Int = 0,
    @SerializedName("average_transaction") val averageTransaction: Double = 0.0,
    val daily: List<DailyAnalytics> = emptyList(),
    val categories: List<CategorySpending> = emptyList(),
    val currency: String = "KES"
)

data class MessageResponse(
    val message: String,
    val status: String = "success"
)

data class ForgotPasswordResponse(
    val message: String,
    @SerializedName("reset_token") val resetToken: String? = null
)

data class TopUpBalanceResponse(
    val balance: Double = 0.0,
    val currency: String = "KES"
)

data class TopUpResponse(
    val message: String,
    @SerializedName("checkout_request_id") val checkoutRequestId: String? = null,
    val status: String = "pending"
)

