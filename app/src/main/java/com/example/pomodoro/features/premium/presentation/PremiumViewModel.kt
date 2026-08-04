package com.example.pomodoro.features.premium.presentation

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pomodoro.features.premium.data.BillingEvent
import com.example.pomodoro.features.premium.data.BillingManager
import com.example.pomodoro.features.premium.data.BillingState
import com.example.pomodoro.features.premium.data.PremiumStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PremiumUiState(
    val isPremium: Boolean = false,
    val isLifetime: Boolean = false,
    val subscriptionActive: Boolean = false,
    /** Renovación aproximada. null si no se conoce: entonces no se muestra cuenta atrás. */
    val renewsAtMillis: Long? = null,
    val subscriptionPrice: String? = null,
    val lifetimePrice: String? = null,
    val billingState: BillingState = BillingState.Connecting,
    val message: String? = null
) {
    /** Los precios llegan de Play; hasta entonces no hay nada que ofrecer. */
    val pricesLoaded: Boolean get() = subscriptionPrice != null || lifetimePrice != null
}

@HiltViewModel
class PremiumViewModel @Inject constructor(
    private val billingManager: BillingManager,
    premiumStorage: PremiumStorage
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PremiumUiState> = combine(
        premiumStorage.isPremium,
        premiumStorage.isLifetime,
        premiumStorage.subscriptionActive,
        premiumStorage.subscriptionRenewsAt,
        combine(billingManager.offers, billingManager.billingState, message) { o, s, m -> Triple(o, s, m) }
    ) { isPremium, isLifetime, subActive, renewsAt, (offers, state, msg) ->
        PremiumUiState(
            isPremium = isPremium,
            isLifetime = isLifetime,
            subscriptionActive = subActive,
            renewsAtMillis = renewsAt,
            subscriptionPrice = offers[BillingManager.PRODUCT_ID_SUBSCRIPTION_3M]?.formattedPrice,
            lifetimePrice = offers[BillingManager.PRODUCT_ID_LIFETIME]?.formattedPrice,
            billingState = state,
            message = msg
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PremiumUiState())

    init {
        billingManager.startConnection()
        viewModelScope.launch {
            billingManager.events.collect { event ->
                message.value = when (event) {
                    BillingEvent.PurchaseCompleted -> "¡Listo! Ya tienes premium."
                    // El pago en efectivo o por transferencia tarda en confirmarse, y
                    // sin este aviso el usuario creía que la compra había fallado.
                    BillingEvent.PurchasePending ->
                        "Tu pago está pendiente de confirmación. Te avisaremos cuando se complete."
                    is BillingEvent.PurchaseFailed -> event.message
                }
            }
        }
    }

    /** Debe llamarse al volver a primer plano: la compra puede haberse hecho fuera de la app. */
    fun refresh() = billingManager.refreshPurchases()

    fun buySubscription(activity: Activity) =
        billingManager.launchPurchaseFlow(activity, BillingManager.PRODUCT_ID_SUBSCRIPTION_3M)

    fun buyLifetime(activity: Activity) =
        billingManager.launchPurchaseFlow(activity, BillingManager.PRODUCT_ID_LIFETIME)

    fun restorePurchases() {
        billingManager.refreshPurchases()
        message.value = "Comprobando tus compras..."
    }

    fun dismissMessage() {
        message.value = null
    }
}
