package com.scanpay.server.qr

import com.scanpay.server.models.Vendor
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EmvcoGeneratorTest {

    @Test
    fun testCrc16CalculationAndValidation() {
        val body = "00020101021153034045802KE5914DOWNTOWN STORE6007NAIROBI610200"
        val bodyWithTag = body + "6304"
        val crc = Crc16Ccitt.calculate(bodyWithTag)
        assertEquals(4, crc.length)

        val fullPayload = bodyWithTag + crc
        assertTrue(Crc16Ccitt.validate(fullPayload))
    }

    @Test
    fun testGenerateAndParseTillStaticQr() {
        val vendor = Vendor(
            id = 1001L,
            name = "Test Merchant",
            business_name = "Super Store",
            business_shortcode = "123456",
            shortcode_type = "TILL",
            email = "vendor@test.com",
            phone = "254712345678",
            password_hash = "hash"
        )

        val payload = EmvcoGenerator.generateMerchantQrPayload(vendor)
        assertNotNull(payload)
        assertTrue(Crc16Ccitt.validate(payload))

        val parsed = QRValidator.parseAndValidate(payload)
        assertTrue(parsed.isValid)
        assertEquals("123456", parsed.shortcode)
        assertEquals("TILL", parsed.transactionType)
        assertFalse(parsed.isDynamic)
        assertNull(parsed.amount)
    }

    @Test
    fun testGenerateAndParsePaybillDynamicQr() {
        val vendor = Vendor(
            id = 1002L,
            name = "Power Company",
            business_name = "Kenya Power",
            business_shortcode = "888888",
            shortcode_type = "PAYBILL",
            paybill_account_number = "ACC-9999",
            email = "power@test.com",
            phone = "254700000000",
            password_hash = "hash"
        )

        val payload = EmvcoGenerator.generateMerchantQrPayload(
            vendor = vendor,
            amount = 1500.0,
            reference = "ACC-9999",
            isDynamic = true
        )
        assertNotNull(payload)
        assertTrue(Crc16Ccitt.validate(payload))

        val parsed = QRValidator.parseAndValidate(payload)
        assertTrue(parsed.isValid)
        assertEquals("888888", parsed.shortcode)
        assertEquals("PAYBILL", parsed.transactionType)
        assertTrue(parsed.isDynamic)
        assertEquals(1500.0, parsed.amount)
        assertEquals("ACC-9999", parsed.reference)
    }
}

