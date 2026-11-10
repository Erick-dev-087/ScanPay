package com.scanpay.server.routes

import com.scanpay.server.config.AppConfig
import com.scanpay.server.db.AstraCassandraClient
import com.scanpay.server.models.*
import com.scanpay.server.payment.*
import com.scanpay.server.qr.QRValidator
import com.scanpay.server.repository.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.security.MessageDigest

fun Route.paymentRoutes() {
    route("/api/payment") {
        authenticate("jwt-auth") {
            post("/initiate") {
                val userId = call.requireRole("customer")
                val req = call.receive<PaymentRequest>()
                if (!validAmount(req.amount)) throw ApiProblem(HttpStatusCode.BadRequest, "Enter a whole KES amount between 1 and 500,000")
                val key = call.request.headers["Idempotency-Key"]
                if (key == null || !key.matches(Regex("[a-zA-Z0-9-]{16,80}"))) throw ApiProblem(HttpStatusCode.BadRequest, "Idempotency-Key is required")
                val user = UserRepository.findById(userId)?.takeIf { it.is_active }
                    ?: throw ApiProblem(HttpStatusCode.Forbidden, "Account is inactive")
                val qr = QRRepository.findById(req.qr_code_id)?.takeIf { it.status == "active" }
                    ?: throw ApiProblem(HttpStatusCode.NotFound, "Active QR code not found")
                val parsed = QRValidator.parseAndValidate(qr.payload_data)
                if (!parsed.isValid || (parsed.isDynamic && parsed.amount != req.amount))
                    throw ApiProblem(HttpStatusCode.BadRequest, "The amount must match the bill encoded in this QR")
                val vendor = VendorRepository.findById(qr.vendor_id)?.takeIf { it.is_active }
                    ?: throw ApiProblem(HttpStatusCode.NotFound, "Merchant is inactive")
                val phone = normalizePhone(req.phone ?: user.phone_number)
                    ?: throw ApiProblem(HttpStatusCode.BadRequest, "Invalid Kenyan mobile number")
                val ref = qr.reference_number ?: parsed.reference ?: vendor.paybill_account_number ?: "SCANPAY"
                if (ref.isBlank() || ref.length > 12) throw ApiProblem(HttpStatusCode.BadRequest, "Paybill account/reference must contain 1–12 characters")
                DarajaService.configurationError(vendor.business_shortcode)?.let { throw ApiProblem(HttpStatusCode.ServiceUnavailable, it) }

                val tx = Transaction(amount = req.amount, status = "initiating", phone = phone, user_id = userId, vendor_id = vendor.id,
                    qrcode_id = qr.id, account_reference = ref, merchant_name = vendor.getDisplayName(), customer_name = user.name, merchant_category = vendor.mcc)
                val reservedId = AstraCassandraClient.claimPayment(userId, key, tx.id)
                if (reservedId != tx.id) {
                    val existing = TransactionRepository.findById(reservedId)
                        ?: throw ApiProblem(HttpStatusCode.Conflict, "Payment request is being prepared; retry with the same key")
                    if (existing.qrcode_id != req.qr_code_id || existing.amount != req.amount || existing.phone != phone)
                        throw ApiProblem(HttpStatusCode.Conflict, "This key belongs to a different payment")
                    call.respond(HttpStatusCode.OK, existing.response(customer = true))
                    return@post
                }
                TransactionRepository.save(tx)
                val result = DarajaService.initiateStkPush(phone, tx.amount, vendor.business_shortcode, vendor.shortcode_type, ref)
                if (!result.success || result.checkoutRequestId == null) {
                    TransactionRepository.complete(tx, if (result.uncertain) "unknown" else "failed")
                    throw ApiProblem(HttpStatusCode.BadGateway, result.message)
                }
                val pending = TransactionRepository.attachCheckout(tx, result.checkoutRequestId)
                call.respond(HttpStatusCode.Created, pending.response(customer = true).copy(message = result.message))
            }
            get("/{transaction_id}/status") {
                val id = call.parameters["transaction_id"]?.toLongOrNull() ?: throw ApiProblem(HttpStatusCode.BadRequest, "Invalid transaction")
                val tx = TransactionRepository.findById(id) ?: throw ApiProblem(HttpStatusCode.NotFound, "Transaction not found")
                val principal = call.principal<JWTPrincipal>()!!
                val role = principal.payload.getClaim("user_type").asString()
                val ownerId = principal.payload.subject.toLong()
                if (!((role == "customer" && tx.user_id == ownerId) || (role == "vendor" && tx.vendor_id == ownerId)))
                    throw ApiProblem(HttpStatusCode.Forbidden, "This transaction belongs to another account")
                call.respond(PaymentStatusResponse(tx.status, tx.id, tx.mpesa_receipt, tx.amount, "Payment ${tx.status}"))
            }
            if (AppConfig.paymentMode == "mock") post("/simulate_callback") {
                val userId = call.requireRole("customer")
                val id = call.receive<Map<String, String>>()["transaction_id"]?.toLongOrNull()
                    ?: throw ApiProblem(HttpStatusCode.BadRequest, "transaction_id is required")
                val tx = TransactionRepository.findById(id) ?: throw ApiProblem(HttpStatusCode.NotFound, "Transaction not found")
                if (tx.user_id != userId || tx.checkout_request_id?.startsWith("mock_") != true)
                    throw ApiProblem(HttpStatusCode.Forbidden, "Only your own test payment can be simulated")
                if (tx.status == "pending") TransactionRepository.complete(tx, "success", "TEST-${tx.id}")
                call.respond(MessageResponse("Test payment confirmed"))
            }
        }
        post("/stk_callback") {
            val supplied = call.request.headers["X-Callback-Secret"] ?: call.request.queryParameters["secret"].orEmpty()
            if (AppConfig.paymentMode != "daraja" || AppConfig.callbackSecret.length < 32 ||
                !MessageDigest.isEqual(supplied.toByteArray(), AppConfig.callbackSecret.toByteArray()))
                throw ApiProblem(HttpStatusCode.Unauthorized, "Invalid callback credentials")
            val callback = try { CallbackParser.parse(call.receiveText()) } catch (_: Exception) {
                throw ApiProblem(HttpStatusCode.BadRequest, "Invalid M-Pesa callback")
            }
            val tx = TransactionRepository.findByCheckoutRequestId(callback.checkoutId)
                ?: throw ApiProblem(HttpStatusCode.ServiceUnavailable, "Checkout not linked yet; retry")
            if (callback.resultCode == 0 && (callback.amount != tx.amount || normalizePhone(callback.phone.orEmpty()) != tx.phone))
                throw ApiProblem(HttpStatusCode.BadRequest, "Callback details do not match this payment")
            if (tx.status == "pending") TransactionRepository.complete(tx,
                when (callback.resultCode) { 0 -> "success"; 1032 -> "cancelled"; else -> "failed" }, callback.receipt)
            call.respond(buildCallbackAck())
        }
    }
}
private fun buildCallbackAck() = kotlinx.serialization.json.buildJsonObject {
    put("ResultCode", kotlinx.serialization.json.JsonPrimitive(0))
    put("ResultDesc", kotlinx.serialization.json.JsonPrimitive("Accepted"))
}

