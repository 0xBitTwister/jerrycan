package com.example.jerrycan.utils

import android.util.Base64
import android.util.Log

/**
 * Base64 编解码工具类
 * 用于处理二进制数据的存储和读取
 */
object Base64Utils {
    
    private const val TAG = "Base64Utils"
    
    /**
     * 将字节数组编码为Base64字符串
     * @param bytes 要编码的字节数组
     * @return Base64编码的字符串
     */
    fun encodeToString(bytes: ByteArray): String {
        return try {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Base64编码失败: ${e.message}", e)
            "" // 出错时返回空字符串
        }
    }
    
    /**
     * 将十六进制字符串编码为Base64字符串
     * @param hexString 十六进制字符串
     * @return Base64编码的字符串
     */
    fun encodeHexToBase64(hexString: String): String {
        return try {
            // 先将十六进制字符串转换为字节数组
            val bytes = hexStringToByteArray(hexString)
            // 再将字节数组编码为Base64
            encodeToString(bytes)
        } catch (e: Exception) {
            Log.e(TAG, "十六进制转Base64失败: ${e.message}", e)
            "" // 出错时返回空字符串
        }
    }
    
    /**
     * 将Base64字符串解码为字节数组
     * @param base64String Base64编码的字符串
     * @return 解码后的字节数组
     */
    fun decode(base64String: String): ByteArray? {
        return try {
            Base64.decode(base64String, Base64.NO_WRAP)
        } catch (e: Exception) {
            Log.e(TAG, "Base64解码失败: ${e.message}", e)
            null // 出错时返回null
        }
    }
    
    /**
     * 将Base64字符串解码为十六进制字符串
     * @param base64String Base64编码的字符串
     * @return 十六进制字符串
     */
    fun decodeToHexString(base64String: String): String {
        val bytes = decode(base64String) ?: return ""
        return bytesToHexString(bytes)
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    private fun hexStringToByteArray(hex: String): ByteArray {
        // 移除所有空格、0x前缀等
        val cleanHex = hex.replace("\\s|0x".toRegex(), "")
        
        // 如果长度为奇数，前面补0
        val normalizedHex = if (cleanHex.length % 2 != 0) "0$cleanHex" else cleanHex
        
        // 转换为字节数组
        val len = normalizedHex.length
        val bytes = ByteArray(len / 2)
        
        for (i in 0 until len step 2) {
            bytes[i / 2] = normalizedHex.substring(i, i + 2).toInt(16).toByte()
        }
        
        return bytes
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    private fun bytesToHexString(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexDigits[v ushr 4]
            hexChars[i * 2 + 1] = hexDigits[v and 0x0F]
        }
        return String(hexChars)
    }
    
    // 十六进制字符
    private val hexDigits = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    )
} 