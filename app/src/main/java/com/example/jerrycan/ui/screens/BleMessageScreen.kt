package com.example.jerrycan.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jerrycan.R
import com.example.jerrycan.model.BluetoothDevice
import com.example.jerrycan.model.BluetoothMessage
import com.example.jerrycan.model.BleMessageType
import com.example.jerrycan.model.MessageStatus
import com.example.jerrycan.ui.components.ConfirmationDialog
import com.example.jerrycan.ui.components.LoadingIndicator
import com.example.jerrycan.ui.theme.NordicBlue
import com.example.jerrycan.ui.theme.NordicLightBlue
import com.example.jerrycan.viewmodel.BluetoothViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.horizontalScroll
import com.example.jerrycan.ui.components.HexDataView
import com.example.jerrycan.model.HexDisplayMode
import com.example.jerrycan.model.HexDisplayModes
import com.example.jerrycan.ui.components.FilterChip
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.foundation.BorderStroke

/**
 * 标准化设备ID (移除所有非字母数字字符并转大写)
 */
private fun normalizeId(id: String): String {
    // 标准化ID格式: 移除冒号,转大写
    return id.replace(":", "").uppercase()
}

/**
 * 截断UUID字符串并使其更易读
 */
private fun formatUuid(uuid: String?): String {
    // 改为返回完整的UUID
    return uuid ?: "未知"
}

/**
 * UUID显示名称
 */
private fun getUuidDisplayName(uuid: String?): String {
    // 这里可以添加已知UUID名称的映射
    val knownUuids = mapOf(
        // 标准蓝牙服务
        "00001800-0000-1000-8000-00805f9b34fb" to "通用访问服务",
        "00001801-0000-1000-8000-00805f9b34fb" to "通用属性服务",
        "0000180a-0000-1000-8000-00805f9b34fb" to "设备信息服务",
        "0000180f-0000-1000-8000-00805f9b34fb" to "电池服务",
        "0000180d-0000-1000-8000-00805f9b34fb" to "心率服务",
        
        // 标准特征
        "00002a29-0000-1000-8000-00805f9b34fb" to "厂商名称",
        "00002a24-0000-1000-8000-00805f9b34fb" to "型号",
        "00002a19-0000-1000-8000-00805f9b34fb" to "电池电量",
        "00002a37-0000-1000-8000-00805f9b34fb" to "心率测量",
        
        // Nordic UART服务和特征
        "6e400001-b5a3-f393-e0a9-e50e24dcca9e" to "Nordic UART服务",
        "6e400002-b5a3-f393-e0a9-e50e24dcca9e" to "Nordic UART TX特征(写入)",
        "6e400003-b5a3-f393-e0a9-e50e24dcca9e" to "Nordic UART RX特征(通知)"
    )
    
    return if (uuid != null && knownUuids.containsKey(uuid.lowercase())) {
        "${knownUuids[uuid.lowercase()]} (${formatUuid(uuid)})"
    } else {
        formatUuid(uuid)
    }
}

/**
 * 特征信息数据类
 */
data class CharacteristicInfo(
    val serviceUuid: String,
    val serviceName: String,
    val characteristicUuid: String,
    val characteristicName: String,
    val properties: String
)

// 定义屏幕状态
private enum class ScreenState {
    ACTIVE,    // 屏幕活跃中
    NAVIGATING // 屏幕正在离开/导航中
}

// 全局菜单状态管理
object LongPressMenuState {
    // 使用可观察的状态
    val _currentMenuMessageId = mutableStateOf<String?>(null)
    
    // 当前显示菜单的消息ID
    var currentMenuMessageId: String?
        get() = _currentMenuMessageId.value
        set(value) {
            _currentMenuMessageId.value = value
        }
    
    // 重置菜单状态
    fun reset() {
        _currentMenuMessageId.value = null
    }
}

sealed class MessageListState {
    object Loading : MessageListState()
    object Empty : MessageListState()
    data class Success(val devices: List<BluetoothDevice>) : MessageListState()
    data class Error(val message: String) : MessageListState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleMessageScreen(
    navController: NavController,
    deviceId: String,
    viewModel: BluetoothViewModel = viewModel()
) {
    // 创建一个Composable范围内的协程作用域
    val coroutineScope = rememberCoroutineScope()
    
    // 获取UI状态
    val uiState by viewModel.uiState.collectAsState()
    val isHexMode by viewModel.isHexMode.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // 添加一个状态变量来触发服务刷新
    var refreshTrigger by remember { mutableStateOf(0) }
    
    // 屏幕状态管理
    var screenState by remember { mutableStateOf(ScreenState.ACTIVE) }
    
    // 标准化设备ID
    val normalizedDeviceId = remember(deviceId) { normalizeId(deviceId) }
    
    // 使用DisposableEffect处理屏幕退出时的资源清理
    DisposableEffect(deviceId) {
        // 屏幕创建时的逻辑
        Log.d("BleMessageScreen", "屏幕已创建 - 设备ID: $deviceId")
        
        // 确保清除任何可能存在的菜单状态
        LongPressMenuState.reset()
        
        // 返回一个清理函数，当组件被销毁时调用
        onDispose {
            // 只有在状态不是NAVIGATING时才断开连接
            // 这避免了在手动导航时断开连接的重复执行
            if (screenState == ScreenState.ACTIVE) {
                Log.d("BleMessageScreen", "屏幕即将销毁（非手动导航） - 执行断开连接")
                viewModel.disconnectDevice()
            } else {
                Log.d("BleMessageScreen", "屏幕即将销毁（手动导航中） - 跳过断开连接")
            }
            
            // 清除菜单状态
            LongPressMenuState.reset()
        }
    }
    
    // 处理返回按钮逻辑的函数
    val handleBackNavigation = {
        // 将状态更改为NAVIGATING，表示正在手动导航
        screenState = ScreenState.NAVIGATING
        
        // 断开设备连接
        Log.d("BleMessageScreen", "用户离开通信界面，断开设备连接")
        viewModel.disconnectDevice()
        
        // 确保返回到设备详情页面
        val deviceDetailsRoute = com.example.jerrycan.navigation.Screen.DeviceDetails.createRoute(deviceId)
        // 检查当前的导航回退栈，确保正确返回
        val canNavigateBack = navController.previousBackStackEntry != null
        if (canNavigateBack) {
            // 正常弹出当前栈，返回上一个页面（应该是设备详情页面）
            Log.d("BleMessageScreen", "返回到上一个界面：设备详情页")
            navController.popBackStack()
        } else {
            // 如果回退栈为空或异常情况，直接导航到设备详情页面
            Log.d("BleMessageScreen", "直接导航到设备详情页：$deviceDetailsRoute")
            navController.navigate(deviceDetailsRoute) {
                // 设置导航选项
                launchSingleTop = true
            }
        }
    }
    
    // 处理系统返回按钮
    BackHandler {
        handleBackNavigation()
    }
    
    // 打印完整的设备ID信息以便调试
    LaunchedEffect(deviceId) {
        Log.d("BleMessageScreen", "接收到设备ID: $deviceId")
        Log.d("BleMessageScreen", "当前已连接设备: ${uiState.connectedDevice?.id ?: "无"}")
        Log.d("BleMessageScreen", "设备列表数量: ${uiState.deviceList.size}")
        Log.d("BleMessageScreen", "已知好友设备数量: ${uiState.friendDevices.size}")
    }
    
    // 查找设备 - 修改监听的依赖项，确保监听所有可能的设备列表变化
    val device = remember(
        uiState.connectedDevice, 
        normalizedDeviceId, 
        uiState.deviceList,
        uiState.friendDevices, // 添加对好友设备列表的监听
        uiState // 监听整个uiState以防遗漏
    ) {
        Log.d("BleMessageScreen", "重新搜索设备, 标准化ID: $normalizedDeviceId")
        
        // 优先检查当前连接的设备
        val connectedDevice = uiState.connectedDevice
        if (connectedDevice != null) {
            val connectedNormalizedId = normalizeId(connectedDevice.id)
            Log.d("BleMessageScreen", "当前连接设备ID: ${connectedDevice.id}, 标准化: $connectedNormalizedId")
            
            if (connectedNormalizedId == normalizedDeviceId) {
                Log.d("BleMessageScreen", "使用当前连接的设备: ${connectedDevice.name}")
                return@remember connectedDevice
            }
        }

        // 从设备列表中查找
        val deviceFromList = uiState.deviceList.find { 
            val normalizedListId = normalizeId(it.id)
            val matched = normalizedListId == normalizedDeviceId
            Log.d("BleMessageScreen", "比较设备列表ID: ${it.id}, 标准化: $normalizedListId, 匹配: $matched")
            matched
        }
        
        if (deviceFromList != null) {
            Log.d("BleMessageScreen", "从设备列表中找到设备: ${deviceFromList.name}")
            return@remember deviceFromList
        }
        
        // 从好友设备列表中查找
        val deviceFromFriends = uiState.friendDevices.find { 
            val normalizedFriendId = normalizeId(it.id)
            val matched = normalizedFriendId == normalizedDeviceId
            Log.d("BleMessageScreen", "比较好友设备ID: ${it.id}, 标准化: $normalizedFriendId, 匹配: $matched")
            matched
        }
        
        if (deviceFromFriends != null) {
            Log.d("BleMessageScreen", "从好友设备列表中找到设备: ${deviceFromFriends.name}")
            return@remember deviceFromFriends
        }
        
        // 尝试创建一个临时设备对象，确保能显示通信界面
        if (uiState.connectedDevice != null && normalizedDeviceId.isNotEmpty()) {
            val currentDevice = uiState.connectedDevice
            val tempDevice = currentDevice?.copy(id = deviceId)
            if (tempDevice != null) {
                Log.d("BleMessageScreen", "创建临时设备对象: ${tempDevice.name}, ID: ${tempDevice.id}")
                return@remember tempDevice
            }
        }
        
        // 日志记录设备ID查找失败
        Log.d("BleMessageScreen", "找不到设备ID: $normalizedDeviceId")
        Log.d("BleMessageScreen", "设备列表数量: ${uiState.deviceList.size}")
        Log.d("BleMessageScreen", "好友设备数量: ${uiState.friendDevices.size}")
        
        uiState.deviceList.forEach { d -> 
            Log.d("BleMessageScreen", "设备列表中: ${d.name}, ID: ${d.id}")
        }
        
        uiState.friendDevices.forEach { d -> 
            Log.d("BleMessageScreen", "好友设备中: ${d.name}, ID: ${d.id}")
        }
        
        // 返回null表示找不到设备
        null
    }
    
    // 派生状态：屏幕是否可以执行自动连接
    // 只有在屏幕处于活跃状态时才允许自动连接
    val canAutoConnect by remember { derivedStateOf { screenState == ScreenState.ACTIVE } }
    
    // 如果找到设备但未连接，并且屏幕处于活跃状态，尝试自动连接
    LaunchedEffect(device?.id, device?.isConnected, canAutoConnect) {
        if (device != null && !device.isConnected && canAutoConnect) {
            Log.d("BleMessageScreen", "自动连接设备: ${device.name}, ID: ${device.id}")
            viewModel.connectDevice(device)
        } else if (!canAutoConnect) {
            Log.d("BleMessageScreen", "屏幕正在导航中，跳过自动连接")
        }
    }
    
    // 修改连接状态LaunchedEffect
    LaunchedEffect(device?.isConnected, device?.id, uiState.servicesUpdated) {
        if (device != null && device.isConnected) {
            Log.d("BleMessageScreen", "设备已连接，处理服务发现")
            // 延迟一小段时间，让蓝牙堆栈完成连接流程
            delay(300)

            // 检查设备服务是否在缓存中可用 - 使用viewModel.getDeviceServices，它会检查缓存
            val cachedServices = viewModel.getDeviceServices(device.id)
            
            if (cachedServices.isNotEmpty()) {
                Log.d("BleMessageScreen", "从缓存中发现 ${cachedServices.size} 个服务")
                // 只有在需要刷新时增加refreshTrigger，避免重复UI刷新
                if (refreshTrigger == 0) {
                    refreshTrigger = 1 // 只设置一次初始值，避免重复刷新
                }
                // 服务已缓存，不需要再触发服务发现
                return@LaunchedEffect
            } else {
                // 服务缓存为空，需要显示加载状态并请求服务发现
                Log.d("BleMessageScreen", "缓存中无服务，触发服务发现")
                refreshTrigger++
                
                // 显式请求刷新设备服务
                viewModel.refreshDeviceServices(device.id)
                
                // 设置超时和重试逻辑
                val startTime = System.currentTimeMillis()
                var retryCount = 0
                var services: List<com.example.jerrycan.bluetooth.BluetoothService.ServiceInfo> = emptyList()
                
                // 最多尝试5次，每次等待600ms，总共不超过3秒
                while (services.isEmpty() && retryCount < 5 && System.currentTimeMillis() - startTime < 3000) {
                    Log.d("BleMessageScreen", "等待服务发现 #${retryCount + 1}")
                    delay(600)
                    services = viewModel.getDeviceServices(device.id)
                    retryCount++
                }
                
                if (services.isNotEmpty()) {
                    Log.d("BleMessageScreen", "经过${retryCount}次尝试后发现 ${services.size} 个服务")
                } else {
                    Log.d("BleMessageScreen", "多次尝试后仍未发现服务")
                }
                
                // 最后更新UI状态，无论成功与否
                refreshTrigger++
            }
        } else if (device != null) {
            Log.d("BleMessageScreen", "设备未连接，无法获取服务列表")
        }
    }
    
    // 下拉菜单状态
    var showMenu by remember { mutableStateOf(false) }
    
    // 服务列表面板显示状态
    var showServicePanel by remember { mutableStateOf(false) }
    
    // 确认对话框状态
    var showClearDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    
    // 获取设备消息
    val messages = if (device?.id != null) uiState.messages[device.id] ?: emptyList() else emptyList()
    
    // 记录滚动状态，用于自动滚动到底部
    val listState = rememberLazyListState()
    
    // 跟踪上一次消息数量，判断是加载历史消息还是收到新消息
    var prevMessageCount by remember(deviceId) { mutableStateOf(0) }
    
    // 判断用户是否在查看底部消息
    val isNearBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.totalItemsCount == 0) {
                true
            } else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem != null && lastVisibleItem.index >= layoutInfo.totalItemsCount - 2
            }
        }
    }
    
    // 在设备连接状态下，监听新消息，确保滚动到底部
    LaunchedEffect(device?.isConnected, device?.id) {
        if (device != null && device.isConnected) {
            // 当设备连接时，监听新消息
            viewModel.bluetoothService.events.collect { event ->
                // 如果收到新消息，则自动滚动到底部
                if (event is com.example.jerrycan.bluetooth.BluetoothEvent.MessageReceived && 
                    event.deviceId == device.id) {
                    // 短暂延迟，确保UI已更新
                    delay(50)
                    // 如果消息列表非空，则滚动到底部
                    val currentMessages = viewModel.uiState.value.messages[device.id] ?: emptyList()
                    if (currentMessages.isNotEmpty()) {
                        listState.animateScrollToItem(currentMessages.size - 1)
                    }
                }
            }
        }
    }
    
    // 滚动逻辑改进
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // 判断是否需要滚动到底部
            val shouldScrollToBottom = when {
                // 首次加载消息时滚动到底部
                prevMessageCount == 0 -> true
                
                // 是否是用户自己发送的新消息（如发送按钮）- 这种情况总是滚动到底部
                messages.size > prevMessageCount && 
                messages.lastOrNull()?.let { !it.isIncoming } == true -> true
                
                // 收到新消息且用户当前在查看底部区域，才自动滚动
                messages.size > prevMessageCount && isNearBottom -> true
                
                // 用户在查看历史消息，不自动滚动
                else -> false
            }
            
            if (shouldScrollToBottom) {
                // 判断是否需要动画
                val useAnimation = prevMessageCount > 0 // 只有在已有消息的情况下才使用动画
                
                if (useAnimation) {
                    listState.animateScrollToItem(messages.size - 1)
                } else {
                    // 首次加载直接跳转到底部，无动画
                    listState.scrollToItem(messages.size - 1)
                }
            }
            
            // 更新上一次消息数量
            prevMessageCount = messages.size
        }
    }
    
    // 可写入特征列表
    val availableServices = viewModel.getDeviceServices(device?.id ?: "")
    
    // 记录所有特征的属性 - 优化修改，减少不必要的日志和更新
    LaunchedEffect(device?.id, availableServices.size, refreshTrigger) {
        // 仅当refreshTrigger变化或设备ID变化，或服务数量变化时才执行日志记录
        // 使用服务数量而不是整个服务列表作为依赖，减少不必要的重组
        if (availableServices.isNotEmpty()) {
            Log.d("BleMessageScreen", "特征列表更新: 设备=${device?.id}, 服务数量=${availableServices.size}, 触发器=${refreshTrigger}")
            
            // 仅在开发模式下记录详细服务信息
            val isDebugMode = true // 替代 BuildConfig.DEBUG
            if (isDebugMode) {
                availableServices.forEach { service ->
                    Log.d("BleMessageScreen", "服务: ${service.name ?: "未知"} (${service.uuid})")
                    service.characteristics.forEach { char ->
                        Log.d("BleMessageScreen", "特征: ${char.name ?: "未知"} (${char.uuid}), 属性: ${char.properties}")
                    }
                }
            }
        } else {
            Log.d("BleMessageScreen", "未找到设备服务或者设备ID为空 (触发器=${refreshTrigger})")
        }
    }
    
    // 可写特征列表
    val writableCharacteristics = availableServices.flatMap { service ->
        service.characteristics.filter { char ->
            // 修正特征写入属性检测逻辑 - 确保大小写不敏感，并处理多种写入属性表示方式
            val properties = char.properties.uppercase()
            val isWritable = properties.contains("WRITE") || 
                properties.contains("WRITE_NO_RESPONSE") ||
                properties.contains("PROPERTY_WRITE") ||
                properties.contains("PROPERTY_WRITE_NO_RESPONSE") ||
                // 特征属性可能使用数字表示
                properties.contains("0X08") || // PROPERTY_WRITE
                properties.contains("0X04")    // PROPERTY_WRITE_NO_RESPONSE
            
            if (isWritable) {
                Log.d("BleMessageScreen", "找到可写入特征: ${char.name ?: "未知"} (${char.uuid}), 属性: ${char.properties}")
            } else {
                Log.d("BleMessageScreen", "特征不可写入: ${char.name ?: "未知"} (${char.uuid}), 属性: ${char.properties}")
            }
            
            isWritable
        }.map { char ->
            CharacteristicInfo(
                serviceUuid = service.uuid,
                serviceName = service.name ?: "未知服务",
                characteristicUuid = char.uuid,
                characteristicName = char.name ?: "未知特征",
                properties = char.properties
            )
        }
    }
    
    // 如果没有可写特征但有服务，尝试使用任何特征作为备选
    val allCharacteristics = if (writableCharacteristics.isEmpty() && availableServices.isNotEmpty()) {
        Log.d("BleMessageScreen", "未找到可写入特征，尝试使用任何特征作为备选")
        availableServices.flatMap { service ->
            service.characteristics.map { char ->
                CharacteristicInfo(
                    serviceUuid = service.uuid,
                    serviceName = service.name ?: "未知服务",
                    characteristicUuid = char.uuid,
                    characteristicName = char.name ?: "未知特征 (不可写入)",
                    properties = char.properties
                )
            }
        }
    } else {
        emptyList()
    }
    
    // 选中的特征
    var selectedCharacteristic by remember { 
        mutableStateOf<CharacteristicInfo?>(null) 
    }
    
    // 初始化选择第一个可写入特征
    LaunchedEffect(writableCharacteristics) {
        if (selectedCharacteristic == null && writableCharacteristics.isNotEmpty()) {
            selectedCharacteristic = writableCharacteristics.first()
        }
    }
    
    // 聊天消息
    val currentDeviceMessages = messages
    
    // 添加分页加载状态
    var isLoadingMoreMessages by remember { mutableStateOf(false) }
    var canLoadMoreMessages by remember { mutableStateOf(true) }
    var messageOffset by remember { mutableStateOf(0) }
    val messagePageSize = 50 // 每页加载的消息数量
    
    // 加载消息历史
    LaunchedEffect(deviceId) {
        messageOffset = 0
        canLoadMoreMessages = true
        isLoadingMoreMessages = true
        
        // 分页加载初始消息
        viewModel.loadDeviceMessageHistoryWithPaging(deviceId, 0, messagePageSize) { loadedMessages, hasMore ->
            isLoadingMoreMessages = false
            canLoadMoreMessages = hasMore
            messageOffset = loadedMessages.size
            
            // 加载完成后强制滚动到最新消息
            if (loadedMessages.isNotEmpty()) {
                // 延迟一小段时间确保UI更新完成
                coroutineScope.launch {
                    delay(100)
                    try {
                        // 重置prevMessageCount，确保LaunchedEffect(messages.size)能正确判断
                        prevMessageCount = loadedMessages.size
                        // 直接滚动到最后一条消息
                        listState.scrollToItem(loadedMessages.size - 1)
                    } catch (e: Exception) {
                        Log.e("BleMessageScreen", "初始滚动时发生错误: ${e.message}")
                    }
                }
            }
        }
    }
    
    // 跟踪滚动位置检测加载更多消息
    LaunchedEffect(listState) {
        if (canLoadMoreMessages) {
            // 使用firstVisibleItemIndex作为滚动位置指标
            val index = listState.firstVisibleItemIndex
            // 当滚动接近顶部且可以加载更多消息时，加载更多历史消息
            if (index <= 3 && !isLoadingMoreMessages) {
                isLoadingMoreMessages = true
                
                // 记录当前第一个可见项目的位置和偏移量
                val firstVisibleItem = listState.firstVisibleItemIndex
                val firstVisibleItemOffset = if (listState.layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    listState.layoutInfo.visibleItemsInfo.first().offset
                } else {
                    0
                }
                val currentSize = messages.size
                
                // 加载下一页消息
                viewModel.loadDeviceMessageHistoryWithPaging(deviceId, messageOffset, messagePageSize) { loadedMessages, hasMore ->
                    isLoadingMoreMessages = false
                    canLoadMoreMessages = hasMore
                    
                    if (loadedMessages.isNotEmpty()) {
                        messageOffset += loadedMessages.size
                        
                        // 计算位置差异，确保滚动位置保持相对稳定
                        // 使用协程延迟执行，等待UI更新
                        coroutineScope.launch {
                            delay(50) // 短暂延迟等待列表刷新
                            if (messages.size > currentSize) {
                                val newPosition = firstVisibleItem + (messages.size - currentSize)
                                try {
                                    // 直接滚动到计算出的位置，保持相对滚动位置
                                    listState.scrollToItem(
                                        index = newPosition,
                                        scrollOffset = firstVisibleItemOffset
                                    )
                                } catch (e: Exception) {
                                    // 防止索引越界等错误
                                    Log.e("BleMessageScreen", "滚动时发生错误: ${e.message}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // UI组件状态
    val messageInputState = viewModel.messageInput.collectAsState()
    
    // 主布局
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = device?.name ?: stringResource(R.string.chat_history),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { handleBackNavigation() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    // 设备状态图标
                    if (device != null) {
                        Icon(
                            imageVector = if (device.isConnected) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                            contentDescription = null,
                            tint = if (device.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    
                    // 菜单按钮
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "菜单")
                    }
                    
                    // 下拉菜单
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_clear)) },
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.chat_disconnect)) },
                            onClick = {
                                showMenu = false
                                showDisconnectDialog = true
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (device == null) {
                // 设备未找到状态
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "设备未找到",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "无法找到ID为 $deviceId 的设备",
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 显示当前连接的设备（如果有）
                    uiState.connectedDevice?.let { connectedDevice ->
                        Text(
                            text = "当前已连接设备:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${connectedDevice.name} (${connectedDevice.id})",
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // 已知设备列表
                    val allDevices = (uiState.deviceList + uiState.friendDevices).distinctBy { it.id }
                    if (allDevices.isNotEmpty()) {
                        Text(
                            text = "已知设备列表:",
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 32.dp)
                        ) {
                            allDevices.forEach { knownDevice ->
                                Text(
                                    text = "${knownDevice.name} (${knownDevice.id})",
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // 操作按钮
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { handleBackNavigation() }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("返回")
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = { viewModel.startScan() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("重新扫描")
                        }
                    }
                }
            } else {
                // 设备找到,显示通信界面
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    // 消息列表
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        // 顶部加载指示器
                        if (isLoadingMoreMessages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "加载更多消息...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (messages.isEmpty() && !isLoadingMoreMessages) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "开始发送消息吧",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        } else {
                            // 添加日志输出消息列表信息
                            Log.d("BleMessageScreen", "显示 ${messages.size} 条消息")
                            
                            // 按时间顺序排序消息
                            val sortedMessages = messages.sortedBy { it.timestamp }
                            
                            // 显示提示，是否还有更多历史消息
                            if (!canLoadMoreMessages && messages.isNotEmpty() && !isLoadingMoreMessages) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "已显示全部历史消息",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                            
                            items(sortedMessages) { message ->
                                Message_MessageBubble(message = message)
                            }
                        }
                    }
                    
                    // 底部输入区域和服务列表面板
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 底部输入区域 - 聊天风格
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 4.dp,
                            color = Color(0xFFF5F5F5) // 输入框背景色
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 文本/十六进制格式按钮
                                IconButton(
                                    onClick = { viewModel.toggleHexMode() },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isHexMode) Icons.Filled.Code else Icons.Filled.TextFormat,
                                        contentDescription = if (isHexMode) "十六进制模式" else "文本模式",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                
                                // 消息输入框 - 显示当前选择的UUID作为提示
                                val placeholderText = if (selectedCharacteristic != null) {
                                    "发送到: ${getUuidDisplayName(selectedCharacteristic!!.characteristicUuid)}"
                                } else {
                                    "输入消息..."
                                }
                                
                                OutlinedTextField(
                                    value = messageInput,
                                    onValueChange = { viewModel.updateMessageInput(it) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    placeholder = { 
                                        Text(
                                            text = placeholderText,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (selectedCharacteristic != null && messageInput.isNotEmpty()) {
                                                viewModel.sendMessage(
                                                    characteristicUuid = selectedCharacteristic!!.characteristicUuid,
                                                    serviceUuid = selectedCharacteristic!!.serviceUuid
                                                )
                                            }
                                            focusManager.clearFocus()
                                        }
                                    ),
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                
                                // 根据是否有内容决定显示发送按钮还是特征选择按钮
                                AnimatedVisibility(
                                    visible = messageInput.isNotEmpty(),
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    // 发送按钮 - 仅在有内容时显示
                                    IconButton(
                                        onClick = {
                                            if (selectedCharacteristic != null) {
                                                viewModel.sendMessage(
                                                    characteristicUuid = selectedCharacteristic!!.characteristicUuid,
                                                    serviceUuid = selectedCharacteristic!!.serviceUuid
                                                )
                                                // 记录特征信息，用于UI显示
                                                val displayName = "${selectedCharacteristic!!.serviceName}\n${selectedCharacteristic!!.characteristicUuid}"
                                                Log.d("BleMessageScreen", "发送消息到特征: $displayName")
                                            }
                                            focusManager.clearFocus()
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "发送",
                                            tint = Color.White
                                        )
                                    }
                                }
                                
                                AnimatedVisibility(
                                    visible = messageInput.isEmpty(),
                                    enter = fadeIn() + scaleIn(),
                                    exit = fadeOut() + scaleOut()
                                ) {
                                    // 服务选择按钮 - 仅在没有内容时显示
                                    IconButton(
                                        onClick = { 
                                            showServicePanel = !showServicePanel 
                                            // 关闭其他面板或菜单
                                            showMenu = false
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.FormatListBulleted,
                                            contentDescription = "选择特征",
                                            tint = if (showServicePanel) 
                                                MaterialTheme.colorScheme.primary 
                                            else 
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // 服务列表面板 - 简洁样式
                        if (showServicePanel) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(260.dp),  // 固定高度
                                shadowElevation = 4.dp,
                                color = Color(0xFFF8F8F8) // 浅灰色背景
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp)
                                ) {
                                    // 面板标题和刷新按钮
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "选择特征",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        
                                        // 修改刷新按钮的点击处理逻辑
                                        IconButton(
                                            onClick = { 
                                                // 刷新服务列表
                                                coroutineScope.launch {
                                                    if (device?.id != null) {
                                                        Log.d("BleMessageScreen", "手动刷新设备服务列表")
                                                        // 增加refreshTrigger以显示加载状态
                                                        refreshTrigger++
                                                        
                                                        // 清除缓存并重新发现服务
                                                        // 注意：这时refreshTrigger > 0，所以UI会显示加载状态
                                                        viewModel.refreshDeviceServices(device.id)
                                                        
                                                        // 等待服务发现完成，降低等待时间以提高响应性
                                                        delay(500)
                                                        refreshTrigger-- // 减少trigger，UI会隐藏加载状态
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            if (refreshTrigger > 0) {
                                                // 显示加载指示器
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.Refresh,
                                                    contentDescription = "刷新服务列表",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                    
                                    HorizontalDivider()
                                    
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    
                                    // 特征列表
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        if (writableCharacteristics.isNotEmpty()) {
                                            items(writableCharacteristics) { characteristic ->
                                                CharacteristicItem(
                                                    characteristic = characteristic,
                                                    isSelected = selectedCharacteristic?.characteristicUuid == characteristic.characteristicUuid,
                                                    onClick = {
                                                        selectedCharacteristic = characteristic
                                                        showServicePanel = false  // 选择后自动隐藏面板
                                                    }
                                                )
                                            }
                                        } else if (allCharacteristics.isNotEmpty()) {
                                            // 显示无可写入特征的提示
                                            item {
                                                Text(
                                                    text = "无可写入特征",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(16.dp)
                                                )
                                                
                                                Text(
                                                    text = "所有可用特征:",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                                                )
                                                
                                                HorizontalDivider()
                                            }
                                            
                                            // 显示所有特征，即使不可写入
                                            items(allCharacteristics) { char ->
                                                CharacteristicItem(
                                                    characteristic = char,
                                                    isSelected = selectedCharacteristic?.characteristicUuid == char.characteristicUuid,
                                                    onClick = {
                                                        selectedCharacteristic = char
                                                        showServicePanel = false  // 选择后自动隐藏面板
                                                    }
                                                )
                                            }
                                        } else {
                                            // 没有发现任何特征时显示
                                            item {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(16.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                    if (uiState.isScanning || refreshTrigger > 0) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.padding(8.dp)
                                                        )
                                                        Text("正在获取服务列表...")
                                                    } else {
                                                        Text(
                                                            text = "未找到设备服务",
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            color = MaterialTheme.colorScheme.error
                                                        )
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Text(
                                                            text = "点击刷新按钮重试，或检查设备连接状态",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 加载指示器
            if (uiState.isScanning) {
                LoadingIndicator(message = "正在扫描设备...")
            }
            
            // 确认清空对话框
            if (showClearDialog) {
                ConfirmationDialog(
                    title = stringResource(R.string.chat_clear),
                    message = "确定要清空聊天记录吗？此操作不可撤销。",
                    onConfirm = {
                        viewModel.clearMessages()
                    },
                    onDismiss = {
                        showClearDialog = false
                    }
                )
            }
            
            // 确认断开连接对话框
            if (showDisconnectDialog) {
                ConfirmationDialog(
                    title = stringResource(R.string.chat_disconnect),
                    message = "确定要断开当前设备连接吗？",
                    onConfirm = {
                        // 设置屏幕状态为导航中
                        screenState = ScreenState.NAVIGATING
                        
                        // 先断开设备连接
                        viewModel.disconnectDevice()
                        showDisconnectDialog = false
                        // 延迟短暂时间后再返回，确保断开连接完成
                        coroutineScope.launch {
                            delay(300)
                            navController.popBackStack()
                        }
                    },
                    onDismiss = {
                        showDisconnectDialog = false
                    }
                )
            }
            
            // 记录所有消息情况的日志，用于调试
            LaunchedEffect(deviceId) {
                val allMessages = viewModel.bluetoothService.messages.value
                Log.d("BleMessageScreen", "所有消息映射：${allMessages.size} 个设备")
                allMessages.forEach { (devId, msgs) ->
                    Log.d("BleMessageScreen", "设备 $devId 的消息：${msgs.size} 条")
                    Log.d("BleMessageScreen", "接收消息：${msgs.count { it.isIncoming }} 条，发送消息：${msgs.count { !it.isIncoming }} 条")
                }
            }
        }
    }
}

/**
 * 特征列表项组件
 */
@Composable
fun CharacteristicItem(
    characteristic: CharacteristicInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 左侧特征信息
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = characteristic.characteristicName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = characteristic.characteristicUuid,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = characteristic.serviceName,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // 右侧选中指示器
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.BluetoothConnected,
                    contentDescription = "已选择",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Message_MessageBubble(
    message: BluetoothMessage,
    modifier: Modifier = Modifier
) {
    val isIncoming = message.isIncoming
    var displayMode by remember { mutableStateOf(HexDisplayMode.GROUPED) }
    
    // 使用全局菜单状态
    val currentMenuId by remember { LongPressMenuState._currentMenuMessageId }
    val showMenu = currentMenuId == message.id
    
    // 获取ViewModel
    val viewModel: BluetoothViewModel = viewModel()
    
    // 提前获取Context，避免在lambda中调用Composable函数
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(
        android.content.Context.CLIPBOARD_SERVICE
    ) as android.content.ClipboardManager
    
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = if (isIncoming) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // UUID显示在气泡外上方，小字体灰色
            val uuidText = formatUuid(message.sourceUuid)
            if (uuidText != "未知") {
                Text(
                    text = uuidText,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        start = if (isIncoming) 12.dp else 0.dp,
                        end = if (isIncoming) 0.dp else 12.dp,
                        bottom = 2.dp
                    )
                )
            }
            
            // 获取消息长度和类型
            val messageLength = message.content.length
            val isShortMessage = messageLength < 20 && !message.isHex
            val isVeryShortMessage = messageLength <= 5 && !message.isHex // 特别短的消息特殊处理
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isIncoming) Arrangement.Start else Arrangement.End
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isIncoming) 
                            Color.White // 接收消息使用白色背景
                        else 
                            MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(
                        topStart = if (isIncoming) 2.dp else 16.dp,
                        topEnd = if (isIncoming) 16.dp else 2.dp,
                        bottomStart = if (isIncoming) 16.dp else 16.dp,
                        bottomEnd = if (isIncoming) 16.dp else 16.dp
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isIncoming) 0.5.dp else 1.dp
                    ),
                    // 为接收消息添加淡灰色边框，增强立体感
                    border = if (isIncoming) BorderStroke(0.5.dp, Color(0xFFE0E0E0)) else null,
                    modifier = Modifier
                        // 根据消息长度和类型调整宽度限制
                        .let { mod ->
                            when {
                                isVeryShortMessage -> {
                                    // 非常短的消息（5个字符以内）使用紧凑宽度
                                    mod.widthIn(min = 10.dp, max = 80.dp)
                                }
                                isShortMessage -> {
                                    // 短消息使用有限制的自适应宽度
                                    mod.widthIn(min = 35.dp, max = 120.dp)
                                }
                                message.isHex && message.content.length > 20 -> {
                                    // 长十六进制数据使用宽显示区域
                                    mod.widthIn(min = 200.dp, max = 280.dp)
                                }
                                message.isHex -> {
                                    // 短十六进制数据使用中等宽度
                                    mod.widthIn(min = 120.dp, max = 200.dp)
                                }
                                messageLength > 100 -> {
                                    // 非常长的消息限制更宽的最大宽度
                                    mod.widthIn(min = 100.dp, max = 280.dp)
                                }
                                messageLength > 50 -> {
                                    // 中长消息
                                    mod.widthIn(min = 80.dp, max = 260.dp)
                                }
                                else -> {
                                    // 普通消息
                                    mod.widthIn(min = 60.dp, max = 240.dp)
                                }
                            }
                        }
                        .combinedClickable(
                            onClick = { 
                                // 点击消息会关闭任何打开的菜单
                                LongPressMenuState.reset()
                            },
                            onLongClick = { 
                                // 先关闭任何打开的菜单，再打开此菜单
                                LongPressMenuState.reset()
                                LongPressMenuState.currentMenuMessageId = message.id
                            },
                            onLongClickLabel = "长按菜单"
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(
                            // 为短消息减少水平内边距
                            start = if (isVeryShortMessage) 8.dp else 12.dp,
                            end = if (isVeryShortMessage) 8.dp else 12.dp,
                            top = 8.dp,
                            bottom = 8.dp
                        )
                    ) {
                        // 消息内容
                        when {
                            message.isHex -> {
                                // 十六进制数据视图
                                HexDataView(
                                    hexData = message.content,
                                    displayMode = displayMode,
                                    bytesPerLine = 8,
                                    showAscii = true,
                                    showAddresses = true,
                                    onDisplayModeChange = { displayMode = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                )
                            }
                            else -> {
                                // 文本消息 - 直接使用fillMaxWidth让文本自动换行
                                Text(
                                    text = message.content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isIncoming) 
                                        Color.Black.copy(alpha = 0.8f) // 接收消息文本颜色调整为黑色
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.fillMaxWidth() // 确保文本填充气泡宽度
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // 消息底部时间戳和状态
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 时间戳
                            val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                            val formattedTime = timeFormatter.format(message.timestamp)
                            
                            Text(
                                text = formattedTime,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = if (isIncoming) 
                                    Color.Gray.copy(alpha = 0.7f) // 调整颜色
                                else
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                            )
                            
                            // 只有发送的消息才显示状态
                            if (!isIncoming) {
                                Spacer(modifier = Modifier.width(4.dp))
                                val statusIcon = when (message.status) {
                                    MessageStatus.SENDING -> Icons.Default.Schedule
                                    MessageStatus.SENT -> Icons.Default.Done
                                    MessageStatus.RECEIVED -> Icons.Default.DoneAll
                                    MessageStatus.FAILED -> Icons.Default.Error
                                }
                                val statusColor = when (message.status) {
                                    MessageStatus.FAILED -> MaterialTheme.colorScheme.error
                                    else -> if (isIncoming) 
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    else
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                }
                                Icon(
                                    imageVector = statusIcon,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // 菜单弹出层
        if (showMenu) {
            // 添加全屏遮罩层
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        LongPressMenuState.reset()
                    }
            ) {
                // 菜单内容
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = if (isIncoming) Alignment.TopStart else Alignment.TopEnd
                ) {
                    // 菜单卡片 
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xDD333333), // 微信菜单深灰色背景
                        modifier = Modifier
                            .padding(
                                top = 8.dp, 
                                start = if(isIncoming) 16.dp else 0.dp,
                                end = if(isIncoming) 0.dp else 16.dp
                            )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                        ) {
                            // 复制选项
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .clickable {
                                        // 复制到剪贴板
                                        val clip = android.content.ClipData.newPlainText(
                                            "消息内容",
                                            message.content
                                        )
                                        clipboardManager.setPrimaryClip(clip)
                                        LongPressMenuState.reset()
                                    }
                            ) {
                                Text(
                                    text = "复制",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                                )
                            }
                            
                            // 分隔符
                            if (message.isHex || (!isIncoming && message.status == MessageStatus.FAILED)) {
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(28.dp)
                                        .background(Color.White.copy(alpha = 0.5f))
                                )
                            }
                            
                            // 十六进制数据模式选择
                            if (message.isHex) {
                                HexDisplayModes.forEach { mode ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .padding(horizontal = 8.dp)
                                            .clickable {
                                                displayMode = mode
                                                LongPressMenuState.reset()
                                            }
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                            
                            // 重试选项（对于失败的消息）
                            if (!isIncoming && message.status == MessageStatus.FAILED) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp)
                                        .clickable {
                                            viewModel.retryFailedMessage(message.id)
                                            LongPressMenuState.reset()
                                        }
                                ) {
                                    Text(
                                        text = "重试",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 