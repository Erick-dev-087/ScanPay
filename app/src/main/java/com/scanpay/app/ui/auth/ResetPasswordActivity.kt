package com.scanpay.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.data.api.ApiClient
import com.scanpay.app.data.request.ResetPasswordRequest
import com.scanpay.app.data.response.MessageResponse
import com.scanpay.app.databinding.ActivityResetPasswordBinding
import com.scanpay.app.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityResetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val token = intent.getStringExtra("EXTRA_TOKEN") ?: ""
        if (token.isNotBlank()) {
            binding.etToken.setText(token)
        }

        binding.btnResetPassword.setOnClickListener {
            val resetToken = binding.etToken.text?.toString()?.trim().orEmpty()
            val newPass = binding.etNewPassword.text?.toString()?.trim().orEmpty()
            val confirmPass = binding.etConfirmPassword.text?.toString()?.trim().orEmpty()

            if (resetToken.isBlank()) { binding.etToken.error = "Token is required"; return@setOnClickListener }
            if (!ValidationUtils.isValidPassword(newPass)) { binding.etNewPassword.error = "Minimum 6 characters"; return@setOnClickListener }
            if (newPass != confirmPass) { binding.etConfirmPassword.error = "Passwords do not match"; return@setOnClickListener }

            binding.progressBar.visibility = View.VISIBLE
            binding.btnResetPassword.isEnabled = false

            val req = ResetPasswordRequest(token = resetToken, newPassword = newPass)
            ApiClient.getService().resetPassword(req).enqueue(object : Callback<MessageResponse> {
                override fun onResponse(call: Call<MessageResponse>, response: Response<MessageResponse>) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnResetPassword.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity, "Password reset successful! Please log in.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@ResetPasswordActivity, LoginActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })
                        finish()
                    } else {
                        Toast.makeText(this@ResetPasswordActivity, "Failed to reset password", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MessageResponse>, t: Throwable) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnResetPassword.isEnabled = true
                    Toast.makeText(this@ResetPasswordActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
}

