package com.scanpay.server.routes

import com.scanpay.server.analytics.AnalyticsEngine
import com.scanpay.server.models.*
import com.scanpay.server.repository.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes() {
    route("/api/user") {
        authenticate("jwt-auth") {
            get("/profile") {
                val user = UserRepository.findById(call.requireRole("customer")) ?: throw ApiProblem(HttpStatusCode.NotFound, "Account not found")
                call.respond(AuthResponse("", "customer", user = user.summary()))
            }
            get("/transactions") {
                val id = call.requireRole("customer")
                val page = (call.request.queryParameters["page"]?.toIntOrNull() ?: 1).coerceIn(1, 100)
                val size = (call.request.queryParameters["per_page"]?.toIntOrNull() ?: 50).coerceIn(1, 100)
                call.respond(TransactionRepository.findByUserId(id, page * size).drop((page - 1) * size).map { it.response(true) })
            }
            get("/analytics") {
                val id = call.requireRole("customer")
                call.respond(AnalyticsEngine.getUserAnalytics(id, call.request.queryParameters["days"]?.toIntOrNull() ?: 30))
            }
        }
    }
}

