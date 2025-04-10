package com.example.jerrycan.navigation

/**
 * 定义应用中的所有屏幕路由
 */
sealed class Screen(val route: String) {
    // 主要屏幕（底部导航）
    object Chat : Screen("chat")
    object Devices : Screen("devices") 
    object Logs : Screen("logs")
    object Settings : Screen("settings")
    
    // 其他屏幕
    object DeviceDetails : Screen("device_details/{deviceId}") {
        fun createRoute(deviceId: String) = "device_details/$deviceId"
    }
    
    object DeviceSearch : Screen("device_search")
    
    object ChatDetail : Screen("chat_detail/{deviceId}") {
        fun createRoute(deviceId: String) = "chat_detail/$deviceId"
    }
    
    object ChatHistory : Screen("chat_history")
    
    object BleMessage : Screen("ble_message/{deviceId}") {
        fun createRoute(deviceId: String) = "ble_message/$deviceId"
    }
    
    object LogDetail : Screen("log_detail/{logId}") {
        fun createRoute(logId: String) = "log_detail/$logId"
    }
} 