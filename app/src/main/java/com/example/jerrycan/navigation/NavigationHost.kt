package com.example.jerrycan.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.jerrycan.ui.screens.BleMessageScreen
import com.example.jerrycan.ui.screens.ChatHistoryScreen
import com.example.jerrycan.ui.screens.ChatScreen
import com.example.jerrycan.ui.screens.DeviceDetailsScreen
import com.example.jerrycan.ui.screens.DeviceSearchScreen
import com.example.jerrycan.ui.screens.DevicesScreen
import com.example.jerrycan.ui.screens.LogDetailScreen
import com.example.jerrycan.ui.screens.LogsScreen
import com.example.jerrycan.ui.screens.SettingsScreen

@Composable
fun NavigationHost(
    navController: NavHostController,
    startDestination: String = Screen.Chat.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 主要屏幕（底部导航）
        composable(Screen.Chat.route) {
            ChatHistoryScreen(navController)
        }
        
        composable(Screen.Devices.route) {
            DevicesScreen(navController)
        }
        
        composable(Screen.Logs.route) {
            LogsScreen(navController)
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(navController)
        }
        
        // 设备搜索页面
        composable(Screen.DeviceSearch.route) {
            DeviceSearchScreen(navController)
        }
        
        // 日志详情页面
        composable(
            route = Screen.LogDetail.route,
            arguments = listOf(
                navArgument("logId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val logId = backStackEntry.arguments?.getString("logId") ?: ""
            LogDetailScreen(navController, logId)
        }
        
        // 聊天历史页面
        composable(Screen.ChatHistory.route) {
            ChatHistoryScreen(navController)
        }
        
        // 聊天详情页面
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("deviceId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            ChatScreen(navController, deviceId)
        }
        
        // BLE消息界面
        composable(
            route = Screen.BleMessage.route,
            arguments = listOf(
                navArgument("deviceId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            BleMessageScreen(navController, deviceId)
        }
        
        // 设备详情页面
        composable(
            route = Screen.DeviceDetails.route,
            arguments = listOf(
                navArgument("deviceId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DeviceDetailsScreen(navController, deviceId)
        }
    }
} 