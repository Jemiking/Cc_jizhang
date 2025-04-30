package com.ccjizhang.utils

import android.content.Context
import android.util.Log
import com.ccjizhang.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 数据库结构分析器
 * 用于分析数据库结构并识别优化机会
 */
@Singleton
class DatabaseStructureAnalyzer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase
) {
    companion object {
        private const val TAG = "DatabaseStructureAnalyzer"
        private const val REPORTS_FOLDER = "db_structure_reports"
    }

    /**
     * 表信息
     */
    data class TableInfo(
        val name: String,
        val rowCount: Int,
        val columnCount: Int,
        val columns: List<ColumnInfo>,
        val indices: List<IndexInfo>,
        val hasRowId: Boolean,
        val withoutRowId: Boolean,
        val autoIncrement: Boolean
    )

    /**
     * 列信息
     */
    data class ColumnInfo(
        val name: String,
        val type: String,
        val notNull: Boolean,
        val defaultValue: String?,
        val primaryKey: Boolean
    )

    /**
     * 索引信息
     */
    data class IndexInfo(
        val name: String,
        val unique: Boolean,
        val columns: List<String>
    )

    /**
     * 分析数据库结构
     */
    suspend fun analyzeDatabase(): Map<String, TableInfo> = withContext(Dispatchers.IO) {
        val tableInfoMap = mutableMapOf<String, TableInfo>()

        try {
            Log.i(TAG, "开始分析数据库结构")

            // 获取所有表
            val tableNames = mutableListOf<String>()
            appDatabase.query("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tableNames.add(cursor.getString(0))
                }
            }

            Log.i(TAG, "找到 ${tableNames.size} 个表")

            // 分析每个表
            for (tableName in tableNames) {
                try {
                    // 获取表的行数
                    var rowCount = 0
                    appDatabase.query("SELECT count(*) FROM `$tableName`", null).use { cursor ->
                        if (cursor.moveToFirst()) {
                            rowCount = cursor.getInt(0)
                        }
                    }

                    // 获取表的列信息
                    val columns = mutableListOf<ColumnInfo>()
                    appDatabase.query("PRAGMA table_info(`$tableName`)", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val columnName = cursor.getString(1)
                            val columnType = cursor.getString(2)
                            val notNull = cursor.getInt(3) == 1
                            val defaultValue = cursor.getString(4)
                            val primaryKey = cursor.getInt(5) == 1

                            columns.add(ColumnInfo(
                                name = columnName,
                                type = columnType,
                                notNull = notNull,
                                defaultValue = defaultValue,
                                primaryKey = primaryKey
                            ))
                        }
                    }

                    // 获取表的索引信息
                    val indices = mutableListOf<IndexInfo>()
                    appDatabase.query("PRAGMA index_list(`$tableName`)", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val indexName = cursor.getString(1)
                            val unique = cursor.getInt(2) == 1

                            val indexColumns = mutableListOf<String>()
                            appDatabase.query("PRAGMA index_info(`$indexName`)", null).use { indexCursor ->
                                while (indexCursor.moveToNext()) {
                                    val columnName = indexCursor.getString(2)
                                    indexColumns.add(columnName)
                                }
                            }

                            indices.add(IndexInfo(
                                name = indexName,
                                unique = unique,
                                columns = indexColumns
                            ))
                        }
                    }

                    // 检查表是否有ROWID
                    var hasRowId = true
                    var withoutRowId = false
                    var autoIncrement = false

                    appDatabase.query("SELECT sql FROM sqlite_master WHERE type='table' AND name='$tableName'", null).use { cursor ->
                        if (cursor.moveToFirst()) {
                            val sql = cursor.getString(0)
                            withoutRowId = sql.contains("WITHOUT ROWID", ignoreCase = true)
                            hasRowId = !withoutRowId
                            autoIncrement = sql.contains("AUTOINCREMENT", ignoreCase = true)
                        }
                    }

                    tableInfoMap[tableName] = TableInfo(
                        name = tableName,
                        rowCount = rowCount,
                        columnCount = columns.size,
                        columns = columns,
                        indices = indices,
                        hasRowId = hasRowId,
                        withoutRowId = withoutRowId,
                        autoIncrement = autoIncrement
                    )

                    Log.i(TAG, "分析表 $tableName 完成，行数: $rowCount, 列数: ${columns.size}, 索引数: ${indices.size}")
                } catch (e: Exception) {
                    Log.e(TAG, "分析表 $tableName 失败", e)
                }
            }

            Log.i(TAG, "数据库结构分析完成")
            return@withContext tableInfoMap
        } catch (e: Exception) {
            Log.e(TAG, "分析数据库结构失败", e)
            return@withContext emptyMap()
        }
    }

    /**
     * 生成数据库结构报告
     */
    suspend fun generateStructureReport(): File? = withContext(Dispatchers.IO) {
        try {
            val tableInfoMap = analyzeDatabase()
            if (tableInfoMap.isEmpty()) return@withContext null

            // 创建报告目录
            val reportsDir = File(context.filesDir, REPORTS_FOLDER).apply {
                if (!exists()) mkdirs()
            }

            // 创建报告文件
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val reportFile = File(
                reportsDir,
                "db_structure_${dateFormat.format(Date())}.txt"
            )

            // 生成报告内容
            val reportContent = StringBuilder()
            reportContent.append("CC记账数据库结构分析报告\n")
            reportContent.append("===========================\n")
            reportContent.append("生成时间: ${dateFormat.format(Date())}\n\n")

            reportContent.append("数据库概览:\n")
            reportContent.append("- 表数量: ${tableInfoMap.size}\n")
            reportContent.append("- 总行数: ${tableInfoMap.values.sumOf { it.rowCount }}\n")
            reportContent.append("- 总索引数: ${tableInfoMap.values.sumOf { it.indices.size }}\n\n")

            reportContent.append("表详情:\n")
            for (tableInfo in tableInfoMap.values.sortedByDescending { it.rowCount }) {
                reportContent.append("----------------------------\n")
                reportContent.append("表名: ${tableInfo.name}\n")
                reportContent.append("行数: ${tableInfo.rowCount}\n")
                reportContent.append("列数: ${tableInfo.columnCount}\n")
                reportContent.append("索引数: ${tableInfo.indices.size}\n")
                reportContent.append("有ROWID: ${tableInfo.hasRowId}\n")
                reportContent.append("WITHOUT ROWID: ${tableInfo.withoutRowId}\n")
                reportContent.append("AUTOINCREMENT: ${tableInfo.autoIncrement}\n\n")

                reportContent.append("列:\n")
                for (column in tableInfo.columns) {
                    reportContent.append("- ${column.name} (${column.type})")
                    if (column.primaryKey) reportContent.append(" PRIMARY KEY")
                    if (column.notNull) reportContent.append(" NOT NULL")
                    if (column.defaultValue != null) reportContent.append(" DEFAULT ${column.defaultValue}")
                    reportContent.append("\n")
                }
                reportContent.append("\n")

                reportContent.append("索引:\n")
                for (index in tableInfo.indices) {
                    reportContent.append("- ${index.name}")
                    if (index.unique) reportContent.append(" (UNIQUE)")
                    reportContent.append(": ${index.columns.joinToString(", ")}\n")
                }
                reportContent.append("\n")
            }

            reportContent.append("优化建议:\n")
            reportContent.append(generateOptimizationSuggestions(tableInfoMap))

            reportFile.writeText(reportContent.toString())

            Log.i(TAG, "数据库结构报告已生成: ${reportFile.path}")
            return@withContext reportFile
        } catch (e: Exception) {
            Log.e(TAG, "生成数据库结构报告失败", e)
            return@withContext null
        }
    }

    /**
     * 生成优化建议
     */
    private fun generateOptimizationSuggestions(tableInfoMap: Map<String, TableInfo>): String {
        val suggestions = StringBuilder()

        // 检查大表
        val largeTables = tableInfoMap.values.filter { it.rowCount > 10000 }
        if (largeTables.isNotEmpty()) {
            suggestions.append("1. 大表优化:\n")
            for (table in largeTables) {
                suggestions.append("   - 表 ${table.name} 有 ${table.rowCount} 行，考虑以下优化:\n")

                // 检查索引
                val indexedColumns = table.indices.flatMap { it.columns }.toSet()
                val unindexedColumns = table.columns.filter { it.name !in indexedColumns && !it.primaryKey }

                if (unindexedColumns.isNotEmpty()) {
                    suggestions.append("     * 考虑为常用查询条件添加索引，如: ${unindexedColumns.take(3).joinToString(", ") { it.name }}\n")
                }

                // 检查是否有分区的必要
                if (table.rowCount > 100000) {
                    suggestions.append("     * 考虑按时间或其他维度分区，减少单表数据量\n")
                }

                // 检查是否需要归档
                suggestions.append("     * 考虑归档旧数据，保持活跃数据量在合理范围内\n")
            }
            suggestions.append("\n")
        }

        // 检查索引过多的表
        val heavyIndexedTables = tableInfoMap.values.filter { it.indices.size > 5 }
        if (heavyIndexedTables.isNotEmpty()) {
            suggestions.append("2. 索引优化:\n")
            for (table in heavyIndexedTables) {
                suggestions.append("   - 表 ${table.name} 有 ${table.indices.size} 个索引，可能影响写入性能:\n")
                suggestions.append("     * 检查是否所有索引都必要，考虑移除不常用的索引\n")

                // 检查是否有重叠索引
                val indexColumns = table.indices.map { it.columns }
                for (i in 0 until indexColumns.size) {
                    for (j in i + 1 until indexColumns.size) {
                        val index1 = indexColumns[i]
                        val index2 = indexColumns[j]

                        if (index1.size > index2.size && index1.take(index2.size) == index2) {
                            suggestions.append("     * 索引 ${table.indices[i].name} 和 ${table.indices[j].name} 可能重叠，考虑合并\n")
                        }
                    }
                }
            }
            suggestions.append("\n")
        }

        // 检查没有索引的表
        val noIndexTables = tableInfoMap.values.filter { it.indices.isEmpty() && it.rowCount > 1000 }
        if (noIndexTables.isNotEmpty()) {
            suggestions.append("3. 缺少索引的表:\n")
            for (table in noIndexTables) {
                suggestions.append("   - 表 ${table.name} 有 ${table.rowCount} 行但没有索引:\n")
                suggestions.append("     * 考虑为常用查询条件添加索引\n")

                // 推荐可能需要索引的列
                val potentialIndexColumns = table.columns.filter {
                    !it.primaryKey &&
                    (it.name.endsWith("Id") || it.name.endsWith("Date") || it.name.endsWith("Time") || it.name == "type" || it.name == "status")
                }

                if (potentialIndexColumns.isNotEmpty()) {
                    suggestions.append("     * 可能需要索引的列: ${potentialIndexColumns.joinToString(", ") { it.name }}\n")
                }
            }
            suggestions.append("\n")
        }

        // 检查数据类型
        val tablesWithTextPK = tableInfoMap.values.filter { table ->
            table.columns.any { it.primaryKey && it.type.equals("TEXT", ignoreCase = true) }
        }

        if (tablesWithTextPK.isNotEmpty()) {
            suggestions.append("4. 数据类型优化:\n")
            for (table in tablesWithTextPK) {
                suggestions.append("   - 表 ${table.name} 使用TEXT类型作为主键，考虑使用INTEGER类型提高性能\n")
            }
            suggestions.append("\n")
        }

        // 检查是否有表没有使用WITHOUT ROWID
        val smallTablesWithRowId = tableInfoMap.values.filter {
            it.hasRowId && !it.autoIncrement && it.columns.any { col -> col.primaryKey } && it.rowCount < 10000
        }

        if (smallTablesWithRowId.isNotEmpty()) {
            suggestions.append("5. WITHOUT ROWID优化:\n")
            for (table in smallTablesWithRowId) {
                suggestions.append("   - 表 ${table.name} 可以考虑使用WITHOUT ROWID优化，减少存储空间\n")
            }
            suggestions.append("\n")
        }

        // 如果没有特别的建议
        if (suggestions.isEmpty()) {
            suggestions.append("当前数据库结构良好，无需特别优化。\n")
            suggestions.append("继续定期监控数据库性能和结构。\n")
        }

        return suggestions.toString()
    }
}
