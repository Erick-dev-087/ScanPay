package com.scanpay.app.ui.analytics

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.data.api.*
import com.scanpay.app.databinding.ActivityAnalyticsBinding
import com.scanpay.app.utils.CurrencyUtils
import kotlinx.coroutines.*

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAnalyticsBinding
    private var load: Job? = null
    private val merchant get() = ScanPayApplication.sessionManager.isMerchant
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnalyticsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvTitle.text = if (merchant) "Revenue insights" else "Spending insights"
        binding.btnBack.setOnClickListener { finish() }
        binding.periodGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked) refresh(when (id) { binding.btnWeek.id -> 7; binding.btnQuarter.id -> 90; else -> 30 })
        }
        refresh(30)
    }
    private fun refresh(days: Int) {
        load?.cancel()
        load = lifecycleScope.launch {
            binding.tvSummary.text = "Loading confirmed payments…"
            try {
                val api = ApiClient.getService()
                val a = (if (merchant) api.getMerchantAnalytics(days) else api.getUserAnalytics(days)).awaitBody()
                binding.tvTotal.text = CurrencyUtils.formatKes(a.totalAmount)
                binding.tvSummary.text = "${a.totalCount} payments · Average ${CurrencyUtils.formatKes(a.averageTransaction)}\nLast $days days"
                ChartRenderer.show(binding.lineChart, a.daily, if (merchant) "Received (KES)" else "Spent (KES)")
                binding.tvCategories.text = if (a.categories.isEmpty()) "No confirmed payments in this period."
                    else a.categories.joinToString("\n") { "${it.category}    ${CurrencyUtils.formatKes(it.amount)}    ${it.percentage.toInt()}%" }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { binding.tvSummary.text = e.message }
        }
    }
}

