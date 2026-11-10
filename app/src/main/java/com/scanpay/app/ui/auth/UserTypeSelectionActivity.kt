package com.scanpay.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.databinding.ActivityUserTypeSelectionBinding
import com.scanpay.app.utils.Constants

class UserTypeSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserTypeSelectionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserTypeSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cardCustomer.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java).apply {
                putExtra("SELECTED_USER_TYPE", Constants.USER_TYPE_CUSTOMER)
            }
            startActivity(intent)
        }

        binding.cardMerchant.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java).apply {
                putExtra("SELECTED_USER_TYPE", Constants.USER_TYPE_MERCHANT)
            }
            startActivity(intent)
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}

