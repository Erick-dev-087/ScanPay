package com.scanpay.app.ui.merchant

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scanpay.app.*
import com.scanpay.app.data.api.*
import com.scanpay.app.databinding.ActivityMerchantMainBinding
import com.scanpay.app.ui.analytics.*
import com.scanpay.app.ui.history.HistoryActivity
import com.scanpay.app.ui.more.ProfileActivity
import com.scanpay.app.utils.CurrencyUtils
import kotlinx.coroutines.*

class MerchantMainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMerchantMainBinding
    private var load: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMerchantMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvMerchantGreeting.text = ScanPayApplication.sessionManager.userName
        binding.btnCreateQr.setOnClickListener { startActivity(Intent(this, QRGeneratorActivity::class.java)) }
        binding.btnRefresh.setOnClickListener { refresh() }
        binding.btnViewAllReceipts.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_dashboard -> {}
                R.id.navigation_qr_codes -> startActivity(Intent(this, QRGeneratorActivity::class.java))
                R.id.navigation_analytics -> startActivity(Intent(this, AnalyticsActivity::class.java))
                R.id.navigation_more -> startActivity(Intent(this, ProfileActivity::class.java))
            }
            true
        }
    }
    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_dashboard
        refresh()
    }
    private fun refresh() {
        load?.cancel()
        load = lifecycleScope.launch {
            binding.tvStatus.text = "Loading payments…"
            try {
                val analytics = ApiClient.getService().getMerchantAnalytics(7).awaitBody()
                binding.tvWeekRevenue.text = CurrencyUtils.formatKes(analytics.totalAmount)
                binding.tvTodayRevenue.text = "Today: ${CurrencyUtils.formatKes(analytics.todayAmount)} · ${analytics.todayCount} payments"
                binding.tvStatus.text = if (analytics.totalCount == 0) "Your first payment starts with a QR."
                    else "${analytics.totalCount} confirmed payments this week"
                ChartRenderer.show(binding.lineChart, analytics.daily, "Received (KES)")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { binding.tvStatus.text = e.message }
        }
    }
}

