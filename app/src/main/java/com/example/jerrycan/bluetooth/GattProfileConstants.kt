package com.example.jerrycan.bluetooth

import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery1Bar
import androidx.compose.material.icons.filled.Battery6Bar
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bloodtype
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.*
import kotlinx.coroutines.launch

/**
 * BLE GATT 标准服务和特征UUID定义
 */
object GattProfileConstants {
    
    /**
     * 标准GATT服务UUID
     */
    object Services {
        // 通用访问配置文件服务
        const val GENERIC_ACCESS = "00001800-0000-1000-8000-00805f9b34fb"
        // 通用属性配置文件服务
        const val GENERIC_ATTRIBUTE = "00001801-0000-1000-8000-00805f9b34fb"
        // 设备信息服务
        const val DEVICE_INFORMATION = "0000180a-0000-1000-8000-00805f9b34fb"
        // 电池服务
        const val BATTERY_SERVICE = "0000180f-0000-1000-8000-00805f9b34fb"
        // 心率服务
        const val HEART_RATE_SERVICE = "0000180d-0000-1000-8000-00805f9b34fb"
        // 健康温度计服务
        const val HEALTH_THERMOMETER = "00001809-0000-1000-8000-00805f9b34fb"
        // 血压服务
        const val BLOOD_PRESSURE = "00001810-0000-1000-8000-00805f9b34fb"
        // 位置和导航服务
        const val LOCATION_AND_NAVIGATION = "00001819-0000-1000-8000-00805f9b34fb"
        // 即时警报服务
        const val IMMEDIATE_ALERT = "00001802-0000-1000-8000-00805f9b34fb"
        // 当前时间服务
        const val CURRENT_TIME_SERVICE = "00001805-0000-1000-8000-00805f9b34fb"
        // 运行速度和步进服务
        const val RUNNING_SPEED_AND_CADENCE = "00001814-0000-1000-8000-00805f9b34fb"
        // 体重秤服务
        const val WEIGHT_SCALE = "0000181d-0000-1000-8000-00805f9b34fb"
        // 葡萄糖服务
        const val GLUCOSE = "00001808-0000-1000-8000-00805f9b34fb"
    }
    
    /**
     * 标准GATT特征UUID
     */
    object Characteristics {
        // 设备信息特征
        const val MANUFACTURER_NAME = "00002a29-0000-1000-8000-00805f9b34fb"
        const val MODEL_NUMBER = "00002a24-0000-1000-8000-00805f9b34fb"
        const val SERIAL_NUMBER = "00002a25-0000-1000-8000-00805f9b34fb"
        const val HARDWARE_REVISION = "00002a27-0000-1000-8000-00805f9b34fb"
        const val FIRMWARE_REVISION = "00002a26-0000-1000-8000-00805f9b34fb"
        const val SOFTWARE_REVISION = "00002a28-0000-1000-8000-00805f9b34fb"
        const val SYSTEM_ID = "00002a23-0000-1000-8000-00805f9b34fb"
        
        // 电池特征
        const val BATTERY_LEVEL = "00002a19-0000-1000-8000-00805f9b34fb"
        
        // 心率特征
        const val HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
        const val HEART_RATE_CONTROL_POINT = "00002a39-0000-1000-8000-00805f9b34fb"
        const val BODY_SENSOR_LOCATION = "00002a38-0000-1000-8000-00805f9b34fb"
        
        // 健康温度计特征
        const val TEMPERATURE_MEASUREMENT = "00002a1c-0000-1000-8000-00805f9b34fb"
        const val TEMPERATURE_TYPE = "00002a1d-0000-1000-8000-00805f9b34fb"
        
        // 血压特征
        const val BLOOD_PRESSURE_MEASUREMENT = "00002a35-0000-1000-8000-00805f9b34fb"
        const val INTERMEDIATE_CUFF_PRESSURE = "00002a36-0000-1000-8000-00805f9b34fb"
        
        // 位置和导航特征
        const val LOCATION_AND_SPEED = "00002a67-0000-1000-8000-00805f9b34fb"
        const val NAVIGATION = "00002a68-0000-1000-8000-00805f9b34fb"
        const val POSITION_QUALITY = "00002a69-0000-1000-8000-00805f9b34fb"
        
        // 当前时间特征
        const val CURRENT_TIME = "00002a2b-0000-1000-8000-00805f9b34fb"
        
        // 运行速度和步进特征
        const val RSC_MEASUREMENT = "00002a53-0000-1000-8000-00805f9b34fb"
    }
    
    /**
     * 定义标准服务的信息类
     */
    data class StandardServiceInfo(
        val uuid: String,
        val name: String,
        val icon: ImageVector,
        val autoRead: Boolean = true,
        val description: String = ""
    )
    
    /**
     * 获取指定UUID的标准服务信息
     */
    fun getStandardServiceInfo(uuid: String): StandardServiceInfo? {
        return standardServices[uuid.lowercase()]
    }
    
    /**
     * 检查是否为标准服务
     */
    fun isStandardService(uuid: String): Boolean {
        return standardServices.containsKey(uuid.lowercase())
    }
    
    /**
     * 标准服务信息映射表
     */
    private val standardServices = mapOf(
        Services.DEVICE_INFORMATION.lowercase() to StandardServiceInfo(
            uuid = Services.DEVICE_INFORMATION,
            name = "设备信息服务",
            icon = Icons.Default.DeviceHub,
            autoRead = true,
            description = "提供设备硬件和软件信息"
        ),
        
        Services.BATTERY_SERVICE.lowercase() to StandardServiceInfo(
            uuid = Services.BATTERY_SERVICE,
            name = "电池服务",
            icon = Icons.Default.BatteryFull,
            autoRead = true,
            description = "提供设备电池电量信息"
        ),
        
        Services.HEART_RATE_SERVICE.lowercase() to StandardServiceInfo(
            uuid = Services.HEART_RATE_SERVICE,
            name = "心率服务",
            icon = Icons.Default.MonitorHeart,
            autoRead = true,
            description = "提供心率测量数据"
        ),
        
        Services.HEALTH_THERMOMETER.lowercase() to StandardServiceInfo(
            uuid = Services.HEALTH_THERMOMETER,
            name = "健康温度计服务",
            icon = Icons.Default.Thermostat,
            autoRead = true,
            description = "提供体温测量数据"
        ),
        
        Services.BLOOD_PRESSURE.lowercase() to StandardServiceInfo(
            uuid = Services.BLOOD_PRESSURE,
            name = "血压服务",
            icon = Icons.Default.Bloodtype,
            autoRead = true,
            description = "提供血压测量数据"
        ),
        
        Services.LOCATION_AND_NAVIGATION.lowercase() to StandardServiceInfo(
            uuid = Services.LOCATION_AND_NAVIGATION,
            name = "位置和导航服务",
            icon = Icons.Default.LocationOn,
            autoRead = true,
            description = "提供位置和导航信息"
        ),
        
        Services.IMMEDIATE_ALERT.lowercase() to StandardServiceInfo(
            uuid = Services.IMMEDIATE_ALERT,
            name = "即时警报服务",
            icon = Icons.Default.Notifications,
            autoRead = false,
            description = "用于发送即时警报"
        ),
        
        Services.CURRENT_TIME_SERVICE.lowercase() to StandardServiceInfo(
            uuid = Services.CURRENT_TIME_SERVICE,
            name = "当前时间服务",
            icon = Icons.Default.WatchLater,
            autoRead = true,
            description = "提供当前时间信息"
        ),
        
        Services.RUNNING_SPEED_AND_CADENCE.lowercase() to StandardServiceInfo(
            uuid = Services.RUNNING_SPEED_AND_CADENCE,
            name = "运行速度和步进服务",
            icon = Icons.AutoMirrored.Filled.DirectionsRun,
            autoRead = true,
            description = "提供运动数据"
        ),
        
        Services.WEIGHT_SCALE.lowercase() to StandardServiceInfo(
            uuid = Services.WEIGHT_SCALE,
            name = "体重秤服务",
            icon = Icons.Default.Person,
            autoRead = true,
            description = "提供体重测量数据"
        ),
        
        Services.GLUCOSE.lowercase() to StandardServiceInfo(
            uuid = Services.GLUCOSE,
            name = "葡萄糖服务",
            icon = Icons.Default.LocalHospital,
            autoRead = true,
            description = "提供血糖测量数据"
        ),
        
        Services.GENERIC_ACCESS.lowercase() to StandardServiceInfo(
            uuid = Services.GENERIC_ACCESS,
            name = "通用访问服务",
            icon = Icons.Default.Settings,
            autoRead = false,
            description = "提供设备配置信息"
        ),
        
        Services.GENERIC_ATTRIBUTE.lowercase() to StandardServiceInfo(
            uuid = Services.GENERIC_ATTRIBUTE,
            name = "通用属性服务",
            icon = Icons.Default.Info,
            autoRead = false,
            description = "提供GATT服务相关信息"
        )
    )
    
    /**
     * 根据电池电量获取对应的图标
     */
    fun getBatteryIcon(level: Int): ImageVector {
        return when {
            level > 80 -> Icons.Default.BatteryFull
            level > 30 -> Icons.Default.Battery6Bar
            else -> Icons.Default.Battery1Bar
        }
    }
    
    /**
     * 根据心率值获取心率描述
     */
    fun getHeartRateDescription(rate: Int): String {
        return when {
            rate < 60 -> "心率较低 ($rate bpm)"
            rate < 100 -> "正常心率 ($rate bpm)"
            rate < 140 -> "中度偏高 ($rate bpm)"
            else -> "心率偏高 ($rate bpm)"
        }
    }
    
    /**
     * 解析电池电量 (0x00-0x64)
     * 返回0-100的整数
     */
    fun parseBatteryLevel(bytes: ByteArray): Int {
        if (bytes.isEmpty()) return 0
        return bytes[0].toInt().coerceIn(0, 100)
    }
    
    /**
     * 解析心率测量
     * 心率数据格式定义见: https://www.bluetooth.com/specifications/specs/gatt-specification-supplement-5/
     * 返回心率值，单位为bpm
     */
    fun parseHeartRate(bytes: ByteArray): Int {
        if (bytes.isEmpty()) return 0
        
        // 检查格式位(第0位)
        val format = bytes[0].toInt() and 0x01
        
        return if (format == 0x00) { // UINT8格式
            if (bytes.size < 2) return 0
            bytes[1].toInt() and 0xFF
        } else { // UINT16格式
            if (bytes.size < 3) return 0
            (bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8)
        }
    }
    
    /**
     * 解析温度测量值
     * 温度数据格式定义见: https://www.bluetooth.com/specifications/specs/gatt-specification-supplement-5/
     * 返回温度值，单位为摄氏度
     */
    fun parseTemperature(bytes: ByteArray): Float {
        if (bytes.size < 5) return 0f
        
        // 使用IEEE-11073 32位浮点数格式
        val tempValue = ((bytes[1].toInt() and 0xFF) or ((bytes[2].toInt() and 0xFF) shl 8) or
                ((bytes[3].toInt() and 0xFF) shl 16) or ((bytes[4].toInt() and 0xFF) shl 24))
        
        // 转换为浮点数
        return Float.fromBits(tempValue)
    }
} 