package com.example.pomodoro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.pomodoro.data.local.TaskDatabase
import com.example.pomodoro.data.repository.TaskRepository
import com.example.pomodoro.receiver.TaskAlarmScheduler
import com.example.pomodoro.ui.premium.PremiumUpgradeScreen
import com.example.pomodoro.ui.screens.HomeScreen
import com.example.pomodoro.ui.screens.SettingsScreen
import com.example.pomodoro.ui.screens.StatsScreen
import com.example.pomodoro.ui.tasks.*

@Composable
fun NavGraph(navController: NavHostController) {
    val pomodoroViewModel: com.example.pomodoro.viewmodel.PomodoroViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            HomeScreen(navController = navController, viewModel = pomodoroViewModel)
        }
        composable("settings") {
            SettingsScreen(navController = navController)
        }
        composable("stats") {
            StatsScreen(navController = navController)
        }
        composable("premium") {
            PremiumUpgradeScreen(onBackClick = { navController.popBackStack() })
        }

        // --- Task Dashboard (Nueva pantalla principal de tareas) ---
        composable("tasks_list") {
            val context = LocalContext.current
            val database = remember { TaskDatabase.getInstance(context) }
            val scheduler = remember { TaskAlarmScheduler(context) }
            val repository = remember { TaskRepository(database.taskDao, scheduler) }
            val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repository))
            
            TaskDashboardScreen(
                viewModel = taskViewModel,
                onTaskClick = { task ->
                    navController.navigate("task_detail/${task.id}")
                },
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
            val context = LocalContext.current
            val database = remember { TaskDatabase.getInstance(context) }
            val scheduler = remember { TaskAlarmScheduler(context) }
            val repository = remember { TaskRepository(database.taskDao, scheduler) }
            val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repository))
            
            com.example.pomodoro.ui.screens.CalendarWeekScreen(
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
            
            val context = LocalContext.current
            val database = remember { TaskDatabase.getInstance(context) }
            val scheduler = remember { TaskAlarmScheduler(context) }
            val repository = remember { TaskRepository(database.taskDao, scheduler) }
            val taskViewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repository))
            
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
                    onEdit = { t ->
                        navController.navigate("task_editor?taskId=${t.id}")
                    },
                    onAddPomodoro = {
                        taskViewModel.incrementPomodoro(currentTask.id)
                    },
                    onResetProgress = {
                        taskViewModel.resetTaskProgress(currentTask.id)
                    },
                    onDelete = {
                        taskViewModel.deleteTask(currentTask)
                        navController.popBackStack()
                    }
                )
            }
        }

        // --- Task Editor (para crear/editar tareas) ---
        composable(
            route = "task_editor?taskId={taskId}",
            arguments = listOf(navArgument("taskId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val taskIdStr = backStackEntry.arguments?.getString("taskId")
            val taskId = taskIdStr?.toIntOrNull()
            
            val context = LocalContext.current
            val database = remember { TaskDatabase.getInstance(context) }
            val scheduler = remember { TaskAlarmScheduler(context) }
            val repository = remember { TaskRepository(database.taskDao, scheduler) }
            val viewModel: TaskViewModel = viewModel(factory = TaskViewModel.Factory(repository))

            TaskEditorScreen(
                taskId = taskId,
                viewModel = viewModel,
                repository = repository,
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
