package com.scanpay.app.ui.payment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.databinding.ActivityPaymentConfirmationBinding
import com.scanpay.app.utils.Constants
import kotlinx.coroutines.launch

class PaymentConfirmationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentConfirmationBinding
    private val model: PaymentViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val qrId = intent.getLongExtra(Constants.EXTRA_QR_CODE_ID, 0)
        if (qrId <= 0) { finish(); return }
        val merchant = intent.getStringExtra(Constants.EXTRA_VENDOR_NAME).orEmpty()
        val code = intent.getStringExtra(Constants.EXTRA_SHORTCODE).orEmpty()
        val type = intent.getStringExtra(Constants.EXTRA_SHORTCODE_TYPE) ?: "TILL"
        val dynamic = intent.getBooleanExtra(Constants.EXTRA_IS_DYNAMIC, false)
        binding.tvMerchantName.text = merchant
        binding.tvMerchantCode.text = "$type: $code\nM-Pesa: ${ScanPayApplication.sessionManager.userPhone}"
        val amount = model.lockedAmount ?: intent.getDoubleExtra(Constants.EXTRA_AMOUNT, 0.0)
        if (amount > 0) binding.etAmount.setText(amount.toLong().toString())
        binding.tilAmount.hint = if (dynamic) "Bill amount (KES)" else "Amount (KES)"
        binding.btnBack.setOnClickListener { finish() }
        binding.btnPayNow.setOnClickListener {
            val entered = binding.etAmount.text.toString().toDoubleOrNull()
            if (entered == null || !entered.isFinite() || entered !in 1.0..500000.0 || entered % 1.0 != 0.0) {
                binding.etAmount.error = "Enter a whole KES amount from 1 to 500,000"; return@setOnClickListener
            }
            model.pay(qrId, entered, ScanPayApplication.sessionManager.userPhone)
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.state.collect { state ->
                    binding.progressBar.visibility = if (state.loading) View.VISIBLE else View.GONE
                    binding.btnPayNow.isEnabled = !state.loading
                    binding.btnPayNow.text = if (state.pending) "Check payment status" else "Pay with M-Pesa"
                    binding.etAmount.isEnabled = !dynamic && !state.pending
                    binding.tvPaymentStatus.visibility = View.VISIBLE
                    binding.tvPaymentStatus.text = state.message.ifBlank { "Confirm your payment. Enter your PIN only in the M-Pesa prompt." }
                    state.paid?.let { paid ->
                        startActivity(Intent(this@PaymentConfirmationActivity, PaymentSuccessActivity::class.java).apply {
                            putExtra(Constants.EXTRA_TRANSACTION_ID, paid.transactionId)
                            putExtra(Constants.EXTRA_AMOUNT, paid.amount)
                            putExtra(Constants.EXTRA_VENDOR_NAME, merchant)
                            putExtra(Constants.EXTRA_SHORTCODE, code)
                            putExtra(Constants.EXTRA_MPESA_RECEIPT, paid.mpesaReceipt)
                        })
                        finish()
                    }
                }
            }
        }
        model.resumePending()
    }
}

