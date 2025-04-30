package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户反馈管理器
 * 用于收集、分类和分析用户反馈
 */
@Singleton
class UserFeedbackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UserFeedbackManager"
        private const val FEEDBACK_FOLDER = "user_feedback"
        private const val MAX_FEEDBACK_FILES = 100
    }
    
    /**
     * 反馈类型
     */
    enum class FeedbackType {
        BUG_REPORT,        // 错误报告
        FEATURE_REQUEST,   // 功能请求
        PERFORMANCE_ISSUE, // 性能问题
        UI_SUGGESTION,     // UI建议
        DATABASE_ISSUE,    // 数据库问题
        OTHER              // 其他
    }
    
    /**
     * 反馈优先级
     */
    enum class FeedbackPriority {
        LOW,    // 低
        MEDIUM, // 中
        HIGH,   // 高
        CRITICAL // 关键
    }
    
    /**
     * 反馈状态
     */
    enum class FeedbackStatus {
        NEW,        // 新建
        REVIEWING,  // 审核中
        ACCEPTED,   // 已接受
        REJECTED,   // 已拒绝
        IN_PROGRESS,// 处理中
        RESOLVED,   // 已解决
        CLOSED      // 已关闭
    }
    
    /**
     * 用户反馈数据类
     */
    data class UserFeedback(
        val id: String = UUID.randomUUID().toString(),
        val timestamp: Long = System.currentTimeMillis(),
        val userId: String = "",
        val deviceInfo: String = "",
        val appVersion: String = "",
        val type: FeedbackType = FeedbackType.OTHER,
        val priority: FeedbackPriority = FeedbackPriority.MEDIUM,
        val status: FeedbackStatus = FeedbackStatus.NEW,
        val title: String = "",
        val description: String = "",
        val attachments: List<String> = emptyList(),
        val tags: List<String> = emptyList(),
        val relatedFeedbackIds: List<String> = emptyList()
    )
    
    /**
     * 提交用户反馈
     */
    suspend fun submitFeedback(feedback: UserFeedback): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "提交用户反馈: ${feedback.id}, 类型: ${feedback.type}, 标题: ${feedback.title}")
            
            // 创建反馈目录
            val feedbackDir = File(context.filesDir, FEEDBACK_FOLDER).apply {
                if (!exists()) mkdirs()
            }
            
            // 创建反馈文件
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val feedbackFile = File(
                feedbackDir,
                "feedback_${dateFormat.format(Date(feedback.timestamp))}_${feedback.id}.json"
            )
            
            // 将反馈转换为JSON并保存
            val json = """
                {
                    "id": "${feedback.id}",
                    "timestamp": ${feedback.timestamp},
                    "userId": "${feedback.userId}",
                    "deviceInfo": "${feedback.deviceInfo}",
                    "appVersion": "${feedback.appVersion}",
                    "type": "${feedback.type}",
                    "priority": "${feedback.priority}",
                    "status": "${feedback.status}",
                    "title": "${feedback.title}",
                    "description": "${feedback.description}",
                    "attachments": [${feedback.attachments.joinToString { "\"$it\"" }}],
                    "tags": [${feedback.tags.joinToString { "\"$it\"" }}],
                    "relatedFeedbackIds": [${feedback.relatedFeedbackIds.joinToString { "\"$it\"" }}]
                }
            """.trimIndent()
            
            feedbackFile.writeText(json)
            
            // 清理旧反馈文件
            cleanupOldFeedbackFiles(feedbackDir)
            
            Log.i(TAG, "用户反馈已保存: ${feedbackFile.path}")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "保存用户反馈失败", e)
            return@withContext false
        }
    }
    
    /**
     * 获取所有反馈
     */
    suspend fun getAllFeedback(): List<UserFeedback> = withContext(Dispatchers.IO) {
        try {
            val feedbackDir = File(context.filesDir, FEEDBACK_FOLDER)
            if (!feedbackDir.exists()) return@withContext emptyList()
            
            val feedbackFiles = feedbackDir.listFiles()?.filter { it.name.endsWith(".json") } ?: return@withContext emptyList()
            
            return@withContext feedbackFiles.mapNotNull { file ->
                try {
                    val json = file.readText()
                    parseJsonToFeedback(json)
                } catch (e: Exception) {
                    Log.e(TAG, "解析反馈文件失败: ${file.name}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取所有反馈失败", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * 按类型获取反馈
     */
    suspend fun getFeedbackByType(type: FeedbackType): List<UserFeedback> = withContext(Dispatchers.IO) {
        val allFeedback = getAllFeedback()
        return@withContext allFeedback.filter { it.type == type }
    }
    
    /**
     * 按优先级获取反馈
     */
    suspend fun getFeedbackByPriority(priority: FeedbackPriority): List<UserFeedback> = withContext(Dispatchers.IO) {
        val allFeedback = getAllFeedback()
        return@withContext allFeedback.filter { it.priority == priority }
    }
    
    /**
     * 按状态获取反馈
     */
    suspend fun getFeedbackByStatus(status: FeedbackStatus): List<UserFeedback> = withContext(Dispatchers.IO) {
        val allFeedback = getAllFeedback()
        return@withContext allFeedback.filter { it.status == status }
    }
    
    /**
     * 更新反馈状态
     */
    suspend fun updateFeedbackStatus(feedbackId: String, newStatus: FeedbackStatus): Boolean = withContext(Dispatchers.IO) {
        try {
            val allFeedback = getAllFeedback()
            val feedback = allFeedback.find { it.id == feedbackId } ?: return@withContext false
            
            val updatedFeedback = feedback.copy(status = newStatus)
            return@withContext submitFeedback(updatedFeedback)
        } catch (e: Exception) {
            Log.e(TAG, "更新反馈状态失败", e)
            return@withContext false
        }
    }
    
    /**
     * 生成反馈分析报告
     */
    suspend fun generateFeedbackAnalysisReport(): File? = withContext(Dispatchers.IO) {
        try {
            val allFeedback = getAllFeedback()
            if (allFeedback.isEmpty()) return@withContext null
            
            // 创建报告目录
            val reportsDir = File(context.filesDir, "feedback_reports").apply {
                if (!exists()) mkdirs()
            }
            
            // 创建报告文件
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val reportFile = File(
                reportsDir,
                "feedback_analysis_${dateFormat.format(Date())}.txt"
            )
            
            // 按类型统计
            val typeStats = FeedbackType.values().associateWith { type ->
                allFeedback.count { it.type == type }
            }
            
            // 按优先级统计
            val priorityStats = FeedbackPriority.values().associateWith { priority ->
                allFeedback.count { it.priority == priority }
            }
            
            // 按状态统计
            val statusStats = FeedbackStatus.values().associateWith { status ->
                allFeedback.count { it.status == status }
            }
            
            // 生成报告内容
            val reportContent = """
                CC记账用户反馈分析报告
                ===========================
                生成时间: ${dateFormat.format(Date())}
                
                总反馈数: ${allFeedback.size}
                
                按类型统计:
                ${typeStats.entries.joinToString("\n") { "- ${it.key}: ${it.value} (${(it.value * 100.0 / allFeedback.size).toInt()}%)" }}
                
                按优先级统计:
                ${priorityStats.entries.joinToString("\n") { "- ${it.key}: ${it.value} (${(it.value * 100.0 / allFeedback.size).toInt()}%)" }}
                
                按状态统计:
                ${statusStats.entries.joinToString("\n") { "- ${it.key}: ${it.value} (${(it.value * 100.0 / allFeedback.size).toInt()}%)" }}
                
                数据库相关反馈:
                ${allFeedback.filter { it.type == FeedbackType.DATABASE_ISSUE }.joinToString("\n\n") { 
                    """
                    ID: ${it.id}
                    标题: ${it.title}
                    描述: ${it.description}
                    优先级: ${it.priority}
                    状态: ${it.status}
                    时间: ${dateFormat.format(Date(it.timestamp))}
                    """.trimIndent()
                }}
                
                高优先级反馈:
                ${allFeedback.filter { it.priority == FeedbackPriority.HIGH || it.priority == FeedbackPriority.CRITICAL }.joinToString("\n\n") { 
                    """
                    ID: ${it.id}
                    类型: ${it.type}
                    标题: ${it.title}
                    描述: ${it.description}
                    状态: ${it.status}
                    时间: ${dateFormat.format(Date(it.timestamp))}
                    """.trimIndent()
                }}
                
                未解决反馈:
                ${allFeedback.filter { it.status != FeedbackStatus.RESOLVED && it.status != FeedbackStatus.CLOSED }.joinToString("\n\n") { 
                    """
                    ID: ${it.id}
                    类型: ${it.type}
                    优先级: ${it.priority}
                    标题: ${it.title}
                    状态: ${it.status}
                    时间: ${dateFormat.format(Date(it.timestamp))}
                    """.trimIndent()
                }}
                
                建议优先处理的反馈:
                ${allFeedback.filter { 
                    (it.priority == FeedbackPriority.HIGH || it.priority == FeedbackPriority.CRITICAL) && 
                    (it.status != FeedbackStatus.RESOLVED && it.status != FeedbackStatus.CLOSED)
                }.joinToString("\n\n") { 
                    """
                    ID: ${it.id}
                    类型: ${it.type}
                    优先级: ${it.priority}
                    标题: ${it.title}
                    描述: ${it.description}
                    状态: ${it.status}
                    时间: ${dateFormat.format(Date(it.timestamp))}
                    """.trimIndent()
                }}
            """.trimIndent()
            
            reportFile.writeText(reportContent)
            
            Log.i(TAG, "反馈分析报告已生成: ${reportFile.path}")
            return@withContext reportFile
        } catch (e: Exception) {
            Log.e(TAG, "生成反馈分析报告失败", e)
            return@withContext null
        }
    }
    
    /**
     * 清理旧反馈文件
     */
    private fun cleanupOldFeedbackFiles(feedbackDir: File) {
        val feedbackFiles = feedbackDir.listFiles()?.filter { it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() } ?: return
        
        if (feedbackFiles.size > MAX_FEEDBACK_FILES) {
            feedbackFiles.drop(MAX_FEEDBACK_FILES).forEach { it.delete() }
            Log.i(TAG, "已清理 ${feedbackFiles.size - MAX_FEEDBACK_FILES} 个旧反馈文件")
        }
    }
    
    /**
     * 解析JSON为反馈对象
     * 简单实现，实际应用中可以使用Gson或Moshi等库
     */
    private fun parseJsonToFeedback(json: String): UserFeedback? {
        // 这里使用简单的字符串解析，实际应用中应该使用JSON解析库
        try {
            val id = json.substringAfter("\"id\": \"").substringBefore("\"")
            val timestamp = json.substringAfter("\"timestamp\": ").substringBefore(",").toLong()
            val userId = json.substringAfter("\"userId\": \"").substringBefore("\"")
            val deviceInfo = json.substringAfter("\"deviceInfo\": \"").substringBefore("\"")
            val appVersion = json.substringAfter("\"appVersion\": \"").substringBefore("\"")
            val typeStr = json.substringAfter("\"type\": \"").substringBefore("\"")
            val priorityStr = json.substringAfter("\"priority\": \"").substringBefore("\"")
            val statusStr = json.substringAfter("\"status\": \"").substringBefore("\"")
            val title = json.substringAfter("\"title\": \"").substringBefore("\"")
            val description = json.substringAfter("\"description\": \"").substringBefore("\"")
            
            val type = try {
                FeedbackType.valueOf(typeStr)
            } catch (e: Exception) {
                FeedbackType.OTHER
            }
            
            val priority = try {
                FeedbackPriority.valueOf(priorityStr)
            } catch (e: Exception) {
                FeedbackPriority.MEDIUM
            }
            
            val status = try {
                FeedbackStatus.valueOf(statusStr)
            } catch (e: Exception) {
                FeedbackStatus.NEW
            }
            
            return UserFeedback(
                id = id,
                timestamp = timestamp,
                userId = userId,
                deviceInfo = deviceInfo,
                appVersion = appVersion,
                type = type,
                priority = priority,
                status = status,
                title = title,
                description = description
            )
        } catch (e: Exception) {
            Log.e(TAG, "解析JSON为反馈对象失败", e)
            return null
        }
    }
}
