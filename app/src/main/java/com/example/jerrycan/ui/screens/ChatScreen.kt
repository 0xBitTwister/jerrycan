package com.example.jerrycan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jerrycan.R
import com.example.jerrycan.model.BluetoothMessage
import com.example.jerrycan.model.MessageStatus
import com.example.jerrycan.navigation.Screen
import com.example.jerrycan.ui.components.ConfirmationDialog
import com.example.jerrycan.ui.components.EmptyStateView
import com.example.jerrycan.ui.components.StatusIndicator
import com.example.jerrycan.ui.theme.NordicBlue
import com.example.jerrycan.ui.theme.NordicLightBlue
import com.example.jerrycan.viewmodel.BluetoothViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    deviceId: String? = null,
    viewModel: BluetoothViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isHexMode by viewModel.isHexMode.collectAsState()
    val messageInput by viewModel.messageInput.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // 处理从聊天历史传入的设备ID
    LaunchedEffect(deviceId) {
        if (deviceId != null) {
            // 如果有设备ID，自动连接该设备
            val device = uiState.deviceList.find { it.id == deviceId } ?: 
                          viewModel.friendDevices.value.find { it.id == deviceId }
            
            device?.let {
                if (!it.isConnected) {
                    // 如果设备未连接，尝试连接它
                    viewModel.connectDevice(it)
                }
            }
        }
    }
    
    // 获取当前连接设备的消息
    val currentDeviceId = deviceId ?: uiState.connectedDevice?.id
    val messages = if (currentDeviceId != null) uiState.messages[currentDeviceId] ?: emptyList() else emptyList()
    
    // 记录滚动状态，用于自动滚动到底部
    val listState = rememberLazyListState()
    
    // 每当有新消息时，自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // 下拉菜单状态
    var showMenu by remember { mutableStateOf(false) }
    
    // 确认对话框状态
    var showClearDialog by remember { mutableStateOf(false) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    
    // 新的布局方式
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = uiState.connectedDevice?.name ?: stringResource(R.string.chat_placeholder),
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                actions = {
                    if (uiState.connectedDevice != null) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "更多选项",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_disconnect)) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Filled.BluetoothDisabled, 
                                        contentDescription = null
                                    ) 
                                },
                                onClick = {
                                    showMenu = false
                                    showDisconnectDialog = true
                                }
                            )
                            
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.chat_clear)) },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Filled.Clear, 
                                        contentDescription = null
                                    ) 
                                },
                                onClick = {
                                    showMenu = false
                                    showClearDialog = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        // 主要内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp)
        ) {
            // 聊天内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (uiState.connectedDevice == null) {
                    // 未连接设备时显示中央标题
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "通信界面",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = stringResource(R.string.chat_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                // 导航到设备页面，但使用popUpTo避免创建循环导航栈
                                navController.navigate(Screen.Devices.route) {
                                    // 弹出当前聊天页面，避免返回栈中的循环
                                    popUpTo(Screen.Chat.route) {
                                        // inclusive = true表示弹出包括当前页面在内的返回栈
                                        inclusive = true
                                    }
                                }
                            }
                        ) {
                            Text(text = stringResource(R.string.chat_connect_device))
                        }
                    }
                } else if (messages.isEmpty()) {
                    // 已连接但没有消息时的提示
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // 设备信息
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.connectedDevice?.name ?: "",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            StatusIndicator(
                                isActive = true,
                                activeLabel = stringResource(R.string.devices_connected),
                                inactiveLabel = stringResource(R.string.devices_disconnected)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "开始发送消息吧",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    // 显示消息列表
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                    ) {
                        items(messages) { message ->
                            MessageItem(message = message)
                        }
                    }
                }
            }
            
            // 底部输入区域
            if (uiState.connectedDevice != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        // 十六进制模式切换
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHexMode) 
                                    stringResource(R.string.chat_hex_mode)
                                else 
                                    stringResource(R.string.chat_text_mode),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Switch(
                                checked = isHexMode,
                                onCheckedChange = { viewModel.toggleHexMode() }
                            )
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 消息输入框
                            OutlinedTextField(
                                value = messageInput,
                                onValueChange = { viewModel.updateMessageInput(it) },
                                modifier = Modifier.weight(1f),
                                placeholder = { 
                                    Text(stringResource(R.string.chat_hint))
                                },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        viewModel.sendMessage()
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            // 发送按钮
                            IconButton(
                                onClick = {
                                    if (messageInput.isNotEmpty()) {
                                        viewModel.sendMessage()
                                    }
                                },
                                enabled = messageInput.isNotEmpty() && uiState.connectedDevice != null
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "发送",
                                    tint = if (messageInput.isNotEmpty() && uiState.connectedDevice != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 确认清空对话框
    if (showClearDialog) {
        ConfirmationDialog(
            title = stringResource(R.string.chat_clear),
            message = "确定要清空聊天记录吗？此操作不可撤销。",
            onConfirm = {
                viewModel.clearMessages()
                showClearDialog = false
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
                viewModel.disconnectDevice()
                showDisconnectDialog = false
                // 断开连接后返回聊天历史列表
                navController.navigateUp()
            },
            onDismiss = {
                showDisconnectDialog = false
            }
        )
    }
}

@Composable
fun MessageItem(message: BluetoothMessage) {
    val isIncoming = message.isIncoming
    val backgroundColor = if (isIncoming) 
        MaterialTheme.colorScheme.surface 
    else 
        NordicBlue
    val textColor = if (isIncoming) 
        MaterialTheme.colorScheme.onSurface 
    else 
        Color.White
    
    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormatter.format(message.timestamp)
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
    ) {
        Card(
            modifier = Modifier.padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = backgroundColor)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // 显示消息状态（仅发送消息）
            if (!isIncoming) {
                Text(
                    text = when (message.status) {
                        MessageStatus.SENDING -> "发送中..."
                        MessageStatus.SENT -> "已发送"
                        MessageStatus.RECEIVED -> "已送达"
                        MessageStatus.FAILED -> "发送失败"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.status == MessageStatus.FAILED)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
} 