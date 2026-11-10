package com.scanpay.server.models

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
data class User(
    val _id: String = UUID.randomUUID().toString(),
    val id: Long = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE),
    val name: String,
    val phone_number: String,
    val email: String,
    val password_hash: String,
    val is_active: Boolean = true,
    val created_at: String = Instant.now().toString(),
    val updated_at: String = Instant.now().toString(),
    val last_login: String? = null
)

@Serializable
data class Vendor(
    val _id: String = UUID.randomUUID().toString(),
    val id: Long = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE),
    val name: String,
    val business_name: String? = null,
    val business_shortcode: String, // Till Number or Paybill Number
    val shortcode_type: String = "TILL", // "TILL" or "PAYBILL"
    val paybill_account_number: String? = null,
    val merchant_id: String? = null,
    val mcc: String? = "5411", // Merchant Category Code
    val country_code: String = "KE",
    val currency_code: String = "404", // 404 = KES
    val store_label: String? = null,
    val email: String,
    val phone: String,
    val password_hash: String,
    val psp_id: String? = "PSP001",
    val psp_name: String? = "Safaricom",
    val payout_method: String = "personal_phone",
    val is_active: Boolean = true,
    val created_at: String = Instant.now().toString(),
    val updated_at: String = Instant.now().toString()
) {
    fun getDisplayName(): String = business_name?.takeIf { it.isNotBlank() } ?: name
}

@Serializable
data class QRCode(
    val _id: String = UUID.randomUUID().toString(),
    val id: Long = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE),
    val vendor_id: Long,
    val payload_data: String, // Full EMVCo compliant string
    val payload_json: String? = null,
    val qr_type: String = "STATIC", // "STATIC" or "DYNAMIC"
    val status: String = "active", // "active", "inactive", "expired"
    val currency_code: String = "404",
    val reference_number: String? = null,
    val created_at: String = Instant.now().toString(),
    val last_scanned_at: String = Instant.now().toString(),
    val qr_code_image_url: String? = null
)

@Serializable
data class Transaction(
    val _id: String = UUID.randomUUID().toString(),
    val id: Long = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE),
    val amount: Double,
    val currency: String = "404",
    val type: String = "incoming", // "incoming" or "outgoing"
    val status: String = "pending", // "pending", "success", "failed", "cancelled"
    val account_reference: String? = null,
    val checkout_request_id: String? = null,
    val mpesa_receipt: String? = null,
    val phone: String? = null,
    val user_id: Long? = null,
    val vendor_id: Long? = null,
    val qrcode_id: Long? = null,
    val initiated_at: String = Instant.now().toString(),
    val completed_at: String? = null,
    val merchant_name: String? = null,
    val customer_name: String? = null,
    val merchant_category: String? = null
)

@Serializable
data class ScanLog(
    val _id: String = UUID.randomUUID().toString(),
    val id: Long = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE),
    val qr_id: Long,
    val user_id: Long,
    val status: String = "scanned_only", // "scanned_only", "payment_initiated", "payment_success", "payment_failed"
    val timestamp: String = Instant.now().toString()
)

@Serializable
data class PaymentSession(
    val _id: String = UUID.randomUUID().toString(),
    val id: Long = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE),
    val amount: Double,
    val status: String = "initiated", // "initiated", "pending", "succeeded", "expired"
    val started_at: String = Instant.now().toString(),
    val expired_at: String? = null,
    val qr_id: Long,
    val user_id: Long,
    val transaction_id: Long? = null
)

@Serializable
data class Wallet(
    val _id: String = UUID.randomUUID().toString(),
    val vendor_id: Long,
    val pending_balance: Double = 0.0,
    val available_balance: Double = 0.0,
    val updated_at: String = Instant.now().toString()
)


