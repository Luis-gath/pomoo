package com.example.pomodoro.features.premium.presentation

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.pomodoro.R
import com.example.pomodoro.features.premium.data.BillingState
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumUpgradeScreen(
    onBackClick: () -> Unit,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    // La compra puede completarse fuera de la app (pago pendiente, otro dispositivo),
    // así que hay que releer el estado al volver, no solo al conectar.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.premium_upgrade_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                BenefitItem(text = stringResource(id = R.string.premium_benefit_1))
                BenefitItem(text = stringResource(id = R.string.premium_benefit_2))
                BenefitItem(text = stringResource(id = R.string.premium_benefit_3))

                Spacer(modifier = Modifier.height(32.dp))

                PremiumStatus(uiState, Modifier.align(Alignment.CenterHorizontally))
            }

            PurchaseSection(
                uiState = uiState,
                onBuySubscription = { activity?.let(viewModel::buySubscription) },
                onBuyLifetime = { activity?.let(viewModel::buyLifetime) },
                onRestore = viewModel::restorePurchases
            )
        }
    }
}

@Composable
private fun PremiumStatus(uiState: PremiumUiState, modifier: Modifier = Modifier) {
    when {
        uiState.isLifetime -> Text(
            text = stringResource(id = R.string.premium_lifetime_active),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            modifier = modifier
        )

        uiState.subscriptionActive -> {
            val renewsAt = uiState.renewsAtMillis
            val days = renewsAt
                ?.minus(System.currentTimeMillis())
                ?.takeIf { it > 0 }
                ?.let { TimeUnit.MILLISECONDS.toDays(it) }

            Text(
                // Sin fecha fiable se dice solo que está activa. Antes se mostraba una
                // cuenta atrás calculada con noventa días fijos, que no era real.
                text = if (days != null) {
                    stringResource(id = R.string.premium_remaining, days)
                } else {
                    stringResource(id = R.string.premium_subscription_active)
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }
    }
}

@Composable
private fun PurchaseSection(
    uiState: PremiumUiState,
    onBuySubscription: () -> Unit,
    onBuyLifetime: () -> Unit,
    onRestore: () -> Unit
) {
    // Con licencia permanente no hay nada más que vender.
    if (uiState.isLifetime) return

    // Antes, cualquier fallo de facturación dejaba la pantalla con "..." de precio
    // indefinidamente y sin ninguna explicación.
    val explanation = when (uiState.billingState) {
        is BillingState.Unavailable -> R.string.premium_billing_unavailable
        BillingState.NoProducts -> R.string.premium_no_products
        else -> null
    }
    if (explanation != null) {
        Text(
            text = stringResource(id = explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        return
    }

    if (!uiState.pricesLoaded) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(28.dp))
        }
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        uiState.subscriptionPrice?.let { price ->
            Button(
                onClick = onBuySubscription,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(id = R.string.premium_option_3_months))
                    Text(text = price, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        uiState.lifetimePrice?.let { price ->
            OutlinedButton(
                onClick = onBuyLifetime,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = stringResource(id = R.string.premium_option_lifetime))
                    Text(text = price, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        TextButton(
            onClick = onRestore,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(text = stringResource(id = R.string.restore_purchases))
        }
    }
}

@Composable
fun BenefitItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
