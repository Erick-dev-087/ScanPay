package com.scanpay.app.ui.merchant

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.data.api.ApiClient
import com.scanpay.app.data.request.TopUpRequest
import com.scanpay.app.data.response.TopUpResponse
import com.scanpay.app.databinding.ActivityTopupBinding
import com.scanpay.app.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TopUpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTopupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTopupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val session = ScanPayApplication.sessionManager
        binding.etPhone.setText(session.userPhone)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAmount500.setOnClickListener { binding.etAmount.setText("500") }
        binding.btnAmount1000.setOnClickListener { binding.etAmount.setText("1000") }
        binding.btnAmount2000.setOnClickListener { binding.etAmount.setText("2000") }

        binding.btnInitiatePayment.setOnClickListener {
            val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
            val amountStr = binding.etAmount.text?.toString()?.trim().orEmpty()
            val amount = amountStr.toDoubleOrNull()

            if (!ValidationUtils.isValidPhone(phone)) {
                binding.etPhone.error = "Please enter a valid phone number"
                return@setOnClickListener
            }

            if (amount == null || amount < 1) {
                binding.etAmount.error = "Please enter an amount of at least 1 KES"
                return@setOnClickListener
            }

            binding.progressTopup.visibility = View.VISIBLE
            binding.btnInitiatePayment.isEnabled = false
            binding.tvTopupStatus.visibility = View.VISIBLE
            binding.tvTopupStatus.text = "Sending STK push to $phone..."

            val req = TopUpRequest(amount = amount, phone = ValidationUtils.normalizePhone(phone))
            ApiClient.getService().initiateTopUp(req).enqueue(object : Callback<TopUpResponse> {
                override fun onResponse(call: Call<TopUpResponse>, response: Response<TopUpResponse>) {
                    binding.progressTopup.visibility = View.GONE
                    binding.btnInitiatePayment.isEnabled = true
                    if (response.isSuccessful && response.body() != null) {
                        binding.tvTopupStatus.text = "Top-up successful! Balance updated."
                        Toast.makeText(this@TopUpActivity, "Top-up confirmed!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        binding.tvTopupStatus.text = "Failed to initiate top-up"
                    }
                }

                override fun onFailure(call: Call<TopUpResponse>, t: Throwable) {
                    binding.progressTopup.visibility = View.GONE
                    binding.btnInitiatePayment.isEnabled = true
                    binding.tvTopupStatus.text = "Error: ${t.message}"
                }
            })
        }
    }
}

