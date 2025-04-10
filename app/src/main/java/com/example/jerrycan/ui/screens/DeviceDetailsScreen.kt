package com.example.jerrycan.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jerrycan.R
import com.example.jerrycan.model.BluetoothDevice
import com.example.jerrycan.navigation.Screen
import com.example.jerrycan.ui.components.LoadingIndicator
import com.example.jerrycan.viewmodel.BluetoothViewModel
import com.example.jerrycan.bluetooth.BluetoothService.ServiceInfo
import com.example.jerrycan.bluetooth.BluetoothService.CharacteristicInfo
import com.example.jerrycan.bluetooth.GattProfileConstants
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.jerrycan.ui.screens.getSignalIcon
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BluetoothSearching
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.BackHandler
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import com.example.jerrycan.utils.FileUtils
import java.io.File
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailsScreen(
    navController: NavController,
    deviceId: String,
    viewModel: BluetoothViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val favoriteDevices by viewModel.favoriteDevices.collectAsState()
    
    // Add the context declaration
    val context = LocalContext.current
    
    // 使用记忆化的设备对象，防止设备列表更新时丢失
    var deviceState by remember { mutableStateOf<BluetoothDevice?>(null) }
    
    // 显示服务区域的状态
    var showServices by remember { mutableStateOf(false) }
    
    // 首次进入页面时，尝试获取设备并保存，同时发起连接
    LaunchedEffect(key1 = deviceId) {
        // 使用ViewModel中的getDevice方法获取设备
        val device = viewModel.getDevice(deviceId)
        if (device != null) {
            deviceState = device
            
            // 移除自动连接逻辑，只保存设备信息
            // 不再自动连接：if (!device.isConnected) { viewModel.connectDevice(device) }
        } else {
            // 如果找不到设备，尝试启动一次扫描
            viewModel.startScan(5000) // 短时间扫描
        }
    }
    
    // 监听设备列表变化，尝试更新当前设备
    LaunchedEffect(key1 = uiState.deviceList) {
        if (deviceState == null) {
            val device = viewModel.getDevice(deviceId)
            if (device != null) {
                deviceState = device
                
                // 移除自动连接逻辑，只保存设备信息
                // 不再自动连接：if (!device.isConnected) { viewModel.connectDevice(device) }
            }
        }
    }
    
    // 使用本地保存的设备对象
    val device = deviceState
    
    // 连接状态
    val isConnecting = uiState.isConnecting
    // 已连接设备（用于检查当前设备是否已连接）
    val connectedDevice = uiState.connectedDevice
    val isConnected = connectedDevice != null && connectedDevice.id == deviceId
    
    // 收藏状态
    val isFavorite = favoriteDevices.contains(deviceId)
    
    // 剪贴板管理器
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    
    // 服务发现相关状态
    var lastServicesUpdate by remember { mutableLongStateOf(0L) }
    
    // 添加一个状态变量来跟踪当前展开的服务UUID
    var expandedServiceUuid by remember { mutableStateOf<String?>(null) }
    
    // 获取服务列表，使用remember来缓存，依赖连接状态、设备ID和服务更新时间戳变化
    var servicesList by remember { mutableStateOf<List<ServiceInfo>>(emptyList()) }
    
    // 使用servicesUpdated时间戳跟踪服务更新
    val servicesUpdatedTimestamp = uiState.servicesUpdated
    val coroutineScope = rememberCoroutineScope()
    
    // 服务刷新状态
    var isRefreshing by remember { mutableStateOf(false) }
    
    // 当连接状态变化时，更新设备信息
    LaunchedEffect(isConnected) {
        if (isConnected) {
            // 更新设备状态为已连接的设备
            deviceState = connectedDevice
            
            // 设备刚连接成功时，自动发现服务
            Log.d("DeviceDetailsScreen", "设备连接成功，开始自动发现服务")
            isRefreshing = true
            viewModel.refreshDeviceServices(deviceId)
            
            // 等待服务发现完成
            delay(500)
            servicesList = viewModel.getDeviceServices(deviceId)
            
            // 如果还是没有服务，再多等几次
            var retryCount = 0
            while (servicesList.isEmpty() && retryCount < 5) {
                delay(500)
                servicesList = viewModel.getDeviceServices(deviceId)
                retryCount++
            }
            
            isRefreshing = false
            Log.d("DeviceDetailsScreen", "服务发现完成，发现 ${servicesList.size} 个服务")
        }
    }
    
    // 确保服务列表被缓存，并避免频繁触发
    LaunchedEffect(deviceId, isConnected, lastServicesUpdate, servicesUpdatedTimestamp) {
        // 仅当设备已连接、但不是在主动刷新服务时，才从缓存获取服务列表
        if (isConnected && device?.id != null && !isRefreshing) {
            // 设备已连接时，尝试获取服务列表
            val deviceServices = viewModel.getDeviceServices(device.id)
            Log.d("DeviceDetailsScreen", "从缓存获取服务列表: ${deviceServices.size}个服务")
            servicesList = deviceServices
            
            // 如果没有服务且未在刷新，则可能需要手动刷新服务
            if (servicesList.isEmpty()) {
                Log.d("DeviceDetailsScreen", "服务列表为空，可能需要手动刷新")
            }
        } else if (!isConnected) {
            // 设备未连接，清空服务列表
            servicesList = emptyList()
        }
    }
    
    // 处理返回按钮逻辑的函数
    val handleBackNavigation = {
        // 如果设备是连接的，断开连接
        if (isConnected) {
            Log.d("DeviceDetailsScreen", "用户离开设备详情页面，断开连接")
            viewModel.disconnectDevice()
        }
        // 返回上一个页面
        navController.popBackStack()
    }
    
    // 处理系统返回按钮
    BackHandler {
        handleBackNavigation()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = device?.name ?: "设备详情") },
                navigationIcon = {
                    IconButton(onClick = { handleBackNavigation() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 添加导出历史菜单
                    var showExportMenu by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showExportMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多选项"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showExportMenu,
                        onDismissRequest = { showExportMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出消息历史") },
                            onClick = {
                                showExportMenu = false
                                // 导出当前设备的消息历史
                                device?.let { dev ->
                                    viewModel.exportDeviceMessageHistory(dev.id) { path ->
                                        // 导出完成
                                        if (path != null) {
                                            Toast.makeText(
                                                context, 
                                                "已导出消息历史到: $path",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            
                                            // 打开导出的文件
                                            val exportedFile = File(path)
                                            if (exportedFile.exists()) {
                                                try {
                                                    FileUtils.shareFile(
                                                        context, 
                                                        exportedFile, 
                                                        "分享消息历史文件",
                                                        "text/plain"
                                                    )
                                                } catch (e: Exception) {
                                                    // 如果无法分享，提示用户路径
                                                    Toast.makeText(
                                                        context,
                                                        "文件已保存到: ${exportedFile.absolutePath}",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            Toast.makeText(
                                                context, 
                                                "导出失败，该设备可能没有历史消息",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                    
                    IconButton(
                        onClick = {
                            device?.let { viewModel.toggleFavoriteDevice(it.id) }
                        }
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = if (isFavorite) "取消收藏" else "收藏设备",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        // 添加底部按钮栏，固定在屏幕底部
        bottomBar = {
            if (device != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        if (isConnected) {
                            // 简洁样式的底部按钮区域
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // 刷新服务按钮
                                    TextButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                isRefreshing = true
                                                viewModel.refreshDeviceServices(deviceId)
                                                
                                                // 等待服务发现完成
                                                delay(500)
                                                servicesList = viewModel.getDeviceServices(deviceId)
                                                
                                                // 如果还是没有服务，再多等几次
                                                var retryCount = 0
                                                while (servicesList.isEmpty() && retryCount < 5) {
                                                    delay(500)
                                                    servicesList = viewModel.getDeviceServices(deviceId)
                                                    retryCount++
                                                }
                                                
                                                isRefreshing = false
                                            }
                                        },
                                        enabled = !isRefreshing && isConnected,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            if (isRefreshing) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = if (isRefreshing) "正在刷新..." else "刷新服务列表",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                    
                                    HorizontalDivider()
                                    
                                    // 开始数据交互按钮
                                    TextButton(
                                        onClick = {
                                            // 确保设备已连接
                                            if (isConnected) {
                                                // 使用设备ID创建路由并导航
                                                Log.d("DeviceDetailsScreen", "导航到BleMessage屏幕，设备ID: ${device.id}")
                                                navController.navigate(Screen.BleMessage.createRoute(device.id)) {
                                                    // 不再使用popUpTo，保留导航栈中的设备详情页面
                                                    // 这样用户从通信界面返回时可以回到详情页面
                                                    launchSingleTop = true
                                                }
                                            } else {
                                                // 如果未连接，提示用户
                                                viewModel.showToast("请先连接设备")
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        enabled = isConnected
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                                contentDescription = null,
                                                tint = if (isConnected) 
                                                    MaterialTheme.colorScheme.primary 
                                                else 
                                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            Text(
                                                text = "开始数据交互",
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        
                        // 断开连接按钮 - 标准样式的红色按钮
                        Button(
                            onClick = {
                                if (isConnected) {
                                    viewModel.disconnectDevice()
                                    
                                    // 更新服务列表状态和刷新UI
                                    servicesList = emptyList()
                                    lastServicesUpdate = System.currentTimeMillis()
                                    isRefreshing = false
                                    
                                    // 记录日志
                                    Log.d("DeviceDetailsScreen", "用户手动断开设备连接，清除服务缓存")
                                } else if (!isConnecting) {
                                    // 连接设备前先确保之前的连接已完全断开
                                    val currentConnectedDevice = uiState.connectedDevice
                                    if (currentConnectedDevice != null && currentConnectedDevice.id != device.id) {
                                        Log.d("DeviceDetailsScreen", "先断开当前设备，再连接新设备")
                                        viewModel.disconnectDevice()
                                        // 短暂延迟以确保断开完成
                                        coroutineScope.launch {
                                            delay(300)
                                            viewModel.connectDevice(device)
                                            // 不再需要在这里进行服务发现，因为已经在连接状态变化时自动发现服务了
                                        }
                                    } else {
                                        Log.d("DeviceDetailsScreen", "连接设备：${device.name}")
                                        viewModel.connectDevice(device)
                                        // 不再需要在这里进行服务发现，因为已经在连接状态变化时自动发现服务了
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isConnecting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isConnected) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isConnected) 
                                        Icons.Default.BluetoothConnected 
                                    else 
                                        Icons.Default.Bluetooth,
                                    contentDescription = null
                                )
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Text(
                                    text = if (isConnected) 
                                        "断开连接" 
                                    else if (isConnecting)
                                        "连接中..."
                                    else 
                                        "连接设备",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (device == null) {
            // 设备不存在
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "设备不存在或已被移除",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "设备ID: $deviceId",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "设备列表大小: ${uiState.deviceList.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        viewModel.startScan(5000) // 再次尝试扫描
                    }
                ) {
                    Text(text = "重新扫描")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { navController.popBackStack() }
                ) {
                    Text(text = "返回")
                }
            }
            return@Scaffold
        }
        
        // 显示设备详情
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 创建一个可滚动的内容区域
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)  // 增加底部padding，防止内容被底部栏遮挡
                    .verticalScroll(rememberScrollState()) // 只在这一层使用滚动
            ) {
                // 头部设备信息（最基本的信息和状态）
                DeviceHeader(device, isConnected)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 设备信息卡片
                DeviceInfoCard(device)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 广播数据卡片
                AdvertisementDataCard(
                    device = device, 
                    clipboardManager = clipboardManager
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 如果设备已连接，显示服务区域
                if (isConnected) {
                    // 服务区域 - 默认折叠，使用白色背景
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White  // 使用纯白色背景
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            // 服务标题行，可点击展开/折叠
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showServices = !showServices }
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Services",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = "${servicesList.size}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                
                                // 展开/折叠图标
                                Icon(
                                    imageVector = if (showServices) 
                                        Icons.Default.KeyboardArrowUp 
                                    else 
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (showServices) "折叠" else "展开",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 如果展开状态，显示服务列表
                            if (showServices) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                if (servicesList.isEmpty()) {
                                    Text(
                                        text = "No services found. Try refreshing.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    // 在Column中直接渲染服务列表项
                                    servicesList.forEach { service ->
                                        ServiceItem(
                                            service = service,
                                            onClick = {
                                                // 更新展开/折叠状态
                                                expandedServiceUuid = if (expandedServiceUuid == service.uuid) {
                                                    null // 折叠当前项
                                                } else {
                                                    service.uuid // 展开该项
                                                }
                                            }
                                        )
                                        
                                        // 如果当前服务被展开，显示其特性列表
                                        if (expandedServiceUuid == service.uuid) {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
                                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                )
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp)
                                                ) {
                                                    if (service.characteristics.isEmpty()) {
                                                        Text(
                                                            text = "No characteristics found for this service.",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            modifier = Modifier.padding(8.dp)
                                                        )
                                                    } else {
                                                        Text(
                                                            text = "${service.characteristics.size} characteristics",
                                                            style = MaterialTheme.typography.titleSmall,
                                                            modifier = Modifier.padding(8.dp)
                                                        )
                                                        
                                                        HorizontalDivider()
                                                        
                                                        service.characteristics.forEach { characteristic ->
                                                            CharacteristicItem(
                                                                characteristic = characteristic,
                                                                clipboardManager = clipboardManager,
                                                                serviceUuid = service.uuid,
                                                                deviceId = device.id
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        // 如果不是最后一项，添加一个分隔符
                                        if (service != servicesList.last()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 添加额外的底部间距，确保底部固定按钮不会遮挡内容
                    Spacer(modifier = Modifier.height(180.dp))
                }
            }
        }
        
        // 连接中加载指示器
        if (isConnecting) {
            LoadingIndicator(message = "正在连接设备...")
        } else if (isRefreshing) {
            LoadingIndicator(message = "正在发现服务...")
        }
    }
}

@Composable
fun DeviceHeader(device: BluetoothDevice, isConnected: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 设备图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 设备名称和地址
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            // 连接状态
            Box(
                modifier = Modifier
                    .background(
                        color = if (isConnected) 
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isConnected) "已连接" else "未连接",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isConnected) 
                        MaterialTheme.colorScheme.tertiary
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ServiceItem(
    service: ServiceInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 检查是否是标准服务
    val isStandardService = try {
        GattProfileConstants.isStandardService(service.uuid)
    } catch (e: Exception) {
        false
    }
    
    val standardInfo = if (isStandardService) {
        try {
            GattProfileConstants.getStandardServiceInfo(service.uuid)
        } catch (e: Exception) {
            null
        }
    } else null
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 服务图标或标准图标
            if (isStandardService && standardInfo != null) {
                Icon(
                    imageVector = standardInfo.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // 服务名称和UUID
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isStandardService && standardInfo != null) 
                        standardInfo.name 
                    else 
                        service.name ?: "Unknown Service",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Text(
                    text = service.uuid,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                // 显示特征数量
                Text(
                    text = "${service.characteristics.size} characteristics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // 向右箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 标准服务内容展示组件，在服务展开时显示标准服务的信息
 */
@Composable
fun StandardServiceContent(
    service: ServiceInfo,
    standardInfo: GattProfileConstants.StandardServiceInfo,
    deviceId: String,
    viewModel: BluetoothViewModel = viewModel()
) {
    // 特定服务数据的state
    var batteryLevel by remember { mutableStateOf<Int?>(null) }
    var heartRate by remember { mutableStateOf<Int?>(null) }
    var temperature by remember { mutableStateOf<Float?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    // 自动读取特性数据
    LaunchedEffect(key1 = service.uuid) {
        if (standardInfo.autoRead) {
            isLoading = true
            
            // 根据服务类型读取不同的特征
            when (service.uuid.lowercase()) {
                GattProfileConstants.Services.BATTERY_SERVICE.lowercase() -> {
                    // 在此处调用读取电池电量的方法
                    // 这里仅模拟，实际项目中应从viewModel调用读取方法
                    batteryLevel = 85 // 示例：模拟电池电量为85%
                }
                
                GattProfileConstants.Services.HEART_RATE_SERVICE.lowercase() -> {
                    // 在此处调用读取心率的方法
                    // 这里仅模拟，实际项目中应从viewModel调用读取方法
                    heartRate = 72 // 示例：模拟心率为72bpm
                }
                
                GattProfileConstants.Services.HEALTH_THERMOMETER.lowercase() -> {
                    // 在此处调用读取温度的方法
                    // 这里仅模拟，实际项目中应从viewModel调用读取方法
                    temperature = 36.5f // 示例：模拟温度为36.5℃
                }
            }
            
            isLoading = false
        }
    }
    
    // 服务信息和数据展示
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 服务描述
        Text(
            text = standardInfo.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 显示标准服务的数据
        when (service.uuid.lowercase()) {
            // 电池服务
            GattProfileConstants.Services.BATTERY_SERVICE.lowercase() -> {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (batteryLevel != null) {
                    // 电池电量进度条
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "电池电量",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = GattProfileConstants.getBatteryIcon(batteryLevel!!),
                                    contentDescription = null,
                                    tint = if (batteryLevel!! > 20) 
                                        MaterialTheme.colorScheme.tertiary 
                                    else 
                                        MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Text(
                                    text = "$batteryLevel%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (batteryLevel!! > 20) 
                                        MaterialTheme.colorScheme.tertiary 
                                    else 
                                        MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LinearProgressIndicator(
                            progress = { batteryLevel!!.toFloat() / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = if (batteryLevel!! > 20) 
                                MaterialTheme.colorScheme.tertiary 
                            else 
                                MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Text(
                        text = "无电池数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // 心率服务
            GattProfileConstants.Services.HEART_RATE_SERVICE.lowercase() -> {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (heartRate != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "心率",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonitorHeart,
                                contentDescription = null,
                                tint = if (heartRate!! < 60 || heartRate!! > 100) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = "$heartRate bpm",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (heartRate!! < 60 || heartRate!! > 100) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = GattProfileConstants.getHeartRateDescription(heartRate!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "无心率数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // 健康温度计服务
            GattProfileConstants.Services.HEALTH_THERMOMETER.lowercase() -> {
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                } else if (temperature != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "体温",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Thermostat,
                                contentDescription = null,
                                tint = if (temperature!! > 37.5f) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(4.dp))
                            
                            Text(
                                text = String.format("%.1f °C", temperature),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (temperature!! > 37.5f) 
                                    MaterialTheme.colorScheme.error 
                                else 
                                    MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = if (temperature!! > 37.5f) "体温偏高" else "体温正常",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Text(
                        text = "无体温数据",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            
            // 其他标准服务可以添加在此处
            else -> {
                Text(
                    text = "支持自动数据展示",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun CharacteristicItem(
    characteristic: CharacteristicInfo,
    clipboardManager: ClipboardManager,
    serviceUuid: String = "",
    deviceId: String = "",
    viewModel: BluetoothViewModel = viewModel()
) {
    var expanded by remember { mutableStateOf(false) }
    var characteristicValue by remember { mutableStateOf<String?>(null) }
    var isReading by remember { mutableStateOf(false) }
    
    // 检查是否是关键特征（如电池电量、心率测量等）
    val isKeyCharacteristic = isKeyCharacteristic(serviceUuid, characteristic.uuid)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // 特征标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 特征名称和UUID
            Column(modifier = Modifier.weight(1f)) {
                if (characteristic.name.isNotEmpty()) {
                    Text(
                        text = characteristic.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Text(
                    text = "UUID: ${characteristic.uuid}",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "Properties: ${characteristic.properties}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 操作图标
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 复制按钮
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(characteristic.uuid))
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "复制UUID",
                        modifier = Modifier.size(14.dp)
                    )
                }
                
                // 下载图标 - 显示READ功能
                if (characteristic.properties.contains("READ", ignoreCase = true)) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Read",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(2.dp))
                }
                
                // 上传图标 - 显示WRITE功能
                if (characteristic.properties.contains("WRITE", ignoreCase = true)) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = "Write",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    
                    Spacer(modifier = Modifier.width(2.dp))
                }
                
                // 通知图标 - 显示NOTIFY或INDICATE功能
                if (characteristic.properties.contains("NOTIFY", ignoreCase = true) || 
                    characteristic.properties.contains("INDICATE", ignoreCase = true)) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notify",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        
        // 如果特征展开，显示更多内容
        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // 根据特征属性显示不同的操作按钮
                val hasRead = characteristic.properties.contains("READ", ignoreCase = true)
                val hasWrite = characteristic.properties.contains("WRITE", ignoreCase = true)
                val hasNotify = characteristic.properties.contains("NOTIFY", ignoreCase = true) || 
                                characteristic.properties.contains("INDICATE", ignoreCase = true)
                
                if (hasRead) {
                    Button(
                        onClick = { 
                            // 读取特征值
                            isReading = true
                            // 实际应用中，这里应该调用viewModel的方法来读取特征值
                            // 模拟读取数据
                            characteristicValue = "读取的数据值"
                            isReading = false
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        ),
                        enabled = !isReading
                    ) {
                        if (isReading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "读取", 
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                if (hasWrite) {
                    Button(
                        onClick = { /* 写入操作 */ },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = "写入", 
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                }
                
                if (hasNotify) {
                    Button(
                        onClick = { /* 通知操作 */ },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        )
                    ) {
                        Text(
                            text = "通知", 
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // 如果有读取的特征值，显示它
            if (characteristicValue != null) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "当前值:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = characteristicValue!!,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    // 对关键特征显示解析后的值
                    if (isKeyCharacteristic) {
                        Spacer(modifier = Modifier.height(4.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 根据服务和特征类型解析
                        when {
                            // 电池服务 - 电池电量
                            serviceUuid.lowercase() == GattProfileConstants.Services.BATTERY_SERVICE.lowercase() &&
                                    characteristic.uuid.lowercase() == GattProfileConstants.Characteristics.BATTERY_LEVEL.lowercase() -> {
                                Text(
                                    text = "电池电量: 85%", // 假设解析出85%
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            
                            // 心率服务 - 心率测量
                            serviceUuid.lowercase() == GattProfileConstants.Services.HEART_RATE_SERVICE.lowercase() &&
                                    characteristic.uuid.lowercase() == GattProfileConstants.Characteristics.HEART_RATE_MEASUREMENT.lowercase() -> {
                                Text(
                                    text = "心率: 72 bpm", // 假设解析出72 bpm
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            
                            // 健康温度计服务 - 温度测量
                            serviceUuid.lowercase() == GattProfileConstants.Services.HEALTH_THERMOMETER.lowercase() &&
                                    characteristic.uuid.lowercase() == GattProfileConstants.Characteristics.TEMPERATURE_MEASUREMENT.lowercase() -> {
                                Text(
                                    text = "体温: 36.5 °C", // 假设解析出36.5°C
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 检查是否是关键特征（如电池电量、心率测量等）
 */
private fun isKeyCharacteristic(serviceUuid: String, characteristicUuid: String): Boolean {
    return when {
        // 电池服务 - 电池电量
        serviceUuid.lowercase() == GattProfileConstants.Services.BATTERY_SERVICE.lowercase() &&
                characteristicUuid.lowercase() == GattProfileConstants.Characteristics.BATTERY_LEVEL.lowercase() -> true
        
        // 心率服务 - 心率测量
        serviceUuid.lowercase() == GattProfileConstants.Services.HEART_RATE_SERVICE.lowercase() &&
                characteristicUuid.lowercase() == GattProfileConstants.Characteristics.HEART_RATE_MEASUREMENT.lowercase() -> true
        
        // 健康温度计服务 - 温度测量
        serviceUuid.lowercase() == GattProfileConstants.Services.HEALTH_THERMOMETER.lowercase() &&
                characteristicUuid.lowercase() == GattProfileConstants.Characteristics.TEMPERATURE_MEASUREMENT.lowercase() -> true
        
        // 添加更多关键特征判断条件
        
        else -> false
    }
}

@Composable
fun DeviceInfoCard(device: BluetoothDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 卡片标题
            Text(
                text = "设备信息",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 设备信息列表
            InfoRow(label = "MAC地址", value = device.address)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 信号强度
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "信号强度",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = "${device.rssi} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // 信号强度图标
                    Icon(
                        imageVector = getSignalIcon(device.rssi),
                        contentDescription = null,
                        tint = getSignalColor(device.rssi),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 信号强度条
                val signalStrength = (device.rssi + 100) / 100f  // 将-100到0的范围映射到0-1
                val clampedStrength = signalStrength.coerceIn(0f, 1f)
                
                LinearProgressIndicator(
                    progress = { clampedStrength },
                    modifier = Modifier.fillMaxWidth(),
                    color = getSignalColor(device.rssi)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 连接状态
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val lastConnected = device.lastConnected?.let { dateFormatter.format(it) } ?: "从未连接"
            
            InfoRow(
                label = "连接状态", 
                value = if (device.isConnected) "已连接" else "未连接",
                valueColor = if (device.isConnected) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoRow(label = "上次连接", value = lastConnected)
        }
    }
}

@Composable
fun AdvertisementDataCard(
    device: BluetoothDevice,
    clipboardManager: ClipboardManager
) {
    var isRawDataExpanded by remember { mutableStateOf(false) }
    
    // 使用解析器解析原始广播数据
    val parsedAdvData = remember(device.rawData) {
        if (device.rawData.isNotEmpty()) {
            com.example.jerrycan.bluetooth.AdvertisementDataParser.parseAdvertisementData(device.rawData)
        } else {
            emptyList()
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 标题
            Text(
                text = "广播数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 解析后的广播数据区域 - 始终展示
            if (parsedAdvData.isNotEmpty()) {
                Text(
                    text = "解析数据",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 解析后的广播数据列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    parsedAdvData.forEachIndexed { index, item ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // 类型信息
                            Text(
                                text = item.first,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            // 数据内容行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 根据数据类型和长度决定显示方式
                                if (item.first.contains("UUID") || item.second.length > 60) {
                                    // 长数据使用垂直布局分行显示
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = item.second,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 0.5.sp
                                            ),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    // 短数据单行显示
                                    Text(
                                        text = item.second,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 2.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(item.second))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "复制",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            if (index < parsedAdvData.size - 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            } else if (device.advertisementData.isNotEmpty()) {
                // 如果没有解析到新数据，但有旧的广告数据，仍然显示
                Text(
                    text = "广播数据",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 广播数据列表
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(vertical = 8.dp, horizontal = 12.dp)
                ) {
                    val items = device.advertisementData.entries.toList()
                    items.forEachIndexed { index, entry ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            // 类型信息
                            Text(
                                text = entry.key,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.height(2.dp))
                            
                            // 数据内容行
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 根据数据类型和长度决定显示方式
                                if (entry.key.contains("UUID") || entry.value.length > 60) {
                                    // 长数据使用垂直布局分行显示
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = entry.value,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 0.5.sp
                                            ),
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                                } else {
                                    // 短数据单行显示
                                    Text(
                                        text = entry.value,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = FontFamily.Monospace,
                                            letterSpacing = 0.5.sp
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(vertical = 2.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(entry.value))
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "复制",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            if (index < items.size - 1) {
                                Spacer(modifier = Modifier.height(6.dp))
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            } else {
                // 无广播数据
                Text(
                    text = "未检测到广播数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 原始广播数据 - 可折叠区域
            if (device.rawData.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isRawDataExpanded = !isRawDataExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "原始数据",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isRawDataExpanded) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(device.rawData))
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        IconButton(
                            onClick = { isRawDataExpanded = !isRawDataExpanded }
                        ) {
                            Icon(
                                imageVector = if (isRawDataExpanded) 
                                    Icons.Default.ExpandLess 
                                else 
                                    Icons.Default.ExpandMore,
                                contentDescription = if (isRawDataExpanded) "折叠" else "展开"
                            )
                        }
                    }
                }
                
                // 展开时显示原始数据内容
                if (isRawDataExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(device.rawData))
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "复制",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        // 分段显示原始数据，每32个字符一行
                        val rawData = device.rawData.replace("0x", "")
                        val formattedData = if (rawData.length > 32) {
                            rawData.chunked(32).joinToString("\n")
                        } else {
                            rawData
                        }
                        
                        Text(
                            text = formattedData,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

// 获取信号强度颜色
@Composable
fun getSignalColor(rssi: Int): androidx.compose.ui.graphics.Color {
    return when {
        rssi > -60 -> MaterialTheme.colorScheme.tertiary  // 绿色，信号很好
        rssi > -70 -> MaterialTheme.colorScheme.primary   // 蓝色，信号好
        rssi > -80 -> MaterialTheme.colorScheme.secondary // 橙色，信号一般
        else -> MaterialTheme.colorScheme.error           // 红色，信号差
    }
} 