package com.example.pomodoro.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pomodoro.features.areas.presentation.AreaDetailScreen
import com.example.pomodoro.features.areas.presentation.AreasScreen
import com.example.pomodoro.features.areas.presentation.ReceiveShareScreen
import com.example.pomodoro.features.areas.presentation.UpcomingDeliverablesScreen
import com.example.pomodoro.features.auth.presentation.SignInScreen
import com.example.pomodoro.features.premium.presentation.PremiumUpgradeScreen
import com.example.pomodoro.features.tasks.presentation.CalendarWeekScreen
import com.example.pomodoro.features.timer.presentation.HomeScreen
import com.example.pomodoro.features.timer.presentation.SettingsScreen
import com.example.pomodoro.features.stats.presentation.StatsScreen
import com.example.pomodoro.features.tasks.presentation.TaskDashboardScreen
import com.example.pomodoro.features.tasks.presentation.TaskDetailScreen
import com.example.pomodoro.features.tasks.presentation.TaskEditorScreen
import com.example.pomodoro.features.tasks.presentation.TaskViewModel
import com.example.pomodoro.features.timer.presentation.PomodoroViewModel

/** Rutas con nombre, para no repetir cadenas sueltas por el código. */
object NavGraph {
    const val AREAS = "areas"
    const val RECEIVE_SHARE = "receive_share"
    const val SIGN_IN = "sign_in"
    const val UPCOMING = "upcoming_deliverables"
}

@Composable
fun NavGraph(navController: NavHostController) {
    // Scoped a la Activity para que todas las pantallas compartan el mismo enlace al Service.
    val pomodoroViewModel: PomodoroViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        // Navigation Compose usa un fundido largo por defecto. Al mezclar durante unos
        // instantes la fotografía de Inicio con una superficie lisa parecía que cambiaba
        // el brillo del teléfono. El desplazamiento mantiene ambas pantallas opacas.
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 5 },
                animationSpec = tween(240, easing = FastOutSlowInEasing)
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 5 },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            )
        }
    ) {
        composable("home") {
            HomeScreen(navController = navController, viewModel = pomodoroViewModel)
        }

        // --- Áreas: cursos, habilidades y proyectos con su material ---
        composable(NavGraph.AREAS) {
            AreasScreen(
                onAreaClick = { area -> navController.navigate("area_detail/${area.id}") },
                onBack = { navController.popBackStack() },
                onUpcomingClick = { navController.navigate(NavGraph.UPCOMING) }
            )
        }

        composable(
            route = "area_detail/{areaId}",
            arguments = listOf(navArgument("areaId") { type = NavType.IntType })
        ) {
            AreaDetailScreen(onBack = { navController.popBackStack() })
        }

        // Vista transversal a todas las áreas: responde "qué tengo que entregar" sin
        // tener que entrar área por área.
        composable(NavGraph.UPCOMING) {
            UpcomingDeliverablesScreen(
                onBack = { navController.popBackStack() },
                onAreaClick = { areaId -> navController.navigate("area_detail/$areaId") }
            )
        }

        // Destino al que se llega desde el menú "Compartir" de otra aplicación.
        composable(NavGraph.RECEIVE_SHARE) {
            ReceiveShareScreen(
                onDone = {
                    if (!navController.popBackStack()) {
                        navController.navigate("home") { popUpTo(0) }
                    }
                }
            )
        }
        composable("settings") {
            SettingsScreen(navController = navController, viewModel = pomodoroViewModel)
        }
        composable("stats") {
            StatsScreen(navController = navController)
        }
        composable("premium") {
            PremiumUpgradeScreen(onBackClick = { navController.popBackStack() })
        }

        composable(NavGraph.SIGN_IN) {
            SignInScreen(
                onSignedIn = { navController.popBackStack() },
                onSkip = { navController.popBackStack() }
            )
        }

        // --- Task Dashboard (pantalla principal de tareas) ---
        composable("tasks_list") {
            val taskViewModel: TaskViewModel = hiltViewModel()

            TaskDashboardScreen(
                viewModel = taskViewModel,
                onTaskClick = { task -> navController.navigate("task_detail/${task.id}") },
                onStartTask = { task ->
                    pomodoroViewModel.setActiveTaskWithConfig(task)
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
                onOpenCalendar = { navController.navigate("calendar") }
            )
        }

        composable("calendar") {
            val taskViewModel: TaskViewModel = hiltViewModel()

            CalendarWeekScreen(
                navController = navController,
                viewModel = taskViewModel
            )
        }

        // --- Task Detail ---
        composable(
            route = "task_detail/{taskId}",
            arguments = listOf(navArgument("taskId") { type = NavType.IntType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getInt("taskId") ?: return@composable
            val taskViewModel: TaskViewModel = hiltViewModel()

            val dashboardState by taskViewModel.dashboardState.collectAsState()
            val task = remember(dashboardState.allTasks, taskId) {
                dashboardState.allTasks.find { it.id == taskId }
            }

            task?.let { currentTask ->
                TaskDetailScreen(
                    task = currentTask,
                    onBack = { navController.popBackStack() },
                    onStartTask = { t ->
                        pomodoroViewModel.setActiveTaskWithConfig(t)
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    },
                    onEdit = { t -> navController.navigate("task_editor?taskId=${t.id}") },
                    onAddPomodoro = { taskViewModel.incrementPomodoro(currentTask.id) },
                    onResetProgress = { taskViewModel.resetTaskProgress(currentTask.id) },
                    onDelete = {
                        taskViewModel.deleteTask(currentTask)
                        navController.popBackStack()
                    }
                )
            }
        }

        // --- Task Editor (crear/editar tareas) ---
        composable(
            route = "task_editor?taskId={taskId}",
            arguments = listOf(
                navArgument("taskId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")?.toIntOrNull()
            val taskViewModel: TaskViewModel = hiltViewModel()

            TaskEditorScreen(
                taskId = taskId,
                viewModel = taskViewModel,
                onBack = { navController.popBackStack() },
                onStartTask = { task ->
                    pomodoroViewModel.setActiveTaskWithConfig(task)
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                }
            )
        }
    }
}
