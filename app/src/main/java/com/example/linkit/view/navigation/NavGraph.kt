package com.example.linkit.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.linkit.util.UiEvent
import com.example.linkit.view.screens.*
import com.example.linkit.viewmodel.AuthViewModel
import com.example.linkit.viewmodel.ProfileViewModel
import com.example.linkit.viewmodel.ProjectViewModel
import kotlinx.coroutines.flow.collectLatest

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object GetStarted : Screen("get_started")
    object Auth : Screen("auth")
    object EnterDetails : Screen("enter_details")
    object Main : Screen("main")
    object Profile : Screen("profile")
    object CreateProject : Screen("create_project")

    object EditProject : Screen("edit_project/{projectId}") {
        fun createRoute(projectId: Long) = "edit_project/$projectId"
    }
    object TaskScreen : Screen("task_screen/{projectId}") {
        fun createRoute(projectId: Long) = "task_screen/$projectId"
    }
    object CreateTask : Screen("create_task/{projectId}") {
        fun createRoute(projectId: Long) = "create_task/$projectId"
    }
    object TaskDetail : Screen("task_detail/{taskId}") {
        fun createRoute(taskId: Long) = "task_detail/$taskId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val projectViewModel: ProjectViewModel = hiltViewModel()

    // Handle AuthViewModel events
    LaunchedEffect(Unit) {
        authViewModel.uiEvent.collectLatest { event ->
            when (event) {
                UiEvent.NavigateToGetStarted -> {
                    // Clear all state when navigating to get started (logout)
                    authViewModel.clearAllState()
                    profileViewModel.clearAllState()
                    navController.navigate(Screen.GetStarted.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                UiEvent.NavigateToAuth -> navController.navigate(Screen.Auth.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                UiEvent.NavigateToEnterDetails -> {
                    // Clear profile state when navigating to enter details for new user
                    profileViewModel.clearAllState()
                    navController.navigate(Screen.EnterDetails.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                UiEvent.NavigateToMain -> {
                    // Refresh profile data when navigating to main
                    profileViewModel.refreshAllData()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                else -> Unit
            }
        }
    }

    // Handle ProfileViewModel events
    LaunchedEffect(Unit) {
        profileViewModel.uiEvent.collectLatest { event ->
            when (event) {
                UiEvent.NavigateToGetStarted -> {
                    // Clear all state when navigating to get started (logout)
                    authViewModel.clearAllState()
                    profileViewModel.clearAllState()
                    navController.navigate(Screen.GetStarted.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                UiEvent.NavigateToEnterDetails -> navController.navigate(Screen.EnterDetails.route) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
                UiEvent.NavigateToMain -> {
                    // Refresh profile data when navigating to main
                    profileViewModel.refreshAllData()
                    navController.navigate(Screen.Main.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                UiEvent.NavigateToProfile -> navController.navigate(Screen.Profile.route)
                UiEvent.NavigateToAuth -> {
                    // Clear all state when navigating back to auth
                    authViewModel.clearAllState()
                    profileViewModel.clearAllState()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                else -> Unit
            }
        }
    }

    // Handle ProjectViewModel events
    LaunchedEffect(Unit) {
        projectViewModel.uiEvent.collectLatest { event ->
            when (event) {
                UiEvent.NavigateBack -> navController.popBackStack()
                UiEvent.NavigateToAuth -> {
                    authViewModel.clearAllState()
                    profileViewModel.clearAllState()
                    navController.navigate(Screen.Auth.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                UiEvent.NavigateToGetStarted -> {
                    authViewModel.clearAllState()
                    profileViewModel.clearAllState()
                    navController.navigate(Screen.GetStarted.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
                is UiEvent.NavigateToProject -> {
                    navController.navigate(Screen.TaskScreen.createRoute(event.projectId))
                }
                is UiEvent.NavigateToCreateTask -> {
                    navController.navigate(Screen.CreateTask.createRoute(event.projectId))
                }
                is UiEvent.NavigateToTask -> {
                    navController.navigate(Screen.TaskDetail.createRoute(event.taskId))
                }
                else -> Unit
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(viewModel = authViewModel)
        }
        composable(Screen.GetStarted.route) {
            GetStartedScreen(onGetStarted = { authViewModel.onGetStarted() })
        }
        composable(Screen.Auth.route) {
            AuthScreen(viewModel = authViewModel)
        }
        composable(Screen.EnterDetails.route) {
            EnterDetailsScreen(
                viewModel = profileViewModel,
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToCreateProject = { navController.navigate(Screen.CreateProject.route) },
                onNavigateToTaskScreen = { projectId ->
                    navController.navigate(Screen.TaskScreen.createRoute(projectId))
                },
                onNavigateToEditProject = { projectId -> // ADDED
                    navController.navigate(Screen.EditProject.createRoute(projectId))
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.CreateProject.route) {
            CreateProjectScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.EditProject.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            EditProjectScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.TaskScreen.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            TaskScreen(
                projectId = projectId,
                viewModel = projectViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateTask = { pId ->
                    navController.navigate(Screen.CreateTask.createRoute(pId))
                }
            )
        }
        composable(
            Screen.CreateTask.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            CreateTaskScreen(
                projectId = projectId,
                viewModel = projectViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getLong("taskId") ?: 0L
            TaskDetailScreen(
                taskId = taskId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}