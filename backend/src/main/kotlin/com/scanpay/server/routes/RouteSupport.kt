package com.scanpay.server.routes

import com.scanpay.server.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

class ApiProblem(val status: HttpStatusCode, override val message: String) : RuntimeException(message)
fun ApplicationCall.requireRole(role: String): Long {
    val jwt = principal<JWTPrincipal>() ?: throw ApiProblem(HttpStatusCode.Unauthorized, "Please sign in")
    if (jwt.payload.getClaim("user_type").asString() != role) throw ApiProblem(HttpStatusCode.Forbidden, "This account cannot access this feature")
    return jwt.payload.subject.toLongOrNull() ?: throw ApiProblem(HttpStatusCode.Unauthorized, "Invalid account")
}
fun Vendor.summary() = VendorSummary(id, name, business_name, business_shortcode, shortcode_type, email, phone, store_label, mcc, is_active)
fun User.summary() = UserSummary(id, name, email, phone_number, is_active)
fun QRCode.details() = QRCodeDetails(id, vendor_id, payload_data, qr_type, status, reference_number, created_at)
fun Transaction.response(customer: Boolean = false) = TransactionResponse(
    id, amount, currency, if (customer) "outgoing" else "incoming", status, phone, account_reference, checkout_request_id,
    mpesa_receipt, initiated_at, completed_at, merchant_name, customer_name, transaction_id = id)
fun validAmount(amount: Double) = amount.isFinite() && amount in 1.0..500000.0 && amount % 1.0 == 0.0
fun normalizePhone(phone: String): String? {
    if (!phone.matches(Regex("[+0-9\\s()-]+"))) return null
    val digits = phone.filter(Char::isDigit)
    val normalized = when {
        digits.length == 10 && digits.startsWith("0") -> "254" + digits.drop(1)
        digits.length == 9 -> "254$digits"
        else -> digits
    }
    return normalized.takeIf { it.matches(Regex("254[17][0-9]{8}")) }
}

