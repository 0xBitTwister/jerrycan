package com.example.jerrycan.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.jerrycan.model.BluetoothDevice
import com.example.jerrycan.model.BluetoothLog
import com.example.jerrycan.model.BluetoothMessage
import com.example.jerrycan.model.LogAction
import com.example.jerrycan.model.MessageStatus
import com.example.jerrycan.model.BleMessageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean
import com.example.jerrycan.utils.PermissionUtils
import java.util.concurrent.ConcurrentHashMap
import android.bluetooth.BluetoothStatusCodes

class BluetoothService(private val context: Context) {
    private val TAG = "BluetoothService"
    
    // 蓝牙适配器
    private val bluetoothManager: BluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager.adapter
    }
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 状态流
    private val _deviceList = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val deviceList: StateFlow<List<BluetoothDevice>> = _deviceList.asStateFlow()
    
    private val _scanningState = MutableStateFlow(false)
    val scanningState: StateFlow<Boolean> = _scanningState.asStateFlow()
    
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val connectedDevice: StateFlow<BluetoothDevice?> = _connectedDevice.asStateFlow()
    
    private val _messages = MutableStateFlow<Map<String, List<BluetoothMessage>>>(emptyMap())
    val messages: StateFlow<Map<String, List<BluetoothMessage>>> = _messages.asStateFlow()
    
    private val _logs = MutableStateFlow<List<BluetoothLog>>(emptyList())
    val logs: StateFlow<List<BluetoothLog>> = _logs.asStateFlow()
    
    // 事件流
    private val _events = MutableSharedFlow<BluetoothEvent>()
    val events: SharedFlow<BluetoothEvent> = _events.asSharedFlow()
    
    // 扫描相关
    private var scanJob: Job? = null
    private var scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()
    private val scanFilters = mutableListOf<ScanFilter>()
    
    // 添加服务事件类
    data class ServiceInfo(
        val uuid: String,
        val name: String? = null,
        val characteristics: List<CharacteristicInfo> = emptyList()
    )

    data class CharacteristicInfo(
        val name: String,
        val uuid: String,
        val properties: String
    )

    // 存储设备的服务列表
    private val _deviceServices = MutableStateFlow<Map<String, List<ServiceInfo>>>(emptyMap())
    val deviceServices: StateFlow<Map<String, List<ServiceInfo>>> = _deviceServices.asStateFlow()
    
    // 添加活跃GATT连接映射表
    private val activeGattConnections = mutableMapOf<String, android.bluetooth.BluetoothGatt>()

    // 使用中的连接锁
    private val connectingInProgress = AtomicBoolean(false)
    
    // 扫描回调
    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
                return
            }
            
            val scanDevice = result.device
            val deviceName = scanDevice.name ?: "未知设备"
            val deviceAddress = scanDevice.address
            val rssi = result.rssi
            
            // 提取原始广播数据和广告数据
            val scanRecord = result.scanRecord
            val rawData = scanRecord?.bytes?.joinToString("") { 
                String.format("%02X", it) 
            } ?: ""
            
            // 解析广告数据
            val advertisementData = mutableMapOf<String, String>()
            scanRecord?.manufacturerSpecificData?.let { data ->
                for (i in 0 until data.size()) {
                    val manufacturerId = data.keyAt(i)
                    val manufacturerData = data.get(manufacturerId)
                    if (manufacturerData != null) {
                        val hexString = manufacturerData.joinToString("") { 
                            String.format("%02X", it) 
                        }
                        advertisementData["Manufacturer(0x${String.format("%04X", manufacturerId)})"] = hexString
                    }
                }
            }
            
            scanRecord?.serviceData?.forEach { (uuid, data) ->
                val hexString = data.joinToString("") { 
                    String.format("%02X", it) 
                }
                advertisementData["Service(${uuid})"] = hexString
            }
            
            // 创建设备对象
            val device = BluetoothDevice(
                id = deviceAddress,
                name = deviceName,
                address = deviceAddress,
                rssi = rssi,
                isPaired = false,
                isConnected = false,
                discoveryTime = Date(), // 设置当前时间为发现时间
                rawData = if (rawData.isNotEmpty()) "0x$rawData" else "",
                advertisementData = advertisementData
            )
            
            serviceScope.launch {
                // 更新设备列表，避免重复添加
                val currentList = _deviceList.value.toMutableList()
                val existingDeviceIndex = currentList.indexOfFirst { it.id == deviceAddress }
                
                if (existingDeviceIndex >= 0) {
                    // 更新现有设备但保留原始位置，只更新RSSI等信息
                    val existingDevice = currentList[existingDeviceIndex]
                    currentList[existingDeviceIndex] = existingDevice.copy(
                        rssi = rssi,
                        rawData = if (rawData.isNotEmpty()) "0x$rawData" else "",
                        advertisementData = advertisementData
                    )
                } else {
                    // 添加新设备到列表末尾
                    currentList.add(device)
                }
                
                // 保持现有顺序，不进行排序
                _deviceList.value = currentList
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "扫描失败: $errorCode")
            serviceScope.launch {
                _scanningState.value = false
                _events.emit(BluetoothEvent.ScanFailed("扫描失败: 错误码 $errorCode"))
                
                // 记录日志
                addLog(
                    BluetoothLog(
                        id = UUID.randomUUID().toString(),
                        deviceId = "",
                        deviceName = "",
                        action = LogAction.ERROR,
                        details = "扫描失败: 错误码 $errorCode"
                    )
                )
            }
        }
    }
    
    // 检查蓝牙是否支持和启用
    fun isBluetoothSupported(): Boolean {
        return bluetoothAdapter != null
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    // 开始扫描设备
    @SuppressLint("MissingPermission")
    fun startScan(durationMs: Long = 10000) {
        if (!isBluetoothEnabled()) {
            serviceScope.launch {
                _events.emit(BluetoothEvent.Error("蓝牙未启用"))
            }
            return
        }
        
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) 
            != PackageManager.PERMISSION_GRANTED) {
            serviceScope.launch {
                _events.emit(BluetoothEvent.Error("缺少蓝牙扫描权限"))
            }
            return
        }
        
        // 记录扫描开始日志
        addLog(
            BluetoothLog(
                id = UUID.randomUUID().toString(),
                deviceId = "",
                deviceName = "",
                action = LogAction.SCAN_START,
                details = "开始扫描蓝牙设备, 持续时间: ${durationMs}ms"
            )
        )
        
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            try {
                // 不再清空设备列表，而是保留现有设备
                // _deviceList.value = emptyList()  // 删除此行
                _scanningState.value = true
                
                // 开始扫描
                bluetoothAdapter?.bluetoothLeScanner?.startScan(
                    scanFilters,
                    scanSettings,
                    scanCallback
                )
                
                // 发送事件
                _events.emit(BluetoothEvent.ScanStarted(System.currentTimeMillis()))
                
                // 延迟指定时间后停止扫描
                delay(durationMs)
                stopScan()
            } catch (e: Exception) {
                Log.e(TAG, "扫描出错", e)
                _scanningState.value = false
                _events.emit(BluetoothEvent.Error("扫描出错: ${e.message}"))
            }
        }
    }
    
    // 停止扫描
    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) 
            != PackageManager.PERMISSION_GRANTED) {
            return
        }
        
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        _scanningState.value = false
        
        // 记录扫描停止日志
        addLog(
            BluetoothLog(
                id = UUID.randomUUID().toString(),
                deviceId = "",
                deviceName = "",
                action = LogAction.SCAN_STOP,
                details = "停止扫描蓝牙设备, 找到设备数量: ${_deviceList.value.size}"
            )
        )
        
        serviceScope.launch {
            // 不进行排序，保持原有设备顺序
            _events.emit(BluetoothEvent.ScanFinished(deviceList.value))
        }
    }
    
    // 通过ID查找设备的实用方法（支持各种可能的ID格式）
    private fun findDeviceById(deviceId: String): BluetoothDevice? {
        if (deviceId.isEmpty()) {
            Log.d(TAG, "设备ID为空，无法查找设备")
            return null
        }
        
        // 标准化设备ID进行比较
        val normalizedId = normalizeId(deviceId)
        
        // 首先检查当前连接的设备
        val connectedDevice = _connectedDevice.value
        if (connectedDevice != null && normalizeId(connectedDevice.id) == normalizedId) {
            Log.d(TAG, "当前连接的设备匹配ID: $deviceId")
            return connectedDevice
        }
        
        // 然后检查设备列表
        val deviceFromList = _deviceList.value.find { 
            normalizeId(it.id) == normalizedId 
        }
        
        if (deviceFromList != null) {
            Log.d(TAG, "从设备列表找到设备: ${deviceFromList.name}")
            return deviceFromList
        }
        
        // 如果仍然找不到，记录当前设备列表中的所有设备ID进行调试
        if (_deviceList.value.isNotEmpty()) {
            Log.d(TAG, "设备列表内容 (${_deviceList.value.size}个设备):")
            _deviceList.value.forEach {
                Log.d(TAG, "  - 设备ID: ${it.id}, 名称: ${it.name}")
            }
        } else {
            Log.d(TAG, "设备列表为空")
        }
        
        // 检查蓝牙GATT连接的设备
        if (bluetoothGatt != null) {
            val gattDeviceAddress = bluetoothGatt?.device?.address
            if (gattDeviceAddress != null && (gattDeviceAddress == deviceId || normalizeId(gattDeviceAddress) == normalizedId)) {
                Log.d(TAG, "从当前GATT连接找到设备: 地址=$gattDeviceAddress")
                // 创建一个临时设备对象
                return BluetoothDevice(
                    id = gattDeviceAddress,
                    name = bluetoothGatt?.device?.name ?: "未知设备",
                    address = gattDeviceAddress,
                    rssi = 0,
                    isPaired = false,
                    isConnected = true,
                    rawData = "",
                    advertisementData = emptyMap()
                )
            }
        }
        
        // 最后尝试使用原始ID进行直接匹配
        return _deviceList.value.find { it.id == deviceId }
    }
    
    // 标准化ID的辅助方法
    private fun normalizeId(id: String): String {
        return id.replace(":", "").uppercase()
    }
    
    // 存储GATT连接
    private var bluetoothGatt: android.bluetooth.BluetoothGatt? = null
    
    // GATT回调
    private val gattCallback = object : android.bluetooth.BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: android.bluetooth.BluetoothGatt, status: Int, newState: Int) {
            val deviceAddress = gatt.device.address
            val deviceName = gatt.device.name ?: "未知设备"
            
            when (newState) {
                android.bluetooth.BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "成功连接到GATT服务器: $deviceName")
                    
                    // 设置成员变量，确保后续的操作能够访问到GATT连接
                    bluetoothGatt = gatt
                    
                    // 保存到活跃连接Map
                    activeGattConnections[deviceAddress] = gatt
                    
                    // 更新设备状态
                    val device = _deviceList.value.find { it.address == deviceAddress }
                    if (device != null) {
                        val updatedDevice = device.copy(
                            isConnected = true,
                            lastConnected = Date()
                        )
                        
                        serviceScope.launch {
                            // 更新已连接设备
                            _connectedDevice.value = updatedDevice
                            
                            // 更新设备列表
                            val currentList = _deviceList.value.toMutableList()
                            val deviceIndex = currentList.indexOfFirst { it.id == device.id }
                            
                            if (deviceIndex >= 0) {
                                currentList[deviceIndex] = updatedDevice
                                _deviceList.value = currentList
                            }
                            
                            // 发送连接成功事件
                            _events.emit(BluetoothEvent.Connected(updatedDevice))
                            
                            // 清除服务缓存
                            clearDeviceServices(device.id)
                        }
                        
                        // 连接成功后，启动服务发现
                        Log.d(TAG, "设备连接成功，开始服务发现")
                        val result = gatt.discoverServices()
                        Log.d(TAG, "服务发现请求结果: $result")
                    }
                }
                android.bluetooth.BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "与GATT服务器断开连接: $deviceName")
                    
                    // 从活跃连接Map中移除
                    activeGattConnections.remove(deviceAddress)
                    
                    // 关闭GATT连接
                    gatt.close()
                    
                    // 如果当前全局GATT连接是这个设备，也清除它
                    if (bluetoothGatt == gatt) {
                        bluetoothGatt = null
                    }
                    
                    // 更新设备状态
                    val device = _deviceList.value.find { it.address == deviceAddress }
                    if (device != null) {
                        val updatedDevice = device.copy(isConnected = false)
                        
                        serviceScope.launch {
                            // 更新设备列表
                            val currentList = _deviceList.value.toMutableList()
                            val deviceIndex = currentList.indexOfFirst { it.id == device.id }
                            
                            if (deviceIndex >= 0) {
                                currentList[deviceIndex] = updatedDevice
                                _deviceList.value = currentList
                            }
                            
                            // 清除已连接设备（如果是当前连接的设备）
                            if (_connectedDevice.value?.id == device.id) {
                                _connectedDevice.value = null
                            }
                            
                            // 发送断开连接事件
                            _events.emit(BluetoothEvent.Disconnected(updatedDevice))
                            
                            // 记录断开连接日志
                            addLog(
                                BluetoothLog(
                                    id = UUID.randomUUID().toString(),
                                    deviceId = device.id,
                                    deviceName = device.name,
                                    action = LogAction.DISCONNECT,
                                    details = "设备连接已断开: ${device.name} (${device.address})"
                                )
                            )
                        }
                    }
                }
            }
        }
        
        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: android.bluetooth.BluetoothGatt, status: Int) {
            if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                val deviceAddress = gatt.device.address
                // 尝试多种方式找到设备
                var device = _deviceList.value.find { it.address == deviceAddress }
                
                // 如果在设备列表中找不到，检查当前连接的设备
                if (device == null && _connectedDevice.value?.address == deviceAddress) {
                    device = _connectedDevice.value
                    Log.d(TAG, "从当前连接设备获取设备信息: ${device?.name}")
                }
                
                // 如果仍然找不到，创建一个临时设备对象
                if (device == null) {
                    Log.d(TAG, "在设备列表中找不到设备，创建临时设备对象")
                    device = BluetoothDevice(
                        id = deviceAddress,
                        name = gatt.device.name ?: "未知设备",
                        address = deviceAddress,
                        rssi = 0,
                        isPaired = false,
                        isConnected = true,
                        rawData = "",
                        advertisementData = emptyMap()
                    )
                    
                    // 更新设备列表
                    val currentList = _deviceList.value.toMutableList()
                    currentList.add(device)
                    _deviceList.value = currentList
                    
                    // 更新连接设备
                    _connectedDevice.value = device
                }
                
                // 解析服务列表
                val services = parseGattServices(gatt.services)
                Log.d(TAG, "设备 ${device.name} 发现了 ${services.size} 个服务")
                
                // 查找并启用通知特性
                val notifyCharacteristic = findNotifyCharacteristic(gatt)
                if (notifyCharacteristic != null) {
                    val success = enableNotifications(notifyCharacteristic)
                    if (success) {
                        Log.d(TAG, "已启用特性通知: ${notifyCharacteristic.uuid}")
                    } else {
                        Log.e(TAG, "启用特性通知失败: ${notifyCharacteristic.uuid}")
                    }
                }
                
                serviceScope.launch {
                    // 缓存服务列表
                    val currentMap = _deviceServices.value.toMutableMap()
                    currentMap[device.id] = services
                    _deviceServices.value = currentMap
                    
                    // 通知服务发现完成
                    _events.emit(BluetoothEvent.ServicesDiscovered(device.id, services))
                    
                    // 记录服务发现日志
                    addLog(
                        BluetoothLog(
                            id = UUID.randomUUID().toString(),
                            deviceId = device.id,
                            deviceName = device.name,
                            action = LogAction.CONNECT,
                            details = "服务发现完成: 发现 ${services.size} 个服务"
                        )
                    )
                }
            } else {
                Log.w(TAG, "onServicesDiscovered received: $status")
            }
        }
        
        // 特性写入回调
        override fun onCharacteristicWrite(
            gatt: android.bluetooth.BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == android.bluetooth.BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "特性写入成功: ${characteristic.uuid}")
            } else {
                Log.e(TAG, "特性写入失败: ${characteristic.uuid}, 状态: $status")
            }
        }
        
        // 特性值变化回调 - Android 13+方法重载
        @SuppressLint("NewApi")
        override fun onCharacteristicChanged(
            gatt: android.bluetooth.BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic,
            data: ByteArray
        ) {
            val characteristicUuid = characteristic.uuid.toString()
            val serviceUuid = characteristic.service.uuid.toString()
            
            Log.d(TAG, "特性值已更改: $characteristicUuid (服务: $serviceUuid)")
            
            val deviceAddress = gatt.device.address
            val device = _deviceList.value.find { it.address == deviceAddress }
            
            if (device != null) {
                val isNordicUart = characteristicUuid.lowercase() == 
                    "6e400003-b5a3-f393-e0a9-e50e24dcca9e"
                
                // 转换接收到的数据
                var content: String
                var isHex: Boolean
                
                if (isNordicUart) {
                    // 对于Nordic UART服务，首先尝试作为文本处理
                    try {
                        // 检查数据是否为可打印ASCII文本
                        val isAsciiText = data.all { it in 32..126 || it == 10.toByte() || it == 13.toByte() }
                        
                        if (isAsciiText) {
                            // 如果是可打印ASCII，则作为文本处理
                            content = String(data)
                            isHex = false
                        } else {
                            // 如果不是可打印ASCII，则作为十六进制显示
                            content = data.joinToString("") { String.format("%02X", it) }
                            isHex = true
                        }
                    } catch (e: Exception) {
                        // 如果解析失败，作为十六进制处理
                        content = data.joinToString("") { String.format("%02X", it) }
                        isHex = true
                    }
                } else {
                    // 非UART服务，直接作为十六进制显示
                    content = data.joinToString("") { String.format("%02X", it) }
                    isHex = true
                }
                
                // 确定消息类型
                val messageType = when {
                    characteristic.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 -> {
                        BleMessageType.NOTIFICATION
                    }
                    characteristic.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 -> {
                        BleMessageType.INDICATION
                    }
                    else -> {
                        BleMessageType.UNKNOWN
                    }
                }
                
                // 创建消息
                val message = BluetoothMessage(
                    id = UUID.randomUUID().toString(),
                    deviceId = device.id,
                    content = content,
                    isIncoming = true,
                    isHex = isHex, // 使用新的isHex判断
                    status = MessageStatus.RECEIVED,
                    sourceUuid = characteristicUuid,
                    messageType = messageType
                )
                
                serviceScope.launch {
                    // 添加消息
                    addMessageToDevice(device.id, message)
                    
                    // 记录接收日志
                    addLog(
                        BluetoothLog(
                            id = UUID.randomUUID().toString(),
                            deviceId = device.id,
                            deviceName = device.name,
                            action = LogAction.RECEIVE_DATA,
                            details = "接收数据: $content (从特征: $characteristicUuid)"
                        )
                    )
                    
                    // 发送消息接收事件
                    _events.emit(BluetoothEvent.MessageReceived(device.id, message))
                }
            }
        }
        
        // 特性值变化回调 - 兼容旧版本API
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: android.bluetooth.BluetoothGatt,
            characteristic: android.bluetooth.BluetoothGattCharacteristic
        ) {
            // 获取旧版API的值
            val value = characteristic.value ?: ByteArray(0)
            // 调用处理方法
            onCharacteristicChanged(gatt, characteristic, value)
        }
    }
    
    // 解析GATT服务
    private fun parseGattServices(gattServices: List<android.bluetooth.BluetoothGattService>): List<ServiceInfo> {
        val result = mutableListOf<ServiceInfo>()
        
        for (gattService in gattServices) {
            val serviceUuid = gattService.uuid.toString()
            val characteristics = mutableListOf<CharacteristicInfo>()
            
            for (gattCharacteristic in gattService.characteristics) {
                val uuid = gattCharacteristic.uuid.toString()
                val properties = parseCharacteristicProperties(gattCharacteristic.properties)
                
                characteristics.add(
                    CharacteristicInfo(
                        name = getCharacteristicName(uuid),
                        uuid = uuid,
                        properties = properties
                    )
                )
            }
            
            result.add(
                ServiceInfo(
                    uuid = serviceUuid,
                    name = getServiceName(serviceUuid),
                    characteristics = characteristics
                )
            )
        }
        
        return result
    }
    
    // 解析特征属性
    private fun parseCharacteristicProperties(properties: Int): String {
        val props = mutableListOf<String>()
        
        if (properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ != 0) {
            props.add("READ")
        }
        if (properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE != 0) {
            props.add("WRITE")
        }
        if (properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
            props.add("WRITE_NO_RESPONSE")
        }
        if (properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
            props.add("NOTIFY")
        }
        if (properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) {
            props.add("INDICATE")
        }
        
        return props.joinToString(", ")
    }
    
    // 获取服务名称
    private fun getServiceName(uuid: String): String {
        return when (uuid.lowercase()) {
            "00001800-0000-1000-8000-00805f9b34fb" -> "Generic Access"
            "00001801-0000-1000-8000-00805f9b34fb" -> "Generic Attribute"
            "0000180a-0000-1000-8000-00805f9b34fb" -> "Device Information"
            "0000180f-0000-1000-8000-00805f9b34fb" -> "Battery Service"
            "0000180d-0000-1000-8000-00805f9b34fb" -> "Heart Rate Service"
            else -> uuid
        }
    }
    
    // 获取特征名称
    private fun getCharacteristicName(uuid: String): String {
        return when (uuid.lowercase()) {
            "00002a00-0000-1000-8000-00805f9b34fb" -> "Device Name"
            "00002a01-0000-1000-8000-00805f9b34fb" -> "Appearance"
            "00002a05-0000-1000-8000-00805f9b34fb" -> "Service Changed"
            "00002a29-0000-1000-8000-00805f9b34fb" -> "Manufacturer Name"
            "00002a24-0000-1000-8000-00805f9b34fb" -> "Model Number"
            "00002a25-0000-1000-8000-00805f9b34fb" -> "Serial Number"
            "00002a27-0000-1000-8000-00805f9b34fb" -> "Hardware Revision"
            "00002a26-0000-1000-8000-00805f9b34fb" -> "Firmware Revision"
            "00002a28-0000-1000-8000-00805f9b34fb" -> "Software Revision"
            "00002a19-0000-1000-8000-00805f9b34fb" -> "Battery Level"
            else -> uuid
        }
    }
    
    /**
     * 获取特征所属的服务UUID
     */
    fun getServiceUuidForCharacteristic(deviceId: String, characteristicUuid: String?): String? {
        if (characteristicUuid == null) return null
        
        // 从缓存的服务列表中查找特征所属的服务
        val services = _deviceServices.value[deviceId] ?: return null
        
        for (service in services) {
            val characteristic = service.characteristics.find { 
                it.uuid.equals(characteristicUuid, ignoreCase = true) 
            }
            
            if (characteristic != null) {
                Log.d(TAG, "找到特征 $characteristicUuid 所属的服务: ${service.uuid}")
                return service.uuid
            }
        }
        
        // 如果无法找到服务UUID，尝试使用一些常见的服务UUID
        if (characteristicUuid.equals("6e400002-b5a3-f393-e0a9-e50e24dcca9e", ignoreCase = true) ||
            characteristicUuid.equals("6e400003-b5a3-f393-e0a9-e50e24dcca9e", ignoreCase = true)) {
            // Nordic UART服务的特征
            return "6e400001-b5a3-f393-e0a9-e50e24dcca9e"
        }
        
        Log.d(TAG, "无法找到特征 $characteristicUuid 所属的服务")
        return null
    }
    
    // 断开连接
    @SuppressLint("MissingPermission")
    fun disconnectDevice() {
        val device = _connectedDevice.value ?: return
        
        try {
            // 如果存在活动的GATT连接，则断开它
            if (bluetoothGatt != null) {
                Log.d(TAG, "正在断开GATT连接: ${device.name}")
                bluetoothGatt?.disconnect()
                // 注意：断开连接的处理将在onConnectionStateChange回调中完成
            } else {
                // 如果没有活动的GATT连接，则直接更新UI状态
                serviceScope.launch {
                    // 记录断开连接日志
                    addLog(
                        BluetoothLog(
                            id = UUID.randomUUID().toString(),
                            deviceId = device.id,
                            deviceName = device.name,
                            action = LogAction.DISCONNECT,
                            details = "断开与设备的连接: ${device.name} (${device.address})"
                        )
                    )
                    
                    // 更新设备状态
                    val updatedDevice = device.copy(isConnected = false)
                    
                    // 更新设备列表
                    val currentList = _deviceList.value.toMutableList()
                    val deviceIndex = currentList.indexOfFirst { it.id == device.id }
                    
                    if (deviceIndex >= 0) {
                        currentList[deviceIndex] = updatedDevice
                        _deviceList.value = currentList
                    }
                    
                    // 清除已连接设备
                    _connectedDevice.value = null
                    
                    // 发送断开连接事件
                    _events.emit(BluetoothEvent.Disconnected(updatedDevice))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "断开连接失败", e)
            serviceScope.launch {
                _events.emit(BluetoothEvent.Error("断开连接失败: ${e.message}"))
            }
        }
    }
    
    // 发送消息到连接的BLE设备
    @SuppressLint("MissingPermission")
    fun sendMessage(content: String, deviceId: String, isHex: Boolean, 
                    characteristicUuid: String? = null, serviceUuid: String? = null,
                    onComplete: ((success: Boolean) -> Unit)? = null): Boolean {
        
        Log.d(TAG, "发送消息到设备 $deviceId, 内容: ${content.take(20)}..., " +
              "isHex: $isHex, 特征UUID: $characteristicUuid, 服务UUID: $serviceUuid")
        
        // 获取当前连接的BluetoothGatt实例
        val bluetoothGatt = activeGattConnections[deviceId] ?: return false
        
        // 确定消息类型
        val messageType = if (characteristicUuid != null) {
            if (serviceUuid != null && isCharacteristicWriteNoResponse(serviceUuid, characteristicUuid)) {
                BleMessageType.WRITE_NO_RESPONSE
            } else {
                BleMessageType.WRITE
            }
        } else {
            BleMessageType.UNKNOWN
        }
        
        // 创建消息对象，添加特征UUID和消息类型
        val message = BluetoothMessage(
            id = UUID.randomUUID().toString(),
            deviceId = deviceId,
            content = content,
            timestamp = Date(),
            isIncoming = false,  // 设置为false表示这是发送的消息
            isHex = isHex,
            status = MessageStatus.SENDING,
            // 添加特征UUID信息
            sourceUuid = characteristicUuid,
            // 设置消息类型
            messageType = messageType
        )
        
        Log.d(TAG, "创建的消息对象: ID=${message.id}, isIncoming=${message.isIncoming}, " +
              "timestamp=${message.timestamp}, timestampLong=${message.timestampLong}, sourceUuid=${message.sourceUuid}")
        
        // 首先检查设备对象
        val device = _connectedDevice.value
        if (device == null || device.id != deviceId) {
            Log.e(TAG, "无法发送消息：当前设备未连接或ID不匹配")
            onComplete?.invoke(false)
            return false
        }
        
        // 添加消息到列表
        serviceScope.launch {
            try {
                // 添加消息到列表
                addMessageToDevice(device.id, message)
                
                // 记录发送日志
                addLog(
                    BluetoothLog(
                        id = UUID.randomUUID().toString(),
                        deviceId = device.id,
                        deviceName = device.name,
                        action = LogAction.SEND_DATA,
                        details = "发送数据: $content, 格式: ${if (isHex) "十六进制" else "文本"}" +
                                 (if (characteristicUuid != null) ", 目标特征: $characteristicUuid" else "")
                    )
                )
                
                // 查找通用写入特性
                val writeCharacteristic = findWriteCharacteristic(characteristicUuid, serviceUuid)
                
                if (writeCharacteristic != null) {
                    // 检查特性是否可写
                    if (isCharacteristicWritable(writeCharacteristic)) {
                        Log.d(TAG, "找到可写特征: ${writeCharacteristic.uuid}")
                        
                        // 解析数据并写入
                        val data = if (isHex) {
                            convertHexToBytes(content)
                        } else {
                            content.toByteArray(Charsets.UTF_8)
                        }
                        
                        val writeResult = writeCharacteristic(bluetoothGatt!!, writeCharacteristic, data)
                        Log.d(TAG, "写入特征结果: $writeResult")
                        
                        // 添加延迟模拟发送过程
                        delay(300)
                        
                        // 需要更新消息状态 - 创建新的消息对象，而不是修改现有对象
                        val updatedMessage = message.copy(
                            status = if (writeResult) MessageStatus.SENT else MessageStatus.FAILED
                        )
                        updateMessageStatus(updatedMessage)
                        
                        // 发送完成通知回调
                        onComplete?.invoke(writeResult)
                        return@launch
                    } else {
                        Log.e(TAG, "特征不可写: ${writeCharacteristic.uuid}")
                    }
                } else {
                    Log.e(TAG, "未找到指定的写入特征")
                }
                
                // 无法实际发送数据时，记录错误日志
                Log.d(TAG, "无法实际发送数据，没有可用的写入特征")
                // 更新消息状态为失败 - 创建新的消息对象，而不是修改现有对象
                val failedMessage = message.copy(status = MessageStatus.FAILED)
                updateMessageStatus(failedMessage)
                onComplete?.invoke(false)
                
            } catch (e: Exception) {
                Log.e(TAG, "发送消息时发生异常: ${e.message}")
                // 更新消息状态为失败 - 创建新的消息对象，而不是修改现有对象
                val failedMessage = message.copy(status = MessageStatus.FAILED)
                updateMessageStatus(failedMessage)
                onComplete?.invoke(false)
            }
        }
        
        return true
    }
    
    // 查找写入特性
    private fun findWriteCharacteristic(
        characteristicUuid: String? = null, 
        serviceUuid: String? = null
    ): android.bluetooth.BluetoothGattCharacteristic? {
        if (bluetoothGatt == null) return null
        
        // 如果提供了特定的UUID，尝试查找它
        if (characteristicUuid != null) {
            // 如果同时提供了服务UUID，在指定服务中查找
            if (serviceUuid != null) {
                val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
                val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
                
                if (characteristic != null && isCharacteristicWritable(characteristic)) {
                    Log.d(TAG, "找到指定的特征: $characteristicUuid 在服务 $serviceUuid")
                    return characteristic
                }
            } else {
                // 在所有服务中查找指定特征
                for (service in bluetoothGatt?.services.orEmpty()) {
                    val characteristic = service.getCharacteristic(UUID.fromString(characteristicUuid))
                    if (characteristic != null && isCharacteristicWritable(characteristic)) {
                        Log.d(TAG, "找到指定的特征: $characteristicUuid 在服务 ${service.uuid}")
                        return characteristic
                    }
                }
            }
            
            Log.d(TAG, "未找到指定的可写特征: $characteristicUuid")
        }
        
        // 常见的通信服务和特性UUID
        val commonCharacteristicUuids = listOf(
            "0000ffe1-0000-1000-8000-00805f9b34fb", // 常见的BLE通信特性
            "6e400002-b5a3-f393-e0a9-e50e24dcca9e"  // Nordic UART TX特性
        )
        
        // 首先尝试查找常见的写入特性
        for (service in bluetoothGatt?.services.orEmpty()) {
            for (characteristic in service.characteristics) {
                val uuid = characteristic.uuid.toString()
                
                // 检查是否是已知的写入特性
                if (uuid in commonCharacteristicUuids) {
                    // 检查特性是否可写
                    if (isCharacteristicWritable(characteristic)) {
                        Log.d(TAG, "找到常见的可写特征: $uuid")
                        return characteristic
                    }
                }
            }
        }
        
        // 如果没有找到已知特性，尝试查找任何可写的特性
        for (service in bluetoothGatt?.services.orEmpty()) {
            for (characteristic in service.characteristics) {
                if (isCharacteristicWritable(characteristic)) {
                    Log.d(TAG, "找到可写特征: ${characteristic.uuid}")
                    return characteristic
                }
            }
        }
        
        Log.d(TAG, "未找到任何可写特征")
        return null
    }
    
    // 检查特性是否可写
    private fun isCharacteristicWritable(characteristic: android.bluetooth.BluetoothGattCharacteristic): Boolean {
        return characteristic.properties and 
            (android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE or
             android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0
    }
    
    // 写入特征
    @SuppressLint("MissingPermission")
    private fun writeCharacteristic(
        gatt: android.bluetooth.BluetoothGatt,
        characteristic: android.bluetooth.BluetoothGattCharacteristic,
        data: ByteArray
    ): Boolean {
        try {
            // 检查是否可写
            if (!isCharacteristicWritable(characteristic)) {
                Log.e(TAG, "特征不可写: ${characteristic.uuid}")
                return false
            }
            
            // 针对不同Android版本使用不同的写入方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13及以上
                val writeType = if (characteristic.properties and 
                                 android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                    android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                
                val status = gatt.writeCharacteristic(
                    characteristic, 
                    data, 
                    writeType
                )
                
                Log.d(TAG, "Android 13+ 写入特征结果: $status")
                return status == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                // Android 12及以下
                @Suppress("DEPRECATION")
                characteristic.setValue(data)
                @Suppress("DEPRECATION")
                val success = gatt.writeCharacteristic(characteristic)
                
                Log.d(TAG, "Android 12- 写入特征结果: $success")
                return success
            }
        } catch (e: Exception) {
            Log.e(TAG, "写入特征时出错: ${e.message}", e)
            return false
        }
    }
    
    // 转换十六进制字符串为字节数组
    private fun convertHexToBytes(hexString: String): ByteArray {
        // 移除所有空格和其他不相关字符
        val cleanHex = hexString.replace("\\s|0x".toRegex(), "")
        
        // 如果不是偶数长度，补0
        val paddedHex = if (cleanHex.length % 2 != 0) "0$cleanHex" else cleanHex
        
        // 转换为字节数组
        return try {
            val length = paddedHex.length
            val data = ByteArray(length / 2)
            
            for (i in 0 until length step 2) {
                data[i / 2] = ((Character.digit(paddedHex[i], 16) shl 4) + 
                              Character.digit(paddedHex[i + 1], 16)).toByte()
            }
            
            data
        } catch (e: Exception) {
            Log.e(TAG, "十六进制字符串转换错误: ${e.message}", e)
            byteArrayOf() // 返回空数组
        }
    }
    
    // 启用特性通知
    @SuppressLint("MissingPermission")
    private fun enableNotifications(characteristic: android.bluetooth.BluetoothGattCharacteristic): Boolean {
        val gatt = bluetoothGatt ?: return false
        
        // 设置客户端特性配置描述符
        val descriptor = characteristic.getDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") // 客户端特性配置描述符
        ) ?: return false
        
        // 确定使用通知还是指示
        val useIndication = characteristic.properties and 
            android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE != 0
            
        // 设置通知或指示
        val success = gatt.setCharacteristicNotification(characteristic, true)
        
        if (success) {
            // 写入描述符以启用通知/指示
            val value = if (useIndication) {
                android.bluetooth.BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            } else {
                android.bluetooth.BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            }
            
            // 使用新的API根据Android版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用新的API
                val result = gatt.writeDescriptor(descriptor, value)
                return result == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                // Android 12及以下 使用旧的API
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                return gatt.writeDescriptor(descriptor)
            }
        }
        
        return false
    }
    
    // 将消息添加到设备的消息列表
    private fun addMessageToDevice(deviceId: String, message: BluetoothMessage) {
        val currentMessages = _messages.value.toMutableMap()
        val deviceMessages = currentMessages[deviceId]?.toMutableList() ?: mutableListOf()
        
        // 添加日志
        Log.d(TAG, "添加消息到设备 $deviceId - 内容: ${message.content.take(20)}..., " +
              "方向: ${if(message.isIncoming) "接收" else "发送"}, " +
              "时间: ${message.timestamp}, ID: ${message.id}")
        
        deviceMessages.add(message)
        currentMessages[deviceId] = deviceMessages
        _messages.value = currentMessages
        
        // 添加日志，显示当前设备的消息总数
        Log.d(TAG, "设备 $deviceId 当前消息数: ${deviceMessages.size}, " +
              "接收消息: ${deviceMessages.count { it.isIncoming }}, " +
              "发送消息: ${deviceMessages.count { !it.isIncoming }}")
    }
    
    // 更新消息状态
    private fun updateMessageStatus(message: BluetoothMessage) {
        val currentMessages = _messages.value.toMutableMap()
        val deviceMessages = currentMessages[message.deviceId]?.toMutableList() ?: return
        
        val messageIndex = deviceMessages.indexOfFirst { it.id == message.id }
        if (messageIndex >= 0) {
            deviceMessages[messageIndex] = message
            currentMessages[message.deviceId] = deviceMessages
            _messages.value = currentMessages
        }
    }
    
    // 添加日志
    private fun addLog(log: BluetoothLog) {
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, log) // 添加到列表头部
        _logs.value = currentLogs
    }
    
    // 清空消息历史
    fun clearMessages(deviceId: String) {
        val currentMessages = _messages.value.toMutableMap()
        currentMessages[deviceId] = emptyList()
        _messages.value = currentMessages
    }
    
    // 设置消息（用于从持久化存储恢复）
    fun setMessages(messages: Map<String, List<BluetoothMessage>>) {
        // 添加日志，显示要设置的消息总数和每个设备的消息数
        Log.d(TAG, "正在设置消息, 共包含 ${messages.size} 个设备的消息")
        messages.forEach { (deviceId, msgList) ->
            Log.d(TAG, "设备 $deviceId 的消息: ${msgList.size} 条, " +
                  "接收消息: ${msgList.count { it.isIncoming }}, " +
                  "发送消息: ${msgList.count { !it.isIncoming }}")
            
            if (msgList.isNotEmpty()) {
                // 显示第一条和最后一条消息的信息
                val firstMsg = msgList.first()
                val lastMsg = msgList.last()
                Log.d(TAG, "第一条消息: ${firstMsg.content.take(20)}..., " +
                      "方向: ${if(firstMsg.isIncoming) "接收" else "发送"}, " +
                      "时间: ${firstMsg.timestampLong}")
                Log.d(TAG, "最后一条消息: ${lastMsg.content.take(20)}..., " +
                      "方向: ${if(lastMsg.isIncoming) "接收" else "发送"}, " +
                      "时间: ${lastMsg.timestampLong}")
            }
        }
        
        // 需要处理从JSON反序列化后的时间戳
        val processedMessages = messages.mapValues { (_, messageList) ->
            messageList.map { msg ->
                // 使用timestampLong重建timestamp
                val date = Date(msg.timestampLong)
                val processedMsg = msg.copy(timestamp = date)
                Log.d(TAG, "处理消息 ID: ${msg.id}, 内容: ${msg.content.take(20)}..., " +
                      "时间戳: ${msg.timestampLong} -> ${date}")
                processedMsg
            }
        }
        _messages.value = processedMessages
        
        // 添加日志，确认设置后的消息状态
        Log.d(TAG, "消息设置完成, 当前消息映射包含 ${_messages.value.size} 个设备")
    }
    
    /**
     * 设置指定设备的消息（用于从持久化存储恢复）
     */
    fun setDeviceMessages(deviceId: String, messages: List<BluetoothMessage>) {
        // 添加日志
        Log.d(TAG, "设置设备 $deviceId 的消息，共 ${messages.size} 条")
        
        // 处理从JSON反序列化后的时间戳
        val processedMessages = messages.map { msg ->
            // 使用timestampLong重建timestamp
            val date = Date(msg.timestampLong)
            msg.copy(timestamp = date)
        }
        
        // 更新消息映射
        val currentMessages = _messages.value.toMutableMap()
        currentMessages[deviceId] = processedMessages
        _messages.value = currentMessages
        
        // 添加日志，确认设置后的状态
        Log.d(TAG, "已设置设备 $deviceId 的消息，当前该设备消息数: ${processedMessages.size}")
    }
    
    // 清空所有日志
    fun clearLogs() {
        _logs.value = emptyList()
    }
    
    // 销毁服务，取消所有协程
    fun destroy() {
        stopScan()
        disconnectDevice()
        serviceScope.cancel()
    }
    
    // 清除设备服务缓存
    fun clearDeviceServices(deviceId: String) {
        val currentMap = _deviceServices.value.toMutableMap()
        currentMap.remove(deviceId)
        _deviceServices.value = currentMap
        Log.d(TAG, "已清除设备($deviceId)的服务缓存")
    }

    // 服务发现时间戳记录
    private val lastServiceDiscoveryTime = ConcurrentHashMap<String, Long>()
    // 正在进行服务发现的设备标记
    private val serviceDiscoveryInProgress = ConcurrentHashMap<String, Boolean>()

    // 发现设备服务
    fun discoverServices(deviceId: String): List<ServiceInfo> {
        if (deviceId.isEmpty()) {
            Log.d(TAG, "设备ID为空，无法发现服务")
            return emptyList()
        }
        
        Log.d(TAG, "开始发现设备($deviceId)的服务")
        
        // 检查是否有相同设备的服务发现正在进行
        if (serviceDiscoveryInProgress[deviceId] == true) {
            Log.d(TAG, "设备($deviceId)服务发现已在进行中，跳过重复请求")
            // 返回缓存的服务（如果有）
            val cachedServices = _deviceServices.value[deviceId]
            if (cachedServices != null && cachedServices.isNotEmpty()) {
                return cachedServices
            }
            return emptyList()
        }
        
        // 检查服务发现请求频率
        val currentTime = System.currentTimeMillis()
        val lastDiscoveryTime = lastServiceDiscoveryTime[deviceId] ?: 0L
        if (currentTime - lastDiscoveryTime < 1000) { // 1秒内不重复发现服务
            Log.d(TAG, "设备($deviceId)服务发现请求过于频繁，间隔: ${currentTime - lastDiscoveryTime}ms")
            // 返回缓存的服务（如果有）
            val cachedServices = _deviceServices.value[deviceId]
            if (cachedServices != null) {
                return cachedServices
            }
        }
        
        // 标记服务发现进行中
        serviceDiscoveryInProgress[deviceId] = true
        lastServiceDiscoveryTime[deviceId] = currentTime
        
        try {
            // 首先检查缓存
            val cachedServices = _deviceServices.value[deviceId]
            if (cachedServices != null && cachedServices.isNotEmpty()) {
                Log.d(TAG, "从缓存获取设备($deviceId)的服务列表: ${cachedServices.size}个服务")
                return cachedServices
            }
            
            // 检查是否设备已连接且GATT连接存在
            if (bluetoothGatt != null) {
                // 确认连接的设备是否匹配deviceId，使用标准化ID比较
                val normalizedId = normalizeId(deviceId)
                val gattDeviceAddress = bluetoothGatt?.device?.address
                val gattDeviceId = gattDeviceAddress // 在这个应用中，地址即ID
                
                val isConnectedDevice = normalizedId == normalizeId(gattDeviceId ?: "") || 
                                       (_connectedDevice.value != null && normalizeId(_connectedDevice.value!!.id) == normalizedId)
                
                if (isConnectedDevice) {
                    Log.d(TAG, "设备已连接，使用GATT连接发现服务")
                    // 触发服务发现
                    val success = bluetoothGatt?.discoverServices() ?: false
                    if (success) {
                        Log.d(TAG, "服务发现请求已发送")
                    } else {
                        Log.d(TAG, "服务发现请求发送失败")
                    }
                    
                    // 如果没有缓存，可能是服务发现尚未完成，返回当前GATT中的服务（如果有）
                    val currentGattServices = bluetoothGatt?.services
                    if (currentGattServices != null && currentGattServices.isNotEmpty()) {
                        Log.d(TAG, "从当前GATT获取服务列表: ${currentGattServices.size}个服务")
                        val parsedServices = parseGattServices(currentGattServices)
                        
                        // 缓存服务列表
                        serviceScope.launch {
                            val currentMap = _deviceServices.value.toMutableMap()
                            currentMap[deviceId] = parsedServices
                            _deviceServices.value = currentMap
                            
                            // 通知服务发现完成
                            _events.emit(BluetoothEvent.ServicesDiscovered(deviceId, parsedServices))
                        }
                        
                        return parsedServices
                    }
                    
                    // 等待服务发现完成的逻辑
                    Log.d(TAG, "等待服务发现完成...")
                    return emptyList() // 返回空列表，让调用方稍后重试
                } else {
                    Log.d(TAG, "GATT连接存在但不是目标设备，GATT设备地址: $gattDeviceAddress, 目标设备ID: $deviceId")
                }
            } else {
                Log.d(TAG, "没有活跃的GATT连接")
            }
            
            // 如果设备未连接或GATT为空，尝试连接
            val device = findDeviceById(deviceId)
            if (device != null) {
                Log.d(TAG, "设备未连接或需要重新连接: ${device.name}")
                
                // 检查设备是否已连接
                if (device.isConnected) {
                    Log.d(TAG, "设备已标记为连接状态，但没有活跃GATT连接，尝试刷新连接")
                }
                
                // 尝试连接设备
                connectDevice(device)
                // 注意：这里不会阻塞等待连接完成，但返回空列表
                return emptyList()
            }
            
            // 如果找不到设备或无法连接
            Log.d(TAG, "未找到设备($deviceId)，无法发现服务")
            return emptyList()
        } finally {
            // 完成后清除标记
            serviceDiscoveryInProgress[deviceId] = false
        }
    }

    // 获取设备的服务列表
    fun getDeviceServices(deviceId: String): List<ServiceInfo> {
        if (deviceId.isEmpty()) {
            Log.d(TAG, "设备ID为空，无法获取服务列表")
            return emptyList()
        }
        
        val services = _deviceServices.value[deviceId]
        if (services != null && services.isNotEmpty()) {
            Log.d(TAG, "从缓存获取设备($deviceId)的服务列表: ${services.size}个服务")
            return services
        }
        
        // 如果没有缓存，则发起服务发现
        return discoverServices(deviceId)
    }

    // 连接设备
    @SuppressLint("MissingPermission")
    fun connectDevice(device: BluetoothDevice) {
        Log.d(TAG, "尝试连接设备: ${device.name} (${device.address})")
        
        // 检查权限
        if (!PermissionUtils.hasRequiredBluetoothPermissions(context)) {
            Log.e(TAG, "缺少蓝牙权限，无法连接设备")
            return
        }
        
        // 查找系统蓝牙设备
        val bluetoothDevice = findSystemBluetoothDevice(device.address)
        if (bluetoothDevice == null) {
            Log.e(TAG, "找不到对应的系统蓝牙设备，无法连接")
            return
        }
        
        // 检查设备是否已连接
        if (_connectedDevice.value?.id == device.id && activeGattConnections.containsKey(device.id)) {
            Log.d(TAG, "设备已经连接，无需重新连接")
            
            // 通知设备已连接
            val updatedDevice = device.copy(isConnected = true)
            serviceScope.launch {
                _connectedDevice.value = updatedDevice
                
                // 更新设备列表
                val currentList = _deviceList.value.toMutableList()
                val deviceIndex = currentList.indexOfFirst { it.id == device.id }
                if (deviceIndex >= 0) {
                    currentList[deviceIndex] = updatedDevice
                    _deviceList.value = currentList
                }
                
                _events.emit(BluetoothEvent.Connected(updatedDevice))
                
                // 记录连接日志
                addLog(
                    BluetoothLog(
                        id = UUID.randomUUID().toString(),
                        deviceId = device.id,
                        deviceName = device.name,
                        action = LogAction.CONNECT,
                        details = "设备已连接状态：${updatedDevice.name} (${updatedDevice.id})"
                    )
                )
            }
            return
        }
        
        // 如果已有此设备的连接，先断开
        val existingGatt = activeGattConnections[device.id]
        if (existingGatt != null) {
            Log.d(TAG, "设备有现有GATT连接，先断开再重连")
            try {
                existingGatt.disconnect()
                existingGatt.close()
            } catch (e: Exception) {
                Log.e(TAG, "断开旧连接时出错", e)
            }
            activeGattConnections.remove(device.id)
            bluetoothGatt = null
        }
        
        serviceScope.launch {
            try {
                _events.emit(BluetoothEvent.Connecting(device))
                
                // 记录连接日志
                addLog(
                    BluetoothLog(
                        id = UUID.randomUUID().toString(),
                        deviceId = device.id,
                        deviceName = device.name,
                        action = LogAction.CONNECT,
                        details = "正在连接到设备: ${device.name} (${device.address})"
                    )
                )
                
                // 确保设备有消息列表
                if (!_messages.value.containsKey(device.id)) {
                    val currentMessages = _messages.value.toMutableMap()
                    currentMessages[device.id] = emptyList()
                    _messages.value = currentMessages
                }
                
                // 建立GATT连接
                val newGatt = bluetoothDevice.connectGatt(
                    context,
                    false, // 不自动连接 
                    gattCallback
                )
                
                // 保存新连接到Map
                bluetoothGatt = newGatt
                
                // 添加到活跃连接映射
                bluetoothGatt?.let { gatt ->
                    activeGattConnections[device.id] = gatt
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "连接设备失败", e)
                _events.emit(BluetoothEvent.ConnectionFailed(device, e.message ?: "连接失败"))
                
                // 记录错误日志
                addLog(
                    BluetoothLog(
                        id = UUID.randomUUID().toString(),
                        deviceId = device.id,
                        deviceName = device.name,
                        action = LogAction.ERROR,
                        details = "连接失败: ${e.message}"
                    )
                )
            }
        }
    }

    // 查找Android BluetoothDevice
    @SuppressLint("MissingPermission")
    private fun findBluetoothDevice(address: String): android.bluetooth.BluetoothDevice? {
        val normalizedAddress = address.uppercase() // 使用大写
        Log.d(TAG, "尝试查找设备: 原始地址=$address, 标准化地址=$normalizedAddress")
        
        // 尝试不同格式的地址
        val addressFormats = listOf(
            address,                           // 原始格式
            normalizedAddress,                 // 标准化格式（大写）
            normalizedAddress.replace(":", ""), // 无分隔符
            address.lowercase(),               // 小写原始
            normalizedAddress.lowercase()      // 小写标准化
        ).distinct()
        
        for (addrFormat in addressFormats) {
            try {
                val device = bluetoothAdapter?.getRemoteDevice(addrFormat)
                if (device != null) {
                    Log.d(TAG, "成功查找到设备，使用地址格式: $addrFormat")
                    return device
                }
            } catch (e: IllegalArgumentException) {
                Log.d(TAG, "尝试地址格式 $addrFormat 失败: ${e.message}")
                // 继续尝试下一种格式
            }
        }
        
        Log.e(TAG, "无法找到设备，所有地址格式均尝试失败: $address")
        return null
    }

    /**
     * 连接设备
     */
    @SuppressLint("MissingPermission")
    fun connectDevice(id: String) {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "没有BLUETOOTH_CONNECT权限")
            serviceScope.launch {
                _events.emit(BluetoothEvent.Error("没有蓝牙连接权限"))
            }
            return
        }
        
        // 检查是否已经连接该设备
        val device = findDeviceById(id)
        if (device != null && device.isConnected) {
            Log.d(TAG, "设备已连接: ${device.name}")
            serviceScope.launch {
                _events.emit(BluetoothEvent.Connected(device))
            }
            return
        }
        
        // 检查是否有活跃的GATT连接
        val normalizedId = normalizeId(id)
        if (activeGattConnections.containsKey(normalizedId) && activeGattConnections[normalizedId] != null) {
            Log.d(TAG, "已有活跃的GATT连接，复用现有连接")
            bluetoothGatt = activeGattConnections[normalizedId]
            
            if (bluetoothGatt?.device?.address == normalizedId) {
                // 发送连接成功事件
                serviceScope.launch {
                    val updatedDevice = device?.copy(isConnected = true) ?: BluetoothDevice(
                        id = id,
                        name = bluetoothGatt?.device?.name ?: "未知设备",
                        address = id,
                        isConnected = true,
                        lastConnected = Date()
                    )
                    _connectedDevice.value = updatedDevice
                    _events.emit(BluetoothEvent.Connected(updatedDevice))
                }
                
                // 开始发现服务
                discoverServices(id)
                return
            }
        }

        // 防止重复连接操作
        if (!connectingInProgress.compareAndSet(false, true)) {
            Log.d(TAG, "已有连接操作正在进行中，跳过重复请求")
            return
        }
        
        try {
            Log.d(TAG, "尝试连接设备: ${device?.name ?: "未知设备"} ($id)")
            
            // 尝试查找设备
            Log.d(TAG, "尝试查找设备: 原始地址=$id, 标准化地址=$normalizedId")
            
            // 查找连接设备
            val bluetoothDevice = findBluetoothDevice(normalizedId)
            if (bluetoothDevice == null) {
                Log.e(TAG, "无法找到地址为 $normalizedId 的蓝牙设备")
                serviceScope.launch {
                    _events.emit(BluetoothEvent.ConnectionFailed(
                        device?.copy(isConnected = false) ?: BluetoothDevice(
                            id = id,
                            name = "未知设备",
                            address = id,
                            isConnected = false
                        ),
                        "无法找到设备"
                    ))
                }
                return
            }
            
            Log.d(TAG, "成功查找到设备，使用地址格式: ${bluetoothDevice.address}")
            
            // 通知正在连接
            serviceScope.launch {
                _events.emit(BluetoothEvent.Connecting(
                    device?.copy(isConnected = false) ?: BluetoothDevice(
                        id = id,
                        name = bluetoothDevice.name ?: "未知设备",
                        address = bluetoothDevice.address,
                        isConnected = false
                    )
                ))
            }
            
            // 关闭现有连接
            closeGattConnection()
            
            // 创建新的GATT连接
            bluetoothGatt = bluetoothDevice.connectGatt(
                context,
                false, // 不自动连接
                gattCallback
            )
            
            // 添加到活跃连接映射
            bluetoothGatt?.let { gatt ->
                activeGattConnections[normalizedId] = gatt
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "连接设备时出错: ${e.message}")
            serviceScope.launch {
                _events.emit(BluetoothEvent.Error("连接设备出错: ${e.message}"))
            }
        } finally {
            // 重置锁定状态
            connectingInProgress.set(false)
        }
    }

    /**
     * 关闭GATT连接，释放资源
     */
    @SuppressLint("MissingPermission")
    private fun closeGattConnection() {
        try {
            bluetoothGatt?.let { gatt ->
                gatt.close()
                
                // 从活跃连接映射中移除
                val deviceId = gatt.device?.address
                if (deviceId != null) {
                    activeGattConnections.remove(deviceId)
                }
                
                bluetoothGatt = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "关闭GATT连接出错: ${e.message}")
        }
    }

    /**
     * 根据MAC地址查找系统蓝牙设备
     */
    @SuppressLint("MissingPermission")
    private fun findSystemBluetoothDevice(address: String): android.bluetooth.BluetoothDevice? {
        try {
            // 使用新的API获取BluetoothAdapter
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val adapter = bluetoothManager.adapter
            
            return adapter?.getRemoteDevice(address)
        } catch (e: Exception) {
            Log.e(TAG, "查找系统蓝牙设备失败: ${e.message}")
            return null
        }
    }

    // 查找通知特性
    private fun findNotifyCharacteristic(gatt: android.bluetooth.BluetoothGatt): android.bluetooth.BluetoothGattCharacteristic? {
        for (service in gatt.services) {
            for (characteristic in service.characteristics) {
                if (characteristic.properties and android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                    return characteristic
                }
            }
        }
        return null
    }

    // 检查特性是否可写
    private fun isCharacteristicWriteNoResponse(serviceUuid: String, characteristicUuid: String): Boolean {
        if (bluetoothGatt == null) return false
        
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        
        return characteristic?.properties?.and(
            android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
        ) != 0
    }

    /**
     * 清除指定设备的消息
     * @param deviceId 设备ID
     */
    fun clearDeviceMessages(deviceId: String) {
        serviceScope.launch {
            val currentMessages = _messages.value.toMutableMap()
            currentMessages.remove(deviceId)
            _messages.value = currentMessages
            
            // 添加日志
            addLog(
                BluetoothLog(
                    id = UUID.randomUUID().toString(),
                    deviceId = deviceId,
                    deviceName = getDeviceNameById(deviceId),
                    action = LogAction.INFO,
                    details = "已清除设备消息历史"
                )
            )
            
            Log.d(TAG, "已清除设备 $deviceId 的消息历史")
        }
    }
    
    /**
     * 根据设备ID获取设备名称
     */
    private fun getDeviceNameById(deviceId: String): String {
        // 从连接设备和设备列表中查找
        val connDevice = _connectedDevice.value
        if (connDevice != null && connDevice.id == deviceId) {
            return connDevice.name
        }
        
        // 从设备列表中查找
        val device = _deviceList.value.find { it.id == deviceId }
        return device?.name ?: "未知设备"
    }
}
