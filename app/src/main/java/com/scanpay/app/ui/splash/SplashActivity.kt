package com.scanpay.app.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.scanpay.app.ScanPayApplication
import com.scanpay.app.ui.auth.UserTypeSelectionActivity
import com.scanpay.app.ui.main.MainActivity
import com.scanpay.app.ui.merchant.MerchantMainActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = ScanPayApplication.sessionManager
        val target = when {
            !session.isLoggedIn() -> UserTypeSelectionActivity::class.java
            session.isMerchant -> MerchantMainActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, target))
        finish()
    }
}

