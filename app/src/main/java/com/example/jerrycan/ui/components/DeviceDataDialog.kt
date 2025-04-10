package com.example.jerrycan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.jerrycan.model.BluetoothDevice

/**
 * 显示设备原始广播数据的对话框
 */
@Composable
fun DeviceDataDialog(
    device: BluetoothDevice,
    onDismiss: () -> Unit
) {
    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题和复制按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "原始数据:",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (device.rawData.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(device.rawData))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "复制"
                            )
                        }
                    }
                }
                
                // 原始数据显示区域
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = if (device.rawData.isNotEmpty()) device.rawData else "无广播数据",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.padding(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 详细数据部分
                if (device.advertisementData.isNotEmpty()) {
                    Text(
                        text = "详细信息:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 表头
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "类型",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(0.4f)
                        )
                        
                        Text(
                            text = "数据",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                    
                    // 数据行
                    device.advertisementData.forEach { (type, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = type,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(0.4f)
                            )
                            
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // 解释
                    Text(
                        text = "数据类型定义遵循蓝牙规范中的广播数据格式。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 底部按钮
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("确定")
                }
            }
        }
    }
} 