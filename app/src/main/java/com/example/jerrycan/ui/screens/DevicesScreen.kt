package com.example.jerrycan.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jerrycan.R
import com.example.jerrycan.model.BluetoothDevice
import com.example.jerrycan.navigation.Screen
import com.example.jerrycan.ui.components.DeviceDataDialog
import com.example.jerrycan.ui.components.EmptyStateView
import com.example.jerrycan.ui.components.ErrorDialog
import com.example.jerrycan.ui.components.LoadingIndicator
import com.example.jerrycan.ui.components.StatusIndicator
import com.example.jerrycan.viewmodel.BluetoothViewModel
import com.example.jerrycan.viewmodel.DeviceType
import com.example.jerrycan.viewmodel.ScanFilterSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateListOf
import com.example.jerrycan.ui.screens.getFilterSummary
import com.example.jerrycan.ui.screens.ScanFilterDialog
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.geometry.Offset
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    // 设置NavController到ViewModel
    LaunchedEffect(key1 = navController) {
        viewModel.setNavController(navController)
    }
    
    // 在Composable函数顶层获取Context
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    val friendDevices by viewModel.friendDevices.collectAsState() 
    val onlineDeviceIds by viewModel.onlineDeviceIds.collectAsState()
    
    var showErrorDialog by remember { mutableStateOf(false) }
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var showDeviceDataDialog by remember { mutableStateOf(false) }
    
    // 长按功能变量
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var longPressedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    
    // 检查蓝牙权限和启用状态
    LaunchedEffect(key1 = Unit) {
        // 检查蓝牙是否启用
        if (!viewModel.isBluetoothEnabled()) {
            showErrorDialog = true
        } else {
            // 检查权限并自动刷新设备状态
            if (viewModel.checkBluetoothPermissions()) {
                // 首先加载好友设备（强制刷新）
                viewModel.loadFriendDevices(true)
                
                // 然后刷新设备状态
                viewModel.refreshFriendDevicesStatus()
            } else {
                showErrorDialog = true
            }
        }
    }
    
    // 新的布局方式，使用联系人列表风格
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题和刷新按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "设备",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                
                IconButton(
                    onClick = { 
                        // 刷新好友设备状态
                        viewModel.refreshFriendDevicesStatus() 
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新设备状态",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 扫描进度条
            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.devices_searching),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            
            // 设备列表区域
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // "新的朋友"条目
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                navController.navigate(Screen.DeviceSearch.route)
                            }
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 图标容器
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加设备",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text(
                            text = "新的设备",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    HorizontalDivider()
                }
                
                // 如果没有好友设备
                if (friendDevices.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "暂无设备，请点击\"新的设备\"添加",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    // 好友设备组
                    // 创建按首字母分组的设备
                    val sortedDevices = friendDevices.sortedBy { it.name }
                    val groupedDevices = sortedDevices.groupBy { 
                        val firstChar = it.name.firstOrNull() ?: '-'
                        if (firstChar.isLetter()) {
                            firstChar.uppercaseChar()
                        } else {
                            '#' // 非字母字符归为#组
                        }
                    }
                    
                    // 生成字母列表
                    val alphabet = groupedDevices.keys.sorted()
                    
                    // 对每个字母组进行遍历
                    alphabet.forEach { letter ->
                        item {
                            // 分组标题
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        
                        // 该字母下的所有设备
                        val devicesInGroup = groupedDevices[letter] ?: emptyList()
                        items(devicesInGroup) { device ->
                            FriendDeviceItem(
                                device = device,
                                isOnline = onlineDeviceIds.contains(device.id),
                                onClick = {
                                    Log.d("DevicesScreen", "设备点击回调开始: ${device.name}, ID: ${device.id}")
                                    try {
                                        // 直接导航到设备详情页面
                                        val route = Screen.DeviceDetails.createRoute(device.id)
                                        Log.d("DevicesScreen", "即将导航到路由: $route")
                                        navController.navigate(route)
                                        Log.d("DevicesScreen", "导航完成")
                                    } catch (e: Exception) {
                                        Log.e("DevicesScreen", "导航发生异常: ${e.message}", e)
                                        Toast.makeText(context, "导航错误: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onLongClick = { offset ->
                                    longPressedDevice = device
                                    contextMenuPosition = offset
                                    showContextMenu = true
                                }
                            )
                            
                            // 分隔线，为最后一个设备不添加
                            if (device != devicesInGroup.last()) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 72.dp),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 长按菜单
        if (showContextMenu && longPressedDevice != null) {
            // 点击背景关闭菜单 - 必须放在菜单前面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        showContextMenu = false
                    }
            )
            
            // 菜单内容 - 必须放在背景后面
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.8f),
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        // 设备信息
                        Text(
                            text = longPressedDevice?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        HorizontalDivider()
                        
                        // 删除选项
                        TextButton(
                            onClick = {
                                longPressedDevice?.id?.let { deviceId ->
                                    Log.d("DevicesScreen", "点击删除按钮，设备ID: $deviceId")
                                    Toast.makeText(context, "正在删除设备...", Toast.LENGTH_SHORT).show()
                                    try {
                                        // 区分好友列表和扫描列表
                                        if (longPressedDevice in friendDevices) {
                                            Log.d("DevicesScreen", "从好友列表删除设备")
                                            viewModel.removeFriendDevice(deviceId)
                                            Toast.makeText(context, "已从好友列表删除设备", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Log.d("DevicesScreen", "从扫描列表删除设备")
                                            viewModel.removeDevice(deviceId)
                                            Toast.makeText(context, "已从扫描列表删除设备", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("DevicesScreen", "删除设备失败: ${e.message}", e)
                                        Toast.makeText(context, "删除设备失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                showContextMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                        
                        // 取消选项
                        TextButton(
                            onClick = { showContextMenu = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("取消")
                        }
                    }
                }
            }
        }
        
        // 错误提示对话框
        if (showErrorDialog) {
            ErrorDialog(
                errorMessage = if (!viewModel.isBluetoothEnabled()) {
                    stringResource(R.string.error_bluetooth_not_enabled)
                } else {
                    // 根据Android版本显示正确的权限错误消息
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "需要蓝牙扫描权限来发现设备。请在设置中开启应用的蓝牙权限。"
                    } else {
                        stringResource(R.string.error_location_permission)
                    }
                },
                onDismiss = { 
                    showErrorDialog = false
                    // 在错误对话框关闭后，再次尝试刷新数据(如果蓝牙已启用)
                    if (viewModel.isBluetoothEnabled() && viewModel.checkBluetoothPermissions()) {
                        viewModel.refreshFriendDevicesStatus()
                    }
                }
            )
        }
        
        // 设备详情对话框
        if (showDeviceDataDialog && selectedDevice != null) {
            DeviceDataDialog(
                device = selectedDevice!!,
                onDismiss = { 
                    showDeviceDataDialog = false
                    selectedDevice = null
                }
            )
        }
    }

    // 在DevicesScreen函数中修改LaunchedEffect来使用collect
    LaunchedEffect(key1 = Unit) {
        Log.d("DevicesScreen", "设置LaunchedEffect监听tempDeviceId变化")
        viewModel.tempDeviceId.collect { deviceId ->
            Log.d("DevicesScreen", "收到tempDeviceId变化: $deviceId")
            deviceId?.let { id ->
                // 执行导航
                Log.d("DevicesScreen", "正在导航到设备详情页面: $id")
                navController.navigate(Screen.DeviceDetails.createRoute(id))
                // 重置tempDeviceId
                viewModel.setTempDeviceId(null)
            }
        }
    }
}

@Composable
fun FriendDeviceItem(
    device: BluetoothDevice,
    isOnline: Boolean,
    onClick: () -> Unit,
    onLongClick: (androidx.compose.ui.geometry.Offset) -> Unit
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        Log.d("FriendDeviceItem", "点击设备开始处理: ${device.name}, ID: ${device.id}")
                        try {
                            Log.d("FriendDeviceItem", "准备调用onClick函数")
                            onClick() 
                            Log.d("FriendDeviceItem", "onClick函数调用完成")
                        } catch (e: Exception) {
                            Log.e("FriendDeviceItem", "点击处理发生异常: ${e.message}", e)
                            Toast.makeText(context, "设备点击处理错误: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLongPress = { offset ->
                        Log.d("FriendDeviceItem", "长按设备: ${device.name}, ID: ${device.id}")
                        onLongClick(offset)
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 使用可复用组件
            com.example.jerrycan.ui.components.FriendDeviceListItem(
                device = device,
                isOnline = isOnline,
                showSignalStrength = true
            )
        }
    }
}

@Composable
fun getSignalStrengthDescription(rssi: Int): String {
    return com.example.jerrycan.ui.components.getRssiDescription(rssi)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceSearchScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    // 设置NavController到ViewModel
    LaunchedEffect(key1 = navController) {
        viewModel.setNavController(navController)
    }
    
    // 在Composable函数顶层获取Context
    val context = LocalContext.current
    
    val uiState by viewModel.uiState.collectAsState()
    val filteredDevices by viewModel.filteredDeviceList.collectAsState()
    val scanFilterSettings by viewModel.scanFilterSettings.collectAsState()
    val friendDevices by viewModel.friendDevices.collectAsState()
    
    var searchDuration by remember { mutableStateOf(10000L) } // 10秒
    var selectedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var showScanFilterDialog by remember { mutableStateOf(false) }
    
    // 长按功能变量
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var longPressedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    
    // 错误对话框状态
    var showErrorDialog by remember { mutableStateOf(false) }
    
    // 获取过滤器摘要
    val filterSummary = com.example.jerrycan.ui.screens.getFilterSummary(scanFilterSettings)
    
    // 启动搜索前先检查权限
    LaunchedEffect(key1 = true) {
        // 检查蓝牙是否启用
        if (!viewModel.isBluetoothEnabled()) {
            showErrorDialog = true
        } else {
            // 检查权限并启动扫描
            if (viewModel.checkBluetoothPermissions()) {
                viewModel.startScan(searchDuration)
            } else {
                showErrorDialog = true
            }
        }
    }
    
    // 新的布局，去掉Scaffold和顶部应用栏
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 标题栏和返回按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                Text(
                    text = "添加新设备",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 过滤器状态显示
            if (filterSummary != stringResource(R.string.settings_scan_filter_no_filter)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = filterSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    TextButton(
                        onClick = { showScanFilterDialog = true },
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = "编辑",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showScanFilterDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Text(text = stringResource(R.string.settings_scan_filter))
                    }
                }
            }
            
            // 扫描进度条
            if (uiState.isScanning) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.devices_searching),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            
            // 搜索结果标题
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "发现设备",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "(${filteredDevices.size})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (filterSummary != stringResource(R.string.settings_scan_filter_no_filter)) {
                    Text(
                        text = "已过滤",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            // 设备列表区域
            if (filteredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isScanning) {
                        // 正在扫描中的提示
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = "正在搜索附近的蓝牙设备...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else {
                        // 未找到设备的提示，添加过滤器提示
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.devices_no_devices),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            
                            if (filterSummary != stringResource(R.string.settings_scan_filter_no_filter)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = "尝试修改过滤条件以查看更多设备",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                TextButton(onClick = { showScanFilterDialog = true }) {
                                    Text("修改过滤器")
                                }
                            }
                        }
                    }
                }
            } else {
                // 显示设备列表
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDevices) { device ->
                        DeviceSearchItem(
                            device = device,
                            isFriend = friendDevices.any { it.id == device.id },
                            onClick = {
                                Log.d("DeviceSearchScreen", "设备点击回调开始: ${device.name}, ID: ${device.id}")
                                try {
                                    // 点击导航到设备详情页面
                                    val route = Screen.DeviceDetails.createRoute(device.id)
                                    Log.d("DeviceSearchScreen", "即将导航到路由: $route")
                                    navController.navigate(route)
                                    Log.d("DeviceSearchScreen", "导航完成")
                                } catch (e: Exception) {
                                    Log.e("DeviceSearchScreen", "导航发生异常: ${e.message}", e)
                                    Toast.makeText(context, "导航错误: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onAddFriend = {
                                // 添加为好友
                                viewModel.addFriendDevice(device)
                                
                                // 刷新设备状态
                                viewModel.refreshFriendDevicesStatus()
                                
                                // 显示添加成功提示
                                Toast.makeText(
                                    context, 
                                    "已成功添加设备\"${device.name}\"，正在返回设备列表...", 
                                    Toast.LENGTH_SHORT
                                ).show()
                                
                                // 添加后返回设备列表页面
                                navController.popBackStack()
                            },
                            onLongClick = { offset ->
                                longPressedDevice = device
                                contextMenuPosition = offset
                                showContextMenu = true
                            }
                        )
                    }
                }
            }
            
            // 底部按钮区域
            if (!uiState.isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { viewModel.startScan(searchDuration) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = stringResource(R.string.devices_refresh)
                        )
                    }
                }
            }
        }
        
        // 长按菜单
        if (showContextMenu && longPressedDevice != null) {
            // 点击背景关闭菜单 - 必须放在菜单前面
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        showContextMenu = false
                    }
            )
            
            // 菜单内容 - 必须放在背景后面
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.8f),
                    shape = MaterialTheme.shapes.medium,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        // 设备信息
                        Text(
                            text = longPressedDevice?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        
                        HorizontalDivider()
                        
                        // 删除选项
                        TextButton(
                            onClick = {
                                longPressedDevice?.id?.let { deviceId ->
                                    Log.d("DeviceSearchScreen", "点击删除按钮，设备ID: $deviceId")
                                    Toast.makeText(context, "正在删除设备...", Toast.LENGTH_SHORT).show()
                                    try {
                                        // 区分好友列表和扫描列表
                                        if (longPressedDevice in friendDevices) {
                                            Log.d("DeviceSearchScreen", "从好友列表删除设备")
                                            viewModel.removeFriendDevice(deviceId)
                                            Toast.makeText(context, "已从好友列表删除设备", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Log.d("DeviceSearchScreen", "从扫描列表删除设备")
                                            viewModel.removeDevice(deviceId)
                                            Toast.makeText(context, "已从扫描列表删除设备", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("DeviceSearchScreen", "删除设备失败: ${e.message}", e)
                                        Toast.makeText(context, "删除设备失败: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                                showContextMenu = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                        
                        // 取消选项
                        TextButton(
                            onClick = { showContextMenu = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("取消")
                        }
                    }
                }
            }
        }
        
        // 设备详情对话框
        selectedDevice?.let { device ->
            DeviceDataDialog(
                device = device,
                onDismiss = { selectedDevice = null }
            )
        }
        
        // 过滤器对话框
        if (showScanFilterDialog) {
            ScanFilterDialog(
                currentSettings = viewModel.getScanFilterSettings(),
                onSettingsChanged = { 
                    viewModel.updateScanFilterSettings(it)
                    // 确保在更新设置后关闭对话框
                    showScanFilterDialog = false
                },
                onDismiss = { showScanFilterDialog = false }
            )
        }
        
        // 错误提示对话框
        if (showErrorDialog) {
            ErrorDialog(
                errorMessage = if (!viewModel.isBluetoothEnabled()) {
                    stringResource(R.string.error_bluetooth_not_enabled)
                } else {
                    // 根据Android版本显示正确的权限错误消息
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        "需要蓝牙扫描权限来发现设备。请在设置中开启应用的蓝牙权限。"
                    } else {
                        stringResource(R.string.error_location_permission)
                    }
                },
                onDismiss = { 
                    showErrorDialog = false
                    // 在错误对话框关闭后，再次尝试扫描(如果蓝牙已启用)
                    if (viewModel.isBluetoothEnabled() && viewModel.checkBluetoothPermissions()) {
                        viewModel.startScan(searchDuration)
                    }
                }
            )
        }
    }

    // 在DeviceSearchScreen函数中也修改LaunchedEffect以使用collect
    LaunchedEffect(key1 = Unit) {
        Log.d("DeviceSearchScreen", "设置LaunchedEffect监听tempDeviceId变化")
        viewModel.tempDeviceId.collect { deviceId ->
            Log.d("DeviceSearchScreen", "收到tempDeviceId变化: $deviceId")
            deviceId?.let { id ->
                // 执行导航
                Log.d("DeviceSearchScreen", "正在导航到设备详情页面: $id")
                navController.navigate(Screen.DeviceDetails.createRoute(id))
                // 重置tempDeviceId
                viewModel.setTempDeviceId(null)
            }
        }
    }
}

@Composable
fun DeviceSearchItem(
    device: BluetoothDevice,
    isFriend: Boolean,
    onClick: () -> Unit,
    onAddFriend: () -> Unit,
    onLongClick: (androidx.compose.ui.geometry.Offset) -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { 
                        Log.d("DeviceSearchItem", "点击设备开始处理: ${device.name}, ID: ${device.id}")
                        try {
                            Log.d("DeviceSearchItem", "准备调用onClick函数")
                            onClick() 
                            Log.d("DeviceSearchItem", "onClick函数调用完成")
                        } catch (e: Exception) {
                            Log.e("DeviceSearchItem", "点击处理发生异常: ${e.message}", e)
                            Toast.makeText(context, "设备点击处理错误: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLongPress = { offset ->
                        Log.d("DeviceSearchItem", "长按设备: ${device.name}, ID: ${device.id}")
                        onLongClick(offset)
                    }
                )
            }
    ) {
        Column {
            // 设备项
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 使用可复用组件
                com.example.jerrycan.ui.components.DeviceListItem(
                    device = device,
                    icon = getSignalIcon(device.rssi),
                    iconTint = MaterialTheme.colorScheme.primary,
                    iconBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ) {
                    // 右侧区域内容
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 显示信号强度
                        if (device.rssi != 0) {
                            Text(
                                text = "${device.rssi} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = com.example.jerrycan.ui.components.getRssiColor(device.rssi)
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        
                        // 添加好友按钮
                        if (!isFriend) {
                            TextButton(
                                onClick = { 
                                    onAddFriend()
                                    Toast.makeText(context, "已添加设备: ${device.name}", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "添加",
                                    modifier = Modifier.size(16.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(4.dp))
                                
                                Text(
                                    text = "添加",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        } else {
                            // 已添加标记
                            Text(
                                text = "已添加",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
            
            // 分隔线
            HorizontalDivider(
                modifier = Modifier.padding(start = 68.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

/**
 * 根据信号强度获取对应的图标 - 使用共享实现
 */
@Composable
fun getSignalIcon(rssi: Int): ImageVector {
    return com.example.jerrycan.ui.components.getSignalIconForRssi(rssi)
}

// 获取过滤器摘要文本和对话框已经在SettingsScreen.kt中定义
// 这里不需要重复定义，使用DeviceSearchScreen中已导入的函数 