package com.example.pomodoro.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.pomodoro.ui.components.*
import com.example.pomodoro.viewmodel.StatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Nivel 1: Header & Range Selector
            Spacer(Modifier.height(8.dp))
            StatsHeaderRangeSelector(
                selectedRange = uiState.selectedRange,
                onRangeSelected = { viewModel.setRange(it) }
            )
            
            Spacer(Modifier.height(24.dp))
            
            // Nivel 1 (Cont): KPIs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatKpiCard(
                    value = uiState.focusCount.toString(),
                    label = "Enfoques",
                    icon = Icons.Default.Timer,
                    accentColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatKpiCard(
                    value = uiState.focusMinutes.toString(),
                    label = "Minutos",
                    icon = Icons.Rounded.CheckCircle, // Placeholder
                    accentColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
                StatKpiCard(
                    value = uiState.tasksCompleted.toString(),
                    label = "Tareas",
                    icon = Icons.Default.Assignment,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Nivel 2: Weekly Progress Chart
            Text(
                "Tu Progreso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            AnimatedContent(targetState = uiState.selectedRange, label = "chart_anim") { range ->
                 WeeklyBarChart(
                    data = uiState.weeklyChartData,
                    primaryColor = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Nivel 3: Streak & Productivity
            Text(
                "Productividad",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            StreakCard(
                currentStreak = uiState.currentStreak,
                bestStreak = uiState.bestStreak
            )
            
            Spacer(Modifier.height(32.dp))
            
            // Nivel 4: Top Lists
             if (uiState.topTasks.isNotEmpty()) {
                TopListSection(
                    title = "Top Tareas",
                    items = uiState.topTasks
                )
                Spacer(Modifier.height(32.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
