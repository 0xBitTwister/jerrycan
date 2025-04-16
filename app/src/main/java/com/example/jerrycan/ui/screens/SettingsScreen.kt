package com.example.jerrycan.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.jerrycan.R
import com.example.jerrycan.viewmodel.BluetoothViewModel
import com.example.jerrycan.viewmodel.DeviceType
import com.example.jerrycan.viewmodel.ScanFilterSettings
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalContext
import com.example.jerrycan.utils.FileUtils
import java.io.File
import com.example.jerrycan.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: BluetoothViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scanFilterSettings by viewModel.scanFilterSettings.collectAsState()
    val context = LocalContext.current
    
    // 设置对话框状态
    var showThemeDialog by remember { mutableStateOf(false) }
    var showScanDurationDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showScanFilterDialog by remember { mutableStateOf(false) }
    var showGitInfoDialog by remember { mutableStateOf(false) }
    
    // 设置状态
    var isAutoReconnect by remember { mutableStateOf(false) }
    var isShowNotifications by remember { mutableStateOf(true) }
    var scanDuration by remember { mutableFloatStateOf(10f) } // 默认10秒
    var selectedTheme by remember { mutableStateOf(ThemeOption.SYSTEM) }
    
    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings_title))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 0.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // 蓝牙设置区域
            SettingsSectionHeader(
                title = stringResource(R.string.settings_bluetooth),
                icon = Icons.Default.Settings
            )
            
            // 启用蓝牙
            SwitchSettingItem(
                title = stringResource(R.string.settings_bluetooth_enable),
                checked = viewModel.isBluetoothEnabled(),
                onCheckedChange = { /* 跳转到系统蓝牙设置 */ },
                enabled = false // 不可直接切换，需要跳转到系统设置
            )
            
            // 扫描时长设置
            ClickableSettingItem(
                title = stringResource(R.string.settings_scan_duration),
                subtitle = "${scanDuration.toInt()} 秒",
                onClick = { showScanDurationDialog = true }
            )
            
            // 扫描过滤器设置
            ClickableSettingItem(
                title = stringResource(R.string.settings_scan_filter),
                subtitle = getFilterSummary(scanFilterSettings),
                onClick = { showScanFilterDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 通信设置区域
            SettingsSectionHeader(
                title = stringResource(R.string.settings_communication),
                icon = Icons.Default.Settings
            )
            
            // 默认数据格式
            val hexModeState = viewModel.isHexMode.collectAsState()
            SwitchSettingItem(
                title = stringResource(R.string.settings_default_format),
                subtitle = if (hexModeState.value) 
                    stringResource(R.string.chat_hex_mode) 
                else 
                    stringResource(R.string.chat_text_mode),
                checked = hexModeState.value,
                onCheckedChange = { viewModel.toggleHexMode() }
            )
            
            // 自动重连
            SwitchSettingItem(
                title = stringResource(R.string.settings_auto_reconnect),
                checked = isAutoReconnect,
                onCheckedChange = { isAutoReconnect = it }
            )
            
            // 消息提醒
            SwitchSettingItem(
                title = stringResource(R.string.settings_notifications),
                checked = isShowNotifications,
                onCheckedChange = { isShowNotifications = it }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 显示设置区域
            SettingsSectionHeader(
                title = stringResource(R.string.settings_display),
                icon = Icons.Default.Palette
            )
            
            // 主题设置
            ClickableSettingItem(
                title = stringResource(R.string.settings_theme),
                subtitle = when (selectedTheme) {
                    ThemeOption.LIGHT -> stringResource(R.string.settings_theme_light)
                    ThemeOption.DARK -> stringResource(R.string.settings_theme_dark)
                    ThemeOption.SYSTEM -> stringResource(R.string.settings_theme_system)
                },
                onClick = { showThemeDialog = true }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 消息历史区域
            SettingsSectionHeader(
                title = "消息历史管理",
                icon = Icons.Default.Info
            )
            
            // 导出所有设备消息历史
            ClickableSettingItem(
                title = "导出所有设备消息历史",
                subtitle = "将所有设备的消息记录导出到外部存储",
                onClick = { 
                    viewModel.exportAllMessageHistory { paths ->
                        // 导出完成回调
                        if (paths.isNotEmpty()) {
                            Toast.makeText(
                                context, 
                                "已导出 ${paths.size} 个设备的消息历史到: ${paths.first().substringBeforeLast("/")}",
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // 可选：打开导出目录
                            val exportDir = File(paths.first()).parentFile
                            if (exportDir != null && exportDir.exists()) {
                                try {
                                    FileUtils.openFile(context, exportDir, "resource/folder")
                                } catch (e: Exception) {
                                    // 如果无法打开，提示用户路径
                                    Toast.makeText(
                                        context,
                                        "文件已保存到: ${exportDir.absolutePath}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        } else {
                            Toast.makeText(
                                context, 
                                "没有可导出的消息历史",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
            
            // 导出当前设备消息历史（如果有连接的设备）
            val connectedDevice = uiState.connectedDevice
            if (connectedDevice != null) {
                ClickableSettingItem(
                    title = "导出当前设备消息历史",
                    subtitle = "导出设备 ${connectedDevice.name} 的消息记录",
                    onClick = { 
                        viewModel.exportDeviceMessageHistory(connectedDevice.id) { path ->
                            // 导出完成回调
                            if (path != null) {
                                Toast.makeText(
                                    context, 
                                    "已导出消息历史到: $path",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    context, 
                                    "导出失败，该设备可能没有历史消息",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            // 关于区域
            SettingsSectionHeader(
                title = stringResource(R.string.settings_about),
                icon = Icons.Default.Info
            )
            
            // 版本信息
            ClickableSettingItem(
                title = stringResource(R.string.settings_version),
                subtitle = "1.0.0",
                onClick = { showAboutDialog = true }
            )
            
            // Git信息
            ClickableSettingItem(
                title = stringResource(R.string.settings_git_info),
                subtitle = "${BuildConfig.GIT_COMMIT_HASH} - ${BuildConfig.GIT_BRANCH}",
                onClick = { showGitInfoDialog = true }
            )
            
            // 开发者信息
            ClickableSettingItem(
                title = stringResource(R.string.settings_developer),
                subtitle = "GitHub:0xBitTwister",
                onClick = { /* 跳转到开发者信息页 */ }
            )
            
            // 反馈问题
            ClickableSettingItem(
                title = stringResource(R.string.settings_feedback),
                onClick = { /* 跳转到反馈页 */ }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // 主题选择对话框
        if (showThemeDialog) {
            ThemeSelectionDialog(
                selectedTheme = selectedTheme,
                onThemeSelected = { 
                    selectedTheme = it
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }
        
        // 扫描时长设置对话框
        if (showScanDurationDialog) {
            ScanDurationDialog(
                currentDuration = scanDuration,
                onDurationChanged = { 
                    scanDuration = it
                    showScanDurationDialog = false
                },
                onDismiss = { showScanDurationDialog = false }
            )
        }
        
        // 扫描过滤器设置对话框
        if (showScanFilterDialog) {
            ScanFilterDialog(
                currentSettings = scanFilterSettings,
                onSettingsChanged = { newSettings ->
                    viewModel.updateScanFilterSettings(newSettings)
                    showScanFilterDialog = false
                },
                onDismiss = { showScanFilterDialog = false }
            )
        }
        
        // 关于对话框
        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }
        
        // Git信息对话框
        if (showGitInfoDialog) {
            AlertDialog(
                onDismissRequest = { showGitInfoDialog = false },
                title = { Text(stringResource(R.string.settings_git_info_title)) },
                text = {
                    Column {
                        Text("${stringResource(R.string.settings_git_branch)}: ${BuildConfig.GIT_BRANCH}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${stringResource(R.string.settings_git_commit_hash)}: ${BuildConfig.GIT_COMMIT_HASH}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${stringResource(R.string.settings_git_commit_date)}: ${BuildConfig.GIT_COMMIT_DATE}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("${stringResource(R.string.settings_git_commit_message)}: ${BuildConfig.GIT_COMMIT_MESSAGE}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showGitInfoDialog = false }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            )
        }
    }
}

// 获取过滤器摘要信息
@Composable
fun getFilterSummary(settings: ScanFilterSettings): String {
    val activeFilters = mutableListOf<String>()
    
    if (settings.nameFilter.isNotEmpty()) {
        activeFilters.add("名称")
    }
    
    if (settings.addressFilter.isNotEmpty()) {
        activeFilters.add("地址")
    }
    
    if (settings.rawDataFilter.isNotEmpty()) {
        activeFilters.add("原始数据")
    }
    
    if (settings.deviceType != DeviceType.ANY) {
        activeFilters.add("设备类型")
    }
    
    if (settings.excludedManufacturers.isNotEmpty()) {
        activeFilters.add("排除厂商")
    }
    
    if (settings.minRssi > -100) {
        activeFilters.add("信号强度")
    }
    
    if (settings.onlyFavorites) {
        activeFilters.add("仅收藏")
    }
    
    return if (activeFilters.isEmpty()) {
        stringResource(R.string.settings_scan_filter_no_filter)
    } else {
        stringResource(R.string.settings_scan_filter_active, activeFilters.joinToString(", "))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanFilterDialog(
    currentSettings: ScanFilterSettings,
    onSettingsChanged: (ScanFilterSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val nameFilter = remember { mutableStateOf(currentSettings.nameFilter) }
    val addressFilter = remember { mutableStateOf(currentSettings.addressFilter) }
    val rawDataFilter = remember { mutableStateOf(currentSettings.rawDataFilter) }
    val deviceType = remember { mutableStateOf(currentSettings.deviceType) }
    val minRssi = remember { mutableFloatStateOf(currentSettings.minRssi.toFloat()) }
    val onlyFavorites = remember { mutableStateOf(currentSettings.onlyFavorites) }
    
    // 排除的厂商列表
    val manufacturers = listOf("Apple", "Microsoft", "Samsung", "Google", "Bluetooth Mesh", "Beacons")
    val excludedManufacturers = remember { 
        mutableStateListOf<String>().apply { 
            addAll(currentSettings.excludedManufacturers) 
        } 
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f) // 限制高度为屏幕的90%
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 标题和内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = stringResource(R.string.settings_scan_filter),
                        style = MaterialTheme.typography.titleLarge
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 名称过滤 - 单独一行
                    Text(
                        text = stringResource(R.string.settings_scan_filter_name),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 名称过滤
                    OutlinedTextField(
                        value = nameFilter.value,
                        onValueChange = { nameFilter.value = it },
                        label = { Text("设备名称") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (nameFilter.value.isNotEmpty()) {
                                IconButton(onClick = { nameFilter.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 地址过滤 - 单独一行
                    Text(
                        text = "MAC地址过滤",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 地址过滤
                    OutlinedTextField(
                        value = addressFilter.value,
                        onValueChange = { addressFilter.value = it },
                        label = { Text("MAC地址") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (addressFilter.value.isNotEmpty()) {
                                IconButton(onClick = { addressFilter.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 原始广告数据过滤 - 单独一行
                    Text(
                        text = stringResource(R.string.settings_scan_filter_raw_data),
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 原始广告数据过滤
                    OutlinedTextField(
                        value = rawDataFilter.value,
                        onValueChange = { rawDataFilter.value = it },
                        label = { Text("十六进制数据") },
                        leadingIcon = { Text("0x", modifier = Modifier.padding(start = 8.dp)) },
                        trailingIcon = {
                            if (rawDataFilter.value.isNotEmpty()) {
                                IconButton(onClick = { rawDataFilter.value = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "清除")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // 设备类型 - 单独一行
                    Text(
                        text = stringResource(R.string.settings_scan_filter_device_type),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 使用下拉菜单选择设备类型
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var expanded by remember { mutableStateOf(false) }
                        val deviceTypeName = when (deviceType.value) {
                            DeviceType.ANY -> "任何类型"
                            DeviceType.LE_ONLY -> "仅低功耗蓝牙 (BLE)"
                            DeviceType.CLASSIC_ONLY -> "仅传统蓝牙设备"
                            DeviceType.DUAL_MODE -> "双模式设备"
                        }
                        
                        OutlinedTextField(
                            value = deviceTypeName,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "选择设备类型"
                                    )
                                }
                            }
                        )
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.9f)
                        ) {
                            DeviceType.values().forEach { type ->
                                val typeName = when (type) {
                                    DeviceType.ANY -> "任何类型"
                                    DeviceType.LE_ONLY -> "仅低功耗蓝牙 (BLE)"
                                    DeviceType.CLASSIC_ONLY -> "仅传统蓝牙设备"
                                    DeviceType.DUAL_MODE -> "双模式设备"
                                }
                                
                                DropdownMenuItem(
                                    text = { Text(typeName) },
                                    onClick = {
                                        deviceType.value = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 信号强度 - 单独一行
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.settings_scan_filter_rssi),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Text(
                                text = "${minRssi.floatValue.toInt()} dBm",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Slider(
                            value = minRssi.floatValue,
                            onValueChange = { minRssi.floatValue = it },
                            valueRange = -100f..-40f,
                            steps = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    // 排除厂商 - 单独一行
                    Text(
                        text = stringResource(R.string.settings_scan_filter_exclude),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 使用下拉菜单代替流式布局
                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        var expandedManufacturers by remember { mutableStateOf(false) }
                        val selectedCount = excludedManufacturers.size
                        
                        OutlinedTextField(
                            value = if (selectedCount > 0) "已选择${selectedCount}个厂商" else "请选择要排除的厂商",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                IconButton(onClick = { expandedManufacturers = !expandedManufacturers }) {
                                    Icon(
                                        if (expandedManufacturers) Icons.Default.KeyboardArrowUp 
                                        else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "选择要排除的厂商"
                                    )
                                }
                            }
                        )
                        
                        // 厂商多选下拉菜单
                        DropdownMenu(
                            expanded = expandedManufacturers,
                            onDismissRequest = { expandedManufacturers = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 250.dp)
                        ) {
                            // 添加"全选/取消全选"选项
                            val allSelected = manufacturers.size == excludedManufacturers.size &&
                                    manufacturers.isNotEmpty()
                            
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = allSelected,
                                            onCheckedChange = null
                                        )
                                        
                                        Spacer(modifier = Modifier.width(8.dp))
                                        
                                        Text(if (allSelected) "取消全选" else "全选", 
                                             fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = {
                                    if (allSelected) {
                                        // 取消全选
                                        excludedManufacturers.clear()
                                    } else {
                                        // 全选
                                        excludedManufacturers.clear()
                                        excludedManufacturers.addAll(manufacturers)
                                    }
                                },
                                contentPadding = PaddingValues(
                                    start = 12.dp,
                                    end = 12.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                )
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            manufacturers.forEach { manufacturer ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = excludedManufacturers.contains(manufacturer),
                                                onCheckedChange = null  // 会在整行点击时处理
                                            )
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            Text(manufacturer)
                                        }
                                    },
                                    onClick = {
                                        if (excludedManufacturers.contains(manufacturer)) {
                                            excludedManufacturers.remove(manufacturer)
                                        } else {
                                            excludedManufacturers.add(manufacturer)
                                        }
                                        // 不关闭下拉菜单，允许继续选择
                                    },
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                        start = 12.dp,
                                        end = 12.dp,
                                        top = 8.dp,
                                        bottom = 8.dp
                                    )
                                )
                            }
                            
                            // 底部的确认按钮
                            HorizontalDivider()
                            
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                TextButton(onClick = { expandedManufacturers = false }) {
                                    Text("确认", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 仅收藏 - 单独一行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onlyFavorites.value = !onlyFavorites.value }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = onlyFavorites.value,
                            onCheckedChange = { onlyFavorites.value = it }
                        )
                        
                        Text(
                            text = stringResource(R.string.settings_scan_filter_favorites),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                // 底部按钮 - 固定在底部
                Column(
                    modifier = Modifier.wrapContentHeight()
                ) {
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                // 重置所有过滤器
                                nameFilter.value = ""
                                addressFilter.value = ""
                                rawDataFilter.value = ""
                                deviceType.value = DeviceType.ANY
                                excludedManufacturers.clear()
                                minRssi.floatValue = -100f
                                onlyFavorites.value = false
                            }
                        ) {
                            Text(stringResource(R.string.settings_scan_filter_reset))
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                onSettingsChanged(
                                    ScanFilterSettings(
                                        nameFilter = nameFilter.value,
                                        addressFilter = addressFilter.value,
                                        rawDataFilter = rawDataFilter.value,
                                        deviceType = deviceType.value,
                                        excludedManufacturers = excludedManufacturers.toSet(),
                                        minRssi = minRssi.floatValue.toInt(),
                                        onlyFavorites = onlyFavorites.value
                                    )
                                )
                            }
                        ) {
                            Text(stringResource(R.string.confirm))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SwitchSettingItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            enabled = enabled
        )
    }
}

@Composable
fun ClickableSettingItem(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

enum class ThemeOption {
    LIGHT, DARK, SYSTEM
}

@Composable
fun ThemeSelectionDialog(
    selectedTheme: ThemeOption,
    onThemeSelected: (ThemeOption) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    ThemeOption.values().forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = theme == selectedTheme,
                                    onClick = { onThemeSelected(theme) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = theme == selectedTheme,
                                onClick = null
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val (themeName, themeIcon) = when (theme) {
                                ThemeOption.LIGHT -> Pair(
                                    stringResource(R.string.settings_theme_light),
                                    Icons.Default.LightMode
                                )
                                ThemeOption.DARK -> Pair(
                                    stringResource(R.string.settings_theme_dark),
                                    Icons.Default.DarkMode
                                )
                                ThemeOption.SYSTEM -> Pair(
                                    stringResource(R.string.settings_theme_system),
                                    Icons.Default.Palette
                                )
                            }
                            
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = null
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(themeName)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanDurationDialog(
    currentDuration: Float,
    onDurationChanged: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var sliderPosition by remember { mutableFloatStateOf(currentDuration) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_scan_duration),
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "${sliderPosition.toInt()} 秒",
                    style = MaterialTheme.typography.bodyLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    valueRange = 5f..30f,
                    steps = 4
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    androidx.compose.material3.TextButton(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    androidx.compose.material3.Button(
                        onClick = { onDurationChanged(sliderPosition) }
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}

@Composable
fun AboutDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "版本: 1.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "一个功能强大的蓝牙通信工具，支持与蓝牙BLE设备进行通信。",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "开发者: GitHub:0xBitTwister",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    androidx.compose.material3.Button(
                        onClick = onDismiss
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
} 