package com.example.linkit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.linkit.ui.theme.LinkItTheme
import com.example.linkit.view.navigation.NavGraph
import com.example.linkit.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    lateinit var navController: NavHostController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseMessagingService.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        enableEdgeToEdge()

        setContent {
            LinkItTheme {
                navController = rememberNavController()
                val notificationViewModel: NotificationViewModel = hiltViewModel()

                LaunchedEffect(Unit) {
                    notificationViewModel.getFCMToken()
                    handleNotificationIntent(intent, navController)
                }

                NavGraph(navController = navController)
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent, navController)
    }
    private fun handleNotificationIntent(
        intent: Intent,
        navController: NavHostController
    ) {
        val projectId = intent.getLongExtra("projectId", -1L)
        val taskId = intent.getLongExtra("taskId", -1L)
        val type = intent.getStringExtra("notificationType")

        println("DEBUG: Notification clicked. Type: $type, Project: $projectId, Task: $taskId")

        lifecycleScope.launch {
            kotlinx.coroutines.delay(100)

            when (type) {
                "PROJECT_ASSIGNMENT" -> {
                    if (projectId != -1L) {
                        navController.navigate("task_screen/$projectId")
                    }
                }

                "TASK_ASSIGNMENT", "TASK_DEADLINE" -> {
                    if (taskId != -1L) {
                        navController.navigate("task_detail/$taskId")
                    } else if (projectId != -1L) {
                        navController.navigate("task_screen/$projectId")
                    }
                }

                else -> {
                    if (projectId != -1L) navController.navigate("task_screen/$projectId")
                }
            }
        }
    }
}
