package com.scanpay.server.payment

import com.scanpay.server.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Base64
import java.util.UUID

data class StkPushResult(val success: Boolean, val checkoutRequestId: String? = null, val message: String,
    val uncertain: Boolean = false)

/** Credentials must be provisioned for the recipient; a public till number alone cannot authorize STK requests. */
class DarajaClient(private val http: OkHttpClient, private val baseUrl: String, private val key: String,
    private val secret: String, private val passkey: String, private val callbackUrl: String) {
    @Volatile private var token: String? = null
    @Volatile private var expiresAt = 0L
    @Synchronized private fun accessToken(): String {
        if (token != null && System.currentTimeMillis() < expiresAt) return token!!
        val req = Request.Builder().url("$baseUrl/oauth/v1/generate?grant_type=client_credentials")
            .header("Authorization", Credentials.basic(key, secret)).build()
        http.newCall(req).execute().use {
            check(it.isSuccessful) { "Daraja authentication failed" }
            val root = Json.parseToJsonElement(it.body!!.string()).jsonObject
            token = root.getValue("access_token").jsonPrimitive.content
            expiresAt = System.currentTimeMillis() + ((root["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600) - 300).coerceAtLeast(1) * 1000
            return token!!
        }
    }
    fun initiate(phone: String, amount: Double, shortcode: String, type: String, reference: String): StkPushResult {
        val auth = try { accessToken() } catch (_: Exception) {
            return StkPushResult(false, message = "M-Pesa authentication unavailable")
        }
        val stamp = LocalDateTime.now(ZoneId.of("Africa/Nairobi")).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
        val password = Base64.getEncoder().encodeToString("$shortcode$passkey$stamp".toByteArray())
        val payload = buildJsonObject {
            put("BusinessShortCode", shortcode); put("Password", password); put("Timestamp", stamp)
            put("TransactionType", if (type == "PAYBILL") "CustomerPayBillOnline" else "CustomerBuyGoodsOnline")
            put("Amount", amount.toLong()); put("PartyA", phone); put("PartyB", shortcode); put("PhoneNumber", phone)
            put("CallBackURL", callbackUrl); put("AccountReference", reference); put("TransactionDesc", "ScanPay")
        }
        val req = Request.Builder().url("$baseUrl/mpesa/stkpush/v1/processrequest")
            .header("Authorization", "Bearer $auth").post(payload.toString().toRequestBody("application/json".toMediaType())).build()
        return try {
            http.newCall(req).execute().use {
                val root = Json.parseToJsonElement(it.body!!.string()).jsonObject
                val checkout = root["CheckoutRequestID"]?.jsonPrimitive?.contentOrNull
                if (it.isSuccessful && root["ResponseCode"]?.jsonPrimitive?.content == "0" && !checkout.isNullOrBlank())
                    StkPushResult(true, checkout, "Enter your M-Pesa PIN in the prompt on your phone")
                else StkPushResult(false, message = "M-Pesa rejected the payment request")
            }
        } catch (_: Exception) {
            StkPushResult(false, message = "M-Pesa did not confirm acceptance. Check your M-Pesa messages before trying again.", uncertain = true)
        }
    }
}
object DarajaService {
    private val http = OkHttpClient.Builder().callTimeout(Duration.ofSeconds(25)).retryOnConnectionFailure(false).build()
    private val client by lazy { DarajaClient(http, AppConfig.darajaBaseUrl, AppConfig.darajaConsumerKey,
        AppConfig.darajaConsumerSecret, AppConfig.darajaPasskey, AppConfig.darajaCallbackUrl) }
    fun configurationError(shortcode: String): String? {
        if (AppConfig.paymentMode == "mock") return null
        if (listOf(AppConfig.darajaConsumerKey, AppConfig.darajaConsumerSecret, AppConfig.darajaPasskey).any { it.isBlank() || it.startsWith("mock") })
            return "M-Pesa credentials have not been configured"
        if (!AppConfig.darajaCallbackUrl.startsWith("https://") || AppConfig.callbackSecret.length < 32)
            return "Configure a public HTTPS callback and PAYMENT_CALLBACK_SECRET"
        if (shortcode !in AppConfig.merchantShortcodes) return "This merchant needs M-Pesa payment provisioning before accepting STK payments"
        return null
    }
    suspend fun initiateStkPush(phoneNumber: String, amount: Double, shortcode: String, shortcodeType: String,
        accountReference: String, transactionDesc: String = "ScanPay"): StkPushResult = withContext(Dispatchers.IO) {
        if (AppConfig.paymentMode == "mock") StkPushResult(true, "mock_${UUID.randomUUID()}", "Test payment created; no M-Pesa prompt was sent")
        else client.initiate(phoneNumber, amount, shortcode, shortcodeType, accountReference)
    }
}

