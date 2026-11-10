package com.scanpay.server.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class CustomerRegisterRequest(
    val name: String,
    val phone_number: String,
    val email: String,
    val password: String
)

@Serializable
data class MerchantRegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    val business_shortcode: String,
    val business_name: String? = null,
    val shortcode_type: String = "TILL", // "TILL" or "PAYBILL"
    val paybill_account_number: String? = null,
    val store_label: String? = null,
    val merchant_id: String? = null,
    val mcc: String? = "5411",
    val psp_id: String? = "PSP001",
    val psp_name: String? = "Safaricom"
)

@Serializable
data class UserSummary(
    val id: Long,
    val name: String,
    val email: String,
    val phone_number: String,
    val is_active: Boolean = true
)

@Serializable
data class VendorSummary(
    val id: Long,
    val name: String,
    val business_name: String? = null,
    val business_shortcode: String,
    val shortcode_type: String = "TILL",
    val email: String,
    val phone: String,
    val store_label: String? = null,
    val mcc: String? = "5411",
    val is_active: Boolean = true
)

@Serializable
data class AuthResponse(
    val access_token: String,
    val user_type: String, // "customer" or "merchant" / "vendor"
    val user: UserSummary? = null,
    val vendor: VendorSummary? = null,
    val message: String = "Success"
)

@Serializable
data class VendorAuthResponse(
    val access_token: String,
    val user_type: String = "vendor",
    val vendor: VendorSummary,
    val message: String = "Vendor registered successfully"
)

@Serializable
data class GenerateQRRequest(
    val qr_type: String = "STATIC", // "STATIC" or "DYNAMIC"
    val amount: Double? = null,
    val reference: String? = null,
    val shortcode_type: String? = null // "TILL" or "PAYBILL"
)

@Serializable
data class QRCodeDetails(
    val id: Long,
    val vendor_id: Long,
    val payload_data: String,
    val qr_type: String,
    val status: String,
    val reference_number: String? = null,
    val created_at: String
)

@Serializable
data class GenerateQRResponse(
    val message: String,
    val qr_code: QRCodeDetails,
    val payload: String
)

@Serializable
data class ScanQRRequest(
    val payload: String
)

@Serializable
data class QRScanResponse(
    val valid: Boolean = true,
    val message: String = "QR Code valid",
    val vendor: VendorSummary? = null,
    val qr_code: QRCodeDetails? = null,
    val amount: Double? = null,
    val is_dynamic: Boolean = false,
    val shortcode_type: String = "TILL",
    val error: String? = null
)

@Serializable
data class PaymentRequest(
    val qr_code_id: Long,
    val amount: Double,
    val phone: String? = null
)

@Serializable
data class TransactionResponse(
    val id: Long,
    val amount: Double,
    val currency: String = "404",
    val type: String,
    val status: String,
    val phone: String? = null,
    val account_reference: String? = null,
    val checkout_request_id: String? = null,
    val mpesa_receipt: String? = null,
    val initiated_at: String,
    val completed_at: String? = null,
    val merchant_name: String? = null,
    val customer_name: String? = null,
    val message: String? = null,
    val transaction_id: Long? = null
)

@Serializable
data class PaymentStatusResponse(
    val status: String, // "pending", "success", "failed", "cancelled"
    val transaction_id: Long,
    val mpesa_receipt: String? = null,
    val amount: Double,
    val message: String
)

@Serializable
data class DailyAnalytics(
    val date: String,
    val total: Double,
    val count: Int
)

@Serializable
data class CategorySpending(
    val category: String,
    val amount: Double,
    val count: Int,
    val percentage: Double = 0.0
)

@Serializable
data class AnalyticsResponse(
    val total_amount: Double = 0.0,
    val total_count: Int = 0,
    val today_amount: Double = 0.0,
    val today_count: Int = 0,
    val average_transaction: Double = 0.0,
    val daily: List<DailyAnalytics> = emptyList(),
    val categories: List<CategorySpending> = emptyList(),
    val currency: String = "KES"
)

@Serializable
data class MessageResponse(
    val message: String,
    val status: String = "success"
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ForgotPasswordResponse(
    val message: String,
    val reset_token: String? = null
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val new_password: String
)

@Serializable
data class TopUpRequest(
    val amount: Double,
    val phone: String
)

@Serializable
data class TopUpResponse(
    val message: String,
    val checkout_request_id: String? = null,
    val status: String = "pending"
)

@Serializable
data class TopUpBalanceResponse(
    val balance: Double = 0.0,
    val currency: String = "KES"
)

@Serializable
data class AppConfigResponse(
    val app_name: String = "ScanPay",
    val version: String = "1.0.0",
    val min_supported_version: String = "1.0.0",
    val currency: String = "KES",
    val default_country: String = "KE"
)

