package com.example

import android.app.Application
import com.example.billing.RevenueCatManager
import com.example.data.SettingsManager

class BreadcrumbApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize RevenueCat SDK
        RevenueCatManager.getInstance(this).initialize()
        // Initialize SettingsManager so ProcessTextAlias component state is synced
        SettingsManager.getInstance(this)
    }
}
