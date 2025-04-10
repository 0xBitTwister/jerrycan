package com.example.jerrycan.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jerrycan.R
import com.example.jerrycan.model.BluetoothDevice
import com.example.jerrycan.model.BluetoothMessage
import com.example.jerrycan.navigation.Screen
import com.example.jerrycan.ui.components.EmptyStateView
import com.example.jerrycan.ui.theme.NordicBlue
import com.example.jerrycan.viewmodel.BluetoothViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import java.util.Calendar
import kotlin.math.abs
import com.example.jerrycan.ui.components.DeviceListItem
import com.example.jerrycan.ui.components.ChatDeviceListItem
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import android.widget.Toast
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import com.example.jerrycan.ui.components.ConfirmationDialog
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.Arrangement

// 主题颜色
val ChatGreen = Color(0xFF07C160)
val ChatBlue = Color(0xFF1989FA)
val ChatLightGray = Color(0xFFF7F7F7)  // 背景浅灰色
val ChatDividerGray = Color(0xFFEDEDED) // 分割线颜色
val ChatTextGray = Color(0xFF999999) // 次要文字颜色

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistoryScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    // 添加一个状态计数器，用于在删除操作后强制刷新列表
    var deleteCounter by remember { mutableStateOf(0) }
    // 添加刷新时间戳，用于定期刷新列表
    var refreshTimestamp by remember { mutableStateOf(0L) }
    
    // 使用ViewModel提供的方法获取所有有消息的设备
    // 将deleteCounter和refreshTimestamp作为key，使其在相应情况下重新获取列表
    val sortedDevices = remember(uiState.messages, deleteCounter, refreshTimestamp) {
        Log.d("ChatHistoryScreen", "重新构建设备消息列表，删除计数: $deleteCounter, 刷新时间戳: $refreshTimestamp")
        viewModel.getDevicesWithMessages()
    }
    
    // 长按功能变量
    var showContextMenu by remember { mutableStateOf(false) }
    var longPressedDevice by remember { mutableStateOf<BluetoothDevice?>(null) }
    var menuPosition by remember { mutableStateOf(Offset.Zero) }
    
    // 确认对话框状态
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // 记录日志
    LaunchedEffect(true) {
        Log.d("ChatHistoryScreen", "聊天历史屏幕已启动")
        Log.d("ChatHistoryScreen", "设备列表大小: ${uiState.deviceList.size}")
        Log.d("ChatHistoryScreen", "好友设备大小: ${viewModel.friendDevices.value.size}")
        Log.d("ChatHistoryScreen", "消息映射大小: ${uiState.messages.size}, 设备ID: ${uiState.messages.keys}")
        Log.d("ChatHistoryScreen", "找到 ${sortedDevices.size} 个带消息的设备")
        sortedDevices.forEach { (device, lastMessage) ->
            Log.d("ChatHistoryScreen", "设备: ${device.name}, 最后一条消息: ${lastMessage?.content?.take(20)}")
        }
    }
    
    // 添加启动时的加载
    LaunchedEffect(Unit) {
        // 加载设备和消息历史
        Log.d("ChatHistoryScreen", "初始加载好友设备和消息历史")
        viewModel.loadFriendDevices(true)  // 强制刷新
        viewModel.loadMessageHistory()     // 加载所有设备消息历史
        
        // 设置初始刷新时间戳
        refreshTimestamp = System.currentTimeMillis()
        
        // 定期刷新设备列表和消息
        while (true) {
            delay(5000) // 5秒刷新一次
            Log.d("ChatHistoryScreen", "定期刷新设备列表和消息")
            viewModel.refreshFriendDevicesStatus()
            
            // 更新刷新时间戳触发列表更新
            refreshTimestamp = System.currentTimeMillis()
        }
    }
    
    // 删除确认对话框
    if (showDeleteDialog && longPressedDevice != null) {
        ConfirmationDialog(
            title = "删除聊天记录",
            message = "确定要删除与\"${longPressedDevice?.name}\"的聊天记录吗？此操作不可恢复。",
            onConfirm = {
                // 删除设备消息历史
                longPressedDevice?.id?.let { deviceId ->
                    viewModel.deleteDeviceHistory(deviceId) { success ->
                        if (success) {
                            Toast.makeText(context, "聊天记录已删除", Toast.LENGTH_SHORT).show()
                            // 增加计数器触发列表刷新
                            deleteCounter++
                        } else {
                            Toast.makeText(context, "删除失败，请重试", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                showDeleteDialog = false
                longPressedDevice = null
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
    
    // 整体内容布局
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部区域（包含状态栏和标题栏）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                // 状态栏高度的空白
                Spacer(
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .fillMaxWidth()
                )
                
                // 简洁风格标题栏
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.White)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 标题（居中）
                    Text(
                        text = "聊天",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Color.Black,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                    
                    // 搜索按钮（右侧）
                    IconButton(
                        onClick = { /* 实现搜索功能 */ },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // 添加设备按钮（最右侧）
                    IconButton(
                        onClick = { navController.navigate(Screen.Devices.route) },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加设备",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // 标题栏下方细微分割线
                HorizontalDivider(
                    color = ChatDividerGray,
                    thickness = 0.5.dp
                )
            }
            
            // 内容区域（消息列表）
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ChatLightGray)
            ) {
                if (sortedDevices.isEmpty()) {
                    EmptyStateView(
                        icon = Icons.AutoMirrored.Filled.BluetoothSearching,
                        message = "暂无历史会话",
                        actionLabel = "去连接设备",
                        onAction = { navController.navigate(Screen.Devices.route) }
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedDevices.size) { index ->
                            val (device, lastMessage) = sortedDevices[index]
                            ChatHistoryItem(
                                device = device,
                                lastMessage = lastMessage,
                                onClick = {
                                    // 导航到BleMessageScreen
                                    Log.d("ChatHistoryScreen", "正在导航到BLE消息界面, 设备ID: ${device.id}")
                                    
                                    // 格式化设备ID，确保格式一致 (移除冒号等特殊字符)
                                    val formattedDeviceId = device.id.replace(":", "").uppercase()
                                    Log.d("ChatHistoryScreen", "格式化后的设备ID: $formattedDeviceId")
                                    
                                    val route = Screen.BleMessage.createRoute(device.id)
                                    Log.d("ChatHistoryScreen", "生成的路由: $route")
                                    navController.navigate(route)
                                },
                                onLongClick = { position ->
                                    longPressedDevice = device
                                    menuPosition = position
                                    showContextMenu = true
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp), // 从头像右侧开始
                                thickness = 0.5.dp,
                                color = ChatDividerGray // 分割线颜色
                            )
                        }
                    }
                
                    // 长按菜单
                    if (showContextMenu && longPressedDevice != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showContextMenu = false
                                }
                        )
                        
                        // 菜单内容
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
                                    
                                    // 标记为已读选项
                                    TextButton(
                                        onClick = {
                                            // 未实现标记已读功能，可以后续添加
                                            showContextMenu = false
                                            Toast.makeText(context, "标记为已读功能尚未实现", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "标为已读",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Black
                                        )
                                    }
                                    
                                    // 删除选项
                                    TextButton(
                                        onClick = {
                                            showContextMenu = false
                                            showDeleteDialog = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "删除聊天记录",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Red
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

@Composable
fun ChatHistoryItem(
    device: BluetoothDevice,
    lastMessage: BluetoothMessage?,
    onClick: () -> Unit,
    onLongClick: (Offset) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { 
                Log.d("ChatHistoryScreen", "点击设备: ${device.name} (${device.id})")
                onClick() 
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        Log.d("ChatHistoryScreen", "长按设备: ${device.name}")
                        onLongClick(offset)
                    },
                    onTap = {
                        Log.d("ChatHistoryScreen", "点击设备 (通过手势): ${device.name} (${device.id})")
                        onClick()
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 设备头像
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(ChatBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.BluetoothSearching,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 设备信息和最后消息
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 设备名称和时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 设备名称
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 最后消息时间
                lastMessage?.timestamp?.let {
                    Text(
                        text = formatMessageTime(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = ChatTextGray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // 最后一条消息
            Text(
                text = when {
                    lastMessage == null -> "无消息记录"
                    lastMessage.isHex -> "[数据] ${formatHexContent(lastMessage.content)}"
                    else -> lastMessage.content
                },
                style = MaterialTheme.typography.bodyMedium,
                color = ChatTextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// 将十六进制内容格式化为简短可读的形式
private fun formatHexContent(content: String): String {
    val trimmed = content.replace("\\s+".toRegex(), "").take(12)
    return if (trimmed.length > 10) "$trimmed..." else trimmed
}

// 格式化消息时间
private fun formatMessageTime(date: Date): String {
    val now = Calendar.getInstance()
    val msgTime = Calendar.getInstance().apply { time = date }
    
    return when {
        // 今天的消息显示时间
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) == msgTime.get(Calendar.DAY_OF_YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        // 昨天的消息
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.DAY_OF_YEAR) - msgTime.get(Calendar.DAY_OF_YEAR) == 1 -> {
            "昨天"
        }
        // 同一周的消息显示星期几
        now.get(Calendar.YEAR) == msgTime.get(Calendar.YEAR) &&
        now.get(Calendar.WEEK_OF_YEAR) == msgTime.get(Calendar.WEEK_OF_YEAR) -> {
            when (msgTime.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "周日"
                Calendar.MONDAY -> "周一"
                Calendar.TUESDAY -> "周二"
                Calendar.WEDNESDAY -> "周三"
                Calendar.THURSDAY -> "周四"
                Calendar.FRIDAY -> "周五"
                Calendar.SATURDAY -> "周六"
                else -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date)
            }
        }
        // 其他消息显示日期
        else -> SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(date)
    }
} 