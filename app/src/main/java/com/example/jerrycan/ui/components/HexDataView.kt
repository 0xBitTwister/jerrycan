package com.example.jerrycan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jerrycan.utils.HexUtils
import com.example.jerrycan.model.HexDisplayMode
import com.example.jerrycan.model.HexDisplayModes

/**
 * 显示十六进制数据的自定义组件
 * 提供多种显示模式：纯十六进制、带空格分组、带ASCII对照
 */
@Composable
fun HexDataView(
    hexData: String,
    modifier: Modifier = Modifier,
    bytesPerLine: Int = 8,
    showAscii: Boolean = true,
    showAddresses: Boolean = true,
    displayMode: HexDisplayMode = HexDisplayMode.GROUPED,
    showModeSelector: Boolean = false,
    onDisplayModeChange: (HexDisplayMode) -> Unit = {},
    onRetryFailed: (() -> Unit)? = null,
    showRetryOption: Boolean = false
) {
    Column(modifier = modifier) {
        // 可选的显示模式选择器
        if (showModeSelector) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "数据显示模式",
                    style = MaterialTheme.typography.bodySmall
                )
                
                Row {
                    HexDisplayModes.forEach { mode ->
                        androidx.compose.material3.FilterChip(
                            selected = displayMode == mode,
                            onClick = { onDisplayModeChange(mode) },
                            label = { 
                                Text(
                                    text = mode.label, 
                                    fontSize = 12.sp
                                ) 
                            },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }
        }
        
        // 数据内容
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(6.dp)
        ) {
            when (displayMode) {
                HexDisplayMode.RAW -> RawHexView(hexData)
                HexDisplayMode.GROUPED -> GroupedHexView(hexData, bytesPerLine, showAscii, showAddresses)
                HexDisplayMode.ASCII -> AsciiHexView(hexData)
            }
        }
    }
}

@Composable
private fun RawHexView(hexData: String) {
    val formattedHex = HexUtils.formatHexString(
        hexString = hexData,
        groupSize = 2,
        addSpaces = false,
        addPrefix = false
    )
    
    Text(
        text = formattedHex,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
    )
}

@Composable
private fun GroupedHexView(
    hexData: String,
    bytesPerLine: Int = 8,
    showAscii: Boolean = true,
    showAddresses: Boolean = true
) {
    // 清理输入
    val cleanHex = hexData.replace("\\s|0x".toRegex(), "").uppercase()
    
    if (cleanHex.isEmpty()) {
        Text(
            text = "无数据",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        return
    }
    
    // 保证bytesPerLine至少为1，防止除以零错误
    val safeLineBytes = maxOf(1, bytesPerLine)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 2.dp)
    ) {
        // 计算行数
        val numBytes = cleanHex.length / 2
        val numLines = (numBytes + safeLineBytes - 1) / safeLineBytes
        
        for (line in 0 until numLines) {
            val startByte = line * safeLineBytes
            val endByte = minOf(startByte + safeLineBytes, numBytes)
            val hexLine = cleanHex.substring(startByte * 2, endByte * 2)
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 1.dp)
            ) {
                // 地址列
                if (showAddresses) {
                    Text(
                        text = String.format("%04X", startByte),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(40.dp)
                    )
                }
                
                // 十六进制数据
                val formattedHex = HexUtils.formatHexString(
                    hexString = hexLine,
                    groupSize = 2,
                    addSpaces = true,
                    addPrefix = false
                )
                
                Text(
                    text = formattedHex,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                
                // ASCII表示
                if (showAscii) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "|${HexUtils.hexToAscii(hexLine)}|",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AsciiHexView(hexData: String) {
    val asciiText = HexUtils.hexToAscii(hexData)
    
    Text(
        text = asciiText.ifEmpty { "无法转换为ASCII文本" },
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState())
    )
}

// 显示模式
// enum class HexDisplayMode(val label: String) {
//    RAW("原始"),
//    GROUPED("分组"),
//    ASCII("ASCII")
// }

// private val HexDisplayModes = listOf(
//    HexDisplayMode.GROUPED,
//    HexDisplayMode.RAW,
//    HexDisplayMode.ASCII
// ) 