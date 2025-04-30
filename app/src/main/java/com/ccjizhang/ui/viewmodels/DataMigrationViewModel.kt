package com.ccjizhang.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.utils.AppDataMigrationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用间数据迁移ViewModel
 */
@HiltViewModel
class DataMigrationViewModel @Inject constructor(
    private val migrationHelper: AppDataMigrationHelper
) : ViewModel() {
    
    // 迁移状态
    private val _migrationState = MutableStateFlow<MigrationState>(MigrationState.Idle)
    val migrationState: StateFlow<MigrationState> = _migrationState
    
    // 操作结果
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult: SharedFlow<String> = _operationResult
    
    /**
     * 从其他应用导入数据
     */
    fun importFromThirdPartyApp(uri: Uri, appType: AppDataMigrationHelper.Companion.AppType) {
        viewModelScope.launch {
            try {
                _migrationState.value = MigrationState.Migrating("正在从其他应用导入数据...")
                
                val result = migrationHelper.importFromThirdPartyApp(uri, appType)
                
                when (result) {
                    is AppDataMigrationHelper.MigrationResult.Success -> {
                        val message = "导入成功！" +
                                "导入了 ${result.accounts} 个账户、" +
                                "${result.categories} 个分类和 " +
                                "${result.transactions} 条交易记录。"
                        _operationResult.emit(message)
                        _migrationState.value = MigrationState.Success(message)
                    }
                    is AppDataMigrationHelper.MigrationResult.Error -> {
                        _operationResult.emit("导入失败：${result.message}")
                        _migrationState.value = MigrationState.Error(result.message)
                    }
                }
            } catch (e: Exception) {
                _operationResult.emit("导入过程出错：${e.localizedMessage}")
                _migrationState.value = MigrationState.Error(e.message ?: "未知错误")
            }
        }
    }
    
    /**
     * 重置状态
     */
    fun resetState() {
        _migrationState.value = MigrationState.Idle
    }
    
    /**
     * 迁移状态
     */
    sealed class MigrationState {
        object Idle : MigrationState()
        data class Migrating(val message: String) : MigrationState()
        data class Success(val message: String) : MigrationState()
        data class Error(val message: String) : MigrationState()
    }
} 