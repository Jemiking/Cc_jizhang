package com.ccjizhang.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.AccountCategory
import com.ccjizhang.data.repository.AccountCategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 账户分类ViewModel
 * 负责账户分类数据的管理和UI状态
 */
@HiltViewModel
class AccountCategoryViewModel @Inject constructor(
    private val accountCategoryRepository: AccountCategoryRepository
) : ViewModel() {

    // 账户分类列表
    private val _categories = MutableStateFlow<List<AccountCategory>>(emptyList())
    val categories: StateFlow<List<AccountCategory>> = _categories.asStateFlow()

    // 当前选中的分类
    private val _selectedCategory = MutableStateFlow<AccountCategory?>(null)
    val selectedCategory: StateFlow<AccountCategory?> = _selectedCategory.asStateFlow()

    // 是否正在加载
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误信息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadCategories()
    }

    /**
     * 加载所有账户分类
     */
    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountCategoryRepository.getAllCategories().collect { categories ->
                    _categories.value = categories
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载账户分类失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 添加账户分类
     */
    fun addCategory(name: String, icon: String, color: Int, isDefault: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val newCategory = AccountCategory(
                    name = name,
                    icon = icon,
                    color = color,
                    sortOrder = _categories.value.size,
                    isDefault = isDefault
                )
                accountCategoryRepository.addCategory(newCategory)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "添加账户分类失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 更新账户分类
     */
    fun updateCategory(category: AccountCategory) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountCategoryRepository.updateCategory(category)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "更新账户分类失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 删除账户分类
     */
    fun deleteCategory(category: AccountCategory) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountCategoryRepository.deleteCategory(category)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "删除账户分类失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 选择账户分类
     */
    fun selectCategory(category: AccountCategory?) {
        _selectedCategory.value = category
    }

    /**
     * 更新分类排序
     */
    fun updateCategoryOrder(categories: List<AccountCategory>) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                categories.forEachIndexed { index, category ->
                    if (category.sortOrder != index) {
                        accountCategoryRepository.updateCategory(category.copy(sortOrder = index))
                    }
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "更新分类顺序失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 初始化默认分类
     */
    fun initDefaultCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                accountCategoryRepository.initDefaultCategories()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "初始化默认分类失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
