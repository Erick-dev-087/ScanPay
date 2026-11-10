package com.scanpay.server.db

import com.scanpay.server.analytics.AnalyticsEngine
import com.scanpay.server.models.*
import com.scanpay.server.qr.*
import com.scanpay.server.repository.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.util.UUID

/** Explicit opt-in: inserts uniquely identified fixtures and removes only those fixtures in finally. */
@EnabledIfEnvironmentVariable(named = "SCANPAY_INTEGRATION_TESTS", matches = "true")
class AstraRepositoryIntegrationTest {
    @Test fun persistenceQueriesAndConditionalConfirmation() = runBlocking {
        val suffix = UUID.randomUUID().toString()
        val user = User(name = "ScanPay verification", email = "verify-user-$suffix@example.test",
            phone_number = "254712345678", password_hash = "integration-fixture")
        val vendor = Vendor(name = "ScanPay verification", email = "verify-vendor-$suffix@example.test",
            phone = "254712345679", business_shortcode = "174379", password_hash = "integration-fixture")
        val qr = QRCode(vendor_id = vendor.id, qr_type = "DYNAMIC",
            payload_data = EmvcoGenerator.generateMerchantQrPayload(vendor, 50.0, suffix.take(12), true))
        val tx = Transaction(amount = 50.0, status = "initiating", user_id = user.id, vendor_id = vendor.id,
            qrcode_id = qr.id, merchant_category = "5411")
        AstraCassandraClient.connect()
        try {
            assertTrue(UserRepository.save(user))
            assertTrue(VendorRepository.save(vendor))
            assertTrue(QRRepository.save(qr))
            assertTrue(TransactionRepository.save(tx))
            assertEquals(user.id, UserRepository.findByEmail(user.email)?.id)
            assertEquals(qr.id, QRRepository.findByPayload(qr.payload_data)?.id)
            assertEquals(qr.id, QRRepository.findByVendorId(vendor.id).single().id)
            val pending = TransactionRepository.attachCheckout(tx, "verification-$suffix")
            assertEquals(tx.id, TransactionRepository.findByCheckoutRequestId("verification-$suffix")?.id)
            assertTrue(TransactionRepository.complete(pending, "success", "TEST-$suffix"))
            assertFalse(TransactionRepository.complete(pending, "success", "DUPLICATE"))
            assertEquals("TEST-$suffix", TransactionRepository.findById(tx.id)?.mpesa_receipt)
            assertEquals(50.0, AnalyticsEngine.getUserAnalytics(user.id, 7).total_amount)
            assertEquals(50.0, AnalyticsEngine.getMerchantAnalytics(vendor.id, 7).total_amount)
            // A fresh native session must retrieve the same confirmed state.
            AstraCassandraClient.close()
            AstraCassandraClient.connect(migrate = false)
            assertEquals("success", TransactionRepository.findById(tx.id)?.status)
        } finally {
            try {
                val cleanup = listOf("transactions" to tx.id, "qr_codes" to qr.id, "users" to user.id, "vendors" to vendor.id)
                    .map { (table, id) -> runCatching { AstraCassandraClient.deleteOne(table, id) } }
                cleanup.forEach { it.getOrThrow() }
            } finally { AstraCassandraClient.close() }
        }
    }
}
