package com.ccjizhang.ui.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.DataExportImportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date
import javax.inject.Inject

/**
 * 数据导入导出ViewModel
 * 管理数据备份与恢复功能的状态
 */
@HiltViewModel
class DataExportImportViewModel @Inject constructor(
    private val dataExportImportRepository: DataExportImportRepository
) : ViewModel() {

    companion object {
        private const val TAG = "DataExportImportVM"
    }

    // 操作进行中状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 操作结果事件
    private val _operationResult = MutableSharedFlow<OperationResult>()
    val operationResult: SharedFlow<OperationResult> = _operationResult.asSharedFlow()

    /**
     * 导出数据为JSON格式
     */
    fun exportDataToJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 记录开始导出
                Log.d(TAG, "开始导出数据到JSON，URI: $uri")

                // 调用仓库方法
                val result = try {
                    dataExportImportRepository.exportDataToJson(context, uri)
                } catch (e: Exception) {
                    Log.e(TAG, "调用exportDataToJson失败", e)
                    e.printStackTrace()
                    _operationResult.emit(OperationResult.Error("数据导出失败: ${e.localizedMessage ?: "未知错误"}"))
                    _isLoading.value = false
                    return@launch
                }

                // 处理结果
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "数据导出成功，URI: $uri")
                        _operationResult.emit(OperationResult.Success("数据导出成功"))
                    },
                    onFailure = { error ->
                        Log.e(TAG, "数据导出失败", error)
                        error.printStackTrace()
                        _operationResult.emit(OperationResult.Error("数据导出失败: ${error.localizedMessage ?: "未知错误"}"))
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "导出数据到JSON失败", e)
                e.printStackTrace()
                _operationResult.emit(OperationResult.Error("数据导出失败: ${e.localizedMessage ?: "未知错误"}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 导出数据为CSV格式
     */
    fun exportDataToCsv(context: Context, baseUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = dataExportImportRepository.exportDataToCsv(context, baseUri)
                result.fold(
                    onSuccess = { uriList ->
                        _operationResult.emit(OperationResult.Success("成功导出${uriList.size}个文件"))
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据导出失败: ${error.localizedMessage}"))
                    }
                )
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("数据导出失败: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 从JSON文件导入数据
     */
    fun importDataFromJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = dataExportImportRepository.importDataFromJson(context, uri)
                result.fold(
                    onSuccess = {
                        _operationResult.emit(OperationResult.Success("数据导入成功"))
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据导入失败: ${error.localizedMessage}"))
                    }
                )
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("数据导入失败: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 从CSV文件导入数据
     */
    fun importDataFromCsv(
        context: Context,
        categoryUri: Uri,
        accountUri: Uri,
        budgetUri: Uri,
        transactionUri: Uri
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = dataExportImportRepository.importDataFromCsv(
                    context, categoryUri, accountUri, budgetUri, transactionUri
                )
                result.fold(
                    onSuccess = {
                        _operationResult.emit(OperationResult.Success("数据导入成功"))
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据导入失败: ${error.localizedMessage}"))
                    }
                )
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("数据导入失败: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 生成备份文件名
     */
    fun generateBackupFileName(extension: String): String {
        return dataExportImportRepository.generateBackupFileName(extension)
    }

    /**
     * 清理数据
     */
    fun cleanupData(
        clearTransactions: Boolean,
        clearCategories: Boolean,
        clearAccounts: Boolean,
        clearBudgets: Boolean,
        beforeDate: Date? = null
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = dataExportImportRepository.cleanUpData(
                    clearTransactions,
                    clearCategories,
                    clearAccounts,
                    clearBudgets,
                    beforeDate
                )

                result.fold(
                    onSuccess = { stats ->
                        val message = buildString {
                            append("数据清理成功:\n")
                            if (clearTransactions) {
                                append("- 已删除 ${stats.transactionsDeleted} 条交易记录\n")
                            }
                            if (clearCategories) {
                                append("- 已删除 ${stats.categoriesDeleted} 个自定义分类\n")
                            }
                            if (clearAccounts) {
                                append("- 已删除 ${stats.accountsDeleted} 个账户\n")
                            }
                            if (clearBudgets) {
                                append("- 已删除 ${stats.budgetsDeleted} 个预算\n")
                            }
                        }
                        _operationResult.emit(OperationResult.Success(message))
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据清理失败: ${error.localizedMessage}"))
                    }
                )
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("数据清理失败: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 验证导入数据
     */
    fun validateImportData(context: Context, uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = dataExportImportRepository.validateImportData(context, uri)

                result.fold(
                    onSuccess = { validationResult ->
                        if (validationResult.isValid) {
                            val message = buildString {
                                append("数据验证成功:\n")
                                append("- 分类: ${validationResult.categoryCount} 个\n")
                                append("- 账户: ${validationResult.accountCount} 个\n")
                                append("- 交易: ${validationResult.transactionCount} 个\n")
                                append("- 预算: ${validationResult.budgetCount} 个\n")

                                validationResult.exportTime?.let {
                                    append("- 导出时间: $it\n")
                                }

                                validationResult.version?.let {
                                    append("- 版本: $it\n")
                                }

                                if (validationResult.hasConsistencyIssues) {
                                    append("\n警告: 数据存在一致性问题，导入后可能需要手动修复")
                                }
                            }
                            _operationResult.emit(OperationResult.Success(message))
                        } else {
                            _operationResult.emit(OperationResult.Error("数据无效: 未找到有效的数据记录"))
                        }
                    },
                    onFailure = { error ->
                        _operationResult.emit(OperationResult.Error("数据验证失败: ${error.localizedMessage}"))
                    }
                )
            } catch (e: Exception) {
                _operationResult.emit(OperationResult.Error("数据验证失败: ${e.localizedMessage}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 操作结果密封类
     */
    sealed class OperationResult {
        /**
         * 操作成功
         * @param message 成功消息
         */
        data class Success(val message: String) : OperationResult()

        /**
         * 操作失败
         * @param error 错误消息
         */
        data class Error(val error: String) : OperationResult()
    }
}