package com.scanpay.server.payment

import com.scanpay.server.routes.normalizePhone
import com.scanpay.server.routes.validAmount
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PaymentSafetyTest {
    private fun callback(code: Int = 0, metadata: String = """{"Item":[{"Name":"Amount","Value":1500},{"Name":"MpesaReceiptNumber","Value":"QGK123ABC"},{"Name":"PhoneNumber","Value":254712345678}]}""") =
        """{"Body":{"stkCallback":{"CheckoutRequestID":"ws_123","ResultCode":$code,"CallbackMetadata":$metadata}}}"""

    @Test fun successfulCallbackUsesProviderReceiptAndAmount() {
        val parsed = CallbackParser.parse(callback())
        assertEquals("QGK123ABC", parsed.receipt)
        assertEquals(1500.0, parsed.amount)
        assertEquals("254712345678", parsed.phone)
    }
    @Test fun successfulCallbackRequiresActualReceiptAndPhone() {
        assertThrows(IllegalArgumentException::class.java) { CallbackParser.parse(callback(metadata = """{"Item":[]}""")) }
    }
    @Test fun cancellationDoesNotNeedReceiptMetadata() {
        val parsed = CallbackParser.parse("""{"Body":{"stkCallback":{"CheckoutRequestID":"ws_123","ResultCode":1032}}}""")
        assertEquals(1032, parsed.resultCode)
        assertNull(parsed.receipt)
    }
    @Test fun phoneAndAmountValidationRejectsInvalidPayments() {
        assertEquals("254112345678", normalizePhone("+254 112 345 678"))
        assertEquals("254712345678", normalizePhone("0712345678"))
        assertNull(normalizePhone("254212345678"))
        assertNull(normalizePhone("abc0712345678"))
        assertFalse(validAmount(Double.NaN))
        assertFalse(validAmount(Double.POSITIVE_INFINITY))
        assertFalse(validAmount(1.5))
        assertFalse(validAmount(500001.0))
        assertTrue(validAmount(1500.0))
    }
    @Test fun authenticationFailureNeverFallsBackToSimulatedPayment() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))
            val client = DarajaClient(OkHttpClient(), server.url("/").toString().trimEnd('/'), "key", "secret", "passkey", "https://callback.test")
            val result = client.initiate("254712345678", 1500.0, "123456", "TILL", "SALE")
            assertFalse(result.success)
            assertNull(result.checkoutRequestId)
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun acceptedTillAndPaybillRequestsUseCorrectRailAndAccount() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"access_token":"test","expires_in":"3600"}"""))
            server.enqueue(MockResponse().setBody("""{"ResponseCode":"0","CheckoutRequestID":"ws_till"}"""))
            server.enqueue(MockResponse().setBody("""{"ResponseCode":"0","CheckoutRequestID":"ws_bill"}"""))
            val client = DarajaClient(OkHttpClient(), server.url("/").toString().trimEnd('/'), "key", "secret", "passkey", "https://callback.test")
            assertTrue(client.initiate("254712345678", 50.0, "123456", "TILL", "SALE").success)
            assertTrue(client.initiate("254712345678", 1500.0, "888888", "PAYBILL", "ACC-9999").success)
            server.takeRequest()
            val till = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            val bill = Json.parseToJsonElement(server.takeRequest().body.readUtf8()).jsonObject
            assertEquals("CustomerBuyGoodsOnline", till.getValue("TransactionType").jsonPrimitive.content)
            assertEquals("CustomerPayBillOnline", bill.getValue("TransactionType").jsonPrimitive.content)
            assertEquals("ACC-9999", bill.getValue("AccountReference").jsonPrimitive.content)
            assertEquals(1500, bill.getValue("Amount").jsonPrimitive.int)
            assertEquals("888888", bill.getValue("BusinessShortCode").jsonPrimitive.content)
        }
    }
    @Test fun providerRejectionDoesNotBecomePending() {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"access_token":"test"}"""))
            server.enqueue(MockResponse().setResponseCode(400).setBody("""{"errorCode":"invalid"}"""))
            val client = DarajaClient(OkHttpClient(), server.url("/").toString().trimEnd('/'), "key", "secret", "passkey", "https://callback.test")
            assertFalse(client.initiate("254712345678", 50.0, "123456", "TILL", "SALE").success)
        }
    }
}

