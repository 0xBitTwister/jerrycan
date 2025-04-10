package com.example.jerrycan.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import java.util.Date

/**
 * 蓝牙设备数据模型
 */
@Parcelize
@Serializable
data class BluetoothDevice(
    val id: String,                   // 设备MAC地址作为唯一标识
    val name: String,                 // 设备名称
    val address: String,              // 设备MAC地址
    val rssi: Int = 0,                // 信号强度
    val isPaired: Boolean = false,    // 是否已配对
    val isConnected: Boolean = false, // 是否已连接
    @kotlinx.serialization.Transient
    val lastConnected: Date? = null,  // 上次连接时间
    @kotlinx.serialization.Transient
    val discoveryTime: Date = Date(), // 发现时间
    val rawData: String = "",         // 原始广播数据
    @kotlinx.serialization.Transient
    val advertisementData: Map<String, String> = emptyMap() // 广播数据解析
) : Parcelable

/**
 * 消息类型枚举
 */
@Serializable
enum class BleMessageType {
    READ_RESPONSE,  // 读取响应
    NOTIFICATION,   // 通知
    INDICATION,     // 指示
    WRITE,          // 写入请求
    WRITE_NO_RESPONSE, // 无响应写入
    UNKNOWN         // 未知类型
}

/**
 * 蓝牙设备通信消息模型
 */
@Parcelize
@Serializable
data class BluetoothMessage(
    val id: String,                      // 消息ID
    val deviceId: String,                // 设备ID
    val content: String,                 // 消息内容
    @kotlinx.serialization.Transient
    val timestamp: Date = Date(),        // 发送/接收时间
    val isIncoming: Boolean = false,     // 是否为接收到的消息
    val isHex: Boolean = false,          // 是否为十六进制格式
    val status: MessageStatus = MessageStatus.SENDING, // 消息状态 - 默认为发送中
    val sourceUuid: String? = null,      // 消息来源的UUID (特征或服务)
    val messageType: BleMessageType = BleMessageType.UNKNOWN, // 消息类型
    // 添加时间戳的长整型表示，用于序列化
    val timestampLong: Long = timestamp.time
) : Parcelable

/**
 * 消息状态枚举
 */
@Serializable
enum class MessageStatus {
    SENDING,   // 发送中
    SENT,      // 已发送
    RECEIVED,  // 已接收
    FAILED     // 发送失败
}

/**
 * 日志记录模型
 */
@Parcelize
data class BluetoothLog(
    val id: String,                    // 日志ID
    val deviceId: String,              // 设备ID
    val deviceName: String,            // 设备名称
    val action: LogAction,             // 操作类型
    val timestamp: Date = Date(),      // 操作时间
    val details: String = ""           // 详细信息
) : Parcelable

/**
 * 日志操作类型枚举
 */
@Serializable
enum class LogAction {
    CONNECT,      // 连接设备
    DISCONNECT,   // 断开连接
    SEND_DATA,    // 发送数据
    RECEIVE_DATA, // 接收数据
    SCAN_START,   // 开始扫描
    SCAN_STOP,    // 停止扫描
    PAIR,         // 配对设备
    UNPAIR,       // 取消配对
    ERROR,        // 错误
    INFO          // 信息
} 