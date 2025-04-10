package com.example.jerrycan.bluetooth

import com.example.jerrycan.model.BluetoothDevice
import com.example.jerrycan.model.BluetoothMessage

// 蓝牙事件
sealed class BluetoothEvent {
    data class ScanStarted(val timestamp: Long) : BluetoothEvent()
    data class ScanFinished(val devices: List<BluetoothDevice>) : BluetoothEvent()
    data class ScanFailed(val message: String) : BluetoothEvent()
    data class Connecting(val device: BluetoothDevice) : BluetoothEvent()
    data class Connected(val device: BluetoothDevice) : BluetoothEvent()
    data class ConnectionFailed(val device: BluetoothDevice, val message: String) : BluetoothEvent()
    data class Disconnected(val device: BluetoothDevice) : BluetoothEvent()
    data class MessageReceived(val deviceId: String, val message: BluetoothMessage) : BluetoothEvent()
    data class Error(val message: String) : BluetoothEvent()
    data class ServicesDiscovered(
        val deviceId: String, 
        val services: List<BluetoothService.ServiceInfo>
    ) : BluetoothEvent()
} 