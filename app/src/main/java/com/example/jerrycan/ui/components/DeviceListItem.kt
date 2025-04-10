package com.example.jerrycan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.jerrycan.model.BluetoothDevice

/**
 * 可复用的设备列表项组件 - 基础版本
 * 显示设备图标、名称和MAC地址
 */
@Composable
fun DeviceListItem(
    device: BluetoothDevice,
    icon: ImageVector,
    iconTint: Color,
    iconBackgroundColor: Color,
    showStatus: Boolean = false,
    isOnline: Boolean = false,
    content: @Composable () -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 设备图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = iconBackgroundColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // 设备信息
        Column(modifier = Modifier.weight(1f)) {
            // 设备名称行
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            // MAC地址和状态(可选)行
            if (showStatus) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // MAC地址
                    Text(
                        text = device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 在线/离线状态
                    Text(
                        text = if (isOnline) "在线" else "离线",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOnline) 
                            MaterialTheme.colorScheme.tertiary 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                // 只显示MAC地址
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        
        // 附加内容（可由调用者提供）
        content()
    }
}

/**
 * 设备列表项 - 好友列表版本
 * 用于设备列表界面
 */
@Composable
fun FriendDeviceListItem(
    device: BluetoothDevice,
    isOnline: Boolean,
    showSignalStrength: Boolean = true,
    rssiDescription: String = ""
) {
    DeviceListItem(
        device = device,
        icon = if (isOnline) getSignalIconForRssi(device.rssi) else Icons.Default.BluetoothDisabled,
        iconTint = if (isOnline) 
            MaterialTheme.colorScheme.primary
        else 
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        iconBackgroundColor = if (isOnline) 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else 
            MaterialTheme.colorScheme.surfaceVariant,
        showStatus = false,
        isOnline = isOnline
    ) {
        // 右侧显示状态和信号强度
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 在线/离线状态
            Text(
                text = if (isOnline) "在线" else "离线",
                style = MaterialTheme.typography.bodySmall,
                color = if (isOnline) 
                    MaterialTheme.colorScheme.tertiary 
                else 
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            
            // 只有在线且有信号强度时才显示
            if (isOnline && showSignalStrength && device.rssi != 0) {
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    color = getRssiColor(device.rssi)
                )
            }
        }
    }
}

/**
 * 根据RSSI获取对应的图标
 */
@Composable
fun getSignalIconForRssi(rssi: Int): ImageVector {
    return when {
        rssi > -60 -> Icons.Default.BluetoothConnected    // 很强的信号
        rssi > -70 -> Icons.Default.Bluetooth             // 强信号
        rssi > -80 -> Icons.Default.Bluetooth             // 中等信号
        else -> Icons.Default.BluetoothDisabled           // 极弱信号
    }
}

/**
 * 获取信号强度描述
 */
@Composable
fun getRssiDescription(rssi: Int): String {
    return when {
        rssi > -60 -> "信号极好"
        rssi > -70 -> "信号良好"
        rssi > -80 -> "信号一般"
        else -> "信号较弱"
    }
}

/**
 * 获取信号强度对应的颜色
 */
@Composable
fun getRssiColor(rssi: Int): Color {
    return when {
        rssi > -60 -> MaterialTheme.colorScheme.tertiary  // 极好 - 绿色
        rssi > -70 -> MaterialTheme.colorScheme.primary   // 良好 - 蓝色
        rssi > -80 -> MaterialTheme.colorScheme.secondary // 一般 - 浅蓝色
        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f) // 较弱 - 红色
    }
}

/**
 * 聊天列表设备项 - 用于聊天历史列表
 * 可自定义头像颜色和显示最后一条消息
 */
@Composable
fun ChatDeviceListItem(
    device: BluetoothDevice,
    avatarColor: Color,
    messageText: String = "",
    timestamp: String = "",
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 设备头像（圆形）- 使用传入的颜色
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = device.name.take(1).uppercase(),
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // 设备信息列
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 设备名称
            Text(
                text = device.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // MAC地址
            Text(
                text = device.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (messageText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                
                // 最后一条消息
                Text(
                    text = messageText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        if (timestamp.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            
            // 时间
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
} 