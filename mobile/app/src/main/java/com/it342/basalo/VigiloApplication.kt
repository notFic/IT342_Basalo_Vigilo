package com.it342.basalo

import android.app.Application
import com.it342.basalo.core.network.ApiConfigManager

class VigiloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiConfigManager.initialize(this)
    }
}
