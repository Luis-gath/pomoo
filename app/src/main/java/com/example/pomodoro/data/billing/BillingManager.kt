package com.example.pomodoro.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.example.pomodoro.data.datastore.PremiumStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BillingManager(
    private val context: Context,
    private val premiumStorage: PremiumStorage,
    private val externalScope: CoroutineScope
) {

    private val _billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(::onPurchasesUpdated)
        .enablePendingPurchases()
        .build()

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails.asStateFlow()

    private val _connectionState = MutableStateFlow(BillingClientConnectionState.DISCONNECTED)
    // We can expose a simplified state for UI if needed, but the product details availability is usually enough

    companion object {
        const val PRODUCT_ID_SUBSCRIPTION_3M = "premium_3m"
        const val PRODUCT_ID_LIFETIME = "premium_lifetime"
    }

    fun startConnection() {
        if (_connectionState.value == BillingClientConnectionState.CONNECTED) return
        
        _connectionState.value = BillingClientConnectionState.CONNECTING

        _billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingClientConnectionState.CONNECTED
                    queryProductDetails()
                    queryPurchases() // Restore purchases on start
                } else {
                    _connectionState.value = BillingClientConnectionState.DISCONNECTED
                    Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingClientConnectionState.DISCONNECTED
                // Retry logic could go here
            }
        })
    }

    private fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_SUBSCRIPTION_3M)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRODUCT_ID_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP) // Lifetime is usually INAPP
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        _billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val newMap = productDetailsList.associateBy { it.productId }
                _productDetails.value = newMap
            } else {
                 Log.e("BillingManager", "Query Product Details failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        val details = _productDetails.value[productId] ?: return

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .apply {
                    // For subscriptions, offerToken is mandatory if there are multiple offers. 
                    // Usually taking the first one is safe for simple setups.
                    if (details.productType == BillingClient.ProductType.SUBS) {
                        details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { token ->
                            setOfferToken(token)
                        }
                    }
                }
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        _billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    private fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
            Log.e("BillingManager", "Purchase failed: ${billingResult.debugMessage}")
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                
                _billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                     if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                         // Grant entitlement
                         externalScope.launch {
                             processPurchaseGrant(purchase)
                         }
                     }
                }
            } else {
                externalScope.launch {
                    processPurchaseGrant(purchase)
                }
            }
        }
    }

    private suspend fun processPurchaseGrant(purchase: Purchase) {
        // Logic to update local storage based on what was purchased
        for (productId in purchase.products) {
            if (productId == PRODUCT_ID_LIFETIME) {
                premiumStorage.setPremium(true)
            } else if (productId == PRODUCT_ID_SUBSCRIPTION_3M) {
                // In a real app, verify backend receipt/expiry time. 
                // For this request, we estimate or simple-flag it, but the user asked for "premiumUntil".
                // Since BillingClient V6+, getting expiry date requires backend or checking purchaseTime + duration manually if possible.
                // However, without a backend, we can't be 100% sure of the expiry date from the client easily for ALL cases (cancellations etc).
                // But for "3 months", we can estimate:
                 val threeMonthsMillis = 90L * 24 * 60 * 60 * 1000
                 val expiry = purchase.purchaseTime + threeMonthsMillis
                 premiumStorage.setPremiumUntil(expiry)
            }
        }
    }

    fun queryPurchases() {
        // Check active subscriptions
        _billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
             if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                 for (purchase in purchases) {
                     handlePurchase(purchase)
                 }
             }
        }

        // Check owned in-app products (lifetime)
        _billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    handlePurchase(purchase)
                }
            }
        }
    }
}

enum class BillingClientConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
