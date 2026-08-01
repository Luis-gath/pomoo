package com.example.pomodoro.features.premium.presentation

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.pomodoro.R
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.premium_upgrade_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
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
                // Header / Intro
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Premium",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Benefits List
                BenefitItem(text = stringResource(id = R.string.premium_benefit_1))
                BenefitItem(text = stringResource(id = R.string.premium_benefit_2))
                BenefitItem(text = stringResource(id = R.string.premium_benefit_3))

                Spacer(modifier = Modifier.height(32.dp))

                // Premium Status
                if (uiState.isPremium) {
                    if (uiState.premiumUntilMillis != null && uiState.premiumUntilMillis!! > System.currentTimeMillis()) {
                        val remainingMillis = uiState.premiumUntilMillis!! - System.currentTimeMillis()
                        val days = TimeUnit.MILLISECONDS.toDays(remainingMillis)
                        Text(
                            text = stringResource(id = R.string.premium_remaining, days),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else if (uiState.premiumUntilMillis == null || uiState.premiumUntilMillis == 0L) {
                         // Lifetime
                        Text(
                            text = stringResource(id = R.string.premium_lifetime_active),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    } else {
                         Text(
                            text = stringResource(id = R.string.premium_remaining_expired),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                             modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            // Purchase Options
            if (!uiState.isPremium || (uiState.premiumUntilMillis != null && uiState.premiumUntilMillis!! < System.currentTimeMillis() + (86400000 * 5))) { 
                // Show options if not premium OR logic to extend (e.g. less than 5 days) 
                // For simplicity, showing if not premium or if it's a sub to allow extend.
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 3 Months Subscription
                    Button(
                        onClick = { activity?.let { viewModel.buySubscription(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(id = R.string.premium_option_3_months))
                            Text(text = uiState.subscriptionPrice, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Lifetime
                    OutlinedButton(
                        onClick = { activity?.let { viewModel.buyLifetime(it) } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                         Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = stringResource(id = R.string.premium_option_lifetime))
                            Text(text = uiState.lifetimePrice, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    TextButton(
                        onClick = { viewModel.restorePurchases() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(text = stringResource(id = R.string.restore_purchases))
                    }
                }
            }
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
