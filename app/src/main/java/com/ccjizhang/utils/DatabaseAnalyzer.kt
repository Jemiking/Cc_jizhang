package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.data.db.DatabaseConnectionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库分析器
 * 用于分析数据库结构、性能和优化建议
 */
@Singleton
class DatabaseAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionManager: DatabaseConnectionManager
) {
    companion object {
        private const val TAG = "DatabaseAnalyzer"
        private const val ANALYSIS_FOLDER = "db_analysis"
    }
    
    /**
     * 分析数据库
     * 返回数据库分析结果
     */
    suspend fun analyzeDatabase(): DatabaseAnalysisResult = withContext(Dispatchers.IO) {
        val result = DatabaseAnalysisResult()
        
        try {
            Log.i(TAG, "开始分析数据库")
            
            // 获取数据库文件信息
            val dbFile = context.getDatabasePath("ccjizhang_database_plain")
            if (dbFile.exists()) {
                result.databaseSize = dbFile.length()
                result.databasePath = dbFile.absolutePath
            }
            
            // 使用数据库连接分析数据库
            connectionManager.withConnection { connection ->
                // 分析数据库结构
                analyzeStructure(connection, result)
                
                // 分析索引使用情况
                analyzeIndexUsage(connection, result)
                
                // 分析查询性能
                analyzeQueryPerformance(connection, result)
                
                // 分析数据库配置
                analyzeDatabaseConfig(connection, result)
                
                // 分析数据库碎片化
                analyzeFragmentation(connection, result)
                
                // 生成优化建议
                generateOptimizationSuggestions(result)
            }
            
            // 保存分析结果
            saveAnalysisResult(result)
            
            Log.i(TAG, "数据库分析完成")
        } catch (e: Exception) {
            Log.e(TAG, "数据库分析失败", e)
            result.error = e.message ?: "未知错误"
        }
        
        return@withContext result
    }
    
    /**
     * 分析数据库结构
     */
    private fun analyzeStructure(connection: SupportSQLiteDatabase, result: DatabaseAnalysisResult) {
        try {
            // 获取表信息
            connection.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'").use { cursor ->
                while (cursor.moveToNext()) {
                    val tableName = cursor.getString(0)
                    val tableInfo = TableInfo(name = tableName)
                    
                    // 获取表行数
                    connection.query("SELECT COUNT(*) FROM `$tableName`").use { countCursor ->
                        if (countCursor.moveToFirst()) {
                            tableInfo.rowCount = countCursor.getLong(0)
                        }
                    }
                    
                    // 获取表结构
                    connection.query("PRAGMA table_info(`$tableName`)").use { infoCursor ->
                        while (infoCursor.moveToNext()) {
                            val columnName = infoCursor.getString(1)
                            val columnType = infoCursor.getString(2)
                            val notNull = infoCursor.getInt(3) == 1
                            val isPrimaryKey = infoCursor.getInt(5) > 0
                            
                            tableInfo.columns.add(
                                ColumnInfo(
                                    name = columnName,
                                    type = columnType,
                                    notNull = notNull,
                                    isPrimaryKey = isPrimaryKey
                                )
                            )
                        }
                    }
                    
                    // 获取表索引
                    connection.query("PRAGMA index_list(`$tableName`)").use { indexCursor ->
                        while (indexCursor.moveToNext()) {
                            val indexName = indexCursor.getString(1)
                            val isUnique = indexCursor.getInt(2) == 1
                            
                            val indexInfo = IndexInfo(
                                name = indexName,
                                isUnique = isUnique
                            )
                            
                            // 获取索引列
                            connection.query("PRAGMA index_info(`$indexName`)").use { indexInfoCursor ->
                                while (indexInfoCursor.moveToNext()) {
                                    val columnName = indexInfoCursor.getString(2)
                                    indexInfo.columns.add(columnName)
                                }
                            }
                            
                            tableInfo.indexes.add(indexInfo)
                        }
                    }
                    
                    // 获取表外键
                    connection.query("PRAGMA foreign_key_list(`$tableName`)").use { fkCursor ->
                        while (fkCursor.moveToNext()) {
                            val id = fkCursor.getInt(0)
                            val refTable = fkCursor.getString(2)
                            val fromColumn = fkCursor.getString(3)
                            val toColumn = fkCursor.getString(4)
                            val onUpdate = fkCursor.getString(5)
                            val onDelete = fkCursor.getString(6)
                            
                            tableInfo.foreignKeys.add(
                                ForeignKeyInfo(
                                    id = id,
                                    refTable = refTable,
                                    fromColumn = fromColumn,
                                    toColumn = toColumn,
                                    onUpdate = onUpdate,
                                    onDelete = onDelete
                                )
                            )
                        }
                    }
                    
                    result.tables.add(tableInfo)
                }
            }
            
            // 获取视图信息
            connection.query("SELECT name FROM sqlite_master WHERE type='view' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'").use { cursor ->
                while (cursor.moveToNext()) {
                    val viewName = cursor.getString(0)
                    
                    // 获取视图定义
                    connection.query("SELECT sql FROM sqlite_master WHERE type='view' AND name=?", arrayOf(viewName)).use { sqlCursor ->
                        if (sqlCursor.moveToFirst()) {
                            val sql = sqlCursor.getString(0)
                            result.views.add(ViewInfo(name = viewName, sql = sql))
                        }
                    }
                }
            }
            
            // 获取触发器信息
            connection.query("SELECT name FROM sqlite_master WHERE type='trigger' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'").use { cursor ->
                while (cursor.moveToNext()) {
                    val triggerName = cursor.getString(0)
                    
                    // 获取触发器定义
                    connection.query("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?", arrayOf(triggerName)).use { sqlCursor ->
                        if (sqlCursor.moveToFirst()) {
                            val sql = sqlCursor.getString(0)
                            result.triggers.add(TriggerInfo(name = triggerName, sql = sql))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "分析数据库结构失败", e)
            throw e
        }
    }
    
    /**
     * 分析索引使用情况
     */
    private fun analyzeIndexUsage(connection: SupportSQLiteDatabase, result: DatabaseAnalysisResult) {
        try {
            // 获取索引统计信息
            connection.query("ANALYZE").use { }
            
            // 对每个表的每个索引进行分析
            result.tables.forEach { table ->
                table.indexes.forEach { index ->
                    // 检查索引是否有效
                    val isEffective = isIndexEffective(connection, table.name, index)
                    index.isEffective = isEffective
                    
                    // 如果索引无效，添加到优化建议中
                    if (!isEffective) {
                        result.indexOptimizationSuggestions.add(
                            "考虑删除表 ${table.name} 上的无效索引 ${index.name}"
                        )
                    }
                }
                
                // 检查是否需要添加索引
                val missingIndexes = findMissingIndexes(connection, table)
                if (missingIndexes.isNotEmpty()) {
                    result.missingIndexes[table.name] = missingIndexes
                    
                    missingIndexes.forEach { column ->
                        result.indexOptimizationSuggestions.add(
                            "考虑在表 ${table.name} 的列 $column 上添加索引"
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "分析索引使用情况失败", e)
            // 不抛出异常，继续分析其他部分
        }
    }
    
    /**
     * 检查索引是否有效
     */
    private fun isIndexEffective(connection: SupportSQLiteDatabase, tableName: String, index: IndexInfo): Boolean {
        // 这里只是一个简单的启发式检查，实际上需要更复杂的分析
        
        // 如果是主键索引，通常是有效的
        if (index.name.contains("autoindex") || index.name.endsWith("_pkey")) {
            return true
        }
        
        // 如果是外键索引，通常是有效的
        if (index.name.endsWith("_fkey") || index.name.contains("_fk_")) {
            return true
        }
        
        // 如果是唯一索引，通常是有效的
        if (index.isUnique) {
            return true
        }
        
        // 检查索引列是否经常在WHERE子句中使用
        // 这需要查询日志分析，这里简化处理
        return true
    }
    
    /**
     * 查找可能缺失的索引
     */
    private fun findMissingIndexes(connection: SupportSQLiteDatabase, table: TableInfo): List<String> {
        val missingIndexes = mutableListOf<String>()
        
        // 检查外键列是否有索引
        table.foreignKeys.forEach { fk ->
            val hasIndex = table.indexes.any { index ->
                index.columns.contains(fk.fromColumn)
            }
            
            if (!hasIndex) {
                missingIndexes.add(fk.fromColumn)
            }
        }
        
        // 检查经常在WHERE子句中使用的列
        // 这需要查询日志分析，这里简化处理
        
        return missingIndexes
    }
    
    /**
     * 分析查询性能
     */
    private fun analyzeQueryPerformance(connection: SupportSQLiteDatabase, result: DatabaseAnalysisResult) {
        try {
            // 执行一些测试查询，测量性能
            val testQueries = listOf(
                "SELECT COUNT(*) FROM sqlite_master",
                "PRAGMA integrity_check",
                "PRAGMA quick_check"
            )
            
            testQueries.forEach { query ->
                val startTime = System.currentTimeMillis()
                connection.query(query).use { cursor ->
                    cursor.moveToFirst()
                }
                val executionTime = System.currentTimeMillis() - startTime
                
                result.queryPerformanceTests.add(
                    QueryPerformanceTest(
                        query = query,
                        executionTime = executionTime
                    )
                )
            }
            
            // 对每个表执行计数查询
            result.tables.forEach { table ->
                val query = "SELECT COUNT(*) FROM `${table.name}`"
                val startTime = System.currentTimeMillis()
                connection.query(query).use { cursor ->
                    cursor.moveToFirst()
                }
                val executionTime = System.currentTimeMillis() - startTime
                
                result.queryPerformanceTests.add(
                    QueryPerformanceTest(
                        query = query,
                        executionTime = executionTime
                    )
                )
                
                // 如果查询时间过长，添加到优化建议中
                if (executionTime > 100) {
                    result.queryOptimizationSuggestions.add(
                        "表 ${table.name} 的计数查询耗时 ${executionTime}ms，考虑优化表结构或添加索引"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "分析查询性能失败", e)
            // 不抛出异常，继续分析其他部分
        }
    }
    
    /**
     * 分析数据库配置
     */
    private fun analyzeDatabaseConfig(connection: SupportSQLiteDatabase, result: DatabaseAnalysisResult) {
        try {
            // 获取数据库配置
            val pragmas = listOf(
                "journal_mode",
                "synchronous",
                "cache_size",
                "page_size",
                "auto_vacuum",
                "foreign_keys",
                "secure_delete",
                "temp_store",
                "mmap_size"
            )
            
            pragmas.forEach { pragma ->
                connection.query("PRAGMA $pragma").use { cursor ->
                    if (cursor.moveToFirst()) {
                        val value = when (cursor.getType(0)) {
                            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getInt(0).toString()
                            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getFloat(0).toString()
                            android.database.Cursor.FIELD_TYPE_STRING -> cursor.getString(0)
                            else -> "未知"
                        }
                        
                        result.databaseConfig[pragma] = value
                    }
                }
            }
            
            // 分析配置并生成优化建议
            
            // 检查日志模式
            val journalMode = result.databaseConfig["journal_mode"]
            if (journalMode != "WAL") {
                result.configOptimizationSuggestions.add(
                    "考虑将日志模式设置为 WAL 以提高并发性能"
                )
            }
            
            // 检查同步模式
            val synchronous = result.databaseConfig["synchronous"]
            if (synchronous == "2") {
                result.configOptimizationSuggestions.add(
                    "考虑将同步模式设置为 NORMAL (1) 以提高性能，但会略微降低崩溃安全性"
                )
            }
            
            // 检查缓存大小
            val cacheSize = result.databaseConfig["cache_size"]?.toIntOrNull() ?: 0
            if (cacheSize < 2000) {
                result.configOptimizationSuggestions.add(
                    "考虑增加缓存大小以提高性能，当前值: $cacheSize，建议值: 2000-10000"
                )
            }
            
            // 检查页大小
            val pageSize = result.databaseConfig["page_size"]?.toIntOrNull() ?: 0
            if (pageSize < 4096) {
                result.configOptimizationSuggestions.add(
                    "考虑增加页大小以提高性能，当前值: $pageSize，建议值: 4096"
                )
            }
            
            // 检查自动清理
            val autoVacuum = result.databaseConfig["auto_vacuum"]
            if (autoVacuum != "1" && autoVacuum != "2") {
                result.configOptimizationSuggestions.add(
                    "考虑启用自动清理 (FULL 或 INCREMENTAL) 以减少数据库碎片化"
                )
            }
            
            // 检查内存映射
            val mmapSize = result.databaseConfig["mmap_size"]?.toLongOrNull() ?: 0
            if (mmapSize == 0L) {
                result.configOptimizationSuggestions.add(
                    "考虑启用内存映射以提高性能，建议值: 数据库大小的一半"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "分析数据库配置失败", e)
            // 不抛出异常，继续分析其他部分
        }
    }
    
    /**
     * 分析数据库碎片化
     */
    private fun analyzeFragmentation(connection: SupportSQLiteDatabase, result: DatabaseAnalysisResult) {
        try {
            // 获取数据库碎片化信息
            connection.query("PRAGMA page_count").use { cursor ->
                if (cursor.moveToFirst()) {
                    result.pageCount = cursor.getInt(0)
                }
            }
            
            connection.query("PRAGMA freelist_count").use { cursor ->
                if (cursor.moveToFirst()) {
                    result.freelistCount = cursor.getInt(0)
                }
            }
            
            // 计算碎片化百分比
            if (result.pageCount > 0) {
                result.fragmentationPercent = (result.freelistCount.toFloat() / result.pageCount) * 100
            }
            
            // 如果碎片化程度高，添加到优化建议中
            if (result.fragmentationPercent > 10) {
                result.fragmentationOptimizationSuggestions.add(
                    "数据库碎片化程度高 (${result.fragmentationPercent}%)，建议执行 VACUUM 操作"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "分析数据库碎片化失败", e)
            // 不抛出异常，继续分析其他部分
        }
    }
    
    /**
     * 生成优化建议
     */
    private fun generateOptimizationSuggestions(result: DatabaseAnalysisResult) {
        // 检查数据库大小
        if (result.databaseSize > 50 * 1024 * 1024) { // 50MB
            result.generalOptimizationSuggestions.add(
                "数据库大小较大 (${result.databaseSize / (1024 * 1024)} MB)，考虑实现数据归档或清理旧数据"
            )
        }
        
        // 检查表大小
        result.tables.forEach { table ->
            if (table.rowCount > 100000) {
                result.generalOptimizationSuggestions.add(
                    "表 ${table.name} 的行数较多 (${table.rowCount})，考虑分表或归档旧数据"
                )
            }
        }
        
        // 检查索引数量
        result.tables.forEach { table ->
            if (table.indexes.size > 5) {
                result.generalOptimizationSuggestions.add(
                    "表 ${table.name} 的索引数量较多 (${table.indexes.size})，考虑删除不必要的索引"
                )
            }
        }
        
        // 检查外键约束
        result.tables.forEach { table ->
            if (table.foreignKeys.size > 3) {
                result.generalOptimizationSuggestions.add(
                    "表 ${table.name} 的外键约束较多 (${table.foreignKeys.size})，考虑优化表结构"
                )
            }
        }
    }
    
    /**
     * 保存分析结果
     */
    private fun saveAnalysisResult(result: DatabaseAnalysisResult) {
        try {
            val analysisDir = File(context.filesDir, ANALYSIS_FOLDER).apply {
                if (!exists()) mkdirs()
            }
            
            val analysisFile = File(
                analysisDir,
                "db_analysis_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            )
            
            // 构建分析报告
            val report = buildString {
                append("=== 数据库分析报告 ===\n")
                append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")
                
                append("--- 数据库信息 ---\n")
                append("路径: ${result.databasePath}\n")
                append("大小: ${result.databaseSize / 1024} KB\n")
                append("页数: ${result.pageCount}\n")
                append("空闲页数: ${result.freelistCount}\n")
                append("碎片化程度: ${result.fragmentationPercent}%\n\n")
                
                append("--- 数据库配置 ---\n")
                result.databaseConfig.forEach { (pragma, value) ->
                    append("$pragma: $value\n")
                }
                append("\n")
                
                append("--- 表信息 ---\n")
                result.tables.forEach { table ->
                    append("表名: ${table.name}\n")
                    append("  行数: ${table.rowCount}\n")
                    append("  列数: ${table.columns.size}\n")
                    append("  索引数: ${table.indexes.size}\n")
                    append("  外键数: ${table.foreignKeys.size}\n")
                    
                    append("  列:\n")
                    table.columns.forEach { column ->
                        append("    ${column.name} (${column.type})")
                        if (column.isPrimaryKey) append(" [主键]")
                        if (column.notNull) append(" [非空]")
                        append("\n")
                    }
                    
                    append("  索引:\n")
                    table.indexes.forEach { index ->
                        append("    ${index.name}")
                        if (index.isUnique) append(" [唯一]")
                        if (index.isEffective) append(" [有效]") else append(" [无效]")
                        append(" - 列: ${index.columns.joinToString(", ")}\n")
                    }
                    
                    append("  外键:\n")
                    table.foreignKeys.forEach { fk ->
                        append("    ${fk.fromColumn} -> ${fk.refTable}.${fk.toColumn}")
                        append(" [ON UPDATE ${fk.onUpdate}] [ON DELETE ${fk.onDelete}]\n")
                    }
                    
                    append("\n")
                }
                
                append("--- 视图信息 ---\n")
                if (result.views.isEmpty()) {
                    append("无视图\n")
                } else {
                    result.views.forEach { view ->
                        append("视图名: ${view.name}\n")
                        append("  定义: ${view.sql}\n\n")
                    }
                }
                append("\n")
                
                append("--- 触发器信息 ---\n")
                if (result.triggers.isEmpty()) {
                    append("无触发器\n")
                } else {
                    result.triggers.forEach { trigger ->
                        append("触发器名: ${trigger.name}\n")
                        append("  定义: ${trigger.sql}\n\n")
                    }
                }
                append("\n")
                
                append("--- 查询性能测试 ---\n")
                result.queryPerformanceTests.forEach { test ->
                    append("${test.query} - ${test.executionTime}ms\n")
                }
                append("\n")
                
                append("--- 优化建议 ---\n")
                
                append("一般建议:\n")
                if (result.generalOptimizationSuggestions.isEmpty()) {
                    append("  无\n")
                } else {
                    result.generalOptimizationSuggestions.forEachIndexed { index, suggestion ->
                        append("  ${index + 1}. $suggestion\n")
                    }
                }
                append("\n")
                
                append("索引优化建议:\n")
                if (result.indexOptimizationSuggestions.isEmpty()) {
                    append("  无\n")
                } else {
                    result.indexOptimizationSuggestions.forEachIndexed { index, suggestion ->
                        append("  ${index + 1}. $suggestion\n")
                    }
                }
                append("\n")
                
                append("查询优化建议:\n")
                if (result.queryOptimizationSuggestions.isEmpty()) {
                    append("  无\n")
                } else {
                    result.queryOptimizationSuggestions.forEachIndexed { index, suggestion ->
                        append("  ${index + 1}. $suggestion\n")
                    }
                }
                append("\n")
                
                append("配置优化建议:\n")
                if (result.configOptimizationSuggestions.isEmpty()) {
                    append("  无\n")
                } else {
                    result.configOptimizationSuggestions.forEachIndexed { index, suggestion ->
                        append("  ${index + 1}. $suggestion\n")
                    }
                }
                append("\n")
                
                append("碎片化优化建议:\n")
                if (result.fragmentationOptimizationSuggestions.isEmpty()) {
                    append("  无\n")
                } else {
                    result.fragmentationOptimizationSuggestions.forEachIndexed { index, suggestion ->
                        append("  ${index + 1}. $suggestion\n")
                    }
                }
                append("\n")
                
                if (result.error != null) {
                    append("--- 错误信息 ---\n")
                    append("错误: ${result.error}\n\n")
                }
            }
            
            // 写入分析报告
            analysisFile.writeText(report)
            
            Log.i(TAG, "数据库分析报告已保存: ${analysisFile.path}")
        } catch (e: Exception) {
            Log.e(TAG, "保存分析结果失败", e)
        }
    }
    
    /**
     * 数据库分析结果数据类
     */
    data class DatabaseAnalysisResult(
        // 数据库信息
        var databasePath: String = "",
        var databaseSize: Long = 0,
        var pageCount: Int = 0,
        var freelistCount: Int = 0,
        var fragmentationPercent: Float = 0f,
        
        // 数据库配置
        var databaseConfig: MutableMap<String, String> = mutableMapOf(),
        
        // 表信息
        var tables: MutableList<TableInfo> = mutableListOf(),
        
        // 视图信息
        var views: MutableList<ViewInfo> = mutableListOf(),
        
        // 触发器信息
        var triggers: MutableList<TriggerInfo> = mutableListOf(),
        
        // 缺失的索引
        var missingIndexes: MutableMap<String, List<String>> = mutableMapOf(),
        
        // 查询性能测试
        var queryPerformanceTests: MutableList<QueryPerformanceTest> = mutableListOf(),
        
        // 优化建议
        var generalOptimizationSuggestions: MutableList<String> = mutableListOf(),
        var indexOptimizationSuggestions: MutableList<String> = mutableListOf(),
        var queryOptimizationSuggestions: MutableList<String> = mutableListOf(),
        var configOptimizationSuggestions: MutableList<String> = mutableListOf(),
        var fragmentationOptimizationSuggestions: MutableList<String> = mutableListOf(),
        
        // 错误信息
        var error: String? = null
    )
    
    /**
     * 表信息数据类
     */
    data class TableInfo(
        val name: String,
        var rowCount: Long = 0,
        val columns: MutableList<ColumnInfo> = mutableListOf(),
        val indexes: MutableList<IndexInfo> = mutableListOf(),
        val foreignKeys: MutableList<ForeignKeyInfo> = mutableListOf()
    )
    
    /**
     * 列信息数据类
     */
    data class ColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val isPrimaryKey: Boolean
    )
    
    /**
     * 索引信息数据类
     */
    data class IndexInfo(
        val name: String,
        val isUnique: Boolean,
        val columns: MutableList<String> = mutableListOf(),
        var isEffective: Boolean = true
    )
    
    /**
     * 外键信息数据类
     */
    data class ForeignKeyInfo(
        val id: Int,
        val refTable: String,
        val fromColumn: String,
        val toColumn: String,
        val onUpdate: String,
        val onDelete: String
    )
    
    /**
     * 视图信息数据类
     */
    data class ViewInfo(
        val name: String,
        val sql: String
    )
    
    /**
     * 触发器信息数据类
     */
    data class TriggerInfo(
        val name: String,
        val sql: String
    )
    
    /**
     * 查询性能测试数据类
     */
    data class QueryPerformanceTest(
        val query: String,
        val executionTime: Long
    )
}
