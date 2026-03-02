package com.example.linkit.view.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.hilt.navigation.compose.hiltViewModel
import com.example.linkit.ui.theme.odisansFamily
import com.example.linkit.viewmodel.ProfileViewModel
import com.example.linkit.viewmodel.ProjectViewModel

sealed class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String
) {
    object Dashboard :
        BottomNavItem("dashboard", Icons.Filled.Dashboard, Icons.Outlined.DashboardCustomize, "Projects")

    object Connections :
        BottomNavItem("connections", Icons.Filled.VolunteerActivism, Icons.Outlined.VolunteerActivism, "Connections")
    object Profile :
        BottomNavItem("profile", Icons.Filled.Tag, Icons.Outlined.Tag, "Profile")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToCreateProject: () -> Unit,
    onNavigateToTaskScreen: (Long) -> Unit,
    onNavigateToEditProject: (Long) -> Unit,
    onNavigateToAnalytics: (Long) -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel(),
    projectViewModel: ProjectViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val bottomNavItems = listOf(BottomNavItem.Dashboard,BottomNavItem.Connections, BottomNavItem.Profile)


    Scaffold(
            topBar = {
                if (selectedTab == 0) {
                    TopAppBar(
                        navigationIcon = {
                            Icon(
                                painter = painterResource(id = com.example.linkit.R.drawable.app_icon_logo),
                                contentDescription = "App Logo",
                                modifier = Modifier.size(32.dp),
                                tint = Color.Unspecified
                            )
                        },
                        title = { Text("Subspace", fontFamily = odisansFamily, fontSize = 20.sp) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.White
                        )
                    )

                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    FloatingActionButton(
                        onClick = onNavigateToCreateProject,
                        shape = RoundedCornerShape(16.dp),
                        containerColor = Color.Black.copy(alpha = 0.8f),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                }
            },

        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier.height(100.dp)
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
                    onNavigateToEditProject = onNavigateToEditProject,
                    onNavigateToAnalytics = onNavigateToAnalytics
                )
                1 -> ConnectionsScreen(
                    viewModel = profileViewModel
                )
                2 -> ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateBack = { }
                )
            }
        }
    }
}
