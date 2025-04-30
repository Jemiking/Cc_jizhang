package com.ccjizhang.ui.common

/**
 * 通用的操作结果密封类
 * 用于表示ViewModel操作的状态（加载中、成功、失败）
 */
sealed class OperationResult {
    /** 操作成功，可以附带可选的成功消息 */
    data class Success(val message: String? = null) : OperationResult()
    /** 操作失败，必须附带错误消息 */
    data class Error(val message: String) : OperationResult()
    /** 操作正在进行中 */
    object Loading : OperationResult()
} 