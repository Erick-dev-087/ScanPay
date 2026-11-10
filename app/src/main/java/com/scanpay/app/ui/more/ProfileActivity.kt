package com.scanpay.app.ui.more

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.data.api.*
import com.scanpay.app.databinding.ActivityProfileBinding
import com.scanpay.app.ui.auth.UserTypeSelectionActivity
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val session = ScanPayApplication.sessionManager
        binding.tvProfileName.text = session.userName
        binding.tvProfileEmail.text = session.userEmail
        binding.tvProfilePhone.text = session.userPhone + if (session.isMerchant) "\nTill / Paybill: ${session.businessShortcode.orEmpty()}" else ""
        binding.btnEditProfile.visibility = View.GONE
        binding.btnLogout.setOnClickListener {
            binding.btnLogout.isEnabled = false
            lifecycleScope.launch {
                val revoked = runCatching { ApiClient.getService().logout().awaitBody() }.isSuccess
                session.clearSession()
                Toast.makeText(this@ProfileActivity, if (revoked) "Signed out" else "Signed out on this device", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@ProfileActivity, UserTypeSelectionActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
                finish()
            }
        }
    }
}

