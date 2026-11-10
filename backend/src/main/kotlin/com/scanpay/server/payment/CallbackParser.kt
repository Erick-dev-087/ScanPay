package com.scanpay.server.payment

import kotlinx.serialization.json.*

data class PaymentCallback(val checkoutId: String, val merchantRequestId: String?, val resultCode: Int,
    val amount: Double?, val phone: String?, val receipt: String?)

object CallbackParser {
    fun parse(raw: String): PaymentCallback {
        val root = Json.parseToJsonElement(raw).jsonObject
        val callback = root.getValue("Body").jsonObject.getValue("stkCallback").jsonObject
        val checkout = callback.getValue("CheckoutRequestID").jsonPrimitive.content
        require(checkout.isNotBlank())
        val code = callback.getValue("ResultCode").jsonPrimitive.int
        val items = callback["CallbackMetadata"]?.jsonObject?.get("Item")?.jsonArray.orEmpty()
            .associate { it.jsonObject.getValue("Name").jsonPrimitive.content to it.jsonObject["Value"] }
        val receipt = items["MpesaReceiptNumber"]?.jsonPrimitive?.contentOrNull
        val amount = items["Amount"]?.jsonPrimitive?.doubleOrNull
        val phone = items["PhoneNumber"]?.jsonPrimitive?.contentOrNull
        if (code == 0) {
            require(!receipt.isNullOrBlank() && receipt.length <= 32) { "Missing receipt" }
            require(amount != null && amount.isFinite() && amount > 0) { "Missing amount" }
            require(!phone.isNullOrBlank()) { "Missing phone" }
        }
        return PaymentCallback(checkout, callback["MerchantRequestID"]?.jsonPrimitive?.contentOrNull, code, amount, phone, receipt)
    }
}

