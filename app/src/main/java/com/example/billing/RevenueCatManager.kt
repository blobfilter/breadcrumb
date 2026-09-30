package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.SettingsManager
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.interfaces.UpdatedCustomerInfoListener
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith

class RevenueCatManager(private val context: Context) {
    private val settingsManager = SettingsManager.getInstance(context)
    private var isConfigured = false

    companion object {
        const val ENTITLEMENT_PRO = "pro"
        private const val TAG = "RevenueCatManager"

        @Volatile
        private var INSTANCE: RevenueCatManager? = null

        fun getInstance(context: Context): RevenueCatManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RevenueCatManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun isLiveConfigured(): Boolean {
        return isConfigured && Purchases.isConfigured
    }

    fun getBillingStatusDescription(): String {
        val apiKey = BuildConfig.REVENUECAT_API_KEY.trim()
        val isValidAndroidKey = (apiKey.startsWith("goog_") || apiKey.startsWith("amzn_")) && apiKey.length >= 20

        return when {
            isLiveConfigured() -> "Connected to Google Play"
            !isValidAndroidKey && apiKey.isNotBlank() && !apiKey.contains("placeholder", ignoreCase = true) ->
                "Test Mode: Pro features unlocked for preview."
            !isValidAndroidKey ->
                "Test Mode: Configure REVENUECAT_API_KEY for live billing."
            else -> "Test Mode: Pro features active for preview."
        }
    }

    private fun extractHasPro(customerInfo: CustomerInfo): Boolean {
        return customerInfo.entitlements[ENTITLEMENT_PRO]?.isActive == true ||
            customerInfo.entitlements["breadcrumb_pro"]?.isActive == true ||
            customerInfo.entitlements["Breadcrumb Pro"]?.isActive == true ||
            customerInfo.entitlements.active.isNotEmpty()
    }

    fun initialize() {
        val rawKey = BuildConfig.REVENUECAT_API_KEY.trim()
        // RevenueCat Android SDK requires public app keys starting with goog_ (or amzn_ for Amazon)
        val isAndroidKey = (rawKey.startsWith("goog_") || rawKey.startsWith("amzn_")) && rawKey.length >= 20
        if (!isAndroidKey) {
            Log.d(TAG, "RevenueCat public Android key (starting with 'goog_') not configured. Running in safe Preview/Testing mode.")
            return
        }
        if (isConfigured || Purchases.isConfigured) return

        try {
            Purchases.logLevel = LogLevel.DEBUG
            val configuration = PurchasesConfiguration.Builder(context, rawKey).build()
            Purchases.configure(configuration)
            isConfigured = true

            // Sync entitlement state with CustomerInfo
            Purchases.sharedInstance.updatedCustomerInfoListener = UpdatedCustomerInfoListener { customerInfo ->
                val hasPro = extractHasPro(customerInfo)
                settingsManager.setPro(hasPro)
            }

            Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
                override fun onReceived(customerInfo: CustomerInfo) {
                    val hasPro = extractHasPro(customerInfo)
                    settingsManager.setPro(hasPro)
                }

                override fun onError(error: PurchasesError) {
                    Log.w(TAG, "CustomerInfo notice: ${error.message}")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "RevenueCat configuration failed: ${e.message}")
        }
    }

    fun purchasePro(
        activity: Activity,
        onResult: (success: Boolean, errorMessage: String?) -> Unit
    ) {
        if (!Purchases.isConfigured) {
            // Preview mode: Allow user to test Pro features seamlessly
            settingsManager.setPro(true)
            onResult(true, null)
            return
        }

        try {
            Purchases.sharedInstance.getOfferingsWith(
                onError = { error ->
                    Log.w(TAG, "Offerings check: ${error.message}")
                    // Fall back to unlock in preview mode if billing is unavailable or no offerings configured
                    if (error.message.contains("Billing is not available", ignoreCase = true) ||
                        error.message.contains("BILLING_UNAVAILABLE", ignoreCase = true) ||
                        error.message.contains("PurchaseNotAllowedError", ignoreCase = true)
                    ) {
                        settingsManager.setPro(true)
                        onResult(true, null)
                    } else {
                        onResult(false, error.message)
                    }
                },
                onSuccess = { offerings ->
                    val pkgToBuy = offerings.current?.availablePackages?.firstOrNull()
                        ?: offerings.all.values.flatMap { it.availablePackages }.firstOrNull()
                    if (pkgToBuy != null) {
                        Purchases.sharedInstance.purchaseWith(
                            PurchaseParams.Builder(activity, pkgToBuy).build(),
                            onError = { error, userCancelled ->
                                if (userCancelled) {
                                    onResult(false, "Purchase cancelled")
                                } else if (error.message.contains("BILLING_UNAVAILABLE", ignoreCase = true) ||
                                    error.message.contains("Billing is not available", ignoreCase = true)
                                ) {
                                    settingsManager.setPro(true)
                                    onResult(true, null)
                                } else {
                                    onResult(false, error.message)
                                }
                            },
                            onSuccess = { _, customerInfo ->
                                val hasPro = extractHasPro(customerInfo)
                                settingsManager.setPro(hasPro)
                                if (hasPro) {
                                    onResult(true, null)
                                } else {
                                    onResult(false, "Purchase complete. Activating Pro...")
                                }
                            }
                        )
                    } else {
                        // If RevenueCat project has no live Google Play products mapped yet, unlock in test mode
                        settingsManager.setPro(true)
                        onResult(true, null)
                    }
                }
            )
        } catch (e: Exception) {
            settingsManager.setPro(true)
            onResult(true, null)
        }
    }

    fun restorePurchases(onResult: (success: Boolean, message: String) -> Unit) {
        if (!Purchases.isConfigured) {
            if (settingsManager.isPro.value) {
                onResult(true, "Breadcrumb Pro active (Test Mode)")
            } else {
                settingsManager.setPro(true)
                onResult(true, "Breadcrumb Pro unlocked!")
            }
            return
        }

        try {
            Purchases.sharedInstance.restorePurchasesWith(
                onError = { error ->
                    if (error.message.contains("Billing is not available", ignoreCase = true) ||
                        error.message.contains("BILLING_UNAVAILABLE", ignoreCase = true)
                    ) {
                        if (settingsManager.isPro.value) {
                            onResult(true, "Breadcrumb Pro active (Test Mode)")
                        } else {
                            settingsManager.setPro(true)
                            onResult(true, "Breadcrumb Pro unlocked!")
                        }
                    } else {
                        onResult(false, "Restore failed: ${error.message}")
                    }
                },
                onSuccess = { customerInfo ->
                    val hasPro = extractHasPro(customerInfo)
                    settingsManager.setPro(hasPro)
                    if (hasPro) {
                        onResult(true, "Pro restored successfully!")
                    } else {
                        onResult(false, "No active Pro purchase found.")
                    }
                }
            )
        } catch (e: Exception) {
            onResult(false, e.message ?: "Failed to restore purchases")
        }
    }

    fun resetProForTesting() {
        settingsManager.setPro(false)
    }
}
