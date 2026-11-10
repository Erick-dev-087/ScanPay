package com.scanpay.app.ui.payment

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.databinding.ActivityPaymentSuccessBinding
import com.scanpay.app.ui.main.MainActivity
import com.scanpay.app.ui.merchant.MerchantMainActivity
import com.scanpay.app.utils.Constants
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PaymentSuccessActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentSuccessBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentSuccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val merchantName = intent.getStringExtra(Constants.EXTRA_VENDOR_NAME) ?: "Merchant Store"
        val amount = intent.getDoubleExtra(Constants.EXTRA_AMOUNT, 0.0)
        val receipt = intent.getStringExtra(Constants.EXTRA_MPESA_RECEIPT) ?: "M-PESA-RECEIPT"
        val txId = intent.getLongExtra(Constants.EXTRA_TRANSACTION_ID, 0L)

        binding.tvMerchantName.text = merchantName
        val df = DecimalFormat("#,##0.00")
        binding.tvAmount.text = df.format(amount)
        binding.tvTransactionId.text = "$receipt (#$txId)"

        val dateStr = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        binding.tvDateTime.text = dateStr

        binding.btnDone.setOnClickListener {
            navigateHome()
        }

        binding.ivBack.setOnClickListener {
            navigateHome()
        }
    }

    private fun navigateHome() {
        val session = ScanPayApplication.sessionManager
        val target = if (session.isMerchant) {
            MerchantMainActivity::class.java
        } else {
            MainActivity::class.java
        }

        val intent = Intent(this, target).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}

