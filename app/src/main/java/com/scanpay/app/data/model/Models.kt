package com.scanpay.app.data.model

import com.google.gson.annotations.SerializedName

data class User(
    val id: Long,
    val name: String,
    val email: String,
    @SerializedName("phone_number") val phoneNumber: String,
    @SerializedName("is_active") val isActive: Boolean = true
)

data class Vendor(
    val id: Long,
    val name: String,
    @SerializedName("business_name") val businessName: String? = null,
    @SerializedName("business_shortcode") val businessShortcode: String,
    @SerializedName("shortcode_type") val shortcodeType: String = "TILL",
    val email: String,
    val phone: String,
    @SerializedName("store_label") val storeLabel: String? = null,
    val mcc: String? = "5411",
    @SerializedName("is_active") val isActive: Boolean = true
) {
    fun getDisplayName(): String = businessName?.takeIf { it.isNotBlank() } ?: name
}

data class QRCode(
    val id: Long,
    @SerializedName("vendor_id") val vendorId: Long,
    @SerializedName("payload_data") val payloadData: String,
    @SerializedName("qr_type") val qrType: String = "STATIC",
    val status: String = "active",
    @SerializedName("reference_number") val referenceNumber: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class Transaction(
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
    @SerializedName("customer_name") val customerName: String? = null
)

