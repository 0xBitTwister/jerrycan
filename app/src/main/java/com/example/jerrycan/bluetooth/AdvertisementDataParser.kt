package com.example.jerrycan.bluetooth

/**
 * 蓝牙广播数据解析器
 * 根据Bluetooth SIG Core Specification Supplement标准解析广播数据
 */
object AdvertisementDataParser {
    
    // 广播类型常量定义（按照Bluetooth Core Specification Supplement）
    private const val FLAGS = 0x01
    private const val INCOMPLETE_16BIT_UUIDS = 0x02
    private const val COMPLETE_16BIT_UUIDS = 0x03
    private const val INCOMPLETE_32BIT_UUIDS = 0x04
    private const val COMPLETE_32BIT_UUIDS = 0x05
    private const val INCOMPLETE_128BIT_UUIDS = 0x06
    private const val COMPLETE_128BIT_UUIDS = 0x07
    private const val SHORT_LOCAL_NAME = 0x08
    private const val COMPLETE_LOCAL_NAME = 0x09
    private const val TX_POWER_LEVEL = 0x0A
    private const val DEVICE_CLASS = 0x0D
    private const val SIMPLE_PAIRING_HASH = 0x0E
    private const val SIMPLE_PAIRING_RANDOMIZER = 0x0F
    private const val DEVICE_ID = 0x10
    private const val SERVICE_DATA_16BIT_UUID = 0x16
    private const val SERVICE_DATA_32BIT_UUID = 0x20
    private const val SERVICE_DATA_128BIT_UUID = 0x21
    private const val PUBLIC_TARGET_ADDRESS = 0x17
    private const val RANDOM_TARGET_ADDRESS = 0x18
    private const val APPEARANCE = 0x19
    private const val MANUFACTURER_SPECIFIC_DATA = 0xFF
    
    /**
     * 解析原始广播数据
     * @param rawData 原始数据（十六进制字符串，可以以"0x"开头）
     * @return 解析后的键值对列表
     */
    fun parseAdvertisementData(rawData: String): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        
        // 清理原始数据
        val cleanData = rawData.replace("0x", "").replace(" ", "")
        if (cleanData.isEmpty() || cleanData.length % 2 != 0) {
            return result
        }
        
        try {
            // 转换为字节数组
            val bytes = hexStringToByteArray(cleanData)
            var index = 0
            
            // 循环解析各个广播数据块
            while (index < bytes.size) {
                // 数据块长度
                val length = bytes[index++].toInt() and 0xFF
                if (length == 0 || index + length > bytes.size) {
                    break
                }
                
                // 数据类型
                val type = bytes[index++].toInt() and 0xFF
                val dataLength = length - 1 // 减去类型字节
                
                // 数据内容
                val data = bytes.copyOfRange(index, index + dataLength)
                
                // 解析这个广播数据块
                val parsedData = parseAdvertisementDataType(type, data)
                if (parsedData != null) {
                    result.add(parsedData)
                }
                
                index += dataLength
            }
        } catch (e: Exception) {
            // 解析失败，返回错误信息
            result.add(Pair("解析错误", e.message ?: "未知错误"))
        }
        
        return result
    }
    
    /**
     * 解析特定类型的广播数据
     */
    private fun parseAdvertisementDataType(type: Int, data: ByteArray): Pair<String, String>? {
        return when (type) {
            FLAGS -> {
                val flags = data[0].toInt() and 0xFF
                val flagsText = buildString {
                    if ((flags and 0x01) != 0) append("LE Limited Discoverable Mode, ")
                    if ((flags and 0x02) != 0) append("LE General Discoverable Mode, ")
                    if ((flags and 0x04) != 0) append("BR/EDR Not Supported, ")
                    if ((flags and 0x08) != 0) append("LE and BR/EDR Controller, ")
                    if ((flags and 0x10) != 0) append("LE and BR/EDR Host, ")
                }
                val trimmedText = flagsText.trimEnd { it == ',' || it == ' ' }
                Pair("Flags (${data.size}字节)", "${flags.toString(16).uppercase()} - $trimmedText")
            }
            
            INCOMPLETE_16BIT_UUIDS, COMPLETE_16BIT_UUIDS -> {
                val typeName = if (type == COMPLETE_16BIT_UUIDS) 
                    "完整16-bit UUID列表 (${data.size}字节)" 
                else 
                    "不完整16-bit UUID列表 (${data.size}字节)"
                val uuids = parseUUIDs16(data)
                Pair(typeName, uuids)
            }
            
            INCOMPLETE_32BIT_UUIDS, COMPLETE_32BIT_UUIDS -> {
                val typeName = if (type == COMPLETE_32BIT_UUIDS) 
                    "完整32-bit UUID列表 (${data.size}字节)" 
                else 
                    "不完整32-bit UUID列表 (${data.size}字节)"
                val uuids = parseUUIDs32(data)
                Pair(typeName, uuids)
            }
            
            INCOMPLETE_128BIT_UUIDS, COMPLETE_128BIT_UUIDS -> {
                val typeName = if (type == COMPLETE_128BIT_UUIDS) 
                    "完整128-bit UUID列表 (${data.size}字节)" 
                else 
                    "不完整128-bit UUID列表 (${data.size}字节)"
                val uuids = parseUUIDs128(data)
                Pair(typeName, uuids)
            }
            
            SHORT_LOCAL_NAME -> {
                Pair("短名称 (${data.size}字节)", String(data))
            }
            
            COMPLETE_LOCAL_NAME -> {
                Pair("完整名称 (${data.size}字节)", String(data))
            }
            
            TX_POWER_LEVEL -> {
                val power = data[0].toInt()
                // 转换为有符号值
                val signedPower = if (power > 127) power - 256 else power
                Pair("发射功率 (${data.size}字节)", "$signedPower dBm")
            }
            
            SERVICE_DATA_16BIT_UUID -> {
                if (data.size >= 2) {
                    val uuid = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
                    val uuidHex = String.format("%04X", uuid)
                    val serviceData = if (data.size > 2) {
                        bytesToHexString(data.copyOfRange(2, data.size))
                    } else ""
                    Pair("服务数据 (UUID: 0x$uuidHex, ${data.size}字节)", serviceData)
                } else {
                    Pair("服务数据 (16-bit, ${data.size}字节)", bytesToHexString(data))
                }
            }
            
            SERVICE_DATA_32BIT_UUID -> {
                Pair("服务数据 (32-bit, ${data.size}字节)", bytesToHexString(data))
            }
            
            SERVICE_DATA_128BIT_UUID -> {
                Pair("服务数据 (128-bit, ${data.size}字节)", bytesToHexString(data))
            }
            
            APPEARANCE -> {
                if (data.size >= 2) {
                    val appearance = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
                    Pair("外观 (${data.size}字节)", parseAppearance(appearance))
                } else {
                    Pair("外观 (${data.size}字节)", bytesToHexString(data))
                }
            }
            
            MANUFACTURER_SPECIFIC_DATA -> {
                if (data.size >= 2) {
                    val companyId = ((data[1].toInt() and 0xFF) shl 8) or (data[0].toInt() and 0xFF)
                    val companyHex = String.format("%04X", companyId)
                    val companyName = getCompanyName(companyId)
                    val mfgData = if (data.size > 2) {
                        val dataHex = bytesToHexString(data.copyOfRange(2, data.size))
                        // 厂商数据较长时进行分段展示，每16个字符一组
                        if (dataHex.length > 32) {
                            dataHex.chunked(32).joinToString("\n")
                        } else {
                            dataHex
                        }
                    } else ""
                    Pair("厂商数据 (0x$companyHex - $companyName, ${data.size}字节)", mfgData)
                } else {
                    Pair("厂商数据 (${data.size}字节)", bytesToHexString(data))
                }
            }
            
            else -> {
                // 未知类型，以十六进制显示
                Pair("类型 0x${type.toString(16).uppercase().padStart(2, '0')} (${data.size}字节)", bytesToHexString(data))
            }
        }
    }
    
    // 解析16位UUID列表
    private fun parseUUIDs16(data: ByteArray): String {
        val uuidList = mutableListOf<String>()
        
        for (i in 0 until data.size step 2) {
            if (i + 1 < data.size) {
                val uuid = ((data[i+1].toInt() and 0xFF) shl 8) or (data[i].toInt() and 0xFF)
                val uuidHex = String.format("%04X", uuid)
                val serviceName = getServiceName(uuid)
                if (serviceName.isNotEmpty()) {
                    uuidList.add("0x$uuidHex ($serviceName)")
                } else {
                    uuidList.add("0x$uuidHex")
                }
            }
        }
        
        return uuidList.joinToString(", ")
    }
    
    // 解析32位UUID列表
    private fun parseUUIDs32(data: ByteArray): String {
        val uuidList = mutableListOf<String>()
        
        for (i in 0 until data.size step 4) {
            if (i + 3 < data.size) {
                val uuid = ((data[i+3].toInt() and 0xFF) shl 24) or
                           ((data[i+2].toInt() and 0xFF) shl 16) or
                           ((data[i+1].toInt() and 0xFF) shl 8) or
                           (data[i].toInt() and 0xFF)
                uuidList.add(String.format("0x%08X", uuid))
            }
        }
        
        return uuidList.joinToString(", ")
    }
    
    // 解析128位UUID列表
    private fun parseUUIDs128(data: ByteArray): String {
        val uuidList = mutableListOf<String>()
        
        for (i in 0 until data.size step 16) {
            if (i + 15 < data.size) {
                val sb = StringBuilder()
                for (j in 15 downTo 0) {
                    sb.append(String.format("%02X", data[i+j]))
                    if (j == 13 || j == 11 || j == 9 || j == 7) {
                        sb.append('-')
                    }
                }
                uuidList.add(sb.toString())
            }
        }
        
        // 每个UUID单独一行
        return uuidList.joinToString("\n")
    }
    
    // 外观值解析
    private fun parseAppearance(appearance: Int): String {
        // 常见的外观值，参考Bluetooth Assigned Numbers文档
        val appearances = mapOf(
            0 to "未知",
            64 to "电话",
            128 to "电脑",
            192 to "手表",
            193 to "运动手表",
            256 to "时钟",
            320 to "显示设备",
            384 to "遥控器",
            448 to "眼镜",
            512 to "标签",
            576 to "钥匙环",
            640 to "媒体播放器",
            704 to "条码扫描器",
            768 to "温度计",
            769 to "温度计: 耳温",
            832 to "心率传感器",
            833 to "心率传感器: 心率带",
            896 to "血糖",
            960 to "血压",
            961 to "血压: 手臂式",
            962 to "血压: 腕式",
            1024 to "HID",
            1025 to "HID: 键盘",
            1026 to "HID: 鼠标",
            1027 to "HID: 游戏杆",
            1028 to "HID: 数字板",
            1029 to "HID: 卡片读取器",
            1030 to "HID: 数字钢笔",
            1031 to "HID: 条码扫描器",
            1088 to "诱饵",
            1152 to "控制器",
            1153 to "控制器: 游戏手柄",
            1154 to "控制器: 遥控器"
        )
        
        return appearances[appearance] ?: "未知 (0x${appearance.toString(16).uppercase()})"
    }
    
    // 获取公司名称
    private fun getCompanyName(companyId: Int): String {
        // 这里只列出了一些常见的公司ID
        val companies = mapOf(
            0x004C to "Apple",
            0x0006 to "Microsoft",
            0x000F to "Broadcom",
            0x0075 to "Samsung",
            0x0059 to "Nordic Semiconductor",
            0x004D to "Qualcomm",
            0x0078 to "ESPRESSIF Incorporated",
            0x0002 to "Intel",
            0x0499 to "Ruuvi Innovations",
            0x0001 to "Ericsson"
        )
        
        return companies[companyId] ?: "未知厂商"
    }
    
    // 获取服务名称
    private fun getServiceName(uuid16: Int): String {
        // 常见的16位UUID服务名称
        val services = mapOf(
            0x1800 to "Generic Access",
            0x1801 to "Generic Attribute",
            0x1802 to "Immediate Alert",
            0x1803 to "Link Loss",
            0x1804 to "Tx Power",
            0x1805 to "Current Time",
            0x1806 to "Reference Time Update",
            0x1807 to "Next DST Change",
            0x1808 to "Glucose",
            0x1809 to "Health Thermometer",
            0x180A to "Device Information",
            0x180D to "Heart Rate",
            0x180E to "Phone Alert Status",
            0x180F to "Battery",
            0x1810 to "Blood Pressure",
            0x1812 to "Human Interface Device",
            0x1813 to "Scan Parameters",
            0x1819 to "Location and Navigation"
        )
        
        return services[uuid16] ?: ""
    }
    
    // 辅助函数：十六进制字符串转字节数组
    private fun hexStringToByteArray(hexString: String): ByteArray {
        val result = ByteArray(hexString.length / 2)
        
        for (i in hexString.indices step 2) {
            val value = hexString.substring(i, i + 2).toInt(16)
            result[i / 2] = value.toByte()
        }
        
        return result
    }
    
    // 辅助函数：字节数组转十六进制字符串
    private fun bytesToHexString(bytes: ByteArray): String {
        return bytes.joinToString("") { 
            String.format("%02X", it) 
        }
    }
} 