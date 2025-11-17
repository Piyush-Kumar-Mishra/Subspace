package com.example.linkit.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.linkit.viewmodel.ProfileViewModel
import com.example.linkit.viewmodel.ProjectViewModel

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Dashboard :
        BottomNavItem("dashboard", Icons.Filled.DateRange, Icons.Outlined.DateRange, "Projects")

    object Profile :
        BottomNavItem("profile", Icons.Filled.Person, Icons.Outlined.Person, "Profile")
}

@Composable
fun MainScreen(
    onNavigateToCreateProject: () -> Unit,
    onNavigateToTaskScreen: (Long) -> Unit,
    onNavigateToEditProject: (Long) -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val bottomNavItems = listOf(BottomNavItem.Dashboard, BottomNavItem.Profile)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index)
                                    item.selectedIcon
                                else
                                    item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = Color.Gray,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    viewModel = projectViewModel,
                    onNavigateToCreateProject = onNavigateToCreateProject,
                    onNavigateToTaskScreen = onNavigateToTaskScreen,
                    onNavigateToEditProject = onNavigateToEditProject
                )
                1 -> ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { }
                )
            }
        }
    }
}
