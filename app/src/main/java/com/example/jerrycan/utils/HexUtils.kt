package com.example.jerrycan.utils

import android.util.Log

/**
 * 十六进制数据处理和格式化工具类
 */
object HexUtils {
    private const val TAG = "HexUtils"

    /**
     * 格式化十六进制字符串，使其更易读
     * 可以选择分组、添加空格和前缀等
     *
     * @param hexString 原始十六进制字符串
     * @param groupSize 分组大小 (例如2表示每2个字符一组)
     * @param addSpaces 是否在组之间添加空格
     * @param addPrefix 是否添加0x前缀
     * @param bytesPerLine 每行显示的字节数，0表示不换行
     * @return 格式化后的十六进制字符串
     */
    fun formatHexString(
        hexString: String,
        groupSize: Int = 2,
        addSpaces: Boolean = true,
        addPrefix: Boolean = false,
        bytesPerLine: Int = 0
    ): String {
        // 清理输入，移除所有空格和0x前缀
        val cleanHex = hexString.replace("\\s|0x".toRegex(), "").uppercase()
        
        if (cleanHex.isEmpty()) return ""
        
        val result = StringBuilder()
        var charCount = 0
        
        // 按照指定大小分组
        for (i in cleanHex.indices step groupSize) {
            // 确保不越界
            val endIndex = minOf(i + groupSize, cleanHex.length)
            val group = cleanHex.substring(i, endIndex)
            
            // 添加前缀或空格
            if (bytesPerLine > 0 && i % (groupSize * bytesPerLine) == 0 && addPrefix) {
                // 仅当bytesPerLine > 0时才执行这个条件
                result.append("0x")
            } else if (addPrefix && i == 0) {
                // 当bytesPerLine=0时，仅在开头添加一次前缀
                result.append("0x")
            } else if (i > 0 && addSpaces) {
                result.append(" ")
            }
            
            result.append(group)
            charCount += group.length
            
            // 按指定字节数换行，确保bytesPerLine > 0
            if (bytesPerLine > 0 && (i + groupSize) % (groupSize * bytesPerLine) == 0 && i < cleanHex.length - groupSize) {
                result.append("\n")
                charCount = 0
            }
        }
        
        return result.toString()
    }
    
    /**
     * 解析十六进制字符串并尝试将其转换为ASCII文本
     * 非可打印字符将替换为点号(.)
     *
     * @param hexString 十六进制字符串
     * @return ASCII字符串表示
     */
    fun hexToAscii(hexString: String): String {
        // 清理输入
        val cleanHex = hexString.replace("\\s|0x".toRegex(), "")
        if (cleanHex.isEmpty() || cleanHex.length % 2 != 0) {
            return ""
        }
        
        val result = StringBuilder()
        try {
            for (i in 0 until cleanHex.length step 2) {
                val charCode = cleanHex.substring(i, i + 2).toInt(16)
                // 只显示可打印ASCII字符
                result.append(if (charCode in 32..126) charCode.toChar() else '.')
            }
        } catch (e: Exception) {
            Log.e(TAG, "解析十六进制为ASCII失败: ${e.message}")
            return ""
        }
        
        return result.toString()
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     */
    fun hexStringToByteArray(hexString: String): ByteArray {
        // 清理输入
        val cleanHex = hexString.replace("\\s|0x".toRegex(), "")
        
        // 如果长度为奇数，前面补0
        val normalizedHex = if (cleanHex.length % 2 != 0) "0$cleanHex" else cleanHex
        
        // 转换为字节数组
        val result = ByteArray(normalizedHex.length / 2)
        
        try {
            for (i in normalizedHex.indices step 2) {
                result[i / 2] = normalizedHex.substring(i, i + 2).toInt(16).toByte()
            }
        } catch (e: Exception) {
            Log.e(TAG, "十六进制字符串转换为字节数组失败: ${e.message}")
            return byteArrayOf()
        }
        
        return result
    }
    
    /**
     * 将字节数组转换为格式化的十六进制字符串
     */
    fun bytesToFormattedHexString(
        bytes: ByteArray,
        groupSize: Int = 2,
        addSpaces: Boolean = true,
        addPrefix: Boolean = false,
        bytesPerLine: Int = 0
    ): String {
        if (bytes.isEmpty()) return ""
        
        val hexString = bytes.joinToString("") { 
            String.format("%02X", it) 
        }
        
        return formatHexString(hexString, groupSize, addSpaces, addPrefix, bytesPerLine)
    }
} 