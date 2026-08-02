package com.example.pomodoro.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pomodoro.features.areas.presentation.AreaDetailScreen
import com.example.pomodoro.features.areas.presentation.AreasScreen
import com.example.pomodoro.features.areas.presentation.ReceiveShareScreen
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
}

@Composable
fun NavGraph(navController: NavHostController) {
    // Scoped a la Activity para que todas las pantallas compartan el mismo enlace al Service.
    val pomodoroViewModel: PomodoroViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController, viewModel = pomodoroViewModel)
        }

        // --- Áreas: cursos, habilidades y proyectos con su material ---
        composable(NavGraph.AREAS) {
            AreasScreen(
                onAreaClick = { area -> navController.navigate("area_detail/${area.id}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "area_detail/{areaId}",
            arguments = listOf(navArgument("areaId") { type = NavType.IntType })
        ) {
            AreaDetailScreen(onBack = { navController.popBackStack() })
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
