package com.example.pomodoro.ui.premium

import android.app.Activity
import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.pomodoro.data.billing.BillingManager
import com.example.pomodoro.data.datastore.PremiumStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PremiumUiState(
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val premiumUntilMillis: Long? = null,
    val subscriptionPrice: String = "",
    val lifetimePrice: String = "",
    val error: String? = null
)

class PremiumViewModel(
    private val billingManager: BillingManager,
    private val premiumStorage: PremiumStorage
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
        val subPrice = subDetails?.subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice ?: "..."
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

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application)
                val context = application.applicationContext
                val premiumStorage = PremiumStorage(context)
                // Note: BillingManager needs a CoroutineScope that survives configuration changes but respecting app lifecycle.
                // For simplicity here we use a created scope or similar, but ideally it should be a singleton or scoped to App.
                // We'll pass viewModelScope for "externalScope" for now, but strictly speaking 
                // processing purchases should survive ViewModel death. 
                // Better approach: Use GlobalScope or a custom AppScope, OR assumption is simple app.
                // Let's create a BillingManager unique instance if possible or creating new one is fine if it handles connection checks.
                // Given the instructions, we instantiate here.
                val billingManager = BillingManager(context, premiumStorage, kotlinx.coroutines.GlobalScope) 
                PremiumViewModel(billingManager, premiumStorage)
            }
        }
    }
}
