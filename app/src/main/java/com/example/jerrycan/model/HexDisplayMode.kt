package com.example.jerrycan.model

/**
 * 十六进制数据显示模式
 */
enum class HexDisplayMode(val label: String) {
    RAW("原始"),
    GROUPED("分组"),
    ASCII("ASCII")
}

/**
 * 所有可用的显示模式列表
 */
val HexDisplayModes = listOf(
    HexDisplayMode.GROUPED,
    HexDisplayMode.RAW,
    HexDisplayMode.ASCII
) 