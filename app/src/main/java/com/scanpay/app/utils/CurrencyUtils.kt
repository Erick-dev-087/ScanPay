package com.scanpay.app.utils

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val formatter = (NumberFormat.getNumberInstance(Locale.US) as DecimalFormat).apply {
        applyPattern("#,##0.00")
    }

    fun formatKes(amount: Double): String {
        return "KES ${formatter.format(amount)}"
    }

    fun formatKesWhole(amount: Double): String {
        return "KES ${amount.toInt()}"
    }
}

