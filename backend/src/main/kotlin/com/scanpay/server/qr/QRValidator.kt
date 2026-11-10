package com.scanpay.server.qr

data class ParsedQR(
    val isValid: Boolean,
    val shortcode: String? = null,
    val merchantName: String? = null,
    val amount: Double? = null,
    val isDynamic: Boolean = false,
    val transactionType: String = "TILL", // "TILL" or "PAYBILL"
    val reference: String? = null,
    val currency: String = "404",
    val error: String? = null
)

object QRValidator {

    fun parseTlv(s: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        var i = 0
        while (i < s.length) {
            require(i + 4 <= s.length) { "Incomplete TLV header" }
            val tag = s.substring(i, i + 2)
            val lenStr = s.substring(i + 2, i + 4)
            require(tag.all(Char::isDigit) && !out.containsKey(tag)) { "Invalid or duplicate TLV tag" }
            val len = lenStr.toIntOrNull() ?: throw IllegalArgumentException("Invalid TLV length")
            val start = i + 4
            val end = start + len
            require(end <= s.length) { "Incomplete TLV value" }
            out[tag] = s.substring(start, end)
            i = end
        }
        return out
    }

    fun parseAndValidate(payload: String): ParsedQR {
        return try { parsePayload(payload) } catch (_: IllegalArgumentException) {
            ParsedQR(isValid = false, error = "Malformed QR payload")
        }
    }

    private fun parsePayload(payload: String): ParsedQR {
        val trimmed = payload.trim()
        if (trimmed.length < 20) {
            return ParsedQR(isValid = false, error = "Payload too short")
        }

        // Verify CRC16
        if (!Crc16Ccitt.validate(trimmed)) {
            return ParsedQR(isValid = false, error = "Invalid QR code: checksum validation failed")
        }

        val tags = try { parseTlv(trimmed) } catch (_: IllegalArgumentException) {
            return ParsedQR(isValid = false, error = "Malformed QR payload")
        }
        if (tags["00"] != "01" || tags["53"] != "404" || tags["58"] != "KE" || tags["01"] !in setOf("11", "12")) {
            return ParsedQR(isValid = false, error = "Unsupported payment QR")
        }

        // Point of initiation
        val poi = tags["01"]
        val isDynamic = poi == "12"

        // Amount (Tag 54)
        val amount = tags["54"]?.toDoubleOrNull()
        if ((isDynamic && (amount == null || !amount.isFinite() || amount !in 1.0..500000.0 || amount % 1.0 != 0.0)) ||
            (!isDynamic && tags.containsKey("54"))) {
            return ParsedQR(isValid = false, error = "Invalid bill amount")
        }

        // Merchant Name (Tag 59)
        val merchantName = tags["59"]

        // Primary account info in Tag 28
        var shortcode: String? = null
        val sub28Str = tags["28"]
        if (sub28Str != null) {
            val sub28 = parseTlv(sub28Str)
            shortcode = sub28["01"]
        }

        // If not in 28, check 29
        if (shortcode == null && tags.containsKey("29")) {
            val sub29 = parseTlv(tags["29"] ?: "")
            shortcode = sub29["01"]
        }

        // Detect shortcode type from Routing Block (Tag 83) or Tag 62
        var txType = "TILL"
        val sub83Str = tags["83"]
        if (sub83Str != null) {
            val sub83 = parseTlv(sub83Str)
            val code = sub83["01"]
            if (code == "04") {
                txType = "PAYBILL"
            }
        }

        // Reference from Tag 62
        var ref: String? = null
        val sub62Str = tags["62"]
        if (sub62Str != null) {
            val sub62 = parseTlv(sub62Str)
            ref = sub62["01"] ?: sub62["05"]
        }

        if (shortcode.isNullOrBlank()) {
            return ParsedQR(isValid = false, error = "Invalid QR: missing business shortcode")
        }

        return ParsedQR(
            isValid = true,
            shortcode = shortcode,
            merchantName = merchantName,
            amount = amount,
            isDynamic = isDynamic,
            transactionType = txType,
            reference = ref,
            currency = tags["53"] ?: "404"
        )
    }
}

