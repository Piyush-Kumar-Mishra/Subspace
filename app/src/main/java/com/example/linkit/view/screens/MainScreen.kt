
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.linkit.viewmodel.ProfileViewModel

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Dashboard : BottomNavItem("dashboard", Icons.Filled.DateRange, Icons.Outlined.DateRange, "Projects")
    object Profile : BottomNavItem("profile", Icons.Filled.Person, Icons.Outlined.Person, "Profile")
}

@Composable
fun MainScreen(
    onNavigateToCreateProject: () -> Unit,
    onNavigateToTaskScreen: (Long) -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToEditProject: (Long) -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    val bottomNavItems = listOf(BottomNavItem.Dashboard, BottomNavItem.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = {
                            if (index == 1) {
                                // If the profile tab is clicked, navigate to the separate ProfileScreen
                                onNavigateToProfile()
                            } else {
                                selectedTab = index
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) }
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
            DashboardScreen(
                onNavigateToCreateProject = onNavigateToCreateProject,
                onNavigateToTaskScreen = onNavigateToTaskScreen,
                onNavigateToEditProject = onNavigateToEditProject
            )
        }
    }
}





