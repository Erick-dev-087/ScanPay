package com.scanpay.app

import android.app.Application
import com.scanpay.app.utils.SessionManager

class ScanPayApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionManager = SessionManager(this)
    }

    companion object {
        lateinit var instance: ScanPayApplication
            private set

        lateinit var sessionManager: SessionManager
            private set
    }
}

