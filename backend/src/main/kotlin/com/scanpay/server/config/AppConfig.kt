package com.scanpay.server.config

import kotlinx.serialization.json.*
import java.io.File
import java.security.SecureRandom
import java.util.Base64

object AppConfig {
    private val values = run {
        val explicit = System.getenv("SCANPAY_ENV_FILE")?.let(::File)
        if (explicit != null) require(explicit.isFile) { "SCANPAY_ENV_FILE does not exist" }
        val file = explicit ?: listOf(File(".env"), File("../.env"), File("Kotlin_Scan_Pay/.env")).firstOrNull { it.isFile }
        val content = file?.readText()?.trim().orEmpty()
        if (content.startsWith("{")) Json.parseToJsonElement(content).jsonObject.mapValues { it.value.jsonPrimitive.content }
        else content.lineSequence().map { it.trim().removePrefix("export ") }
            .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
            .associate { val parts = it.split("=", limit = 2); parts[0].trim() to parts[1].trim().removeSurrounding("\"").removeSurrounding("'") }
    }
    private fun setting(vararg names: String, default: String = ""): String =
        names.firstNotNullOfOrNull { System.getenv(it)?.takeIf(String::isNotBlank) }
            ?: names.firstNotNullOfOrNull { values[it]?.takeIf(String::isNotBlank) } ?: default

    val token = setting("ASTRA_TOKEN", "ASTRA_DB_APPLICATION_TOKEN", "token")
    val clientId = setting("ASTRA_CLIENT_ID", "CLIENT_ID", "clientId", default = "token")
    val secret = setting("ASTRA_CLIENT_SECRET", "CLIENT_SECRET", "secret", default = token)
    val databaseId = setting("ASTRA_DATABASE_ID", "DATABASE_ID", "databaseId")
    val keyspace = setting("ASTRA_KEYSPACE", "keyspace", default = "default_keyspace")
    val region = setting("ASTRA_REGION", "region", default = "us-east-2")
    val dataApiUrl = setting("ASTRA_DATA_API_URL", "dataApiUrl", default = "https://$databaseId-$region.apps.astra.datastax.com/api/json/v1/$keyspace")
    val secureConnectBundle = setting("ASTRA_SECURE_CONNECT_BUNDLE")
    val autoMigrate = setting("ASTRA_AUTO_MIGRATE", default = "true").toBooleanStrict()
    val serverPort = setting("PORT", "serverPort", default = "5000").toInt()
    val jwtSecret = setting("JWT_SECRET_KEY", "jwtSecret").ifBlank {
        require(setting("SCANPAY_ALLOW_EPHEMERAL_JWT").toBoolean()) { "Set JWT_SECRET_KEY (at least 32 characters)" }
        Base64.getEncoder().encodeToString(ByteArray(48).also { SecureRandom().nextBytes(it) })
    }.also { require(it.length >= 32) { "JWT_SECRET_KEY must have at least 32 characters" } }
    val paymentMode = setting("PAYMENT_MODE", default = "daraja").lowercase()
    val darajaBaseUrl = setting("DARAJA_BASE_URL", "darajaBaseUrl", default = "https://sandbox.safaricom.co.ke")
    val darajaConsumerKey = setting("DARAJA_CONSUMER_KEY", "darajaConsumerKey")
    val darajaConsumerSecret = setting("DARAJA_CONSUMER_SECRET", "darajaConsumerSecret")
    val darajaShortcode = setting("DARAJA_SHORTCODE", "darajaShortcode")
    val darajaPasskey = setting("DARAJA_PASSKEY", "darajaPasskey")
    val darajaCallbackUrl = setting("DARAJA_CALLBACK_URL", "darajaCallbackUrl")
    val callbackSecret = setting("PAYMENT_CALLBACK_SECRET")
    val merchantShortcodes = setting("DARAJA_APPROVED_SHORTCODES", default = darajaShortcode)
        .split(",").map(String::trim).filter(String::isNotBlank).toSet()

    fun validate() {
        require(keyspace.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*"))) { "Invalid Astra keyspace" }
        require(paymentMode in setOf("daraja", "mock")) { "PAYMENT_MODE must be daraja or mock" }
        require(token.isNotBlank() || secret.isNotBlank()) { "Astra credentials are required" }
        require(dataApiUrl.isNotBlank() || secureConnectBundle.isNotBlank() || databaseId.isNotBlank()) { "Set ASTRA_DATABASE_ID or ASTRA_DATA_API_URL" }
    }
}

