package com.example.jerrycan.navigation

import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.jerrycan.R
import com.example.jerrycan.ui.theme.NordicBlue

// 底部导航项
sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val icon: @Composable () -> Unit
) {
    // 聊天页面
    object Chat : BottomNavItem(
        route = Screen.Chat.route,
        titleResId = R.string.nav_chat,
        icon = { Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null) }
    )
    
    // 设备页面
    object Devices : BottomNavItem(
        route = Screen.Devices.route,
        titleResId = R.string.nav_devices,
        icon = { Icon(Icons.Filled.Contacts, contentDescription = null) }
    )
    
    // 日志页面
    object Logs : BottomNavItem(
        route = Screen.Logs.route,
        titleResId = R.string.nav_logs,
        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) }
    )
    
    // 设置页面
    object Settings : BottomNavItem(
        route = Screen.Settings.route,
        titleResId = R.string.nav_settings,
        icon = { Icon(Icons.Filled.Settings, contentDescription = null) }
    )
}

// 底部导航栏列表
val bottomNavItems = listOf(
    BottomNavItem.Chat,
    BottomNavItem.Devices,
    BottomNavItem.Logs,
    BottomNavItem.Settings
)

@Composable
fun JerryCanBottomNavigation(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    // 底部导航栏
    NavigationBar(
        containerColor = Color(0xFFF7F7F7),
        contentColor = NordicBlue,
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                icon = { item.icon() },
                label = { 
                    Text(
                        text = stringResource(id = item.titleResId),
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
                    ) 
                },
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        // 弹出到起始目的地，避免堆栈无限增长
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 避免重复创建同一目的地的多个副本
                        launchSingleTop = true
                        // 恢复状态
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NordicBlue,
                    selectedTextColor = NordicBlue,
                    unselectedIconColor = Color(0xFF8A8A8A),
                    unselectedTextColor = Color(0xFF8A8A8A),
                    indicatorColor = Color(0xFFF7F7F7)
                )
            )
        }
    }
} 