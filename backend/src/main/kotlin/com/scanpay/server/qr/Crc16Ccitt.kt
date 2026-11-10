package com.scanpay.server.qr

object Crc16Ccitt {
    /**
     * CRC-16/CCITT-FALSE (poly=0x1021, init=0xFFFF, no reflection).
     * Matches CBK KE-QR / Safaricom M-Pesa QR specification.
     * Pass the full payload string up to and INCLUDING "6304" (Tag + declared Length).
     */
    fun calculate(payloadWithTag6304: String): String {
        var crc = 0xFFFF
        val bytes = payloadWithTag6304.toByteArray(Charsets.UTF_8)
        for (b in bytes) {
            crc = crc xor ((b.toInt() and 0xFF) shl 8)
            for (i in 0 until 8) {
                crc = if ((crc and 0x8000) != 0) {
                    ((crc shl 1) xor 0x1021) and 0xFFFF
                } else {
                    (crc shl 1) and 0xFFFF
                }
            }
        }
        return "%04X".format(crc)
    }

    /**
     * Returns true if payload ends with '6304XXXX' and the checksum matches.
     */
    fun validate(fullPayload: String): Boolean {
        if (fullPayload.length < 8) return false
        val prefix = fullPayload.substring(fullPayload.length - 8, fullPayload.length - 4)
        if (prefix != "6304") return false
        val expectedCrc = fullPayload.substring(fullPayload.length - 4).uppercase()
        val calculatedCrc = calculate(fullPayload.substring(0, fullPayload.length - 4))
        return expectedCrc == calculatedCrc
    }
}

