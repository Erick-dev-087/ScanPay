package com.scanpay.app.utils

import android.util.Patterns

object ValidationUtils {
    fun isValidEmail(email: String?): Boolean {
        return !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    fun isValidPassword(password: String?): Boolean {
        return !password.isNullOrBlank() && password.length >= 8 && password.toByteArray().size <= 72
    }

    fun isValidPhone(phone: String?): Boolean {
        if (phone.isNullOrBlank()) return false
        val digits = phone.filter { it.isDigit() }
        return normalizePhone(phone).matches(Regex("254[17][0-9]{8}"))
    }

    fun normalizePhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val digits = phone.filter { it.isDigit() }
        return when {
            digits.length == 10 && digits.startsWith("0") -> "254" + digits.substring(1)
            digits.length == 9 -> "254$digits"
            digits.length == 12 && digits.startsWith("254") -> digits
            else -> phone
        }
    }
}

