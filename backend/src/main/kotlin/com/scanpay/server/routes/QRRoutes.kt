package com.scanpay.server.routes

import com.scanpay.server.models.*
import com.scanpay.server.qr.*
import com.scanpay.server.repository.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.qrRoutes() {
    route("/api/qr") {
        authenticate("jwt-auth") {
            post("/generate") {
                val vendorId = call.requireRole("vendor")
                val vendor = VendorRepository.findById(vendorId)?.takeIf { it.is_active }
                    ?: throw ApiProblem(HttpStatusCode.NotFound, "Merchant is inactive")
                val req = call.receive<GenerateQRRequest>()
                val type = req.qr_type.uppercase()
                if (type !in setOf("STATIC", "DYNAMIC") || (type == "DYNAMIC" && (req.amount == null || !validAmount(req.amount))) ||
                    (type == "STATIC" && req.amount != null))
                    throw ApiProblem(HttpStatusCode.BadRequest, "Choose static QR or a dynamic QR with a whole KES amount")
                val reference = req.reference?.trim()?.takeIf { it.isNotEmpty() } ?: vendor.paybill_account_number
                if (reference != null && reference.length > 12) throw ApiProblem(HttpStatusCode.BadRequest, "Reference/account must be at most 12 characters")
                if (vendor.shortcode_type == "PAYBILL" && reference.isNullOrBlank())
                    throw ApiProblem(HttpStatusCode.BadRequest, "Paybill account number is required")
                val payload = EmvcoGenerator.generateMerchantQrPayload(vendor, req.amount, reference, type == "DYNAMIC")
                val qr = QRCode(vendor_id = vendorId, payload_data = payload, qr_type = type, reference_number = reference)
                QRRepository.save(qr)
                call.respond(HttpStatusCode.Created, GenerateQRResponse("QR ready to share", qr.details(), payload))
            }
            post("/scan") {
                call.requireRole("customer")
                val req = call.receive<ScanQRRequest>()
                val parsed = QRValidator.parseAndValidate(req.payload)
                if (!parsed.isValid) throw ApiProblem(HttpStatusCode.BadRequest, parsed.error ?: "Invalid QR")
                // Match the entire registered payload; a copied shortcode cannot change a stored bill.
                val qr = QRRepository.findByPayload(req.payload)
                    ?: throw ApiProblem(HttpStatusCode.NotFound, "This QR has not been registered with ScanPay")
                if (qr.status != "active") throw ApiProblem(HttpStatusCode.BadRequest, "QR code is inactive")
                val vendor = VendorRepository.findById(qr.vendor_id)?.takeIf { it.is_active }
                    ?: throw ApiProblem(HttpStatusCode.NotFound, "Merchant is inactive")
                call.respond(QRScanResponse(vendor = vendor.summary(), qr_code = qr.details(), amount = parsed.amount,
                    is_dynamic = parsed.isDynamic, shortcode_type = vendor.shortcode_type))
            }
        }
    }
}

