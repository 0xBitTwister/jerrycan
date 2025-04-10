package com.example.jerrycan.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import com.example.jerrycan.model.BluetoothMessage
import com.example.jerrycan.model.MessageDirection
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import java.io.BufferedReader
import java.io.FileReader
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

// 导入PermissionUtils
import com.example.jerrycan.utils.PermissionUtils

// 导入Base64Utils
import com.example.jerrycan.utils.Base64Utils

private const val TAG = "MessageHistoryManager"

/**
 * 消息方向枚举
 */
enum class MessageDirection {
    SEND,    // 发送消息
    RECEIVE  // 接收消息
}

/**
 * 文件消息记录数据模型
 */
@Serializable
data class FileMessageRecord(
    val time: String,                      // ISO 8601 时间戳（含毫秒）
    val direction: MessageDirection,       // 消息方向: send/receive
    val content: String,                   // 消息内容（文本或Base64编码的二进制数据）
    val isHex: Boolean = false             // 是否为十六进制格式
)

/**
 * 消息历史管理类 - 提供基于文件系统的设备通信历史存储
 */
class MessageHistoryManager(private val context: Context) {
    
    private val messageCache = ConcurrentHashMap<String, MutableList<BluetoothMessage>>()
    private val jsonFormat = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = false 
        isLenient = true
    }
    
    companion object {
        private const val MAX_CACHE_SIZE = 100 // 每个设备缓存的最大消息数
        private const val BUFFER_FLUSH_COUNT = 10 // 每缓存10条消息刷新一次
    }
    
    /**
     * 获取历史消息目录
     */
    private fun getHistoryDirectory(): File {
        // 根据Android版本选择合适的存储位置
        val baseDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用应用专用外部存储
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                ?: context.filesDir // 若外部存储不可用，退回到内部存储
        } else {
            // Android 9及以下，尝试使用公共文档目录
            val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (isExternalStorageWritable() && PermissionUtils.hasRequiredStoragePermissions(context)) {
                // 创建应用专用子文件夹
                File(publicDir, "JerryCan").also { 
                    if (!it.exists()) {
                        it.mkdirs()
                    }
                }
            } else {
                // 如果无法写入公共目录，使用应用专用目录
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.filesDir
            }
        }
        
        val appDir = File(baseDir, "MessageHistory")
        if (!appDir.exists()) {
            appDir.mkdirs()
        }
        
        return appDir
    }
    
    /**
     * 检查外部存储是否可写
     */
    private fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
    
    /**
     * 根据MAC地址生成文件名
     */
    private fun getDeviceFileName(macAddress: String): String {
        // 替换MAC地址中的冒号为下划线
        return macAddress.replace(":", "_") + ".log"
    }
    
    /**
     * 将BluetoothMessage转换为FileMessageRecord
     */
    private fun convertToFileRecord(message: BluetoothMessage): FileMessageRecord {
        // 格式化ISO 8601时间戳
        val timestampIso = java.time.format.DateTimeFormatter
            .ISO_INSTANT
            .format(java.time.Instant.ofEpochMilli(message.timestamp.time))
        
        // 如果是十六进制内容，先转换为Base64
        val content = if (message.isHex) {
            val hexContent = message.content.replace("\\s|0x".toRegex(), "")
            // 对十六进制字符串进行Base64编码
            Base64Utils.encodeHexToBase64(hexContent)
        } else {
            message.content
        }
        
        return FileMessageRecord(
            time = timestampIso,
            direction = if (message.isIncoming) MessageDirection.RECEIVE else MessageDirection.SEND,
            content = content,
            isHex = message.isHex
        )
    }
    
    /**
     * 将FileMessageRecord转换为BluetoothMessage
     */
    private fun convertToBluetoothMessage(record: FileMessageRecord, deviceId: String): BluetoothMessage {
        // 解析ISO 8601时间戳
        val instant = java.time.Instant.parse(record.time)
        val timestamp = Date(instant.toEpochMilli())
        
        // 如果是十六进制内容，从Base64解码回十六进制字符串
        val content = if (record.isHex) {
            Base64Utils.decodeToHexString(record.content)
        } else {
            record.content
        }
        
        return BluetoothMessage(
            id = java.util.UUID.randomUUID().toString(),  // 生成新ID
            deviceId = deviceId,
            content = content,
            timestamp = timestamp,
            timestampLong = timestamp.time,
            isIncoming = record.direction == MessageDirection.RECEIVE,
            isHex = record.isHex,
            status = com.example.jerrycan.model.MessageStatus.SENT // 历史消息都标记为已发送/已接收
        )
    }
    
    /**
     * 保存消息到文件
     */
    suspend fun saveMessage(message: BluetoothMessage) = withContext(Dispatchers.IO) {
        val deviceId = message.deviceId
        
        // 将消息添加到内存缓存
        val deviceMessages = messageCache.getOrPut(deviceId) { mutableListOf() }
        deviceMessages.add(message)
        
        // 仅当缓存满足刷盘条件时才写入文件
        if (deviceMessages.size % BUFFER_FLUSH_COUNT == 0) {
            flushDeviceCache(deviceId)
        }
    }
    
    /**
     * 保存设备所有消息（通常在断开连接时调用）
     */
    suspend fun saveAllMessages(deviceId: String, messages: List<BluetoothMessage>) = withContext(Dispatchers.IO) {
        // 更新缓存
        messageCache[deviceId] = messages.toMutableList()
        
        // 将所有消息刷新到磁盘
        flushDeviceCache(deviceId)
    }
    
    /**
     * 将设备消息缓存刷新到文件
     */
    private suspend fun flushDeviceCache(deviceId: String) = withContext(Dispatchers.IO) {
        val messages = messageCache[deviceId] ?: return@withContext
        
        try {
            val historyDir = getHistoryDirectory()
            val deviceFile = File(historyDir, getDeviceFileName(deviceId))
            
            // 创建父目录（如果不存在）
            deviceFile.parentFile?.mkdirs()
            
            // 从缓存中获取需要写入的消息
            val recordsToWrite = messages.map { convertToFileRecord(it) }
            
            // 使用BufferedWriter提高写入性能，追加模式
            BufferedWriter(FileWriter(deviceFile, true)).use { writer ->
                for (record in recordsToWrite) {
                    // 使用JSON Lines格式（每行一个完整JSON对象）
                    val json = jsonFormat.encodeToString(record)
                    writer.write(json)
                    writer.newLine() // 添加换行符
                }
            }
            
            Log.d(TAG, "已将 ${messages.size} 条消息保存到文件 ${deviceFile.name}")
            
            // 写入成功后清除缓存
            messageCache[deviceId]?.clear()
            
        } catch (e: Exception) {
            Log.e(TAG, "保存消息历史失败: ${e.message}", e)
        }
    }
    
    /**
     * 加载设备的历史消息
     */
    suspend fun loadDeviceMessages(deviceId: String): List<BluetoothMessage> = withContext(Dispatchers.IO) {
        val historyDir = getHistoryDirectory()
        val deviceFile = File(historyDir, getDeviceFileName(deviceId))
        val messages = mutableListOf<BluetoothMessage>()
        
        // 如果文件不存在，返回空列表
        if (!deviceFile.exists()) {
            Log.d(TAG, "设备 $deviceId 的历史记录文件不存在")
            return@withContext emptyList()
        }
        
        try {
            // 使用BufferedReader提高读取性能
            BufferedReader(FileReader(deviceFile)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        try {
                            val record = jsonFormat.decodeFromString<FileMessageRecord>(it)
                            val message = convertToBluetoothMessage(record, deviceId)
                            messages.add(message)
                        } catch (e: Exception) {
                            Log.e(TAG, "解析消息记录失败: $it", e)
                        }
                    }
                }
            }
            
            Log.d(TAG, "从文件 ${deviceFile.name} 加载了 ${messages.size} 条消息")
            
        } catch (e: Exception) {
            Log.e(TAG, "加载消息历史失败: ${e.message}", e)
        }
        
        return@withContext messages
    }
    
    /**
     * 加载所有设备的历史消息
     */
    suspend fun loadAllMessages(): Map<String, List<BluetoothMessage>> = withContext(Dispatchers.IO) {
        val historyDir = getHistoryDirectory()
        val result = mutableMapOf<String, List<BluetoothMessage>>()
        
        // 确保目录存在
        if (!historyDir.exists()) {
            return@withContext emptyMap()
        }
        
        // 获取所有日志文件
        val logFiles = historyDir.listFiles { file -> file.name.endsWith(".log") }
        
        logFiles?.forEach { file ->
            try {
                // 从文件名提取设备ID
                val deviceId = file.nameWithoutExtension.replace("_", ":")
                val messages = loadDeviceMessages(deviceId)
                
                if (messages.isNotEmpty()) {
                    result[deviceId] = messages
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理文件 ${file.name} 失败: ${e.message}", e)
            }
        }
        
        Log.d(TAG, "已加载 ${result.size} 个设备的消息历史")
        return@withContext result
    }
    
    /**
     * 清除设备消息历史
     */
    suspend fun clearDeviceMessages(deviceId: String) = withContext(Dispatchers.IO) {
        val historyDir = getHistoryDirectory()
        val deviceFile = File(historyDir, getDeviceFileName(deviceId))
        
        if (deviceFile.exists()) {
            val deleted = deviceFile.delete()
            
            if (deleted) {
                Log.d(TAG, "已删除设备 $deviceId 的消息历史文件")
            } else {
                Log.e(TAG, "删除设备 $deviceId 的消息历史文件失败")
            }
        }
        
        // 清除内存缓存
        messageCache.remove(deviceId)
    }
    
    /**
     * 获取设备最后一条消息
     */
    suspend fun getLastMessage(deviceId: String): BluetoothMessage? = withContext(Dispatchers.IO) {
        // 先尝试从内存缓存获取
        val cachedMessages = messageCache[deviceId]
        if (!cachedMessages.isNullOrEmpty()) {
            return@withContext cachedMessages.maxByOrNull { it.timestamp }
        }
        
        // 如果缓存中没有，从文件加载
        val messages = loadDeviceMessages(deviceId)
        return@withContext messages.maxByOrNull { it.timestamp }
    }
    
    /**
     * 获取所有设备的最后一条消息，用于聊天历史列表
     */
    suspend fun getAllLastMessages(): Map<String, BluetoothMessage> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, BluetoothMessage>()
        val historyDir = getHistoryDirectory()
        
        // 确保目录存在
        if (!historyDir.exists()) {
            return@withContext emptyMap()
        }
        
        // 获取所有日志文件
        val logFiles = historyDir.listFiles { file -> file.name.endsWith(".log") }
        
        logFiles?.forEach { file ->
            try {
                // 从文件名提取设备ID
                val deviceId = file.nameWithoutExtension.replace("_", ":")
                val lastMessage = getLastMessage(deviceId)
                
                if (lastMessage != null) {
                    result[deviceId] = lastMessage
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理文件 ${file.name} 失败: ${e.message}", e)
            }
        }
        
        return@withContext result
    }
    
    /**
     * 刷新所有设备缓存到文件
     */
    suspend fun flushAllCaches() = withContext(Dispatchers.IO) {
        // 获取所有有缓存的设备ID
        val deviceIds = messageCache.keys.toList()
        
        // 逐个刷新设备缓存
        for (deviceId in deviceIds) {
            flushDeviceCache(deviceId)
        }
        
        Log.d(TAG, "已刷新 ${deviceIds.size} 个设备的消息缓存")
    }
    
    /**
     * 确保所有消息写入持久化存储
     * 在应用退出时调用
     */
    fun ensurePersistence() {
        // 在主线程中同步执行，确保应用退出前所有消息都已持久化
        val deviceIds = messageCache.keys.toList()
        
        for (deviceId in deviceIds) {
            val messages = messageCache[deviceId] ?: continue
            if (messages.isNotEmpty()) {
                try {
                    val historyDir = getHistoryDirectory()
                    val deviceFile = File(historyDir, getDeviceFileName(deviceId))
                    
                    // 创建父目录（如果不存在）
                    deviceFile.parentFile?.mkdirs()
                    
                    // 转换为文件记录
                    val recordsToWrite = messages.map { convertToFileRecord(it) }
                    
                    // 使用BufferedWriter提高写入性能，追加模式
                    BufferedWriter(FileWriter(deviceFile, true)).use { writer ->
                        for (record in recordsToWrite) {
                            val json = jsonFormat.encodeToString(record)
                            writer.write(json)
                            writer.newLine() // 添加换行符
                        }
                    }
                    
                    Log.d(TAG, "应用退出前同步写入 ${messages.size} 条消息到设备 $deviceId")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "应用退出前保存消息失败: ${e.message}", e)
                }
            }
        }
        
        // 清空缓存
        messageCache.clear()
    }
    
    /**
     * 将设备消息历史导出到用户可访问位置
     * @param deviceId 设备ID
     * @return 导出的文件路径，如果导出失败则返回null
     */
    suspend fun exportDeviceHistory(deviceId: String): String? = withContext(Dispatchers.IO) {
        try {
            // 1. 读取该设备的历史消息
            val messages = loadDeviceMessages(deviceId)
            if (messages.isEmpty()) {
                Log.d(TAG, "设备 $deviceId 没有消息记录，无法导出")
                return@withContext null
            }
            
            // 2. 获取导出目录（使用公共文档目录）
            val exportDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ 使用应用专用外部导出目录
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(downloadsDir, "JerryCan").also { if (!it.exists()) it.mkdirs() }
            } else {
                // 低版本Android使用公共文档目录
                val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                File(documentsDir, "JerryCan").also { if (!it.exists()) it.mkdirs() }
            }
            
            // 3. 创建导出文件
            val exportFileName = "Export_${getDeviceFileName(deviceId)}"
            val exportFile = File(exportDir, exportFileName)
            
            // 4. 写入导出文件
            BufferedWriter(FileWriter(exportFile)).use { writer ->
                // 写入文件头
                writer.write("# 设备ID: $deviceId 消息历史记录\n")
                writer.write("# 导出时间: ${java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.Instant.now())}\n")
                writer.write("# 消息总数: ${messages.size}\n\n")
                
                // 写入消息 (JSON Lines 格式)
                for (message in messages) {
                    val record = convertToFileRecord(message)
                    val json = jsonFormat.encodeToString(record)
                    writer.write(json)
                    writer.newLine()
                }
            }
            
            Log.d(TAG, "已成功导出设备 $deviceId 的 ${messages.size} 条消息到 ${exportFile.absolutePath}")
            return@withContext exportFile.absolutePath
            
        } catch (e: Exception) {
            Log.e(TAG, "导出设备 $deviceId 消息历史失败: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * 将所有设备的消息历史导出到用户可访问位置
     * @return 导出的文件路径列表，如果导出失败则返回空列表
     */
    suspend fun exportAllDeviceHistory(): List<String> = withContext(Dispatchers.IO) {
        val result = mutableListOf<String>()
        
        try {
            // 1. 获取所有设备ID
            val historyDir = getHistoryDirectory()
            val logFiles = historyDir.listFiles { file -> file.name.endsWith(".log") }
            
            // 2. 为每个设备导出消息历史
            logFiles?.forEach { file ->
                val deviceId = file.nameWithoutExtension.replace("_", ":")
                val exportPath = exportDeviceHistory(deviceId)
                if (exportPath != null) {
                    result.add(exportPath)
                }
            }
            
            Log.d(TAG, "已成功导出 ${result.size} 个设备的消息历史")
            
        } catch (e: Exception) {
            Log.e(TAG, "导出所有设备消息历史失败: ${e.message}", e)
        }
        
        return@withContext result
    }
    
    /**
     * 删除指定设备的消息历史
     * @param deviceId 要删除的设备ID
     * @return 删除操作是否成功
     */
    suspend fun deleteDeviceHistory(deviceId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. 清除内存缓存
            messageCache.remove(deviceId)
            
            // 2. 删除设备消息文件
            val historyDir = getHistoryDirectory()
            val deviceFile = File(historyDir, getDeviceFileName(deviceId))
            
            val success = if (deviceFile.exists()) {
                val deleteResult = deviceFile.delete()
                Log.d(TAG, "删除设备 $deviceId 的消息文件，结果: $deleteResult")
                deleteResult
            } else {
                Log.d(TAG, "设备 $deviceId 的消息文件不存在，无需删除")
                true // 文件不存在，视为删除成功
            }
            
            return@withContext success
            
        } catch (e: Exception) {
            Log.e(TAG, "删除设备 $deviceId 的消息历史失败: ${e.message}", e)
            return@withContext false
        }
    }
    
    /**
     * 分页加载设备消息
     * @param deviceId 设备ID
     * @param offset 起始位置
     * @param limit 每页消息数量
     * @return 指定设备的部分消息
     */
    suspend fun loadDeviceMessagesWithPaging(deviceId: String, offset: Int, limit: Int): List<BluetoothMessage> = withContext(Dispatchers.IO) {
        try {
            // 检查缓存
            val cachedMessages = messageCache[deviceId]
            if (cachedMessages != null) {
                Log.d(TAG, "从内存缓存加载设备 $deviceId 消息，分页 offset=$offset, limit=$limit")
                val totalSize = cachedMessages.size
                val endIndex = (offset + limit).coerceAtMost(totalSize)
                val startIndex = offset.coerceAtMost(endIndex)
                
                if (startIndex < endIndex) {
                    return@withContext cachedMessages.subList(startIndex, endIndex)
                }
                return@withContext emptyList()
            }
            
            // 未找到缓存，从文件加载
            val historyDir = getHistoryDirectory()
            val deviceFile = File(historyDir, getDeviceFileName(deviceId))
            
            if (!deviceFile.exists()) {
                Log.d(TAG, "设备 $deviceId 无历史记录文件")
                return@withContext emptyList()
            }
            
            // 读取文件中的所有消息
            val allMessages = mutableListOf<BluetoothMessage>()
            val uniqueMessageTimes = mutableSetOf<Long>() // 用于去重
            
            BufferedReader(FileReader(deviceFile)).use { reader ->
                var line: String?
                var counter = 0
                var duplicates = 0
                
                // 读取所有消息
                while (reader.readLine().also { line = it } != null) {
                    if (line.isNullOrBlank()) continue
                    
                    try {
                        val record = jsonFormat.decodeFromString<FileMessageRecord>(line!!)
                        val message = convertToBluetoothMessage(record, deviceId)
                        
                        // 根据时间戳和内容进行去重
                        val key = message.timestamp.time
                        if (!uniqueMessageTimes.contains(key)) {
                            uniqueMessageTimes.add(key)
                            allMessages.add(message)
                            counter++
                        } else {
                            duplicates++
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析设备 $deviceId 的第 $counter 条消息失败: ${e.message}")
                    }
                }
                
                if (duplicates > 0) {
                    Log.d(TAG, "设备 $deviceId 消息去重: 跳过了 $duplicates 条重复消息")
                }
            }
            
            // 按时间顺序排序
            allMessages.sortBy { it.timestamp }
            
            // 将消息保存到缓存
            messageCache[deviceId] = allMessages.toMutableList()
            
            // 返回指定范围的消息
            val totalSize = allMessages.size
            val endIndex = (offset + limit).coerceAtMost(totalSize)
            val startIndex = offset.coerceAtMost(endIndex)
            
            Log.d(TAG, "从文件加载设备 $deviceId 的消息，总数: $totalSize, 返回: $startIndex-$endIndex")
            
            if (startIndex < endIndex) {
                return@withContext allMessages.subList(startIndex, endIndex)
            }
            return@withContext emptyList()
            
        } catch (e: Exception) {
            Log.e(TAG, "分页加载设备 $deviceId 消息失败: ${e.message}", e)
            return@withContext emptyList()
        }
    }
} 