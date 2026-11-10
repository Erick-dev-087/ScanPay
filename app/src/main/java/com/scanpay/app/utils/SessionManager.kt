package com.scanpay.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "scanpay_secure_session",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveAuthToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun getAuthToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun isLoggedIn(): Boolean {
        return !getAuthToken().isNullOrBlank()
    }

    fun saveUserSession(
        id: Long,
        name: String,
        email: String,
        phone: String,
        userType: String,
        shortcode: String? = null
    ) {
        prefs.edit()
            .putLong(KEY_USER_ID, id)
            .putString(KEY_USER_NAME, name)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PHONE, phone)
            .putString(KEY_USER_TYPE, userType)
            .putString(KEY_SHORTCODE, shortcode)
            .apply()
    }

    val userId: Long
        get() = prefs.getLong(KEY_USER_ID, 0L)

    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "") ?: ""

    val userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    val userPhone: String
        get() = prefs.getString(KEY_USER_PHONE, "") ?: ""

    val userType: String
        get() = prefs.getString(KEY_USER_TYPE, Constants.USER_TYPE_CUSTOMER) ?: Constants.USER_TYPE_CUSTOMER

    val isMerchant: Boolean
        get() = userType.equals(Constants.USER_TYPE_MERCHANT, ignoreCase = true) || userType.equals("merchant", ignoreCase = true)

    val businessShortcode: String?
        get() = prefs.getString(KEY_SHORTCODE, null)

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_TOKEN = "jwt_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_TYPE = "user_type"
        private const val KEY_SHORTCODE = "business_shortcode"
    }
}

