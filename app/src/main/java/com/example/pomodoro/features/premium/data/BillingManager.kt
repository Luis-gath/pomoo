package com.example.pomodoro.features.premium.data

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Period
import kotlin.coroutines.resume

/** Disponibilidad de la facturación, para que la pantalla pueda explicar qué pasa. */
sealed interface BillingState {
    data object Connecting : BillingState
    data object Ready : BillingState

    /**
     * Conecta con Play pero no hay nada que vender.
     *
     * Pasa cuando los productos no existen o no están activos en Play Console, y muy
     * habitualmente porque la app todavía no se ha subido a un canal de pruebas: hasta
     * entonces Play no reconoce los productos. Sin distinguir este caso la pantalla se
     * quedaba girando para siempre.
     */
    data object NoProducts : BillingState

    /** Play no está disponible: dispositivo sin Play Store, versión antigua, sin red... */
    data class Unavailable(val reason: String) : BillingState
}

/** Avisos puntuales dirigidos al usuario. */
sealed interface BillingEvent {
    data object PurchasePending : BillingEvent
    data class PurchaseFailed(val message: String) : BillingEvent
    data object PurchaseCompleted : BillingEvent
}

/**
 * Una oferta concreta de un producto, ya resuelta.
 *
 * El precio mostrado y el [offerToken] con el que se cobra viajan juntos a propósito:
 * cuando se elegían por separado se podía enseñar el precio de una oferta y cobrar otra.
 */
data class ProductOffer(
    val productId: String,
    val formattedPrice: String,
    val offerToken: String?,
    val billingPeriodIso: String?
)

class BillingManager(
    private val context: Context,
    private val premiumStorage: PremiumStorage,
    private val externalScope: CoroutineScope
) {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(::onPurchasesUpdated)
        // La sobrecarga sin argumentos está obsoleta desde Billing 7.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _offers = MutableStateFlow<Map<String, ProductOffer>>(emptyMap())
    val offers: StateFlow<Map<String, ProductOffer>> = _offers.asStateFlow()

    private val _billingState = MutableStateFlow<BillingState>(BillingState.Connecting)
    val billingState: StateFlow<BillingState> = _billingState.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private var reconnectAttempts = 0

    /** Los ProductDetails hacen falta para lanzar la compra, no solo para mostrar precios. */
    private var cachedDetails: Map<String, ProductDetails> = emptyMap()

    companion object {
        const val PRODUCT_ID_SUBSCRIPTION_3M = "premium_3m"
        const val PRODUCT_ID_LIFETIME = "premium_lifetime"

        private const val TAG = "BillingManager"
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val ACKNOWLEDGE_ATTEMPTS = 3
    }

    fun startConnection() {
        if (billingClient.isReady) {
            refreshPurchases()
            return
        }
        connect()
    }

    private fun connect() {
        Log.i(TAG, "Conectando con Google Play...")
        _billingState.value = BillingState.Connecting
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                Log.i(TAG, "Conexión terminada, código ${result.responseCode}")
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    reconnectAttempts = 0
                    _billingState.value = BillingState.Ready
                    try {
                        queryProductDetails()
                        refreshPurchases()
                    } catch (e: Throwable) {
                        // Esto corre en un hilo de binder: sin capturar, la excepción se
                        // pierde y la pantalla se queda cargando sin explicación.
                        Log.e(TAG, "Fallo al consultar Play tras conectar", e)
                        _billingState.value = BillingState.Unavailable(
                            e.message ?: "Error consultando Google Play"
                        )
                    }
                } else {
                    Log.w(TAG, "Conexión rechazada: ${result.debugMessage}")
                    _billingState.value = BillingState.Unavailable(
                        result.debugMessage.ifBlank { "Google Play no está disponible" }
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                // Play se desconecta con frecuencia. Sin reintento la facturación
                // quedaba muerta hasta reiniciar el proceso.
                _billingState.value = BillingState.Connecting
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            _billingState.value = BillingState.Unavailable("No se pudo conectar con Google Play")
            return
        }
        val attempt = reconnectAttempts++
        externalScope.launch {
            delay(1_000L shl attempt) // 1s, 2s, 4s, 8s, 16s
            connect()
        }
    }

    /** Debe llamarse al volver a primer plano: la compra puede haber ocurrido fuera. */
    fun refreshPurchases() {
        externalScope.launch { reconcilePurchases() }
    }

    /**
     * Pide los productos a Play, **una consulta por tipo**.
     *
     * Mezclar la suscripción y el producto único en la misma llamada hace que
     * QueryProductDetailsParams lance `IllegalArgumentException: All products should be
     * of the same product type`. Como esto se ejecuta en un hilo de binder, la excepción
     * se perdía sin dejar rastro y la pantalla se quedaba sin precios para siempre.
     */
    private fun queryProductDetails() {
        externalScope.launch {
            val details =
                queryDetails(BillingClient.ProductType.SUBS, PRODUCT_ID_SUBSCRIPTION_3M) +
                    queryDetails(BillingClient.ProductType.INAPP, PRODUCT_ID_LIFETIME)

            cachedDetails = details.associateBy { it.productId }
            _offers.value = details.mapNotNull { it.toOffer() }.associateBy { it.productId }

            _billingState.value = if (_offers.value.isEmpty()) {
                Log.w(TAG, "Play no devolvió ningún producto vendible")
                BillingState.NoProducts
            } else {
                BillingState.Ready
            }
        }
    }

    private suspend fun queryDetails(type: String, productId: String): List<ProductDetails> =
        suspendCancellableCoroutine { cont ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(type)
                            .build()
                    )
                )
                .build()

            billingClient.queryProductDetailsAsync(params) { result, details ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "Consulta de $type falló: ${result.debugMessage}")
                }
                cont.resume(details)
            }
        }

    /**
     * Resuelve la oferta con la que se va a comprar.
     *
     * Entre varias ofertas elige la de menor precio recurrente, que es la mejor para el
     * usuario y además es determinista. Antes se cogía la primera de la lista, tanto
     * aquí como al mostrar el precio, sin garantía de que fueran la misma.
     */
    private fun ProductDetails.toOffer(): ProductOffer? = when (productType) {
        BillingClient.ProductType.SUBS -> {
            subscriptionOfferDetails
                ?.mapNotNull { offer ->
                    val recurring = offer.pricingPhases.pricingPhaseList.lastOrNull()
                        ?: return@mapNotNull null
                    Triple(offer, recurring, recurring.priceAmountMicros)
                }
                ?.minByOrNull { it.third }
                ?.let { (offer, recurring, _) ->
                    ProductOffer(
                        productId = productId,
                        formattedPrice = recurring.formattedPrice,
                        offerToken = offer.offerToken,
                        billingPeriodIso = recurring.billingPeriod
                    )
                }
        }

        else -> oneTimePurchaseOfferDetails?.let {
            ProductOffer(
                productId = productId,
                formattedPrice = it.formattedPrice,
                offerToken = null,
                billingPeriodIso = null
            )
        }
    }

    fun launchPurchaseFlow(activity: Activity, productId: String) {
        if (!billingClient.isReady) {
            _events.tryEmit(BillingEvent.PurchaseFailed("La conexión con Google Play no está lista"))
            return
        }

        val offer = _offers.value[productId]
        if (offer == null) {
            // Antes esto era un `?: return` silencioso: el usuario pulsaba Comprar y no
            // ocurría absolutamente nada.
            _events.tryEmit(BillingEvent.PurchaseFailed("Este producto no está disponible ahora mismo"))
            return
        }

        val details = cachedDetails[productId]
        if (details == null) {
            _events.tryEmit(BillingEvent.PurchaseFailed("Este producto no está disponible ahora mismo"))
            return
        }

        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply { offer.offerToken?.let { setOfferToken(it) } }
            .build()

        val result = billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(params))
                .build()
        )

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(
                BillingEvent.PurchaseFailed(
                    result.debugMessage.ifBlank { "No se pudo abrir el pago" }
                )
            )
        }
    }

    private fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                externalScope.launch {
                    purchases?.forEach { acknowledgeIfNeeded(it) }
                    reconcilePurchases()
                    val pending = purchases.orEmpty()
                        .any { it.purchaseState == Purchase.PurchaseState.PENDING }
                    _events.tryEmit(
                        if (pending) BillingEvent.PurchasePending
                        else BillingEvent.PurchaseCompleted
                    )
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> Unit

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // El usuario ya lo tiene: basta con volver a leer el estado real.
                refreshPurchases()
            }

            else -> {
                Log.w(TAG, "Compra fallida: ${result.debugMessage}")
                _events.tryEmit(
                    BillingEvent.PurchaseFailed(
                        result.debugMessage.ifBlank { "No se pudo completar la compra" }
                    )
                )
            }
        }
    }

    /**
     * Lee el estado real en Play y lo vuelca en el almacenamiento local.
     *
     * Concede y retira en la misma pasada: si una compra ya no aparece (cancelada,
     * caducada o reembolsada) el premium se retira. Antes solo se concedía, así que un
     * reembolso dejaba el acceso puesto para siempre.
     */
    private suspend fun reconcilePurchases() {
        if (!billingClient.isReady) return

        val subs = queryPurchases(BillingClient.ProductType.SUBS)
        val inApp = queryPurchases(BillingClient.ProductType.INAPP)

        (subs + inApp).forEach { acknowledgeIfNeeded(it) }

        // Solo cuenta lo efectivamente pagado: una compra PENDING todavía no da acceso.
        val activeSub = subs.firstOrNull {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                it.products.contains(PRODUCT_ID_SUBSCRIPTION_3M)
        }
        val lifetime = inApp.any {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                it.products.contains(PRODUCT_ID_LIFETIME)
        }

        premiumStorage.reconcile(
            lifetimeOwned = lifetime,
            subscriptionActive = activeSub != null,
            subscriptionRenewsAt = activeSub?.let { estimateRenewal(it) }
        )
    }

    /**
     * Fecha aproximada de la próxima renovación, solo para mostrar.
     *
     * Se calcula con el periodo real del plan, no con noventa días fijos como antes.
     * Si no conocemos el periodo devuelve null y la pantalla omite la cuenta atrás:
     * es preferible a enseñar una fecha inventada. El dato exacto solo lo da la
     * Google Play Developer API desde un servidor.
     */
    private fun estimateRenewal(purchase: Purchase): Long? {
        val iso = _offers.value[PRODUCT_ID_SUBSCRIPTION_3M]?.billingPeriodIso ?: return null
        return try {
            val period = Period.parse(iso)
            val days = period.toTotalMonths() * 30 + period.days
            if (days <= 0) null
            else purchase.purchaseTime + days * 24L * 60 * 60 * 1000
        } catch (e: Exception) {
            Log.w(TAG, "Periodo de facturación no reconocido: $iso", e)
            null
        }
    }

    /**
     * Confirma la compra, reintentando si hace falta.
     *
     * Google reembolsa automáticamente cualquier compra que siga sin confirmar a los
     * tres días, así que un fallo silencioso aquí significa venta perdida y usuario sin
     * acceso. Antes no había ni reintento ni registro.
     */
    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        repeat(ACKNOWLEDGE_ATTEMPTS) { attempt ->
            val result = suspendCancellableCoroutine { cont ->
                billingClient.acknowledgePurchase(params) { cont.resume(it) }
            }
            if (result.responseCode == BillingClient.BillingResponseCode.OK) return

            Log.w(TAG, "Acknowledge fallido (intento ${attempt + 1}): ${result.debugMessage}")
            delay(2_000L shl attempt)
        }
        Log.e(TAG, "Compra sin confirmar: Google la reembolsará en 3 días")
    }

    private suspend fun queryPurchases(type: String): List<Purchase> =
        suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(type).build()
            ) { result, purchases ->
                cont.resume(
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases
                    else emptyList()
                )
            }
        }

    /** Cierra la conexión. Antes quedaba abierta durante toda la vida del proceso. */
    fun endConnection() {
        if (billingClient.isReady) billingClient.endConnection()
    }
}
