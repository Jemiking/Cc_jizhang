package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.TagRepository
import com.ccjizhang.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 标签管理ViewModel
 * 负责处理标签相关的业务逻辑和UI状态
 */
@HiltViewModel
class TagViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    // 所有标签列表
    val allTags = tagRepository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<String>()
        )
    
    // 查询关键字
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    // 按关键字搜索的标签列表
    val searchResults = searchQuery
        .filter { it.isNotEmpty() }
        .map { query -> tagRepository.searchTags(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList<String>()
        )
    
    // 正在处理的操作状态
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // 操作结果消息
    private val _resultMessage = MutableStateFlow<String?>(null)
    val resultMessage: StateFlow<String?> = _resultMessage.asStateFlow()
    
    // 当前编辑的标签
    private val _editingTag = MutableStateFlow<String?>(null)
    val editingTag: StateFlow<String?> = _editingTag.asStateFlow()
    
    // 选中的标签列表（用于多选操作）
    private val _selectedTags = MutableStateFlow<Set<String>>(emptySet())
    val selectedTags: StateFlow<Set<String>> = _selectedTags.asStateFlow()
    
    // 每个标签关联的交易数量
    private val _tagTransactionCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val tagTransactionCounts: StateFlow<Map<String, Int>> = _tagTransactionCounts.asStateFlow()
    
    /**
     * 更新搜索关键字
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * 加载每个标签关联的交易数量
     */
    fun loadTagTransactionCounts() {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val tags = allTags.value
                val counts = mutableMapOf<String, Int>()
                
                for (tag in tags) {
                    // 使用Repository层提供的方法获取交易ID列表
                    val transactionIds = transactionRepository.getTransactionsByTag(tag).first()
                    counts[tag] = transactionIds.size
                }
                
                _tagTransactionCounts.value = counts
            } catch (e: Exception) {
                _resultMessage.value = "加载标签使用统计失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 开始编辑标签
     */
    fun startEditTag(tag: String) {
        _editingTag.value = tag
    }
    
    /**
     * 取消编辑
     */
    fun cancelEdit() {
        _editingTag.value = null
    }
    
    /**
     * 重命名标签
     */
    fun renameTag(newName: String) {
        val oldTag = _editingTag.value ?: return
        
        if (oldTag == newName) {
            _editingTag.value = null
            return
        }
        
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                tagRepository.renameTag(oldTag, newName)
                _resultMessage.value = "标签已更新"
                _editingTag.value = null
                
                // 如果当前标签在选中列表中，更新选中状态
                if (_selectedTags.value.contains(oldTag)) {
                    val updatedSelection = _selectedTags.value.toMutableSet()
                    updatedSelection.remove(oldTag)
                    updatedSelection.add(newName)
                    _selectedTags.value = updatedSelection
                }
            } catch (e: Exception) {
                _resultMessage.value = "更新标签失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 删除标签
     */
    fun deleteTag(tag: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                tagRepository.deleteTag(tag)
                _resultMessage.value = "标签已删除"
                
                // 如果删除的标签正在编辑，清除编辑状态
                if (_editingTag.value == tag) {
                    _editingTag.value = null
                }
                
                // 从选中列表中移除
                if (_selectedTags.value.contains(tag)) {
                    val updatedSelection = _selectedTags.value.toMutableSet()
                    updatedSelection.remove(tag)
                    _selectedTags.value = updatedSelection
                }
            } catch (e: Exception) {
                _resultMessage.value = "删除标签失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 清除结果消息
     */
    fun clearResultMessage() {
        _resultMessage.value = null
    }
    
    /**
     * 切换标签选中状态
     */
    fun toggleTagSelection(tag: String) {
        val updatedSelection = _selectedTags.value.toMutableSet()
        if (updatedSelection.contains(tag)) {
            updatedSelection.remove(tag)
        } else {
            updatedSelection.add(tag)
        }
        _selectedTags.value = updatedSelection
    }
    
    /**
     * 清除所有选中标签
     */
    fun clearSelection() {
        _selectedTags.value = emptySet()
    }
    
    /**
     * 删除选中的所有标签
     */
    fun deleteSelectedTags() {
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                val tags = _selectedTags.value
                for (tag in tags) {
                    tagRepository.deleteTag(tag)
                }
                _resultMessage.value = "已删除${tags.size}个标签"
                _selectedTags.value = emptySet()
            } catch (e: Exception) {
                _resultMessage.value = "批量删除标签失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 获取特定交易的标签
     */
    fun getTagsForTransaction(transactionId: Long): Flow<List<String>> {
        return tagRepository.getTagsByTransactionId(transactionId)
    }
    
    /**
     * 为交易设置标签
     */
    fun setTagsForTransaction(transactionId: Long, tags: List<String>) {
        viewModelScope.launch {
            try {
                tagRepository.replaceTransactionTags(transactionId, tags)
            } catch (e: Exception) {
                _resultMessage.value = "更新交易标签失败: ${e.message}"
            }
        }
    }
    
    /**
     * 获取标签使用次数排行
     */
    fun getTopUsedTags(limit: Int = 10): List<Pair<String, Int>> {
        val counts = _tagTransactionCounts.value
        return counts.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { Pair(it.key, it.value) }
    }
    
    /**
     * 合并标签（将多个标签合并为一个）
     */
    fun mergeTags(sourceTags: List<String>, targetTag: String) {
        if (sourceTags.isEmpty() || sourceTags.contains(targetTag)) {
            return
        }
        
        viewModelScope.launch {
            _isProcessing.value = true
            
            try {
                // 1. 获取所有相关的交易ID
                val transactionIds = mutableSetOf<Long>()
                for (tag in sourceTags) {
                    val ids = transactionRepository.getTransactionsByTag(tag).first()
                    transactionIds.addAll(ids)
                }
                
                // 2. 为每个交易添加目标标签
                for (id in transactionIds) {
                    tagRepository.addTagToTransaction(id, targetTag)
                }
                
                // 3. 删除原始标签
                for (tag in sourceTags) {
                    tagRepository.deleteTag(tag)
                }
                
                _resultMessage.value = "已将${sourceTags.size}个标签合并为\"$targetTag\""
                
                // 更新选中状态
                val updatedSelection = _selectedTags.value.toMutableSet()
                updatedSelection.removeAll(sourceTags)
                _selectedTags.value = updatedSelection
                
                // 重新加载标签使用统计
                loadTagTransactionCounts()
            } catch (e: Exception) {
                _resultMessage.value = "合并标签失败: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * 添加新标签
     */
    fun addNewTag(tag: String) {
        if (tag.isBlank()) {
            _resultMessage.value = "标签名称不能为空"
            return
        }
        
        viewModelScope.launch {
            try {
                // 检查标签是否已存在
                val existingTags = allTags.value as? List<String> ?: emptyList()
                if (existingTags.contains(tag)) {
                    _resultMessage.value = "标签\"$tag\"已存在"
                    return@launch
                }
                
                // 为了添加新标签，我们需要至少有一个交易
                // 这里通过获取最新的交易记录来实现
                val latestTransactions = transactionRepository.getRecentTransactions(1).first()
                if (latestTransactions.isEmpty()) {
                    _resultMessage.value = "需要至少有一笔交易才能添加标签"
                    return@launch
                }
                
                // 将标签添加到最新的交易中
                val transactionId = latestTransactions[0].id
                tagRepository.addTagToTransaction(transactionId, tag)
                
                _resultMessage.value = "已添加新标签\"$tag\""
                loadTagTransactionCounts()
            } catch (e: Exception) {
                _resultMessage.value = "添加标签失败: ${e.message}"
            }
        }
    }
} 