package com.scanpay.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.R
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.data.api.ApiClient
import com.scanpay.app.data.request.CustomerRegisterRequest
import com.scanpay.app.data.request.MerchantRegisterRequest
import com.scanpay.app.data.response.AuthResponse
import com.scanpay.app.data.response.VendorAuthResponse
import com.scanpay.app.databinding.ActivityRegisterBinding
import com.scanpay.app.ui.main.MainActivity
import com.scanpay.app.ui.merchant.MerchantMainActivity
import com.scanpay.app.utils.Constants
import com.scanpay.app.utils.ValidationUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private var isMerchantMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val selectedType = intent.getStringExtra("SELECTED_USER_TYPE") ?: Constants.USER_TYPE_CUSTOMER
        isMerchantMode = selectedType == Constants.USER_TYPE_MERCHANT

        setupViews()
        setupListeners()
    }

    private fun setupViews() {
        if (isMerchantMode) {
            binding.tvUserType.text = "Merchant / Business Registration"
            binding.tilBusinessName.visibility = View.VISIBLE
            binding.tilShortCode.visibility = View.VISIBLE
            binding.tilStoreLabel.visibility = View.VISIBLE
            binding.tvPaymentMethodLabel.visibility = View.VISIBLE
            binding.rgPaymentMethod.visibility = View.VISIBLE
            binding.rbTill.isChecked = true

            // Setup MCC options
            val mccList = arrayOf("5411 - Supermarket/Groceries", "4900 - Utilities/Bills", "5812 - Restaurants", "5999 - General Retail")
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mccList)
            binding.dropdownMcc.setAdapter(adapter)
            binding.tilMcc.visibility = View.VISIBLE
        } else {
            binding.tvUserType.text = "Customer Registration"
            binding.tilBusinessName.visibility = View.GONE
            binding.tilShortCode.visibility = View.GONE
            binding.tilStoreLabel.visibility = View.GONE
            binding.tilMcc.visibility = View.GONE
            binding.tvPaymentMethodLabel.visibility = View.GONE
            binding.rgPaymentMethod.visibility = View.GONE
            binding.tilBusinessNumber.visibility = View.GONE
        }
        binding.tilBusinessNumber.hint = "Paybill account number"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        binding.rgPaymentMethod.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.rb_paybill) {
                binding.tilBusinessNumber.visibility = View.VISIBLE
                binding.tilShortCode.hint = "Paybill Number"
            } else {
                binding.tilBusinessNumber.visibility = View.GONE
                binding.tilShortCode.hint = "Till Number"
            }
        }

        binding.btnRegister.setOnClickListener {
            if (isMerchantMode) {
                registerMerchant()
            } else {
                registerCustomer()
            }
        }
    }

    private fun registerCustomer() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()

        if (name.isBlank()) { binding.etName.error = "Name is required"; return }
        if (!ValidationUtils.isValidEmail(email)) { binding.etEmail.error = "Valid email is required"; return }
        if (!ValidationUtils.isValidPhone(phone)) { binding.etPhone.error = "Valid Kenyan phone number is required"; return }
        if (!ValidationUtils.isValidPassword(password)) { binding.etPassword.error = "Password must be 8–72 bytes"; return }
        if (password != confirm) { binding.etConfirmPassword.error = "Passwords do not match"; return }

        setLoading(true)
        val normalizedPhone = ValidationUtils.normalizePhone(phone)
        val req = CustomerRegisterRequest(name = name, email = email, phoneNumber = normalizedPhone, password = password)

        ApiClient.getService().registerCustomer(req).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                setLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    val session = ScanPayApplication.sessionManager
                    session.saveAuthToken(auth.accessToken ?: "")
                    auth.user?.let { u ->
                        session.saveUserSession(u.id, u.name, u.email, u.phoneNumber, Constants.USER_TYPE_CUSTOMER)
                    }

                    Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val err = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(this@RegisterActivity, "Error: $err", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@RegisterActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun registerMerchant() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val bName = binding.etBusinessName.text?.toString()?.trim().orEmpty()
        val shortcode = binding.etShortCode.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val confirm = binding.etConfirmPassword.text?.toString().orEmpty()
        val storeLabel = binding.etStoreLabel.text?.toString()?.trim()

        val isPaybill = binding.rbPaybill.isChecked
        val shortcodeType = if (isPaybill) "PAYBILL" else "TILL"
        val account = binding.etBusinessNumber.text.toString().trim()
        if (isPaybill && (account.isBlank() || account.length > 12)) {
            binding.etBusinessNumber.error = "Enter the paybill account (1â€“12 characters)"
            return
        }

        if (name.isBlank()) { binding.etName.error = "Contact person name is required"; return }
        if (bName.isBlank()) { binding.etBusinessName.error = "Business name is required"; return }
        if (shortcode.isBlank()) { binding.etShortCode.error = "Till or Paybill number is required"; return }
        if (!ValidationUtils.isValidEmail(email)) { binding.etEmail.error = "Valid email is required"; return }
        if (!ValidationUtils.isValidPhone(phone)) { binding.etPhone.error = "Valid phone is required"; return }
        if (!ValidationUtils.isValidPassword(password)) { binding.etPassword.error = "Password must be 8–72 bytes"; return }
        if (password != confirm) { binding.etConfirmPassword.error = "Passwords do not match"; return }

        setLoading(true)
        val normalizedPhone = ValidationUtils.normalizePhone(phone)
        val req = MerchantRegisterRequest(
            name = name,
            businessName = bName,
            businessShortcode = shortcode,
            shortcodeType = shortcodeType,
            paybillAccountNumber = if (isPaybill) account else null,
            storeLabel = storeLabel,
            email = email,
            phone = normalizedPhone,
            password = password,
            mcc = binding.dropdownMcc.text.toString().substringBefore(" ")
                .takeIf { it.matches(Regex("[0-9]{4}")) } ?: if (isPaybill) "4900" else "5411"
        )

        ApiClient.getService().registerVendor(req).enqueue(object : Callback<VendorAuthResponse> {
            override fun onResponse(call: Call<VendorAuthResponse>, response: Response<VendorAuthResponse>) {
                setLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    val auth = response.body()!!
                    val session = ScanPayApplication.sessionManager
                    session.saveAuthToken(auth.accessToken ?: "")
                    val v = auth.vendor
                    session.saveUserSession(
                        id = v.id,
                        name = v.getDisplayName(),
                        email = v.email,
                        phone = v.phone,
                        userType = Constants.USER_TYPE_MERCHANT,
                        shortcode = v.businessShortcode
                    )

                    Toast.makeText(this@RegisterActivity, "Merchant registered successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@RegisterActivity, MerchantMainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    val err = response.errorBody()?.string() ?: "Registration failed"
                    Toast.makeText(this@RegisterActivity, "Error: $err", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<VendorAuthResponse>, t: Throwable) {
                setLoading(false)
                Toast.makeText(this@RegisterActivity, "Network error: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }
}


