package com.example.pomodoro.features.premium.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.premium.data.BillingManager
import com.example.pomodoro.features.premium.data.PremiumStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PremiumUiState(
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val premiumUntilMillis: Long? = null,
    val subscriptionPrice: String = "",
    val lifetimePrice: String = "",
    val error: String? = null
)

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    premiumStorage: PremiumStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(PremiumUiState())

    val uiState: StateFlow<PremiumUiState> = combine(
        _uiState,
        premiumStorage.isPremium,
        premiumStorage.premiumUntil,
        billingManager.productDetails
    ) { state, isPremium, premiumUntil, productDetails ->

        val subDetails = productDetails[BillingManager.PRODUCT_ID_SUBSCRIPTION_3M]
        val lifeDetails = productDetails[BillingManager.PRODUCT_ID_LIFETIME]

        // Extract prices
        val subPrice = subDetails?.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "..."
        val lifePrice = lifeDetails?.oneTimePurchaseOfferDetails?.formattedPrice ?: "..."

        state.copy(
            isPremium = isPremium,
            premiumUntilMillis = premiumUntil,
            subscriptionPrice = subPrice,
            lifetimePrice = lifePrice
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PremiumUiState(isLoading = true))

    init {
        billingManager.startConnection()
    }

    fun buySubscription(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, BillingManager.PRODUCT_ID_SUBSCRIPTION_3M)
    }

    fun buyLifetime(activity: Activity) {
        billingManager.launchPurchaseFlow(activity, BillingManager.PRODUCT_ID_LIFETIME)
    }

    fun restorePurchases() {
        billingManager.queryPurchases()
    }
}
