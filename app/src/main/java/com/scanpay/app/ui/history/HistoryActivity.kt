package com.scanpay.app.ui.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.adapters.TransactionAdapter
import com.scanpay.app.data.api.*
import com.scanpay.app.data.response.TransactionResponse
import com.scanpay.app.databinding.ActivityHistoryBinding
import com.scanpay.app.utils.CurrencyUtils
import kotlinx.coroutines.*

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private val rows = mutableListOf<TransactionResponse>()
    private val adapter = TransactionAdapter { tx ->
        MaterialAlertDialogBuilder(this).setTitle(tx.merchantName ?: "Payment")
            .setMessage("${CurrencyUtils.formatKes(tx.amount)}\nStatus: ${tx.status}\nReceipt: ${tx.mpesaReceipt ?: "Awaiting confirmation"}\nReference: ${tx.accountReference.orEmpty()}\nPayment ID: ${tx.id}")
            .setPositiveButton("Close", null).show()
    }
    private var page = 1
    private var loading = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.rvTransactions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.rvTransactions.adapter = adapter
        binding.btnBack.setOnClickListener { finish() }
        binding.btnMore.setOnClickListener { load() }
        load()
    }
    private fun load() {
        if (loading) return
        loading = true; binding.btnMore.isEnabled = false
        lifecycleScope.launch {
            try {
                val api = ApiClient.getService()
                val result = (if (ScanPayApplication.sessionManager.isMerchant) api.getMerchantTransactions(page, 50)
                    else api.getUserTransactions(page, 50)).awaitBody()
                rows.addAll(result); adapter.updateList(rows.toList()); page++
                binding.tvStatus.text = if (rows.isEmpty()) "No payments yet." else "${rows.size} payments · Past 12 months"
                binding.btnMore.visibility = if (result.size < 50) View.GONE else View.VISIBLE
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { binding.tvStatus.text = e.message }
            finally { loading = false; binding.btnMore.isEnabled = true }
        }
    }
}

