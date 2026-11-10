package com.scanpay.app.ui.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.scanpay.app.data.api.*
import com.scanpay.app.data.request.PaymentRequest
import com.scanpay.app.data.response.PaymentStatusResponse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PaymentUiState(val loading: Boolean = false, val message: String = "", val pending: Boolean = false,
    val paid: PaymentStatusResponse? = null)

class PaymentViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(PaymentUiState(pending = saved.get<Long>("txId") != null || saved.get<String>("key") != null))
    val state = mutableState.asStateFlow()
    val lockedAmount: Double? get() = saved["amount"]
    private var job: Job? = null

    fun pay(qrId: Long, amount: Double, phone: String?) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            mutableState.value = PaymentUiState(loading = true, message = "Sending request to M-Pesa…", pending = true)
            try {
                var id = saved.get<Long>("txId")
                if (id == null) {
                    val key = saved.get<String>("key") ?: UUID.randomUUID().toString().also { saved["key"] = it }
                    saved["amount"] = saved.get<Double>("amount") ?: amount
                    saved["phone"] = saved.get<String>("phone") ?: phone
                    val tx = ApiClient.getService().initiatePayment(PaymentRequest(qrId, saved.get<Double>("amount")!!, saved["phone"]), key).awaitBody()
                    id = tx.id
                    saved["txId"] = id
                    mutableState.value = PaymentUiState(true, tx.message ?: "Enter your M-Pesa PIN in the phone prompt", true)
                }
                poll(id)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                mutableState.value = PaymentUiState(message = (e.message ?: "Payment status unavailable") +
                    "\nCheck your M-Pesa messages. Retry uses the same payment request.", pending = true)
            }
        }
    }
    fun resumePending() {
        val id = saved.get<Long>("txId") ?: return
        if (job?.isActive == true) return
        job = viewModelScope.launch { poll(id) }
    }
    private suspend fun poll(id: Long) {
        repeat(60) {
            try {
                val status = ApiClient.getService().checkPaymentStatus(id).awaitBody()
                when (status.status.lowercase()) {
                    "success" -> {
                        if (status.mpesaReceipt.isNullOrBlank()) {
                            mutableState.value = PaymentUiState(message = "Payment confirmation is missing its receipt. Check status again.", pending = true)
                        } else mutableState.value = PaymentUiState(paid = status)
                        return
                    }
                    "failed", "cancelled" -> {
                        saved.remove<Long>("txId"); saved.remove<String>("key"); saved.remove<Double>("amount"); saved.remove<String>("phone")
                        mutableState.value = PaymentUiState(message = "Payment ${status.status}. You can try again.")
                        return
                    }
                    "unknown" -> {
                        mutableState.value = PaymentUiState(message = "M-Pesa acceptance is uncertain. Check your M-Pesa messages before trying again.", pending = true)
                        return
                    }
                    else -> mutableState.value = PaymentUiState(true, "Waiting for M-Pesa confirmation. Enter your PIN in the phone prompt.", true)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutableState.value = PaymentUiState(true, "Reconnecting to check your payment…", true) }
            delay(2000)
        }
        mutableState.value = PaymentUiState(message = "Payment is still pending. Check your M-Pesa messages, or check status again.", pending = true)
    }
}

