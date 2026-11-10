package com.scanpay.server

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.datastax.oss.driver.api.core.DriverException
import com.scanpay.server.config.AppConfig
import com.scanpay.server.db.*
import com.scanpay.server.models.AppConfigResponse
import com.scanpay.server.routes.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*

fun main() {
    AstraCassandraClient.connect()
    Runtime.getRuntime().addShutdownHook(Thread { AstraCassandraClient.close() })
    embeddedServer(Netty, port = AppConfig.serverPort, host = "0.0.0.0", module = Application::module).start(wait = true)
}
fun Application.module() {
    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
    install(StatusPages) {
        exception<ApiProblem> { call, cause -> call.respond(cause.status, mapOf("error" to cause.message)) }
        exception<BadRequestException> { call, _ -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid request body")) }
        exception<SerializationException> { call, _ -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid JSON")) }
        exception<IllegalArgumentException> { call, cause -> call.respond(HttpStatusCode.BadRequest, mapOf("error" to (cause.message ?: "Invalid request"))) }
        exception<Throwable> { call, cause ->
            val unavailable = cause is DriverException || cause is DatabaseUnavailable
            this@module.log.error("Request failed: ${cause.javaClass.simpleName}")
            call.respond(if (unavailable) HttpStatusCode.ServiceUnavailable else HttpStatusCode.InternalServerError,
                mapOf("error" to if (unavailable) "Database unavailable; please try again" else "Unable to complete this request"))
        }
    }
    install(Authentication) {
        jwt("jwt-auth") {
            verifier(JWT.require(Algorithm.HMAC256(AppConfig.jwtSecret)).withIssuer("ScanPayServer").build())
            validate {
                val id = it.payload.subject?.toLongOrNull()
                val role = it.payload.getClaim("user_type").asString()
                val jti = it.payload.id
                if (id != null && id > 0 && role in setOf("customer", "vendor") && jti != null && !AstraCassandraClient.isRevoked(jti))
                    JWTPrincipal(it.payload) else null
            }
            challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Please sign in again")) }
        }
    }
    routing {
        get("/api/health") {
            if (!AstraCassandraClient.ping()) throw DatabaseUnavailable()
            call.respond(buildJsonObject { put("status", "healthy"); put("database", "Astra Cassandra"); put("payment_mode", AppConfig.paymentMode) })
        }
        get("/api/config") { call.respond(AppConfigResponse()) }
        authRoutes(); qrRoutes(); paymentRoutes(); userRoutes(); merchantRoutes(); topUpRoutes()
    }
}

