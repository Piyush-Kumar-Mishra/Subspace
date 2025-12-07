package com.example.linkit.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.linkit.util.UiEvent
import com.example.linkit.view.screens.*
import com.example.linkit.viewmodel.AuthViewModel
import com.example.linkit.viewmodel.PollViewModel
import com.example.linkit.viewmodel.ProfileViewModel
import com.example.linkit.viewmodel.ProjectViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge

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
    object CreatePoll : Screen("create_poll/{projectId}") {
        fun createRoute(projectId: Long) = "create_poll/$projectId"
    }
    object Analytics : Screen("analytics/{projectId}") {
        fun createRoute(projectId: Long) = "analytics/$projectId"
    }
}

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val projectViewModel: ProjectViewModel = hiltViewModel()
    val pollViewModel: PollViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        merge(authViewModel.uiEvent, pollViewModel.uiEvent).collectLatest { event ->
            when (event) {
                UiEvent.NavigateToGetStarted -> navController.navigate(Screen.GetStarted.route) { popUpTo(0) }
                UiEvent.NavigateToAuth -> navController.navigate(Screen.Auth.route) { popUpTo(0) }
                UiEvent.NavigateToEnterDetails -> navController.navigate(Screen.EnterDetails.route) { popUpTo(0) }
                UiEvent.NavigateToMain -> navController.navigate(Screen.Main.route) { popUpTo(0) }
                UiEvent.NavigateBack -> navController.popBackStack()
                else -> Unit
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) { SplashScreen(viewModel = authViewModel) }
        composable(Screen.GetStarted.route) { GetStartedScreen(onGetStarted = { authViewModel.onGetStarted() }) }
        composable(Screen.Auth.route) { AuthScreen(viewModel = authViewModel) }

        composable(Screen.EnterDetails.route) {
            EnterDetailsScreen(
                viewModel = profileViewModel,
                onNavigateToMain = { navController.navigate(Screen.Main.route) { popUpTo(0) } }
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToCreateProject = { navController.navigate(Screen.CreateProject.route) },
                onNavigateToTaskScreen = { projectId -> navController.navigate(Screen.TaskScreen.createRoute(projectId)) },
                onNavigateToEditProject = { projectId -> navController.navigate(Screen.EditProject.createRoute(projectId)) },
                onNavigateToAnalytics = { projectId -> navController.navigate(Screen.Analytics.createRoute(projectId)) }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(viewModel = profileViewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Screen.CreateProject.route) {
            CreateProjectScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.EditProject.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            EditProjectScreen(
                projectId = it.arguments?.getLong("projectId") ?: 0L,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TaskScreen.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
            val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        projectViewModel.loadProjectById(projectId)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            TaskScreen(
                projectId = projectId,
                viewModel = projectViewModel,
                profileViewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreateTask = { pId -> navController.navigate(Screen.CreateTask.createRoute(pId)) },
                onNavigateToCreatePoll = { pId -> navController.navigate(Screen.CreatePoll.createRoute(pId)) }
            )
        }
        composable(
            route = Screen.CreateTask.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            CreateTaskScreen(
                projectId = it.arguments?.getLong("projectId") ?: 0L,
                viewModel = projectViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.LongType })
        ) {
            TaskDetailScreen(
                taskId = it.arguments?.getLong("taskId") ?: 0L,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.CreatePoll.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) {
            CreatePollScreen(
                projectId = it.arguments?.getLong("projectId") ?: 0L,
                viewModel = pollViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

//        composable(
//            route = Screen.Analytics.route,
//            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
//        ) { backStackEntry ->
//
//            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L
//            val projectVM: ProjectViewModel = hiltViewModel()
//
//            LaunchedEffect(projectId) {
//                projectVM.loadProjectAnalytics(projectId)
//            }
//
//            AnalyticsScreen(
//                projectId = projectId,
//                viewModel = projectVM,
//                onBack = { navController.popBackStack() }
//            )
//        }


        // ... inside NavGraph ...

        composable(
            route = Screen.Analytics.route,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType })
        ) { backStackEntry ->

            val projectId = backStackEntry.arguments?.getLong("projectId") ?: 0L

            AnalyticsScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }

    }
}
