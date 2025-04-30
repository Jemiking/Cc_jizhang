package com.ccjizhang.ui.archive

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.archive.DatabaseArchiveManager
import com.ccjizhang.data.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 归档数据ViewModel
 * 用于管理归档数据的访问和操作
 */
@HiltViewModel
class ArchiveDataViewModel @Inject constructor(
    private val archiveManager: DatabaseArchiveManager
) : ViewModel() {
    
    private val _availableYears = MutableLiveData<List<String>>()
    val availableYears: LiveData<List<String>> = _availableYears
    
    private val _archivedTransactions = MutableLiveData<List<Transaction>>()
    val archivedTransactions: LiveData<List<Transaction>> = _archivedTransactions
    
    private val _archiveDatabaseInfo = MutableLiveData<List<DatabaseArchiveManager.ArchiveDatabaseInfo>>()
    val archiveDatabaseInfo: LiveData<List<DatabaseArchiveManager.ArchiveDatabaseInfo>> = _archiveDatabaseInfo
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _archiveResult = MutableLiveData<DatabaseArchiveManager.ArchiveResult?>()
    val archiveResult: LiveData<DatabaseArchiveManager.ArchiveResult?> = _archiveResult
    
    init {
        loadAvailableYears()
        loadArchiveDatabaseInfo()
    }
    
    /**
     * 加载可用的归档年份
     */
    fun loadAvailableYears() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val years = archiveManager.getAvailableArchiveYears()
                _availableYears.value = years
                _error.value = null
            } catch (e: Exception) {
                Log.e("ArchiveDataViewModel", "加载可用归档年份失败", e)
                _error.value = "加载可用归档年份失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载归档数据库信息
     */
    fun loadArchiveDatabaseInfo() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val info = archiveManager.getArchiveDatabaseInfo()
                _archiveDatabaseInfo.value = info
                _error.value = null
            } catch (e: Exception) {
                Log.e("ArchiveDataViewModel", "加载归档数据库信息失败", e)
                _error.value = "加载归档数据库信息失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载指定年份的归档交易数据
     * @param year 归档年份
     */
    fun loadArchivedTransactions(year: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                archiveManager.queryArchivedTransactions(year)
                    .catch { e ->
                        Log.e("ArchiveDataViewModel", "加载归档交易数据失败", e)
                        _error.value = "加载归档交易数据失败: ${e.message}"
                    }
                    .collect { transactions ->
                        _archivedTransactions.value = transactions
                        _error.value = null
                    }
            } catch (e: Exception) {
                Log.e("ArchiveDataViewModel", "加载归档交易数据失败", e)
                _error.value = "加载归档交易数据失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 执行数据归档
     * @param months 归档多少个月前的数据
     */
    fun performArchive(months: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 计算归档日期阈值
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MONTH, -months)
                val thresholdDate = calendar.time
                
                // 执行归档
                val result = archiveManager.archiveDataBeforeDate(thresholdDate)
                _archiveResult.value = result
                
                if (result.success) {
                    // 归档成功，重新加载数据
                    loadAvailableYears()
                    loadArchiveDatabaseInfo()
                    _error.value = null
                } else {
                    _error.value = "归档失败: ${result.error}"
                }
            } catch (e: Exception) {
                Log.e("ArchiveDataViewModel", "执行归档失败", e)
                _error.value = "执行归档失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 删除归档数据库
     * @param year 要删除的归档数据库年份
     */
    fun deleteArchiveDatabase(year: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = archiveManager.deleteArchiveDatabase(year)
                if (success) {
                    // 删除成功，重新加载数据
                    loadAvailableYears()
                    loadArchiveDatabaseInfo()
                    _error.value = null
                } else {
                    _error.value = "删除归档数据库失败"
                }
            } catch (e: Exception) {
                Log.e("ArchiveDataViewModel", "删除归档数据库失败", e)
                _error.value = "删除归档数据库失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除错误信息
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * 清除归档结果
     */
    fun clearArchiveResult() {
        _archiveResult.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // 关闭所有归档数据库连接
        archiveManager.closeAll()
    }
}
