package com.scanpay.app.data.request

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val password: String
)

data class CustomerRegisterRequest(
    val name: String,
    @SerializedName("phone_number") val phoneNumber: String,
    val email: String,
    val password: String
)

data class MerchantRegisterRequest(
    val name: String,
    val email: String,
    val phone: String,
    val password: String,
    @SerializedName("business_shortcode") val businessShortcode: String,
    @SerializedName("business_name") val businessName: String? = null,
    @SerializedName("shortcode_type") val shortcodeType: String = "TILL",
    @SerializedName("paybill_account_number") val paybillAccountNumber: String? = null,
    @SerializedName("store_label") val storeLabel: String? = null,
    @SerializedName("merchant_id") val merchantId: String? = null,
    val mcc: String? = "5411"
)

data class GenerateQRRequest(
    @SerializedName("qr_type") val qrType: String = "STATIC",
    val amount: Double? = null,
    val reference: String? = null,
    @SerializedName("shortcode_type") val shortcodeType: String? = null
)

data class ScanQRRequest(
    val payload: String
)

data class PaymentRequest(
    @SerializedName("qr_code_id") val qrCodeId: Long,
    val amount: Double,
    val phone: String? = null
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val token: String,
    @SerializedName("new_password") val newPassword: String
)

data class TopUpRequest(
    val amount: Double,
    val phone: String
)

