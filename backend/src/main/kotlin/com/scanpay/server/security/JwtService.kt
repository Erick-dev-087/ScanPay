package com.scanpay.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.scanpay.server.config.AppConfig
import java.util.Date

object JwtService {
    private val algorithm: Algorithm
        get() = Algorithm.HMAC256(AppConfig.jwtSecret)

    private const val ISSUER = "ScanPayServer"
    private const val VALIDITY_MS = 7L * 24 * 60 * 60 * 1000 // 7 days

    fun generateToken(id: Long, userType: String, email: String): String {
        val now = Date()
        val expiresAt = Date(now.time + VALIDITY_MS)

        return JWT.create()
            .withIssuer(ISSUER)
            .withSubject(id.toString())
            .withJWTId(java.util.UUID.randomUUID().toString())
            .withClaim("user_type", userType)
            .withClaim("email", email)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
    }

    fun verifyToken(token: String): DecodedJWT? {
        return try {
            val verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build()
            verifier.verify(token)
        } catch (e: Exception) {
            null
        }
    }

    fun extractId(jwt: DecodedJWT): Long? {
        return jwt.subject?.toLongOrNull()
    }

    fun extractUserType(jwt: DecodedJWT): String? {
        return jwt.getClaim("user_type")?.asString()
    }
}

