package com.example.jerrycan.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jerrycan.R
import com.example.jerrycan.model.BluetoothLog
import com.example.jerrycan.model.LogAction
import com.example.jerrycan.navigation.Screen
import com.example.jerrycan.ui.components.ConfirmationDialog
import com.example.jerrycan.ui.components.EmptyStateView
import com.example.jerrycan.viewmodel.BluetoothViewModel
import com.example.jerrycan.viewmodel.LogFilter
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.TopAppBar

// Filter types for the UI
enum class FilterType {
    ALL, CONNECTION, DATA, SCAN,
    // Action filter types
    CONNECT, DISCONNECT, SEND_DATA, RECEIVE_DATA
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogsScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val logFilter by viewModel.logFilter.collectAsState()
    
    // 菜单状态
    var showMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.logs_title))
                },
                actions = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Filter,
                            contentDescription = stringResource(R.string.logs_filter)
                        )
                    }
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多选项"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logs_clear)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                showClearDialog = true
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logs_share)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                // 导出/分享日志
                                // TODO: 实现日志导出功能
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (uiState.logs.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ActionFilterChip(
                        filterType = FilterType.ALL,
                        isSelected = logFilter == LogFilter.ALL,
                        onClick = { viewModel.setLogFilter(LogFilter.ALL) }
                    )
                    
                    ActionFilterChip(
                        filterType = FilterType.CONNECTION,
                        isSelected = logFilter == LogFilter.CONNECTION,
                        onClick = { viewModel.setLogFilter(LogFilter.CONNECTION) }
                    )
                    
                    ActionFilterChip(
                        filterType = FilterType.DATA,
                        isSelected = logFilter == LogFilter.DATA,
                        onClick = { viewModel.setLogFilter(LogFilter.DATA) }
                    )
                    
                    ActionFilterChip(
                        filterType = FilterType.SCAN,
                        isSelected = logFilter == LogFilter.SCAN,
                        onClick = { viewModel.setLogFilter(LogFilter.SCAN) }
                    )
                }
            }
            
            // 日志列表
            if (uiState.logs.isEmpty()) {
                EmptyStateView(
                    icon = Icons.AutoMirrored.Filled.List,
                    message = stringResource(R.string.logs_no_logs)
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    items(uiState.logs) { log ->
                        LogItem(
                            log = log,
                            onClick = {
                                navController.navigate(Screen.LogDetail.createRoute(log.id))
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        
        // 筛选对话框
        if (showFilterDialog) {
            LogFilterDialog(
                currentFilter = logFilter,
                onFilterApplied = { filter ->
                    viewModel.setLogFilter(filter)
                    showFilterDialog = false
                },
                onDismiss = {
                    showFilterDialog = false
                }
            )
        }
        
        // 清空确认对话框
        if (showClearDialog) {
            ConfirmationDialog(
                title = stringResource(R.string.logs_clear),
                message = "确定要清空所有日志吗？此操作不可撤销。",
                onConfirm = {
                    viewModel.clearLogs()
                },
                onDismiss = {
                    showClearDialog = false
                }
            )
        }
    }
}

@Composable
fun LogItem(
    log: BluetoothLog,
    onClick: () -> Unit
) {
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // 日志头部
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 操作类型标签
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(getLogActionColor(log.action))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getLogActionName(log.action),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // 设备名称
                Text(
                    text = log.deviceName.ifEmpty { "系统" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // 时间戳
                Text(
                    text = dateFormatter.format(log.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 日志详情
            Text(
                text = log.details,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            // 操作按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.logs_details),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDetailScreen(
    navController: NavController,
    logId: String,
    viewModel: BluetoothViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // 查找指定ID的日志
    val log = uiState.logs.find { it.id == logId }
    
    // 菜单状态
    var showMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = "日志详情")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 更多菜单
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "更多选项"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logs_copy)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                // TODO: 复制日志内容到剪贴板
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.logs_export)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                // TODO: 导出日志内容
                            }
                        )
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
    ) { innerPadding ->
        if (log != null) {
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                // 日志信息卡片
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // 操作类型
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "操作类型:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(getLogActionColor(log.action))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getLogActionName(log.action),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White
                                )
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // 设备信息
                        Text(
                            text = "设备名称: ${log.deviceName.ifEmpty { "系统" }}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        if (log.deviceId.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "设备ID: ${log.deviceId}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // 时间戳
                        Text(
                            text = "时间: ${dateFormatter.format(log.timestamp)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        // 详细内容
                        Text(
                            text = "详细内容:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // 日志不存在
            EmptyStateView(
                icon = Icons.AutoMirrored.Filled.List,
                message = "找不到指定日志",
                actionLabel = "返回",
                onAction = {
                    navController.popBackStack()
                }
            )
        }
    }
}

@Composable
fun LogFilterDialog(
    currentFilter: LogFilter?,
    onFilterApplied: (LogFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDeviceId by remember { mutableStateOf(currentFilter?.deviceId) }
    var selectedAction by remember { mutableStateOf(currentFilter?.action) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.logs_filter)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.logs_filter_action),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                
                // 操作类型筛选
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionFilterChip(
                        filterType = FilterType.CONNECT,
                        isSelected = selectedAction == LogAction.CONNECT,
                        onClick = {
                            selectedAction = if (selectedAction == LogAction.CONNECT) null else LogAction.CONNECT
                        }
                    )
                    
                    ActionFilterChip(
                        filterType = FilterType.DISCONNECT,
                        isSelected = selectedAction == LogAction.DISCONNECT,
                        onClick = {
                            selectedAction = if (selectedAction == LogAction.DISCONNECT) null else LogAction.DISCONNECT
                        }
                    )
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionFilterChip(
                        filterType = FilterType.SEND_DATA,
                        isSelected = selectedAction == LogAction.SEND_DATA,
                        onClick = {
                            selectedAction = if (selectedAction == LogAction.SEND_DATA) null else LogAction.SEND_DATA
                        }
                    )
                    
                    ActionFilterChip(
                        filterType = FilterType.RECEIVE_DATA,
                        isSelected = selectedAction == LogAction.RECEIVE_DATA,
                        onClick = {
                            selectedAction = if (selectedAction == LogAction.RECEIVE_DATA) null else LogAction.RECEIVE_DATA
                        }
                    )
                }
                
                // TODO: 可以添加设备筛选和日期筛选
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onFilterApplied(
                        LogFilter(
                            deviceId = selectedDeviceId,
                            action = selectedAction
                        )
                    )
                }
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionFilterChip(
    filterType: FilterType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val label = when(filterType) {
        FilterType.ALL -> stringResource(R.string.logs_filter_all)
        FilterType.CONNECTION -> stringResource(R.string.logs_filter_connection)
        FilterType.DATA -> stringResource(R.string.logs_filter_data)
        FilterType.SCAN -> stringResource(R.string.logs_filter_scan)
        FilterType.CONNECT -> stringResource(R.string.logs_action_connect)
        FilterType.DISCONNECT -> stringResource(R.string.logs_action_disconnect)
        FilterType.SEND_DATA -> stringResource(R.string.logs_action_send)
        FilterType.RECEIVE_DATA -> stringResource(R.string.logs_action_receive)
    }
    
    val color = when(filterType) {
        FilterType.ALL -> MaterialTheme.colorScheme.primary
        FilterType.CONNECTION -> MaterialTheme.colorScheme.tertiary
        FilterType.DATA -> MaterialTheme.colorScheme.secondary
        FilterType.SCAN -> Color(0xFF9C27B0) // Purple
        FilterType.CONNECT -> MaterialTheme.colorScheme.tertiary
        FilterType.DISCONNECT -> Color.Gray
        FilterType.SEND_DATA -> MaterialTheme.colorScheme.primary
        FilterType.RECEIVE_DATA -> MaterialTheme.colorScheme.secondary
    }
    
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = color,
            selectedLabelColor = Color.White
        )
    )
}

@Composable
fun getLogActionName(action: LogAction): String {
    return when (action) {
        LogAction.CONNECT -> stringResource(R.string.logs_action_connect)
        LogAction.DISCONNECT -> stringResource(R.string.logs_action_disconnect)
        LogAction.SEND_DATA -> stringResource(R.string.logs_action_send)
        LogAction.RECEIVE_DATA -> stringResource(R.string.logs_action_receive)
        LogAction.SCAN_START -> "开始扫描"
        LogAction.SCAN_STOP -> "停止扫描"
        LogAction.PAIR -> "配对"
        LogAction.UNPAIR -> "取消配对"
        LogAction.ERROR -> "错误"
        LogAction.INFO -> "信息"
    }
}

@Composable
fun getLogActionColor(action: LogAction): Color {
    return when (action) {
        LogAction.CONNECT -> MaterialTheme.colorScheme.tertiary
        LogAction.DISCONNECT -> Color.Gray
        LogAction.SEND_DATA -> MaterialTheme.colorScheme.primary
        LogAction.RECEIVE_DATA -> MaterialTheme.colorScheme.secondary
        LogAction.SCAN_START, LogAction.SCAN_STOP -> Color(0xFF9C27B0) // 紫色
        LogAction.PAIR, LogAction.UNPAIR -> Color(0xFF009688) // 青色
        LogAction.ERROR -> MaterialTheme.colorScheme.error
        LogAction.INFO -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) // 淡蓝色
    }
} 