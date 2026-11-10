package com.scanpay.app.data.api

import com.scanpay.app.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.header("No-Authentication") != null) {
            val newRequest = original.newBuilder()
                .removeHeader("No-Authentication")
                .build()
            return chain.proceed(newRequest)
        }

        val token = sessionManager.getAuthToken()
        val requestBuilder = original.newBuilder()

        if (!token.isNullOrBlank()) {
            requestBuilder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(requestBuilder.build())
    }
}

