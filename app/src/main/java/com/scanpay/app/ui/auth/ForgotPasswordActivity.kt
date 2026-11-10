package com.scanpay.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.data.api.ApiClient
import com.scanpay.app.data.request.ForgotPasswordRequest
import com.scanpay.app.data.response.ForgotPasswordResponse
import com.scanpay.app.databinding.ActivityForgotPasswordBinding
import com.scanpay.app.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSendReset.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim().orEmpty()
            if (!ValidationUtils.isValidEmail(email)) {
                binding.etEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnSendReset.isEnabled = false

            val req = ForgotPasswordRequest(email)
            ApiClient.getService().forgotPassword(req).enqueue(object : Callback<ForgotPasswordResponse> {
                override fun onResponse(call: Call<ForgotPasswordResponse>, response: Response<ForgotPasswordResponse>) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendReset.isEnabled = true
                    if (response.isSuccessful && response.body() != null) {
                        Toast.makeText(this@ForgotPasswordActivity, response.body()?.message ?: "Reset token generated", Toast.LENGTH_LONG).show()
                        binding.tvGoToReset.visibility = View.VISIBLE
                        binding.tvGoToReset.setOnClickListener {
                            val intent = Intent(this@ForgotPasswordActivity, ResetPasswordActivity::class.java).apply {
                                putExtra("EXTRA_TOKEN", response.body()?.resetToken ?: "")
                            }
                            startActivity(intent)
                            finish()
                        }
                    } else {
                        Toast.makeText(this@ForgotPasswordActivity, "Error sending reset request", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<ForgotPasswordResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnSendReset.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, "Connection error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}

