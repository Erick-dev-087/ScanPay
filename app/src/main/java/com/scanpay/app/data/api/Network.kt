package com.scanpay.app.data.api

import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONObject
import retrofit2.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T : Any> Call<T>.awaitBody(): T = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (!continuation.isActive) return
            val body = response.body()
            if (response.isSuccessful && body != null) continuation.resume(body)
            else {
                val text = response.errorBody()?.string()
                val message = runCatching { JSONObject(text.orEmpty()).optString("error") }.getOrNull()
                continuation.resumeWithException(IllegalStateException(message?.takeIf { it.isNotBlank() } ?: "Unable to load data (HTTP ${response.code()})"))
            }
        }
        override fun onFailure(call: Call<T>, error: Throwable) {
            if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Cannot connect to ScanPay. Check your connection.", error))
        }
    })
}

