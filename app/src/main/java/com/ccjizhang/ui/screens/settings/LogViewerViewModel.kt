package com.ccjizhang.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.utils.LogViewerHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * 日志查看界面的ViewModel
 */
@HiltViewModel
class LogViewerViewModel @Inject constructor(
    private val logViewerHelper: LogViewerHelper
) : ViewModel() {
    
    var logFiles by mutableStateOf<List<File>>(emptyList())
        private set
    
    var diagnosticInfo by mutableStateOf("")
        private set
    
    init {
        loadDiagnosticInfo()
    }
    
    /**
     * 加载日志文件列表
     */
    fun loadLogFiles() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    logFiles = logViewerHelper.getLogFiles()
                }
                Timber.d("加载了${logFiles.size}个日志文件")
            } catch (e: Exception) {
                Timber.e(e, "加载日志文件失败")
                logFiles = emptyList()
            }
        }
    }
    
    /**
     * 读取日志文件内容
     */
    fun readLogFile(file: File): String {
        return try {
            logViewerHelper.readLogFile(file)
        } catch (e: Exception) {
            Timber.e(e, "读取日志文件失败: ${file.name}")
            "无法读取日志文件: ${e.localizedMessage}"
        }
    }
    
    /**
     * 分享日志文件
     */
    fun shareLogFile(context: Context, file: File) {
        try {
            val shareIntent = logViewerHelper.shareLogFile(file)
            if (shareIntent != null) {
                context.startActivity(Intent.createChooser(shareIntent, "分享日志文件"))
                Timber.i("分享日志文件: ${file.name}")
            } else {
                Timber.e("创建分享Intent失败")
            }
        } catch (e: Exception) {
            Timber.e(e, "分享日志文件失败: ${file.name}")
        }
    }
    
    /**
     * 清理所有日志文件
     */
    fun clearAllLogs() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    logViewerHelper.clearAllLogs()
                }
                Timber.i("清理了所有日志文件")
                loadLogFiles()
            } catch (e: Exception) {
                Timber.e(e, "清理日志文件失败")
            }
        }
    }
    
    /**
     * 加载诊断信息
     */
    private fun loadDiagnosticInfo() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    diagnosticInfo = logViewerHelper.collectDiagnosticInfo()
                }
                Timber.d("加载了诊断信息")
            } catch (e: Exception) {
                Timber.e(e, "加载诊断信息失败")
                diagnosticInfo = "无法加载诊断信息: ${e.localizedMessage}"
            }
        }
    }
}
