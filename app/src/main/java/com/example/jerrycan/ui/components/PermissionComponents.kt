package com.example.jerrycan.ui.components

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 请求蓝牙所需的权限
 */
@Composable
fun RequestBluetoothPermissions(
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    val context = LocalContext.current
    val permissionsToRequest = remember {
        buildList {
            // 基本蓝牙权限
            add(Manifest.permission.BLUETOOTH)
            add(Manifest.permission.BLUETOOTH_ADMIN)
            
            // Android 12+需要蓝牙扫描和连接权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            
            // 蓝牙扫描需要位置权限
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }.toTypedArray()
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        if (allGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }
    
    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(permissionsToRequest)
    }
} 