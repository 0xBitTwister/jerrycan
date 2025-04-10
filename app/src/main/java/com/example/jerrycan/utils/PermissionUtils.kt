package com.example.jerrycan.utils

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 蓝牙权限工具类
 */
object PermissionUtils {
    
    // 获取所需的蓝牙权限
    fun getRequiredBluetoothPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        // 基本蓝牙权限
        permissions.add(Manifest.permission.BLUETOOTH)
        permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        
        // Android 12+需要蓝牙扫描和连接权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        
        // 蓝牙扫描需要位置权限
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        
        return permissions.toTypedArray()
    }
    
    // 获取所需的存储权限
    fun getRequiredStoragePermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        // Android 13+ (API 33+) 使用细分的媒体权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        // Android 10-12 (API 29-32) 使用 READ_EXTERNAL_STORAGE
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        // Android 9 及以下 (API 28-) 需要 READ 和 WRITE 权限
        else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        return permissions.toTypedArray()
    }
    
    // 获取所有应用所需权限
    fun getAllRequiredPermissions(): Array<String> {
        return (getRequiredBluetoothPermissions() + getRequiredStoragePermissions()).distinct().toTypedArray()
    }
    
    // 检查是否有所有必要的权限
    fun hasRequiredBluetoothPermissions(context: Context): Boolean {
        // Android 12及以上版本 (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // 对于Android 12+，需要BLUETOOTH_SCAN和BLUETOOTH_CONNECT权限
            val hasScanPermission = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
            
            val hasConnectPermission = ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            
            return hasScanPermission && hasConnectPermission
        } 
        // Android 10及以上, Android 12以下 (API 29-30)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 只需要位置权限
            return ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } 
        // Android 10以下
        else {
            // 需要粗略或精确位置权限
            return ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context, 
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // 检查是否有所需的存储权限
    fun hasRequiredStoragePermissions(context: Context): Boolean {
        // Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        }
        // Android 10-12 (API 29-32)
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        // Android 9 及以下 (API 28-)
        else {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED && 
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    // 检查是否有应用所需的所有权限
    fun hasAllRequiredPermissions(context: Context): Boolean {
        return hasRequiredBluetoothPermissions(context) && hasRequiredStoragePermissions(context)
    }
    
    // 检查蓝牙是否启用
    fun isBluetoothEnabled(bluetoothAdapter: BluetoothAdapter?): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    // 请求启用蓝牙
    fun requestEnableBluetooth(activity: Activity) {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            // 没有连接权限，需要先请求权限
            return
        }
        activity.startActivityForResult(enableBtIntent, 1)
    }
}

/**
 * Compose中使用的蓝牙权限请求Hook
 */
@Composable
fun RequestBluetoothPermissions(
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    var permissionsGranted by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        permissionsGranted = permissionsMap.values.all { it }
        if (permissionsGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }
    
    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(PermissionUtils.getRequiredBluetoothPermissions())
    }
}

/**
 * Compose中使用的所有权限请求Hook
 */
@Composable
fun RequestAllPermissions(
    onPermissionsGranted: () -> Unit = {},
    onPermissionsDenied: () -> Unit = {}
) {
    var permissionsGranted by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        permissionsGranted = permissionsMap.values.all { it }
        if (permissionsGranted) {
            onPermissionsGranted()
        } else {
            onPermissionsDenied()
        }
    }
    
    LaunchedEffect(key1 = true) {
        permissionLauncher.launch(PermissionUtils.getAllRequiredPermissions())
    }
} 