package com.scanpay.server.qr

import com.scanpay.server.models.Vendor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object EmvcoGenerator {

    fun formatTlv(tag: String, value: String): String {
        require(tag.matches(Regex("[0-9]{2}")) && value.length <= 99 && value.all { it.code in 32..126 }) { "Unsupported QR field" }
        val len = "%02d".format(value.length)
        return "$tag$len$value"
    }

    /**
     * Generates a CBK KE-QR standard compliant EMVCo payload for merchants.
     * Supports both TILL (Buy Goods - trx_type 03) and PAYBILL (Pay Bill - trx_type 04).
     */
    fun generateMerchantQrPayload(
        vendor: Vendor,
        amount: Double? = null,
        reference: String? = null,
        isDynamic: Boolean = false
    ): String {
        val isPaybill = vendor.shortcode_type.equals("PAYBILL", ignoreCase = true)
        val shortcode = vendor.business_shortcode.trim()

        val sb = StringBuilder()

        // 00: Payload Format Indicator
        sb.append(formatTlv("00", "01"))

        // 01: Point of Initiation Method: 11 = Static (customer inputs amount), 12 = Dynamic (fixed amount)
        val initiationMethod = if (amount != null && amount > 0 || isDynamic) "12" else "11"
        sb.append(formatTlv("01", initiationMethod))

        // 28: Merchant Account Information (Primary Rail)
        val sub28 = StringBuilder()
        sub28.append(formatTlv("00", "ke.go.qr"))
        sub28.append(formatTlv("01", shortcode))
        sb.append(formatTlv("28", sub28.toString()))

        // 29: Interoperability rail indicator
        val sub29 = formatTlv("00", "ke.go.qr")
        sb.append(formatTlv("29", sub29))

        // 52: Merchant Category Code (MCC)
        val mcc = vendor.mcc?.takeIf { it.isNotBlank() } ?: if (isPaybill) "4900" else "5411"
        sb.append(formatTlv("52", mcc))

        // 53: Transaction Currency (404 = KES)
        sb.append(formatTlv("53", "404"))

        // 54: Transaction Amount (dynamic only)
        if (amount != null && amount > 0) {
            val amountFormatted = "%.2f".format(Locale.US, amount)
            sb.append(formatTlv("54", amountFormatted))
        }

        // 58: Country Code
        sb.append(formatTlv("58", "KE"))

        // 59: Merchant Name (uppercased, max 25 chars)
        val rawName = vendor.getDisplayName().uppercase(Locale.ROOT).filter { it.code in 32..126 }.trim().ifBlank { "SCANPAY MERCHANT" }
        val merchantName = if (rawName.length > 25) rawName.substring(0, 25) else rawName
        sb.append(formatTlv("59", merchantName))

        // 60: Merchant City
        val storeCity = vendor.store_label?.filter { it.code in 32..126 }?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT) ?: "NAIROBI"
        sb.append(formatTlv("60", storeCity.take(15)))

        // 61: Postal Code (standard Safaricom "00")
        sb.append(formatTlv("61", "00"))

        // 62: Additional Data Field Template
        val sub62 = StringBuilder()
        if (isPaybill) {
            val billRef = reference?.takeIf { it.isNotBlank() }
                ?: vendor.paybill_account_number?.takeIf { it.isNotBlank() }
                ?: "ONLINE"
            sub62.append(formatTlv("01", billRef.take(25))) // Bill number / Account number
        } else if (!reference.isNullOrBlank()) {
            sub62.append(formatTlv("05", reference.take(25))) // Reference label
        }
        if (sub62.isNotEmpty()) {
            sb.append(formatTlv("62", sub62.toString()))
        }

        // 82: QR Timestamp
        val now = LocalDateTime.now()
        val timestampStr = now.format(DateTimeFormatter.ofPattern("ddMMyyyy HHmmss"))
        val sub82 = formatTlv("00", "ke.go.qr") + formatTlv("01", timestampStr)
        sb.append(formatTlv("82", sub82))

        // 83: M-Pesa Routing Block
        // 01 = Withdraw, 02 = Send Money, 03 = Buy Goods (Till), 04 = Paybill
        val trxCode = if (isPaybill) "04" else "03"
        val sub83 = formatTlv("00", "m-pesa.com") + formatTlv("01", trxCode)
        sb.append(formatTlv("83", sub83))

        // 63: CRC-16 Checksum
        val bodyWithTag63 = sb.toString() + "6304"
        val crc = Crc16Ccitt.calculate(bodyWithTag63)

        return bodyWithTag63 + crc
    }
}

