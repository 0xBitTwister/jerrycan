package com.example.jerrycan.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jerrycan.bluetooth.BluetoothEvent
import com.example.jerrycan.bluetooth.BluetoothService
import com.example.jerrycan.model.BluetoothDevice
import com.example.jerrycan.model.BluetoothLog
import com.example.jerrycan.model.BluetoothMessage
import com.example.jerrycan.model.MessageStatus
import com.example.jerrycan.model.BleMessageType
import com.example.jerrycan.model.LogAction
import com.example.jerrycan.utils.MessageHistoryManager
import com.example.jerrycan.utils.PermissionUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.delay

// 扫描过滤器设置
data class ScanFilterSettings(
    val nameFilter: String = "", // 设备名称过滤
    val addressFilter: String = "", // 设备地址过滤
    val rawDataFilter: String = "", // 原始广播数据过滤
    val deviceType: DeviceType = DeviceType.ANY, // 设备类型
    val excludedManufacturers: Set<String> = setOf(), // 排除的厂商 (Apple, Microsoft, Samsung, Google等)
    val minRssi: Int = -100, // 最小信号强度
    val onlyFavorites: Boolean = false // 仅显示收藏设备
)

// 设备类型枚举
enum class DeviceType {
    ANY, // 任何类型
    LE_ONLY, // 仅BLE设备
    CLASSIC_ONLY, // 仅传统蓝牙设备
    DUAL_MODE // 双模式设备
}

// UI状态数据类
data class BluetoothUiState(
    val deviceList: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val isConnecting: Boolean = false,
    val connectedDevice: BluetoothDevice? = null,
    val messages: Map<String, List<BluetoothMessage>> = emptyMap(),
    val logs: List<BluetoothLog> = emptyList(),
    val errorMessage: String? = null,
    val friendDevices: List<BluetoothDevice> = emptyList(),
    val lastActionMessage: String? = null,
    val servicesUpdated: Long = 0  // 新增：服务更新时间戳，用于UI感知服务更新
)

// 服务信息数据类
data class ServiceInfo(
    val uuid: String,
    val name: String? = null,
    val characteristics: List<CharacteristicInfo> = emptyList()
)

// 特征信息数据类
data class CharacteristicInfo(
    val name: String,
    val uuid: String,
    val properties: String
)

// 日志筛选器
data class LogFilter(
    val deviceId: String? = null,
    val action: com.example.jerrycan.model.LogAction? = null,
    val fromDate: Date? = null,
    val toDate: Date? = null
) {
    companion object {
        val ALL = LogFilter(null, null, null, null)
        val CONNECTION = LogFilter(null, com.example.jerrycan.model.LogAction.CONNECT, null, null)
        val DATA = LogFilter(null, com.example.jerrycan.model.LogAction.SEND_DATA, null, null)
        val SCAN = LogFilter(null, com.example.jerrycan.model.LogAction.SCAN_START, null, null)
    }
}

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {
    
    // 内部服务实例
    val bluetoothService = BluetoothService(application) // 修改为public属性使外部可访问
    
    // 添加消息历史管理器
    private val messageHistoryManager = MessageHistoryManager(application)
    
    // UI状态
    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()
    
    // 添加缓存的服务列表
    private val _cachedServicesByDevice = MutableStateFlow<Map<String, List<com.example.jerrycan.bluetooth.BluetoothService.ServiceInfo>>>(emptyMap())
    
    // 消息输入
    private val _messageInput = MutableStateFlow("")
    val messageInput: StateFlow<String> = _messageInput.asStateFlow()
    
    // 是否为十六进制模式
    private val _isHexMode = MutableStateFlow(false)
    val isHexMode: StateFlow<Boolean> = _isHexMode.asStateFlow()
    
    // 日志筛选
    private val _logFilter = MutableStateFlow<LogFilter?>(null)
    val logFilter: StateFlow<LogFilter?> = _logFilter.asStateFlow()
    
    // 扫描过滤器设置
    private val _scanFilterSettings = MutableStateFlow(ScanFilterSettings())
    val scanFilterSettings: StateFlow<ScanFilterSettings> = _scanFilterSettings.asStateFlow()
    
    // 过滤后的设备列表
    private val _filteredDeviceList = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val filteredDeviceList: StateFlow<List<BluetoothDevice>> = _filteredDeviceList.asStateFlow()
    
    // 收藏的设备列表
    private val _favoriteDevices = MutableStateFlow<Set<String>>(emptySet()) // 存储设备ID
    val favoriteDevices: StateFlow<Set<String>> = _favoriteDevices.asStateFlow()
    
    // 添加好友设备和在线设备状态
    private val _friendDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val friendDevices: StateFlow<List<BluetoothDevice>> = _friendDevices.asStateFlow()
    
    private val _onlineDeviceIds = MutableStateFlow<List<String>>(emptyList())
    val onlineDeviceIds: StateFlow<List<String>> = _onlineDeviceIds.asStateFlow()
    
    // 添加临时设备ID状态，用于菜单导航
    private val _tempDeviceId = MutableStateFlow<String?>(null)
    val tempDeviceId: StateFlow<String?> = _tempDeviceId.asStateFlow()
    
    // SharedPreferences 键
    private val PREFS_NAME = "bluetooth_prefs"
    private val FRIEND_DEVICES_KEY = "friend_devices"
    private val MESSAGES_KEY = "messages"
    
    // 添加一个暂存的NavController实例，用于直接导航
    private var navController: androidx.navigation.NavController? = null
    
    // 在BluetoothViewModel类中，添加一个存储设备服务的Map
    private val _deviceServices = MutableStateFlow<Map<String, List<ServiceInfo>>>(emptyMap())
    val deviceServices: StateFlow<Map<String, List<ServiceInfo>>> = _deviceServices.asStateFlow()
    
    // 添加服务发现状态标志
    private val servicesDiscoveryInProgress = mutableMapOf<String, Boolean>()
    
    // 添加服务发现时间戳追踪，避免短时间内反复刷新
    private val lastServicesDiscoveryTime = mutableMapOf<String, Long>()
    
    // 缓存超时设置 (毫秒)
    private val SERVICES_CACHE_TIMEOUT = 60000L // 1分钟  
    
    init {
        // 加载保存的好友设备
        loadFriendDevicesInternal()
        
        // 加载所有设备消息历史
        loadMessageHistory()
        
        // 监听蓝牙服务状态变化
        viewModelScope.launch {
            // 设备列表
            launch {
                bluetoothService.deviceList.collectLatest { devices ->
                    _uiState.update { it.copy(deviceList = devices) }
                    
                    // 更新在线设备ID列表
                    _onlineDeviceIds.value = devices.map { it.id }.toSet().toList()
                    
                    // 更新好友设备的在线状态
                    updateFriendDevicesOnlineStatus(devices)
                    
                    applyDeviceFilters()
                }
            }
            
            // 扫描状态
            launch {
                bluetoothService.scanningState.collectLatest { isScanning ->
                    _uiState.update { it.copy(isScanning = isScanning) }
                }
            }
            
            // 已连接设备
            launch {
                bluetoothService.connectedDevice.collectLatest { device ->
                    _uiState.update { it.copy(connectedDevice = device) }
                }
            }
            
            // 消息记录
            launch {
                bluetoothService.messages.collectLatest { messageMap ->
                    _uiState.update { it.copy(messages = messageMap) }
                }
            }
            
            // 操作日志
            launch {
                bluetoothService.logs.collectLatest { logs ->
                    // 应用筛选器
                    val filter = _logFilter.value
                    val filteredLogs = if (filter != null) {
                        logs.filter { log ->
                            (filter.deviceId == null || log.deviceId == filter.deviceId) &&
                            (filter.action == null || log.action == filter.action) &&
                            (filter.fromDate == null || log.timestamp.after(filter.fromDate)) &&
                            (filter.toDate == null || log.timestamp.before(filter.toDate))
                        }
                    } else {
                        logs
                    }
                    
                    _uiState.update { it.copy(logs = filteredLogs) }
                }
            }
            
            // 事件监听
            launch {
                bluetoothService.events.collectLatest { event ->
                    when (event) {
                        is BluetoothEvent.ScanStarted -> {
                            _uiState.update { it.copy(isScanning = true) }
                        }
                        
                        is BluetoothEvent.ScanFinished -> {
                            _uiState.update { it.copy(isScanning = false, deviceList = event.devices) }
                            applyDeviceFilters()
                        }
                        
                        is BluetoothEvent.ScanFailed -> {
                            _uiState.update { it.copy(isScanning = false, errorMessage = event.message) }
                        }
                        
                        is BluetoothEvent.Connecting -> {
                            _uiState.update { it.copy(isConnecting = true) }
                        }
                        
                        is BluetoothEvent.Connected -> {
                            _uiState.update { 
                                it.copy(
                                    isConnecting = false, 
                                    connectedDevice = event.device,
                                    errorMessage = null
                                ) 
                            }
                        }
                        
                        is BluetoothEvent.ConnectionFailed -> {
                            _uiState.update { 
                                it.copy(
                                    isConnecting = false,
                                    errorMessage = event.message
                                )
                            }
                        }
                        
                        is BluetoothEvent.Disconnected -> {
                            _uiState.update { 
                                it.copy(
                                    connectedDevice = null,
                                    errorMessage = null
                                )
                            }
                        }
                        
                        is BluetoothEvent.Error -> {
                            _uiState.update { it.copy(errorMessage = event.message) }
                        }
                        
                        else -> { /* 其他事件暂不处理 */ }
                    }
                }
            }
        }
        
        // 在应用程序退出前确保消息持久化
        // 注意：这在ViewModel销毁时执行，但不保证在应用突然关闭时被调用
        registerPersistenceHandler()
    }
    
    /**
     * 注册应用退出时的持久化处理
     */
    private fun registerPersistenceHandler() {
        // 这里我们可以添加一些逻辑来在ViewModel被销毁时保存数据
    }
    
    /**
     * 保存消息到文件系统
     */
    private fun saveMessageToFile(message: BluetoothMessage) {
        viewModelScope.launch {
            try {
                messageHistoryManager.saveMessage(message)
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "保存消息到文件失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 保存设备所有消息到文件系统
     */
    private fun saveDeviceMessagesToFile(deviceId: String) {
        viewModelScope.launch {
            try {
                // 获取设备的所有消息
                val messages = _uiState.value.messages[deviceId] ?: emptyList()
                if (messages.isNotEmpty()) {
                    messageHistoryManager.saveAllMessages(deviceId, messages)
                    Log.d("BluetoothViewModel", "已将 ${messages.size} 条消息保存到设备 $deviceId 的历史文件")
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "保存设备消息到文件失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 从文件系统加载消息历史 - 公开方法用于在界面中调用强制刷新
     */
    fun loadMessageHistory() {
        viewModelScope.launch {
            try {
                Log.d("BluetoothViewModel", "手动触发消息历史加载")
                // 加载所有设备消息
                val allMessages = messageHistoryManager.loadAllMessages()
                
                // 更新UI状态
                if (allMessages.isNotEmpty()) {
                    // 设置蓝牙服务中的消息
                    bluetoothService.setMessages(allMessages)
                    
                    // 更新UI状态
                    _uiState.update { it.copy(messages = allMessages) }
                    
                    Log.d("BluetoothViewModel", "已从文件加载 ${allMessages.size} 个设备的消息历史")
                    // 添加一些详细日志
                    allMessages.forEach { (deviceId, messages) ->
                        Log.d("BluetoothViewModel", "设备 $deviceId: ${messages.size} 条消息")
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "加载消息历史出错: ${e.message}", e)
            }
        }
    }
    
    /**
     * 加载特定设备的消息历史
     */
    fun loadDeviceMessageHistory(deviceId: String) {
        viewModelScope.launch {
            try {
                // 从文件加载该设备的历史消息
                val deviceMessages = messageHistoryManager.loadDeviceMessages(deviceId)
                
                if (deviceMessages.isNotEmpty()) {
                    // 更新消息列表
                    val currentMessages = _uiState.value.messages.toMutableMap()
                    currentMessages[deviceId] = deviceMessages
                    
                    // 更新UI状态
                    _uiState.update { it.copy(messages = currentMessages) }
                    
                    // 更新蓝牙服务中的消息
                    bluetoothService.setDeviceMessages(deviceId, deviceMessages)
                    
                    Log.d("BluetoothViewModel", "已加载设备 $deviceId 的 ${deviceMessages.size} 条历史消息")
                } else {
                    Log.d("BluetoothViewModel", "设备 $deviceId 没有历史消息记录")
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "加载设备 $deviceId 消息历史出错: ${e.message}", e)
            }
        }
    }
    
    /**
     * 分页加载特定设备的消息历史
     * @param deviceId 设备ID
     * @param offset 起始位置
     * @param limit 每页加载的消息数量
     * @param callback 加载完成后的回调，传递加载的消息列表和是否还有更多消息
     */
    fun loadDeviceMessageHistoryWithPaging(deviceId: String, offset: Int, limit: Int, callback: (List<BluetoothMessage>, Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 从文件分页加载该设备的历史消息
                val deviceMessages = messageHistoryManager.loadDeviceMessagesWithPaging(deviceId, offset, limit)
                
                if (deviceMessages.isNotEmpty()) {
                    // 获取当前设备的所有消息
                    val currentDeviceMessages = _uiState.value.messages[deviceId] ?: emptyList()
                    
                    // 如果是初始加载（offset=0），则直接替换；否则合并消息并排序
                    val mergedMessages = if (offset == 0) {
                        deviceMessages
                    } else {
                        // 合并现有消息和新加载的消息，确保不重复
                        val existingMessageIds = currentDeviceMessages.map { it.id }.toSet()
                        val uniqueNewMessages = deviceMessages.filter { it.id !in existingMessageIds }
                        
                        (currentDeviceMessages + uniqueNewMessages).sortedBy { it.timestamp }
                    }
                    
                    // 更新消息列表
                    val currentMessages = _uiState.value.messages.toMutableMap()
                    currentMessages[deviceId] = mergedMessages
                    
                    // 更新UI状态
                    _uiState.update { it.copy(messages = currentMessages) }
                    
                    // 确定是否还有更多消息可加载
                    val totalLoaded = mergedMessages.size
                    val hasMore = deviceMessages.size >= limit
                    
                    Log.d("BluetoothViewModel", "已加载设备 $deviceId 的 ${deviceMessages.size} 条分页消息，总共 $totalLoaded 条，还有更多: $hasMore")
                    
                    // 返回加载的消息和是否还有更多
                    callback(deviceMessages, hasMore)
                } else {
                    Log.d("BluetoothViewModel", "设备 $deviceId 在位置 $offset 没有更多历史消息")
                    callback(emptyList(), false)
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "分页加载设备 $deviceId 消息历史出错: ${e.message}", e)
                callback(emptyList(), false)
            }
        }
    }
    
    /**
     * 确保所有消息都已持久化存储
     * 在应用即将退出时调用
     */
    fun ensureMessagePersistence() {
        messageHistoryManager.ensurePersistence()
    }
    
    /**
     * 从SharedPreferences加载好友设备
     * @param forceRefresh 是否强制刷新，忽略缓存
     */
    fun loadFriendDevices(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            try {
                val sharedPrefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val deviceListJson = sharedPrefs.getString(FRIEND_DEVICES_KEY, null)
                
                if (deviceListJson != null) {
                    val deviceList = Json.decodeFromString<List<BluetoothDevice>>(deviceListJson)
                    _friendDevices.value = deviceList
                    
                    // 如果强制刷新，也更新UI状态
                    if (forceRefresh) {
                        _uiState.update { it.copy(friendDevices = deviceList) }
                    }
                    
                    Log.d("BluetoothViewModel", "Loaded ${deviceList.size} friend devices")
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error loading friend devices: ${e.message}")
            }
        }
    }
    
    /**
     * 从SharedPreferences加载好友设备 (私有方法，初始化时调用)
     */
    private fun loadFriendDevicesInternal() {
        viewModelScope.launch {
            try {
                val sharedPrefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val deviceListJson = sharedPrefs.getString(FRIEND_DEVICES_KEY, null)
                
                if (deviceListJson != null) {
                    val deviceList = Json.decodeFromString<List<BluetoothDevice>>(deviceListJson)
                    _friendDevices.value = deviceList
                    Log.d("BluetoothViewModel", "Loaded ${deviceList.size} friend devices")
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error loading friend devices: ${e.message}")
            }
        }
    }
    
    // 标准化设备ID格式方法，用于统一各处的ID格式
    private fun normalizeId(id: String): String = id.uppercase().replace("-", ":")
    
    // 添加好友设备
    fun addFriendDevice(device: BluetoothDevice) {
        viewModelScope.launch {
            // 确保不重复添加
            if (_friendDevices.value.none { it.id == device.id }) {
                _friendDevices.update { currentList ->
                    val newList = currentList.toMutableList()
                    newList.add(device)
                    newList
                }
                
                // 持久化保存
                saveFriendDevices()
                
                // 更新在线设备状态
                updateOnlineDevices()
                
                // 更新状态
                _uiState.update { currentState ->
                    currentState.copy(
                        lastActionMessage = "已添加设备 ${device.name} 到好友列表"
                    )
                }
                
                // 将添加的设备标记为在线，优化用户体验
                _onlineDeviceIds.update { current ->
                    if (!current.contains(device.id)) {
                        current + device.id
                    } else {
                        current
                    }
                }
            }
        }
    }
    
    // 移除好友设备
    fun removeFriendDevice(deviceId: String) {
        viewModelScope.launch {
            val device = _friendDevices.value.find { it.id == deviceId }
            
            _friendDevices.update { currentList ->
                currentList.filter { it.id != deviceId }
            }
            
            // 持久化保存
            saveFriendDevices()
            
            // 更新状态
            device?.let {
                _uiState.update { currentState ->
                    currentState.copy(
                        lastActionMessage = "已从好友列表移除设备 ${it.name}"
                    )
                }
            }
        }
    }
    
    // 检查设备是否为好友
    fun isDeviceFriend(deviceId: String): Boolean {
        return _friendDevices.value.any { it.id == deviceId }
    }
    
    // 检查设备是否在线
    fun isDeviceOnline(deviceId: String): Boolean {
        return _onlineDeviceIds.value.contains(deviceId)
    }
    
    // 更新好友设备的在线状态
    private fun updateFriendDevicesOnlineStatus(onlineDevices: List<BluetoothDevice>) {
        val onlineDeviceIds = onlineDevices.map { it.id }.toSet().toList()
        val updatedFriends = _friendDevices.value.map { friendDevice ->
            val matchingOnlineDevice = onlineDevices.find { it.id == friendDevice.id }
            if (matchingOnlineDevice != null) {
                // 更新在线设备的信息（信号强度等）
                matchingOnlineDevice
            } else {
                // 保留离线设备的信息，但可以标记为未连接
                friendDevice.copy(isConnected = false)
            }
        }
        _friendDevices.value = updatedFriends
        _uiState.update { it.copy(friendDevices = updatedFriends) }
    }
    
    // 刷新好友设备状态
    fun refreshFriendDevicesStatus() {
        viewModelScope.launch {
            // 启动扫描以更新设备状态
            startScan(5000) // 扫描5秒钟
            
            // 更新在线设备列表
            updateOnlineDevices()
        }
    }
    
    // 更新在线设备ID列表
    private fun updateOnlineDevices() {
        viewModelScope.launch {
            val currentDeviceIds = _uiState.value.deviceList.map { it.id }
            _onlineDeviceIds.value = currentDeviceIds
        }
    }
    
    // 添加或更新设备到设备列表
    private fun addOrUpdateDevice(device: BluetoothDevice) {
        val currentDevices = _uiState.value.deviceList.toMutableList()
        val index = currentDevices.indexOfFirst { it.id == device.id }
        
        if (index >= 0) {
            // 更新现有设备
            currentDevices[index] = device
        } else {
            // 添加新设备
            currentDevices.add(device)
        }
        
        _uiState.update { it.copy(deviceList = currentDevices) }
    }
    
    /**
     * 保存好友设备到SharedPreferences
     */
    private fun saveFriendDevices() {
        viewModelScope.launch {
            try {
                val sharedPrefs = getApplication<Application>().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val deviceListJson = Json.encodeToString(_friendDevices.value)
                sharedPrefs.edit().putString(FRIEND_DEVICES_KEY, deviceListJson).apply()
                
                Log.d("BluetoothViewModel", "Saved ${_friendDevices.value.size} friend devices")
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "Error saving friend devices: ${e.message}")
            }
        }
    }
    
    // 应用设备过滤器，更新过滤后的设备列表
    private fun applyDeviceFilters() {
        val filters = _scanFilterSettings.value
        val devices = _uiState.value.deviceList
        
        // 应用各种过滤条件
        val filteredList = devices.filter { device ->
            var matches = true
            
            // 名称过滤
            if (filters.nameFilter.isNotEmpty()) {
                matches = matches && device.name.contains(filters.nameFilter, ignoreCase = true)
            }
            
            // 地址过滤
            if (filters.addressFilter.isNotEmpty()) {
                matches = matches && device.address.contains(filters.addressFilter, ignoreCase = true)
            }
            
            // 原始数据过滤
            if (filters.rawDataFilter.isNotEmpty()) {
                matches = matches && device.rawData.contains(filters.rawDataFilter, ignoreCase = true)
            }
            
            // 排除指定厂商
            if (filters.excludedManufacturers.isNotEmpty()) {
                // 这里简单用设备名称判断，实际应该根据厂商ID判断
                val excluded = filters.excludedManufacturers.any { manufacturer ->
                    device.name.contains(manufacturer, ignoreCase = true)
                }
                matches = matches && !excluded
            }
            
            // 信号强度过滤
            matches = matches && device.rssi >= filters.minRssi
            
            // 仅显示收藏设备
            if (filters.onlyFavorites) {
                matches = matches && _favoriteDevices.value.contains(device.id)
            }
            
            matches
        }
        
        // 保持设备的原始顺序，不按发现时间排序
        _filteredDeviceList.value = filteredList
    }
    
    // 获取当前扫描过滤器设置
    fun getScanFilterSettings(): ScanFilterSettings {
        return _scanFilterSettings.value
    }
    
    // 更新扫描过滤器设置
    fun updateScanFilterSettings(settings: ScanFilterSettings) {
        _scanFilterSettings.value = settings
    }
    
    // 切换设备收藏状态
    fun toggleFavoriteDevice(deviceId: String) {
        val currentFavorites = _favoriteDevices.value.toMutableSet()
        
        if (currentFavorites.contains(deviceId)) {
            currentFavorites.remove(deviceId)
        } else {
            currentFavorites.add(deviceId)
        }
        
        _favoriteDevices.value = currentFavorites
        
        // 更新过滤后的设备列表
        if (_scanFilterSettings.value.onlyFavorites) {
            applyDeviceFilters()
        }
    }
    
    // 检查设备是否被收藏
    fun isDeviceFavorite(deviceId: String): Boolean {
        return _favoriteDevices.value.contains(deviceId)
    }
    
    // 检查蓝牙权限
    fun checkBluetoothPermissions(): Boolean {
        return PermissionUtils.hasRequiredBluetoothPermissions(getApplication())
    }
    
    // 检查蓝牙是否启用
    fun isBluetoothEnabled(): Boolean {
        return bluetoothService.isBluetoothEnabled()
    }
    
    // 开始扫描设备
    fun startScan(durationMs: Long = 10000) {
        bluetoothService.startScan(durationMs)
    }
    
    // 停止扫描
    fun stopScan() {
        bluetoothService.stopScan()
    }
    
    // 直接设置已连接设备（不执行实际的BLE连接）
    fun setConnectedDevice(device: BluetoothDevice) {
        Log.d("BluetoothViewModel", "直接设置已连接设备: ${device.name} (${device.id})")
        
        // 确保设备被标记为已连接
        val connectedDevice = if (!device.isConnected) device.copy(isConnected = true) else device
        
        // 更新设备列表
        addOrUpdateDevice(connectedDevice)
        
        // 设置连接设备
        _uiState.update { it.copy(
            connectedDevice = connectedDevice,
            isConnecting = false
        )}
        
        // 记录日志
        val log = BluetoothLog(
            id = UUID.randomUUID().toString(),
            deviceId = device.id,
            deviceName = device.name,
            action = LogAction.CONNECT,
            details = "设置设备为已连接状态"
        )
        addLog(log)
    }
    
    // 连接设备
    fun connectDevice(device: BluetoothDevice) {
        Log.d("BluetoothViewModel", "连接设备: ${device.name} (${device.id})")
        
        viewModelScope.launch {
            try {
                // 检查是否已有连接的设备且不是当前要连接的设备
                val currentDevice = _uiState.value.connectedDevice
                if (currentDevice != null && currentDevice.id != device.id) {
                    Log.d("BluetoothViewModel", "先断开当前连接的设备: ${currentDevice.name}")
                    // 断开当前连接
                    disconnectDevice()
                    
                    // 等待断开连接完成
                    delay(300)
                }
                
                // 更新UI状态
                _uiState.update { it.copy(
                    isConnecting = true,
                    errorMessage = null
                )}
                
                // 尝试连接设备
                bluetoothService.connectDevice(device)
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "连接设备失败", e)
                
                _uiState.update { it.copy(
                    isConnecting = false,
                    errorMessage = "连接失败: ${e.message}"
                )}
            }
        }
    }
    
    // 断开设备连接
    fun disconnectDevice() {
        Log.d("BluetoothViewModel", "断开连接请求")
        val deviceId = _uiState.value.connectedDevice?.id ?: return
        
        // 在断开连接前保存设备消息到文件
        saveDeviceMessagesToFile(deviceId)
        
        // 清除设备服务缓存，但不从设备列表删除设备
        bluetoothService.clearDeviceServices(deviceId)
        
        // 触发服务更新时间戳，以便UI组件感知到变化
        _uiState.update { it.copy(servicesUpdated = System.currentTimeMillis()) }
        
        // 更新UI状态
        _uiState.update { it.copy(
            connectedDevice = null,
            isConnecting = false
        )}
        
        // 更新设备列表中的连接状态
        updateDeviceConnectionState(deviceId, false)
        
        // 调用蓝牙服务断开设备
        bluetoothService.disconnectDevice()
    }
    
    // 新增辅助方法，更新设备连接状态而不删除设备
    private fun updateDeviceConnectionState(deviceId: String, isConnected: Boolean) {
        val devices = _uiState.value.deviceList.toMutableList()
        val deviceIndex = devices.indexOfFirst { it.id == deviceId }
        
        if (deviceIndex >= 0) {
            val device = devices[deviceIndex]
            devices[deviceIndex] = device.copy(isConnected = isConnected)
            _uiState.update { it.copy(deviceList = devices) }
            Log.d("BluetoothViewModel", "设备 ${device.name ?: device.id} 连接状态已更新为: $isConnected")
        }
    }
    
    // 更新消息输入
    fun updateMessageInput(message: String) {
        _messageInput.value = message
    }
    
    // 切换十六进制模式
    fun toggleHexMode() {
        _isHexMode.value = !_isHexMode.value
    }
    
    // 将消息添加到设备的消息列表
    private fun addMessageToList(message: BluetoothMessage) {
        val deviceId = message.deviceId
        val currentMessages = _uiState.value.messages.toMutableMap()
        val deviceMessages = currentMessages[deviceId]?.toMutableList() ?: mutableListOf()
        deviceMessages.add(message)
        currentMessages[deviceId] = deviceMessages
        _uiState.update { it.copy(messages = currentMessages) }
        
        // 添加到文件存储
        saveMessageToFile(message)
    }
    
    // 更新消息状态
    private fun updateMessageStatus(messageId: String, status: MessageStatus) {
        val currentMessages = _uiState.value.messages.toMutableMap()
        
        currentMessages.forEach { (deviceId, messages) ->
            val messagesList = messages.toMutableList()
            val messageIndex = messagesList.indexOfFirst { it.id == messageId }
            
            if (messageIndex >= 0) {
                val updatedMessage = messagesList[messageIndex].copy(status = status)
                messagesList[messageIndex] = updatedMessage
                currentMessages[deviceId] = messagesList
            }
        }
        
        _uiState.update { it.copy(messages = currentMessages) }
    }
    
    // 添加日志记录
    private fun addLog(log: BluetoothLog) {
        val currentLogs = _uiState.value.logs.toMutableList()
        currentLogs.add(0, log) // 添加到列表头部
        _uiState.update { it.copy(logs = currentLogs) }
    }
    
    // 发送消息
    fun sendMessage(characteristicUuid: String? = null, serviceUuid: String? = null) {
        viewModelScope.launch {
            val connectedDeviceId = _uiState.value.connectedDevice?.id ?: return@launch
            val message = messageInput.value.trim()
            
            if (message.isEmpty()) return@launch
            
            // 生成消息ID
            val messageId = UUID.randomUUID().toString()
            // 消息类型
            val messageType = if (characteristicUuid != null) {
                if (serviceUuid != null && isCharacteristicWriteNoResponse(serviceUuid, characteristicUuid)) {
                    BleMessageType.WRITE_NO_RESPONSE
                } else {
                    BleMessageType.WRITE
                }
            } else {
                BleMessageType.UNKNOWN
            }

            // 构建完整的UUID信息
            val targetUuid = if (characteristicUuid != null) {
                // 使用特征UUID作为sourceUuid，这样UI能够显示目标特征
                characteristicUuid
            } else {
                // 如果没有明确指定特征，则尝试使用默认通信特征
                getDefaultCharacteristicUuid(connectedDeviceId)
            }
            
            // 记录UUID信息到日志
            Log.d("BluetoothViewModel", "消息目标特征UUID: $targetUuid, 服务UUID: $serviceUuid")
            
            // 创建消息对象
            val bluetoothMessage = BluetoothMessage(
                id = messageId,
                deviceId = connectedDeviceId,
                content = message,
                timestamp = Date(),
                isIncoming = false,
                isHex = _isHexMode.value,
                status = MessageStatus.SENDING,
                sourceUuid = targetUuid,
                messageType = messageType
            )
            
            // 添加到消息列表
            addMessageToList(bluetoothMessage)
            
            // 重置输入框
            _messageInput.value = ""
            
            // 发送消息
            bluetoothService.sendMessage(
                message, 
                connectedDeviceId, 
                _isHexMode.value,
                characteristicUuid,
                serviceUuid,
                onComplete = { success ->
                    // 在UI线程中更新状态
                    viewModelScope.launch {
                        val newStatus = if (success) MessageStatus.SENT else MessageStatus.FAILED
                        Log.d("BluetoothViewModel", "更新消息状态: ID=$messageId, 状态=$newStatus, 成功=$success")
                        updateMessageStatus(messageId, newStatus)
                        
                        // 如果发送失败，记录错误日志
                        if (!success) {
                            addLog(
                                BluetoothLog(
                                    id = UUID.randomUUID().toString(),
                                    deviceId = connectedDeviceId,
                                    deviceName = _uiState.value.connectedDevice?.name ?: "",
                                    action = LogAction.ERROR,
                                    details = "发送消息失败: $message"
                                )
                            )
                        }
                    }
                }
            )
            
            // 添加发送日志
            addLog(
                BluetoothLog(
                    id = UUID.randomUUID().toString(),
                    deviceId = connectedDeviceId,
                    deviceName = _uiState.value.connectedDevice?.name ?: "",
                    action = LogAction.SEND_DATA,
                    details = "发送数据: $message ${if (characteristicUuid != null) "(特征: $characteristicUuid)" else ""}"
                )
            )
        }
    }
    
    /**
     * 获取默认的通信特征UUID
     * 对于Nordic UART服务，使用TX特征
     */
    private fun getDefaultCharacteristicUuid(deviceId: String): String? {
        val services = getDeviceServices(deviceId)
        
        // 首先尝试Nordic UART TX特征
        val nordicUartService = services.find { service -> 
            service.uuid.lowercase() == "6e400001-b5a3-f393-e0a9-e50e24dcca9e" 
        }
        
        if (nordicUartService != null) {
            val txCharacteristic = nordicUartService.characteristics.find { char ->
                char.uuid.lowercase() == "6e400002-b5a3-f393-e0a9-e50e24dcca9e" // TX特征
            }
            
            if (txCharacteristic != null) {
                return txCharacteristic.uuid
            }
        }
        
        // 如果没有找到特定的特征，返回null
        return null
    }

    /**
     * 检查特征是否支持无响应写入
     */
    private fun isCharacteristicWriteNoResponse(serviceUuid: String, characteristicUuid: String): Boolean {
        val services = getDeviceServices(_uiState.value.connectedDevice?.id ?: return false)
        val service = services.find { it.uuid == serviceUuid } ?: return false
        val characteristic = service.characteristics.find { it.uuid == characteristicUuid } ?: return false
        
        return characteristic.properties.contains("WRITE_NO_RESPONSE", ignoreCase = true)
    }
    
    // 清空聊天记录
    fun clearMessages() {
        val deviceId = _uiState.value.connectedDevice?.id ?: return
        bluetoothService.clearMessages(deviceId)
    }
    
    // 清空指定设备的聊天记录
    fun clearMessages(deviceId: String) {
        bluetoothService.clearMessages(deviceId)
        
        // 更新UI状态
        val currentMessages = _uiState.value.messages.toMutableMap()
        currentMessages[deviceId] = emptyList()
        _uiState.update { it.copy(messages = currentMessages) }
        
        // 从文件存储中清除消息
        viewModelScope.launch {
            messageHistoryManager.clearDeviceMessages(deviceId)
        }
        
        // 记录日志
        Log.d("BluetoothViewModel", "已清空设备 $deviceId 的聊天记录")
    }
    
    // 设置日志筛选器
    fun setLogFilter(filter: LogFilter?) {
        _logFilter.value = filter
    }
    
    // 清空日志
    fun clearLogs() {
        bluetoothService.clearLogs()
    }
    
    // 根据ID获取设备信息，从多个可能的来源
    fun getDevice(deviceId: String): BluetoothDevice? {
        // 首先尝试从设备列表获取
        val device = _uiState.value.deviceList.find { it.id == deviceId }
        if (device != null) {
            return device
        }
        
        // 尝试从过滤后的设备列表获取
        val filteredDevice = _filteredDeviceList.value.find { it.id == deviceId }
        if (filteredDevice != null) {
            return filteredDevice
        }
        
        // 如果仍然找不到，并且设备列表为空，则可能需要启动扫描
        if (_uiState.value.deviceList.isEmpty() && !_uiState.value.isScanning) {
            startScan(5000) // 启动短时间扫描
        }
        
        return null
    }
    
    // 更新getDeviceServices方法，使用BluetoothService的服务发现功能
    fun getDeviceServices(deviceId: String): List<com.example.jerrycan.bluetooth.BluetoothService.ServiceInfo> {
        if (deviceId.isEmpty()) {
            Log.d("BluetoothViewModel", "设备ID为空，无法获取服务列表")
            return emptyList()
        }
        
        // 检查是否已在进行服务发现 - 如果是，直接返回缓存数据，无论缓存是否为空
        if (servicesDiscoveryInProgress[deviceId] == true) {
            Log.d("BluetoothViewModel", "服务发现正在进行中，使用现有缓存数据")
            return _cachedServicesByDevice.value[deviceId] ?: emptyList()
        }
        
        // 检查缓存中是否有该设备的服务列表
        val cachedServices = _cachedServicesByDevice.value[deviceId]
        if (cachedServices != null && cachedServices.isNotEmpty()) {
            // 检查缓存是否过期
            val lastDiscoveryTime = lastServicesDiscoveryTime[deviceId] ?: 0L
            val currentTime = System.currentTimeMillis()
            val cacheAge = currentTime - lastDiscoveryTime
            
            if (cacheAge < SERVICES_CACHE_TIMEOUT) {
                Log.d("BluetoothViewModel", "使用缓存的服务列表，发现 ${cachedServices.size} 个服务")
                return cachedServices
            } else {
                Log.d("BluetoothViewModel", "服务缓存已过期(${cacheAge}ms)，尝试在后台刷新")
                // 缓存过期但仍返回，同时在后台刷新
                // 使用防抖动机制避免频繁刷新
                if (!servicesDiscoveryInProgress[deviceId]!!) {
                    viewModelScope.launch {
                        refreshDeviceServices(deviceId)
                    }
                }
                return cachedServices
            }
        }
        
        // 防止重复服务发现，增加时间间隔检查
        val lastDiscoveryTime = lastServicesDiscoveryTime[deviceId] ?: 0L
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDiscoveryTime < 500) { // 至少间隔500ms
            Log.d("BluetoothViewModel", "服务发现请求过于频繁，跳过")
            return _cachedServicesByDevice.value[deviceId] ?: emptyList()
        }
        
        // 防止同时发起多个服务发现请求
        synchronized(this) {
            // 二次检查，避免多线程情况下的重复发现
            if (servicesDiscoveryInProgress[deviceId] == true) {
                Log.d("BluetoothViewModel", "另一个线程已经开始服务发现，使用缓存数据")
                return _cachedServicesByDevice.value[deviceId] ?: emptyList()
            }
            
            // 设置标志，防止重复请求
            servicesDiscoveryInProgress[deviceId] = true
            lastServicesDiscoveryTime[deviceId] = System.currentTimeMillis()
        }
        
        // 从蓝牙服务获取服务列表
        try {
            val services = bluetoothService.getDeviceServices(deviceId)
            if (services.isNotEmpty()) {
                Log.d("BluetoothViewModel", "从蓝牙服务获取到 ${services.size} 个服务，更新缓存")
                // 更新缓存
                cacheDeviceServices(deviceId, services)
            }
            return services
        } finally {
            // 无论成功失败，都重置发现状态
            servicesDiscoveryInProgress[deviceId] = false
        }
    }
    
    // 缓存设备服务
    private fun cacheDeviceServices(deviceId: String, services: List<com.example.jerrycan.bluetooth.BluetoothService.ServiceInfo>) {
        Log.d("BluetoothViewModel", "缓存设备 $deviceId 的 ${services.size} 个服务")
        
        viewModelScope.launch {
            val currentCache = _cachedServicesByDevice.value.toMutableMap()
            currentCache[deviceId] = services
            _cachedServicesByDevice.value = currentCache
            
            // 更新发现时间戳
            lastServicesDiscoveryTime[deviceId] = System.currentTimeMillis()
            
            // 更新UI状态，以便UI能感知服务列表变化
            _uiState.update {
                it.copy(servicesUpdated = System.currentTimeMillis())
            }
            
            Log.d("BluetoothViewModel", "服务缓存已更新，当前缓存设备数: ${_cachedServicesByDevice.value.size}")
        }
    }
    
    // 清除特定设备的缓存服务
    fun clearCachedServices(deviceId: String) {
        Log.d("BluetoothViewModel", "清除设备 $deviceId 的服务缓存")
        
        viewModelScope.launch {
            val currentCache = _cachedServicesByDevice.value.toMutableMap()
            currentCache.remove(deviceId)
            _cachedServicesByDevice.value = currentCache
            
            // 清除发现时间戳
            lastServicesDiscoveryTime.remove(deviceId)
            
            Log.d("BluetoothViewModel", "服务缓存已清除")
        }
    }
    
    // 刷新设备服务 (手动触发服务重新发现)
    fun refreshDeviceServices(deviceId: String) {
        if (deviceId.isEmpty()) {
            Log.d("BluetoothViewModel", "设备ID为空，无法刷新服务")
            return
        }
        
        Log.d("BluetoothViewModel", "刷新设备 $deviceId 的服务")
        
        // 检查是否处于正在发现状态
        synchronized(this) {
            if (servicesDiscoveryInProgress[deviceId] == true) {
                Log.d("BluetoothViewModel", "已有服务发现正在进行中，跳过重复请求")
                return
            }
            
            // 检查距离上次刷新的时间间隔，避免频繁刷新
            val lastDiscoveryTime = lastServicesDiscoveryTime[deviceId] ?: 0L
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastDiscoveryTime < 2000) { // 至少间隔2秒
                Log.d("BluetoothViewModel", "刷新请求过于频繁，跳过 (${currentTime - lastDiscoveryTime}ms < 2000ms)")
                return
            }
            
            // 设置发现状态标志
            servicesDiscoveryInProgress[deviceId] = true
            lastServicesDiscoveryTime[deviceId] = currentTime
        }
        
        // 清除缓存服务
        bluetoothService.clearDeviceServices(deviceId)
        
        viewModelScope.launch {
            try {
                // 请求蓝牙服务发现设备服务
                val services = bluetoothService.discoverServices(deviceId)
                if (services.isNotEmpty()) {
                    Log.d("BluetoothViewModel", "发现 ${services.size} 个服务")
                    cacheDeviceServices(deviceId, services)
                } else {
                    Log.d("BluetoothViewModel", "没有发现服务")
                    // 服务为空时也更新缓存，防止UI显示旧数据
                    val currentCache = _cachedServicesByDevice.value.toMutableMap()
                    currentCache[deviceId] = emptyList()
                    _cachedServicesByDevice.value = currentCache
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "服务发现过程中发生错误: ${e.message}")
            } finally {
                // 确保无论成功失败都重置发现状态
                servicesDiscoveryInProgress[deviceId] = false
            }
        }
    }
    
    // 修改为方法而不是属性，避免命名冲突
    fun setTempDeviceId(value: String?) {
        Log.d("BluetoothViewModel", "设置临时设备ID: $value")
        _tempDeviceId.value = value
    }
    
    // 从扫描到的设备列表中删除设备
    fun removeDevice(deviceId: String) {
        Log.d("BluetoothViewModel", "尝试删除设备ID: $deviceId")
        
        // 首先检查设备是否存在于好友列表中
        val isFriend = _friendDevices.value.any { it.id == deviceId }
        if (isFriend) {
            Log.d("BluetoothViewModel", "设备在好友列表中，调用removeFriendDevice")
            removeFriendDevice(deviceId)
            return
        }
        
        // 然后检查设备是否存在于扫描的设备列表中
        val deviceInList = _uiState.value.deviceList.any { it.id == deviceId }
        if (!deviceInList) {
            Log.d("BluetoothViewModel", "设备不在扫描列表中，无法删除")
            return
        }
        
        viewModelScope.launch {
            try {
                // 从设备列表中删除
                val currentDevices = _uiState.value.deviceList.toMutableList()
                val device = currentDevices.find { it.id == deviceId }
                
                Log.d("BluetoothViewModel", "删除前设备列表大小: ${currentDevices.size}")
                val removed = currentDevices.removeIf { it.id == deviceId }
                Log.d("BluetoothViewModel", "设备已移除: $removed, 删除后设备列表大小: ${currentDevices.size}")
                
                // 也从过滤后的设备列表中删除
                val filteredDevices = _filteredDeviceList.value.toMutableList()
                filteredDevices.removeIf { it.id == deviceId }
                _filteredDeviceList.value = filteredDevices
                
                // 更新UI状态
                _uiState.update { currentState ->
                    val newState = currentState.copy(
                        deviceList = currentDevices,
                        lastActionMessage = device?.let { "已从列表移除设备 ${it.name}" } ?: "已移除设备"
                    )
                    Log.d("BluetoothViewModel", "更新UI状态后设备列表大小: ${newState.deviceList.size}")
                    newState
                }
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "删除设备过程中发生错误", e)
            }
        }
    }
    
    // 销毁ViewModel时清理资源
    override fun onCleared() {
        super.onCleared()
        
        // 在ViewModel销毁时确保所有消息已持久化
        messageHistoryManager.ensurePersistence()
        
        // 销毁蓝牙服务
        bluetoothService.destroy()
        
        // 取消所有协程
        viewModelScope.cancel()
    }
    
    // 设置导航控制器
    fun setNavController(controller: androidx.navigation.NavController) {
        this.navController = controller
        Log.d("BluetoothViewModel", "已设置NavController")
    }
    
    // 直接导航方法
    fun navigateToDeviceDetails(deviceId: String) {
        Log.d("BluetoothViewModel", "尝试直接导航到设备详情页面: $deviceId")
        navController?.let { nav ->
            try {
                // 使用Screen.DeviceDetails.createRoute创建正确的路径
                val route = com.example.jerrycan.navigation.Screen.DeviceDetails.createRoute(deviceId)
                Log.d("BluetoothViewModel", "导航到路由: $route")
                nav.navigate(route)
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "导航失败: ${e.message}", e)
            }
        } ?: Log.e("BluetoothViewModel", "NavController为空，无法导航")
    }
    
    /**
     * 获取有消息记录的设备列表，按最后一条消息时间倒序排序
     * @return 设备及其最后一条消息的列表，按时间降序排列
     */
    fun getDevicesWithMessages(): List<Pair<BluetoothDevice, BluetoothMessage?>> {
        val result = mutableListOf<Pair<BluetoothDevice, BluetoothMessage?>>()
        val messageMap = _uiState.value.messages
        val allDevices = (_friendDevices.value + _uiState.value.deviceList).distinctBy { it.id }
        
        Log.d("BluetoothViewModel", "正在构建消息设备列表，消息映射大小: ${messageMap.size}, 设备总数: ${allDevices.size}")

        // 遍历所有设备，找出有消息的设备
        allDevices.forEach { device ->
            val messages = messageMap[device.id]
            if (!messages.isNullOrEmpty()) {
                // 获取设备的最后一条消息
                val lastMessage = messages.maxByOrNull { it.timestamp }
                lastMessage?.let {
                    Log.d("BluetoothViewModel", "设备 ${device.name} (${device.id}) 最后消息: ${it.content.take(20)}, 时间: ${it.timestamp}")
                    result.add(Pair(device, it))
                }
            }
        }

        // 按最后一条消息的时间降序排列
        return result.sortedByDescending { it.second?.timestamp }
    }
    
    /**
     * 删除指定设备的消息历史
     * @param deviceId 要删除消息的设备ID
     * @param callback 删除完成后的回调
     */
    fun deleteDeviceHistory(deviceId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                // 1. 从内存中移除该设备的消息
                val currentMessages = _uiState.value.messages.toMutableMap()
                currentMessages.remove(deviceId)
                _uiState.update { it.copy(messages = currentMessages) }
                
                // 2. 从蓝牙服务中移除该设备的消息缓存
                bluetoothService.clearDeviceMessages(deviceId)
                
                // 3. 删除设备的消息历史文件
                val success = messageHistoryManager.deleteDeviceHistory(deviceId)
                
                // 4. 通知回调结果
                callback(success)
                
                Log.d("BluetoothViewModel", "已删除设备 $deviceId 的消息历史，结果: $success")
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "删除设备 $deviceId 消息历史失败: ${e.message}", e)
                callback(false)
            }
        }
    }
    
    /**
     * 导出设备消息历史
     * @param deviceId 设备ID
     * @param onComplete 导出完成回调，传递导出文件路径，如果导出失败则为null
     */
    fun exportDeviceMessageHistory(deviceId: String, onComplete: (String?) -> Unit) {
        viewModelScope.launch {
            try {
                val filePath = messageHistoryManager.exportDeviceHistory(deviceId)
                
                if (filePath != null) {
                    // 导出成功，更新UI状态
                    _uiState.update { it.copy(
                        lastActionMessage = "消息历史已导出到: $filePath"
                    )}
                    
                    Log.d("BluetoothViewModel", "设备 $deviceId 消息历史导出成功: $filePath")
                } else {
                    // 导出失败
                    _uiState.update { it.copy(
                        lastActionMessage = "导出失败，该设备可能没有历史消息"
                    )}
                    
                    Log.e("BluetoothViewModel", "设备 $deviceId 消息历史导出失败")
                }
                
                // 回调通知
                onComplete(filePath)
                
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "导出设备消息历史异常: ${e.message}", e)
                
                // 更新UI状态
                _uiState.update { it.copy(
                    lastActionMessage = "导出过程中出错: ${e.message}"
                )}
                
                // 回调通知
                onComplete(null)
            }
        }
    }
    
    /**
     * 导出所有设备消息历史
     * @param onComplete 导出完成回调，传递导出文件路径列表
     */
    fun exportAllMessageHistory(onComplete: (List<String>) -> Unit) {
        viewModelScope.launch {
            try {
                val filePaths = messageHistoryManager.exportAllDeviceHistory()
                
                if (filePaths.isNotEmpty()) {
                    // 导出成功，更新UI状态
                    _uiState.update { it.copy(
                        lastActionMessage = "已导出 ${filePaths.size} 个设备的消息历史"
                    )}
                    
                    Log.d("BluetoothViewModel", "所有设备消息历史导出成功: ${filePaths.joinToString()}")
                } else {
                    // 没有消息可导出
                    _uiState.update { it.copy(
                        lastActionMessage = "没有发现可导出的消息历史"
                    )}
                    
                    Log.d("BluetoothViewModel", "没有找到可导出的消息历史")
                }
                
                // 回调通知
                onComplete(filePaths)
                
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "导出所有设备消息历史异常: ${e.message}", e)
                
                // 更新UI状态
                _uiState.update { it.copy(
                    lastActionMessage = "导出过程中出错: ${e.message}"
                )}
                
                // 回调通知
                onComplete(emptyList())
            }
        }
    }
    
    /**
     * 显示Toast消息
     */
    fun showToast(message: String) {
        _uiState.update { it.copy(lastActionMessage = message) }
    }
    
    /**
     * 重试发送失败的消息
     * @param messageId 要重试的消息ID
     */
    fun retryFailedMessage(messageId: String) {
        viewModelScope.launch {
            try {
                val connectedDeviceId = _uiState.value.connectedDevice?.id ?: return@launch
                
                // 查找需要重试的消息
                val messages = _uiState.value.messages[connectedDeviceId] ?: emptyList()
                val messageToRetry = messages.find { it.id == messageId } ?: return@launch
                
                // 检查消息状态是否为FAILED
                if (messageToRetry.status != MessageStatus.FAILED) {
                    Log.d("BluetoothViewModel", "只能重试失败的消息，当前消息状态: ${messageToRetry.status}")
                    return@launch
                }
                
                // 更新状态为发送中
                updateMessageStatus(messageId, MessageStatus.SENDING)
                
                // 获取原始消息的特征UUID和服务UUID
                val characteristicUuid = messageToRetry.sourceUuid
                // 服务UUID需要从特征UUID推断或从特征信息中获取
                val serviceUuid = bluetoothService.getServiceUuidForCharacteristic(connectedDeviceId, characteristicUuid)
                
                // 重新发送消息
                bluetoothService.sendMessage(
                    messageToRetry.content, 
                    connectedDeviceId, 
                    messageToRetry.isHex,
                    characteristicUuid,
                    serviceUuid,
                    onComplete = { success ->
                        viewModelScope.launch {
                            val newStatus = if (success) MessageStatus.SENT else MessageStatus.FAILED
                            Log.d("BluetoothViewModel", "重试消息状态更新: ID=$messageId, 状态=$newStatus, 成功=$success")
                            updateMessageStatus(messageId, newStatus)
                            
                            // 如果发送失败，记录错误日志
                            if (!success) {
                                addLog(
                                    BluetoothLog(
                                        id = UUID.randomUUID().toString(),
                                        deviceId = connectedDeviceId,
                                        deviceName = _uiState.value.connectedDevice?.name ?: "",
                                        action = LogAction.ERROR,
                                        details = "重试发送消息失败: ${messageToRetry.content}"
                                    )
                                )
                            }
                        }
                    }
                )
                
                // 添加重试日志
                addLog(
                    BluetoothLog(
                        id = UUID.randomUUID().toString(),
                        deviceId = connectedDeviceId,
                        deviceName = _uiState.value.connectedDevice?.name ?: "",
                        action = LogAction.SEND_DATA,
                        details = "重试发送数据: ${messageToRetry.content} ${if (characteristicUuid != null) "(特征: $characteristicUuid)" else ""}"
                    )
                )
            } catch (e: Exception) {
                Log.e("BluetoothViewModel", "重试发送消息失败", e)
            }
        }
    }
} 