package com.example.jerrycan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.jerrycan.navigation.JerryCanBottomNavigation
import com.example.jerrycan.navigation.NavigationHost
import com.example.jerrycan.navigation.Screen
import com.example.jerrycan.ui.components.RequestBluetoothPermissions
import com.example.jerrycan.ui.theme.JerryCanTheme
import com.example.jerrycan.utils.RequestAllPermissions
import com.example.jerrycan.viewmodel.BluetoothViewModel
import com.jakewharton.threetenabp.AndroidThreeTen
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化ThreeTenABP库，用于处理日期和时间
        AndroidThreeTen.init(this)
        
        enableEdgeToEdge()
        
        setContent {
            JerryCanTheme {
                // 导航控制器
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                
                // 蓝牙ViewModel
                val bluetoothViewModel: BluetoothViewModel = viewModel()
                val uiState by bluetoothViewModel.uiState.collectAsState()
                
                // Snackbar状态
                val snackbarHostState = remember { SnackbarHostState() }
                
                // 请求所有权限（蓝牙和存储）
                RequestAllPermissions(
                    onPermissionsGranted = {
                        // 所有权限获取成功
                    },
                    onPermissionsDenied = {
                        // 一些权限被拒绝
                    }
                )
                
                // 处理错误信息
                LaunchedEffect(uiState.errorMessage) {
                    uiState.errorMessage?.let {
                        snackbarHostState.showSnackbar(it)
                    }
                }
                
                Scaffold(
                    // 底部导航栏
                    bottomBar = {
                        // 只在主要屏幕显示底部导航栏
                        val shouldShowBottomBar = when (currentRoute) {
                            Screen.Chat.route, 
                            Screen.Devices.route, 
                            Screen.Logs.route, 
                            Screen.Settings.route -> true
                            else -> false
                        }
                        
                        if (shouldShowBottomBar) {
                            JerryCanBottomNavigation(navController = navController)
                        }
                    },
                    // Snackbar宿主
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavigationHost(navController = navController)
                    }
                }
            }
        }
    }
    
    override fun onStop() {
        super.onStop()
        // 确保所有消息都已持久化存储
        val viewModel: BluetoothViewModel = ViewModelProvider(this)[BluetoothViewModel::class.java]
        viewModel.ensureMessagePersistence()
    }
}