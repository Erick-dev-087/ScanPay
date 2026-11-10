package com.scanpay.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.data.api.ApiClient
import com.scanpay.app.data.request.LoginRequest
import com.scanpay.app.data.response.AuthResponse
import com.scanpay.app.databinding.ActivityLoginBinding
import com.scanpay.app.ui.main.MainActivity
import com.scanpay.app.ui.merchant.MerchantMainActivity
import com.scanpay.app.utils.Constants
import com.scanpay.app.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, UserTypeSelectionActivity::class.java))
            finish()
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
        binding.tvForgotPassword.visibility = View.GONE
    }

    private fun performLogin() {
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()

        if (!ValidationUtils.isValidEmail(email)) {
            binding.etEmail.error = "Please enter a valid email address"
            return
        }

        if (password.isBlank()) {
            binding.etPassword.error = "Password is required"
            return
        }

        setLoading(true)

        val request = LoginRequest(email = email, password = password)
        ApiClient.getService().login(request).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                setLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    val token = auth.accessToken ?: ""
                    val rawType = auth.userType ?: Constants.USER_TYPE_CUSTOMER
                    val userType = if (rawType.contains("vendor", ignoreCase = true) || rawType.contains("merchant", ignoreCase = true)) {
                        Constants.USER_TYPE_MERCHANT
                    } else {
                        Constants.USER_TYPE_CUSTOMER
                    }

                    val session = ScanPayApplication.sessionManager
                    session.saveAuthToken(token)

                    if (userType == Constants.USER_TYPE_MERCHANT && auth.vendor != null) {
                        val vendor = auth.vendor
                        session.saveUserSession(
                            id = vendor.id,
                            name = vendor.getDisplayName(),
                            email = vendor.email,
                            phone = vendor.phone,
                            userType = Constants.USER_TYPE_MERCHANT,
                            shortcode = vendor.businessShortcode
                        )
                    } else if (auth.user != null) {
                        val user = auth.user
                        session.saveUserSession(
                            id = user.id,
                            name = user.name,
                            email = user.email,
                            phone = user.phoneNumber,
                            userType = Constants.USER_TYPE_CUSTOMER
                        )
                    }

                    Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()

                    val targetActivity = if (session.isMerchant) {
                        MerchantMainActivity::class.java
                    } else {
                        MainActivity::class.java
                    }

                    val intent = Intent(this@LoginActivity, targetActivity).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val errorMsg = response.errorBody()?.string() ?: "Invalid credentials"
                    Toast.makeText(this@LoginActivity, "Login failed: $errorMsg", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@LoginActivity, "Connection error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }
}


