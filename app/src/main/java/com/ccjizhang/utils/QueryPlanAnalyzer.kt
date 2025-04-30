package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ccjizhang.data.db.DatabaseConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 查询计划分析工具
 * 用于分析 SQLite 查询的执行计划，帮助优化查询性能
 */
@Singleton
class QueryPlanAnalyzer @Inject constructor(
    private val context: Context,
    private val connectionManager: DatabaseConnectionManager
) {
    companion object {
        private const val TAG = "QueryPlanAnalyzer"
        private const val QUERY_PLAN_FOLDER = "query_plans"
        private const val MAX_PLANS = 10
    }

    /**
     * 分析查询计划
     * @param query SQL查询语句
     * @param bindArgs 查询参数
     * @return 查询计划分析结果
     */
    suspend fun analyzeQueryPlan(query: String, bindArgs: Array<Any>? = null): QueryPlanResult = withContext(Dispatchers.IO) {
        val result = QueryPlanResult(query = query, bindArgs = bindArgs?.joinToString())

        try {
            connectionManager.withConnection { connection ->
                // 构建 EXPLAIN QUERY PLAN 语句
                val explainQuery = "EXPLAIN QUERY PLAN $query"

                // 执行查询计划分析
                val bindArgsArray = bindArgs?.let { args -> Array<Any?>(args.size) { i -> args[i] } }
                connection.query(explainQuery, bindArgsArray ?: emptyArray()).use { cursor ->
                    val planSteps = mutableListOf<QueryPlanStep>()

                    while (cursor.moveToNext()) {
                        val id = cursor.getInt(0)
                        val parent = cursor.getInt(1)
                        val notUsed = cursor.getInt(2)
                        val detail = cursor.getString(3)

                        planSteps.add(
                            QueryPlanStep(
                                id = id,
                                parent = parent,
                                notUsed = notUsed,
                                detail = detail
                            )
                        )
                    }

                    result.planSteps = planSteps
                }

                // 执行实际查询并测量性能
                val startTime = System.currentTimeMillis()
                connection.query(query, bindArgsArray ?: emptyArray()).use { cursor ->
                    result.resultCount = cursor.count
                }
                val endTime = System.currentTimeMillis()
                result.executionTime = endTime - startTime

                // 分析查询计划
                analyzeQueryPlanSteps(result)
            }

            // 保存查询计划
            saveQueryPlan(result)

        } catch (e: Exception) {
            Log.e(TAG, "分析查询计划失败", e)
            result.error = e.message ?: "未知错误"
        }

        return@withContext result
    }

    /**
     * 分析查询计划步骤
     * @param result 查询计划结果
     */
    private fun analyzeQueryPlanSteps(result: QueryPlanResult) {
        // 检查是否使用了索引
        var usesIndex = false
        var usesFullTableScan = false
        var usesTemporaryTable = false
        var usesSort = false

        for (step in result.planSteps) {
            val detail = step.detail.lowercase()

            // 检查是否使用了索引
            if (detail.contains("using index") || detail.contains("covering index")) {
                usesIndex = true
                result.usedIndexes.add(extractIndexName(detail))
            }

            // 检查是否进行了全表扫描
            if (detail.contains("scan") && !detail.contains("index")) {
                usesFullTableScan = true
                result.scannedTables.add(extractTableName(detail))
            }

            // 检查是否使用了临时表
            if (detail.contains("temp") || detail.contains("temporary")) {
                usesTemporaryTable = true
            }

            // 检查是否进行了排序
            if (detail.contains("sort")) {
                usesSort = true
            }
        }

        // 设置分析结果
        result.usesIndex = usesIndex
        result.usesFullTableScan = usesFullTableScan
        result.usesTemporaryTable = usesTemporaryTable
        result.usesSort = usesSort

        // 生成优化建议
        generateOptimizationSuggestions(result)
    }

    /**
     * 从查询计划详情中提取索引名称
     * @param detail 查询计划详情
     * @return 索引名称
     */
    private fun extractIndexName(detail: String): String {
        // 尝试提取索引名称
        val indexPattern = "using index ([\\w_]+)".toRegex(RegexOption.IGNORE_CASE)
        val match = indexPattern.find(detail)
        return match?.groupValues?.getOrNull(1) ?: "未知索引"
    }

    /**
     * 从查询计划详情中提取表名
     * @param detail 查询计划详情
     * @return 表名
     */
    private fun extractTableName(detail: String): String {
        // 尝试提取表名
        val tablePattern = "scan ([\\w_]+)".toRegex(RegexOption.IGNORE_CASE)
        val match = tablePattern.find(detail)
        return match?.groupValues?.getOrNull(1) ?: "未知表"
    }

    /**
     * 生成优化建议
     * @param result 查询计划结果
     */
    private fun generateOptimizationSuggestions(result: QueryPlanResult) {
        // 如果执行时间过长，添加性能优化建议
        if (result.executionTime > 100) {
            result.optimizationSuggestions.add("查询执行时间较长 (${result.executionTime}ms)，考虑优化查询")
        }

        // 如果进行了全表扫描，建议添加索引
        if (result.usesFullTableScan) {
            result.scannedTables.forEach { table ->
                result.optimizationSuggestions.add("表 $table 进行了全表扫描，考虑添加适当的索引")
            }
        }

        // 如果使用了临时表，建议优化查询
        if (result.usesTemporaryTable) {
            result.optimizationSuggestions.add("查询使用了临时表，考虑优化查询以避免临时表的使用")
        }

        // 如果进行了排序，建议添加索引
        if (result.usesSort) {
            result.optimizationSuggestions.add("查询进行了排序操作，考虑添加包含排序列的索引")
        }

        // 如果结果集较大，建议使用分页
        if (result.resultCount > 100) {
            result.optimizationSuggestions.add("查询返回了大量结果 (${result.resultCount} 行)，考虑使用分页查询")
        }

        // 如果查询包含 SELECT *，建议只选择必要的列
        if (result.query.contains("SELECT *")) {
            result.optimizationSuggestions.add("查询使用了 SELECT *，考虑只选择必要的列以减少数据传输")
        }
    }

    /**
     * 保存查询计划
     * @param result 查询计划结果
     */
    private fun saveQueryPlan(result: QueryPlanResult) {
        try {
            val planDir = File(context.filesDir, QUERY_PLAN_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            val planFile = File(
                planDir,
                "query_plan_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.txt"
            )

            // 构建查询计划报告
            val report = buildString {
                append("=== 查询计划分析报告 ===\n")
                append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

                append("--- 查询信息 ---\n")
                append("查询: ${result.query}\n")
                if (result.bindArgs != null) {
                    append("参数: ${result.bindArgs}\n")
                }
                append("执行时间: ${result.executionTime}ms\n")
                append("结果行数: ${result.resultCount}\n\n")

                append("--- 查询计划 ---\n")
                result.planSteps.forEach { step ->
                    append("${step.id} ${step.parent} ${step.notUsed} ${step.detail}\n")
                }
                append("\n")

                append("--- 分析结果 ---\n")
                append("使用索引: ${if (result.usesIndex) "是" else "否"}\n")
                if (result.usesIndex) {
                    append("使用的索引: ${result.usedIndexes.joinToString(", ")}\n")
                }
                append("全表扫描: ${if (result.usesFullTableScan) "是" else "否"}\n")
                if (result.usesFullTableScan) {
                    append("扫描的表: ${result.scannedTables.joinToString(", ")}\n")
                }
                append("使用临时表: ${if (result.usesTemporaryTable) "是" else "否"}\n")
                append("使用排序: ${if (result.usesSort) "是" else "否"}\n\n")

                append("--- 优化建议 ---\n")
                if (result.optimizationSuggestions.isEmpty()) {
                    append("无优化建议\n")
                } else {
                    result.optimizationSuggestions.forEachIndexed { index, suggestion ->
                        append("${index + 1}. $suggestion\n")
                    }
                }
                append("\n")

                if (result.error != null) {
                    append("--- 错误信息 ---\n")
                    append("错误: ${result.error}\n\n")
                }
            }

            // 写入查询计划报告
            planFile.writeText(report)

            // 清理旧的查询计划
            cleanupOldPlans(planDir)

            Log.i(TAG, "查询计划已保存: ${planFile.path}")
        } catch (e: Exception) {
            Log.e(TAG, "保存查询计划失败", e)
        }
    }

    /**
     * 清理旧的查询计划
     * @param planDir 查询计划目录
     */
    private fun cleanupOldPlans(planDir: File) {
        try {
            val planFiles = planDir.listFiles { file -> file.name.startsWith("query_plan_") && file.extension == "txt" }

            if (planFiles != null && planFiles.size > MAX_PLANS) {
                // 按修改时间排序
                planFiles.sortBy { it.lastModified() }

                // 删除最旧的查询计划
                for (i in 0 until planFiles.size - MAX_PLANS) {
                    planFiles[i].delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧查询计划失败", e)
        }
    }

    /**
     * 分析主要查询
     * 分析应用中的主要查询，生成优化建议
     */
    suspend fun analyzeMainQueries(): List<QueryPlanResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<QueryPlanResult>()

        try {
            // 分析主要查询
            val mainQueries = listOf(
                "SELECT * FROM transactions ORDER BY date DESC LIMIT 20",
                "SELECT * FROM transactions WHERE isIncome = 0 ORDER BY date DESC LIMIT 20",
                "SELECT * FROM transactions WHERE isIncome = 1 ORDER BY date DESC LIMIT 20",
                "SELECT * FROM transactions WHERE categoryId = 1 ORDER BY date DESC LIMIT 20",
                "SELECT * FROM transactions WHERE accountId = 1 ORDER BY date DESC LIMIT 20",
                "SELECT * FROM transactions WHERE date BETWEEN ? AND ? ORDER BY date DESC LIMIT 20",
                "SELECT t.*, c.name as categoryName FROM transactions t INNER JOIN categories c ON t.categoryId = c.id ORDER BY t.date DESC LIMIT 20"
            )

            // 准备一些测试参数
            val today = Date()
            val oneMonthAgo = Date(today.time - 30L * 24 * 60 * 60 * 1000)

            for (query in mainQueries) {
                val bindArgs = if (query.contains("BETWEEN ? AND ?")) {
                    arrayOf<Any>(oneMonthAgo, today)
                } else {
                    null
                }

                val result = analyzeQueryPlan(query, bindArgs)
                results.add(result)
            }

            // 生成总体优化建议
            generateOverallOptimizationSuggestions(results)

        } catch (e: Exception) {
            Log.e(TAG, "分析主要查询失败", e)
        }

        return@withContext results
    }

    /**
     * 生成总体优化建议
     * @param results 查询计划结果列表
     */
    private fun generateOverallOptimizationSuggestions(results: List<QueryPlanResult>) {
        // 统计全表扫描的表
        val fullTableScanTables = mutableMapOf<String, Int>()

        // 统计未使用索引的查询
        val queriesWithoutIndex = mutableListOf<QueryPlanResult>()

        // 统计执行时间较长的查询
        val slowQueries = mutableListOf<QueryPlanResult>()

        for (result in results) {
            // 统计全表扫描的表
            if (result.usesFullTableScan) {
                for (table in result.scannedTables) {
                    fullTableScanTables[table] = (fullTableScanTables[table] ?: 0) + 1
                }
            }

            // 统计未使用索引的查询
            if (!result.usesIndex) {
                queriesWithoutIndex.add(result)
            }

            // 统计执行时间较长的查询
            if (result.executionTime > 100) {
                slowQueries.add(result)
            }
        }

        // 生成总体优化建议
        val overallSuggestions = mutableListOf<String>()

        // 针对频繁全表扫描的表添加索引
        fullTableScanTables.entries
            .filter { it.value > 1 }
            .forEach { (table, count) ->
                overallSuggestions.add("表 $table 在 $count 个查询中进行了全表扫描，建议添加适当的索引")
            }

        // 针对未使用索引的查询优化
        if (queriesWithoutIndex.isNotEmpty()) {
            overallSuggestions.add("有 ${queriesWithoutIndex.size} 个查询未使用索引，建议优化这些查询")
        }

        // 针对执行时间较长的查询优化
        if (slowQueries.isNotEmpty()) {
            overallSuggestions.add("有 ${slowQueries.size} 个查询执行时间较长，建议优化这些查询")
        }

        // 保存总体优化建议
        try {
            val planDir = File(context.filesDir, QUERY_PLAN_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            val suggestionFile = File(
                planDir,
                "query_optimization_suggestions_${SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())}.txt"
            )

            // 构建优化建议报告
            val report = buildString {
                append("=== 查询优化建议 ===\n")
                append("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n")

                append("--- 总体优化建议 ---\n")
                if (overallSuggestions.isEmpty()) {
                    append("无总体优化建议\n")
                } else {
                    overallSuggestions.forEachIndexed { index, suggestion ->
                        append("${index + 1}. $suggestion\n")
                    }
                }
                append("\n")

                append("--- 详细查询分析 ---\n")
                results.forEachIndexed { index, result ->
                    append("查询 ${index + 1}: ${result.query}\n")
                    append("  执行时间: ${result.executionTime}ms\n")
                    append("  结果行数: ${result.resultCount}\n")
                    append("  使用索引: ${if (result.usesIndex) "是" else "否"}\n")
                    append("  全表扫描: ${if (result.usesFullTableScan) "是" else "否"}\n")
                    append("  优化建议:\n")

                    if (result.optimizationSuggestions.isEmpty()) {
                        append("    无优化建议\n")
                    } else {
                        result.optimizationSuggestions.forEach { suggestion ->
                            append("    - $suggestion\n")
                        }
                    }

                    append("\n")
                }
            }

            // 写入优化建议报告
            suggestionFile.writeText(report)

            Log.i(TAG, "查询优化建议已保存: ${suggestionFile.path}")
        } catch (e: Exception) {
            Log.e(TAG, "保存查询优化建议失败", e)
        }
    }

    /**
     * 查询计划结果数据类
     */
    data class QueryPlanResult(
        val query: String,
        val bindArgs: String? = null,
        var executionTime: Long = 0,
        var resultCount: Int = 0,
        var planSteps: List<QueryPlanStep> = emptyList(),
        var usesIndex: Boolean = false,
        var usesFullTableScan: Boolean = false,
        var usesTemporaryTable: Boolean = false,
        var usesSort: Boolean = false,
        var usedIndexes: MutableList<String> = mutableListOf(),
        var scannedTables: MutableList<String> = mutableListOf(),
        var optimizationSuggestions: MutableList<String> = mutableListOf(),
        var error: String? = null
    )

    /**
     * 查询计划步骤数据类
     */
    data class QueryPlanStep(
        val id: Int,
        val parent: Int,
        val notUsed: Int,
        val detail: String
    )
}
