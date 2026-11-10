package com.scanpay.server.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*

fun Route.topUpRoutes() {
    authenticate("jwt-auth") {
        route("/api/topup") {
            get("/balance") { call.requireRole("vendor"); throw ApiProblem(HttpStatusCode.NotImplemented, "ScanPay payments go directly to the merchant's M-Pesa account") }
            post("/initiate") { call.requireRole("vendor"); throw ApiProblem(HttpStatusCode.NotImplemented, "Wallet top-ups are not supported") }
        }
    }
}

