package com.scanpay.app.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.scanpay.app.*
import com.scanpay.app.adapters.TransactionAdapter
import com.scanpay.app.data.api.*
import com.scanpay.app.databinding.ActivityMainBinding
import com.scanpay.app.ui.analytics.AnalyticsActivity
import com.scanpay.app.ui.history.HistoryActivity
import com.scanpay.app.ui.more.ProfileActivity
import com.scanpay.app.ui.scanner.QRScannerActivity
import com.scanpay.app.utils.CurrencyUtils
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val adapter = TransactionAdapter()
    private var load: Job? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvGreeting.text = "Hello, ${ScanPayApplication.sessionManager.userName}"
        binding.tvBalanceDelta.text = "Confirmed spending in the last 30 days"
        binding.rvRecentActivity.layoutManager = LinearLayoutManager(this)
        binding.rvRecentActivity.adapter = adapter
        binding.btnScanPay.setOnClickListener { startActivity(Intent(this, QRScannerActivity::class.java)) }
        binding.btnMenu.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        binding.btnViewAll.setOnClickListener { startActivity(Intent(this, HistoryActivity::class.java)) }
        binding.btnNotifications.visibility = View.GONE
        binding.btnThemeToggle.setOnClickListener { toggleTheme() }
        binding.bottomNavigation.menu.findItem(R.id.navigation_qr_codes).apply { title = "Scan & Pay"; setIcon(R.drawable.ic_scan) }
        binding.bottomNavigation.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.navigation_dashboard -> {}
                R.id.navigation_qr_codes -> startActivity(Intent(this, QRScannerActivity::class.java))
                R.id.navigation_analytics -> startActivity(Intent(this, AnalyticsActivity::class.java))
                R.id.navigation_more -> startActivity(Intent(this, ProfileActivity::class.java))
            }
            true
        }
    }
    override fun onResume() {
        super.onResume()
        binding.bottomNavigation.selectedItemId = R.id.navigation_dashboard
        load?.cancel()
        load = lifecycleScope.launch {
            binding.tvBalanceDelta.text = "Loading spending…"
            try {
                val api = ApiClient.getService()
                val analytics = api.getUserAnalytics(30).awaitBody()
                binding.tvTotalSpent.text = CurrencyUtils.formatKes(analytics.totalAmount)
                val txs = api.getUserTransactions(1, 10).awaitBody()
                adapter.updateList(txs)
                binding.tvBalanceDelta.text = if (txs.isEmpty()) "No payments yet. Scan a merchant QR to get started."
                    else "${analytics.totalCount} confirmed payments · Last 30 days"
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { binding.tvBalanceDelta.text = e.message }
        }
    }
    private fun toggleTheme() {
        val night = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK == android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(if (night) androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO else androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES)
    }
}

