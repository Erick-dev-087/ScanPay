package com.scanpay.server.routes

import com.scanpay.server.analytics.AnalyticsEngine
import com.scanpay.server.repository.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.merchantRoutes() {
    route("/api/merchant") {
        authenticate("jwt-auth") {
            get("/profile") {
                val vendor = VendorRepository.findById(call.requireRole("vendor")) ?: throw ApiProblem(HttpStatusCode.NotFound, "Merchant not found")
                call.respond(vendor.summary())
            }
            get("/transactions") {
                val id = call.requireRole("vendor")
                val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceIn(1, 100)
                val size = (call.request.queryParameters["per_page"]?.toIntOrNull() ?: 50).coerceIn(1, 100)
                call.respond(TransactionRepository.findByVendorId(id, page * size).drop((page - 1) * size).map { it.response() })
            }
            get("/analytics") {
                val id = call.requireRole("vendor")
                call.respond(AnalyticsEngine.getMerchantAnalytics(id, call.request.queryParameters["days"]?.toIntOrNull() ?: 30))
            }
            get("/qr") {
                val id = call.requireRole("vendor")
                call.respond(QRRepository.findByVendorId(id).map { it.details() })
            }
        }
    }
}

