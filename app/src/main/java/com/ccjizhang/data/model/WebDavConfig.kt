package com.ccjizhang.data.model

/**
 * WebDAV服务器配置
 */
data class WebDavConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
    val syncFolder: String = "ccjizhang",
    val autoSync: Boolean = false,
    val syncInterval: Long = 24 // 小时
) 