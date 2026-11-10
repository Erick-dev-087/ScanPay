package com.scanpay.server.routes

import com.scanpay.server.db.AstraCassandraClient
import com.scanpay.server.models.*
import com.scanpay.server.qr.EmvcoGenerator
import com.scanpay.server.repository.*
import com.scanpay.server.security.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun validateAccount(name: String, email: String, phone: String, password: String): String {
    if (name.isBlank() || name.length > 100 || !email.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) || email.length > 254)
        throw ApiProblem(HttpStatusCode.BadRequest, "Enter a name and valid email")
    if (password.length < 8 || password.toByteArray().size > 72)
        throw ApiProblem(HttpStatusCode.BadRequest, "Password must be 8–72 bytes")
    return normalizePhone(phone) ?: throw ApiProblem(HttpStatusCode.BadRequest, "Invalid Kenyan mobile number")
}
private suspend fun claim(email: String, role: String, id: Long) {
    if (!AstraCassandraClient.claimEmail(email, role, id)) throw ApiProblem(HttpStatusCode.Conflict, "An account with this email already exists")
}
fun Route.authRoutes() {
    route("/api/auth") {
        post("/register/user") {
            val req = call.receive<CustomerRegisterRequest>()
            val phone = validateAccount(req.name, req.email.trim(), req.phone_number, req.password)
            val user = User(name = req.name.trim(), email = req.email.trim().lowercase(), phone_number = phone,
                password_hash = PasswordHasher.hashPassword(req.password))
            claim(user.email, "customer", user.id)
            UserRepository.save(user)
            call.respond(HttpStatusCode.Created, AuthResponse(JwtService.generateToken(user.id, "customer", user.email),
                "customer", user = user.summary(), message = "Account created"))
        }
        post("/register/vendor") {
            val req = call.receive<MerchantRegisterRequest>()
            val phone = validateAccount(req.name, req.email.trim(), req.phone, req.password)
            val type = req.shortcode_type.uppercase()
            if (type !in setOf("TILL", "PAYBILL") || !req.business_shortcode.matches(Regex("[0-9]{5,10}")))
                throw ApiProblem(HttpStatusCode.BadRequest, "Enter a valid till or paybill number")
            if (type == "PAYBILL" && (req.paybill_account_number.isNullOrBlank() || req.paybill_account_number.length > 12))
                throw ApiProblem(HttpStatusCode.BadRequest, "Paybill account number must contain 1–12 characters")
            if (req.mcc != null && !req.mcc.matches(Regex("[0-9]{4}"))) throw ApiProblem(HttpStatusCode.BadRequest, "Invalid merchant category")
            val vendor = Vendor(name = req.name.trim(), business_name = req.business_name?.trim(), business_shortcode = req.business_shortcode,
                shortcode_type = type, paybill_account_number = req.paybill_account_number?.trim(),
                email = req.email.trim().lowercase(), phone = phone, password_hash = PasswordHasher.hashPassword(req.password),
                store_label = req.store_label, mcc = req.mcc)
            claim(vendor.email, "vendor", vendor.id)
            VendorRepository.save(vendor)
            // Explicit QR generation happens on the merchant screen, including the bill reference.
            call.respond(HttpStatusCode.Created, VendorAuthResponse(JwtService.generateToken(vendor.id, "vendor", vendor.email), vendor = vendor.summary()))
        }
        post("/login") {
            val req = call.receive<LoginRequest>()
            val email = req.email.trim().lowercase()
            val user = UserRepository.findByEmail(email)
            if (user != null && user.is_active && PasswordHasher.verifyPassword(req.password, user.password_hash)) {
                call.respond(AuthResponse(JwtService.generateToken(user.id, "customer", user.email), "customer", user = user.summary()))
                return@post
            }
            val vendor = VendorRepository.findByEmail(email)
            if (vendor != null && vendor.is_active && PasswordHasher.verifyPassword(req.password, vendor.password_hash)) {
                call.respond(AuthResponse(JwtService.generateToken(vendor.id, "vendor", vendor.email), "vendor", vendor = vendor.summary()))
                return@post
            }
            throw ApiProblem(HttpStatusCode.Unauthorized, "Invalid email or password")
        }
        authenticate("jwt-auth") {
            post("/logout") {
                AstraCassandraClient.revoke(call.principal<JWTPrincipal>()!!.payload.id)
                call.respond(MessageResponse("Signed out"))
            }
        }
        post("/forgot-password") { throw ApiProblem(HttpStatusCode.NotImplemented, "Password reset email delivery is not configured") }
        post("/reset-password") { throw ApiProblem(HttpStatusCode.NotImplemented, "Password reset email delivery is not configured") }
    }
}

