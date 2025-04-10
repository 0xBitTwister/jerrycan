package com.example.jerrycan.model

import kotlinx.serialization.Serializable

/**
 * 消息方向枚举
 */
@Serializable
enum class MessageDirection {
    SEND,    // 发送消息
    RECEIVE  // 接收消息
} 