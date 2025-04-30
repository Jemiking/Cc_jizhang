package com.ccjizhang.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.service.CategorySuggestionService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.ccjizhang.ui.theme.PrimaryDark
import com.ccjizhang.ui.theme.CategoryBlue
import com.ccjizhang.ui.theme.CategoryOrange
import com.ccjizhang.ui.theme.CategoryPink
import com.ccjizhang.ui.theme.IncomeGreen
import javax.inject.Inject

/**
 * 分类数据ViewModel
 */
@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val categorySuggestionService: CategorySuggestionService
) : ViewModel() {

    // 所有分类列表
    private val _categories = MutableStateFlow<List<CategoryWithIcon>>(emptyList())
    val categories: StateFlow<List<CategoryWithIcon>> = _categories.asStateFlow()
    
    // 支出分类
    private val _expenseCategories = MutableStateFlow<List<CategoryWithIcon>>(emptyList())
    val expenseCategories: StateFlow<List<CategoryWithIcon>> = _expenseCategories.asStateFlow()
    
    // 收入分类
    private val _incomeCategories = MutableStateFlow<List<CategoryWithIcon>>(emptyList())
    val incomeCategories: StateFlow<List<CategoryWithIcon>> = _incomeCategories.asStateFlow()
    
    // 顶级支出分类
    private val _topLevelExpenseCategories = MutableStateFlow<List<CategoryWithIcon>>(emptyList())
    val topLevelExpenseCategories: StateFlow<List<CategoryWithIcon>> = _topLevelExpenseCategories.asStateFlow()
    
    // 顶级收入分类
    private val _topLevelIncomeCategories = MutableStateFlow<List<CategoryWithIcon>>(emptyList())
    val topLevelIncomeCategories: StateFlow<List<CategoryWithIcon>> = _topLevelIncomeCategories.asStateFlow()
    
    // 当前选中的父分类ID
    private val _selectedParentCategoryId = MutableStateFlow<Long?>(null)
    val selectedParentCategoryId: StateFlow<Long?> = _selectedParentCategoryId.asStateFlow()
    
    // 当前显示的子分类
    private val _childCategories = MutableStateFlow<List<CategoryWithIcon>>(emptyList())
    val childCategories: StateFlow<List<CategoryWithIcon>> = _childCategories.asStateFlow()
    
    // 当前选中的分类
    private val _selectedCategory = MutableStateFlow<CategoryWithIcon?>(null)
    val selectedCategory: StateFlow<CategoryWithIcon?> = _selectedCategory.asStateFlow()
    
    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // 建议的分类
    private val _suggestedCategories = MutableStateFlow<List<Category>>(emptyList())
    val suggestedCategories: StateFlow<List<Category>> = _suggestedCategories.asStateFlow()
    
    // 分类层级视图模式：true为层级视图，false为平铺视图
    private val _hierarchicalViewMode = MutableStateFlow(true)
    val hierarchicalViewMode: StateFlow<Boolean> = _hierarchicalViewMode.asStateFlow()

    // 初始化 - 加载数据
    init {
        loadCategories()
    }
    
    /**
     * 切换视图模式（层级/平铺）
     */
    fun toggleViewMode() {
        _hierarchicalViewMode.value = !_hierarchicalViewMode.value
        // 重新加载数据以适应新的视图模式
        loadCategories()
    }
    
    /**
     * 加载分类数据
     */
    fun loadCategories() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // 从Repository层获取数据
                val allCategories = categoryRepository.getAllCategories().first()
                val withIcons = allCategories.map { mapCategoryToIcon(it) }
                
                _categories.value = withIcons
                _expenseCategories.value = withIcons.filter { !it.category.isIncome }
                _incomeCategories.value = withIcons.filter { it.category.isIncome }
                
                // 加载顶级分类
                loadTopLevelCategories()
                
            } catch (e: Exception) {
                // 如果发生错误，加载示例数据
                val sampleCategories = getSampleCategoriesWithIcons()
                _categories.value = sampleCategories
                _expenseCategories.value = sampleCategories.filter { !it.category.isIncome }
                _incomeCategories.value = sampleCategories.filter { it.category.isIncome }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 加载顶级分类
     */
    private fun loadTopLevelCategories() {
        viewModelScope.launch {
            try {
                // 加载顶级支出分类
                val topLevelExpense = categoryRepository.getTopLevelCategories(
                    com.ccjizhang.data.model.CategoryType.EXPENSE
                ).first().map { mapCategoryToIcon(it) }
                _topLevelExpenseCategories.value = topLevelExpense
                
                // 加载顶级收入分类
                val topLevelIncome = categoryRepository.getTopLevelCategories(
                    com.ccjizhang.data.model.CategoryType.INCOME
                ).first().map { mapCategoryToIcon(it) }
                _topLevelIncomeCategories.value = topLevelIncome
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }
    
    /**
     * 选择父分类，加载其子分类
     */
    fun selectParentCategory(categoryId: Long?) {
        _selectedParentCategoryId.value = categoryId
        
        if (categoryId == null) {
            _childCategories.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // 加载子分类
                val children = categoryRepository.getChildCategories(categoryId).first()
                _childCategories.value = children.map { mapCategoryToIcon(it) }
            } catch (e: Exception) {
                _childCategories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 返回上级分类列表
     */
    fun navigateToParent() {
        viewModelScope.launch {
            val currentParentId = _selectedParentCategoryId.value ?: return@launch
            
            try {
                // 获取当前父分类的信息
                val parentCategory = categoryRepository.getCategoryById(currentParentId).first()
                
                // 设置新的父分类ID（即当前父分类的父分类ID）
                _selectedParentCategoryId.value = parentCategory?.parentId
                
                // 如果新的父分类ID为null，则加载顶级分类
                if (parentCategory?.parentId == null) {
                    loadTopLevelCategories()
                    _childCategories.value = emptyList()
                } else {
                    // 否则加载新父分类的子分类
                    selectParentCategory(parentCategory.parentId)
                }
            } catch (e: Exception) {
                // 错误处理，回到顶级分类
                _selectedParentCategoryId.value = null
                loadTopLevelCategories()
                _childCategories.value = emptyList()
            }
        }
    }
    
    /**
     * 根据交易描述获取分类建议
     */
    fun getSuggestedCategoriesForDescription(description: String, isIncome: Boolean) {
        if (description.isBlank()) {
            _suggestedCategories.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val suggestions = categorySuggestionService.suggestCategoriesForDescription(
                    description, isIncome
                )
                _suggestedCategories.value = suggestions
            } catch (e: Exception) {
                _suggestedCategories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 根据交易金额获取分类建议
     */
    fun getSuggestedCategoriesForAmount(amount: Double, isIncome: Boolean) {
        if (amount <= 0) {
            _suggestedCategories.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val suggestions = categorySuggestionService.suggestCategoriesForAmount(
                    amount, isIncome
                )
                _suggestedCategories.value = suggestions
            } catch (e: Exception) {
                _suggestedCategories.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 清除分类建议
     */
    fun clearSuggestions() {
        _suggestedCategories.value = emptyList()
    }
    
    /**
     * 通过ID获取分类
     */
    fun getCategoryById(id: Long): CategoryWithIcon? {
        return _categories.value.find { it.category.id == id }
    }
    
    /**
     * 加载分类详情
     */
    fun loadCategoryDetails(id: Long) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // 通过ID查找分类
            val category = getCategoryById(id)
            _selectedCategory.value = category
            
            _isLoading.value = false
        }
    }
    
    /**
     * 添加新分类
     */
    fun addCategory(category: Category, icon: ImageVector, color: Color) {
        viewModelScope.launch {
            // 使用Repository保存分类
            try {
                val id = categoryRepository.addCategory(category)
                val newCategory = category.copy(id = id)
                val categoryWithIcon = CategoryWithIcon(newCategory, icon, color)
                
                // 更新列表
                val updatedList = _categories.value.toMutableList()
                updatedList.add(categoryWithIcon)
                _categories.value = updatedList
                
                // 更新对应类型的分类列表
                if (category.isIncome) {
                    _incomeCategories.value = updatedList.filter { it.category.isIncome }
                    // 如果是顶级分类，也更新顶级分类列表
                    if (category.parentId == null) {
                        loadTopLevelCategories()
                    }
                } else {
                    _expenseCategories.value = updatedList.filter { !it.category.isIncome }
                    // 如果是顶级分类，也更新顶级分类列表
                    if (category.parentId == null) {
                        loadTopLevelCategories()
                    }
                }
                
                // 如果当前正在查看父分类的子分类，且新分类的父分类ID与当前选中的父分类ID相同，则更新子分类列表
                if (_selectedParentCategoryId.value != null && _selectedParentCategoryId.value == category.parentId) {
                    val updatedChildren = _childCategories.value.toMutableList()
                    updatedChildren.add(categoryWithIcon)
                    _childCategories.value = updatedChildren
                }
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }
    
    /**
     * 添加子分类
     */
    fun addChildCategory(parentId: Long, childCategory: Category, icon: ImageVector, color: Color) {
        viewModelScope.launch {
            try {
                // 使用Repository添加子分类
                val childId = categoryRepository.addChildCategory(parentId, childCategory)
                
                if (childId > 0) {
                    // 重新加载子分类列表
                    val children = categoryRepository.getChildCategories(parentId).first()
                    _childCategories.value = children.map { mapCategoryToIcon(it) }
                    
                    // 同时更新全局分类列表
                    loadCategories()
                }
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }
    
    /**
     * 更新分类
     */
    fun updateCategory(category: Category, icon: ImageVector, color: Color) {
        viewModelScope.launch {
            try {
                // 使用Repository更新分类
                categoryRepository.updateCategory(category)
                
                val categoryWithIcon = CategoryWithIcon(category, icon, color)
                val updatedList = _categories.value.toMutableList()
                val index = updatedList.indexOfFirst { it.category.id == category.id }
                
                if (index != -1) {
                    updatedList[index] = categoryWithIcon
                    _categories.value = updatedList
                    
                    // 更新对应类型的分类列表
                    _expenseCategories.value = updatedList.filter { !it.category.isIncome }
                    _incomeCategories.value = updatedList.filter { it.category.isIncome }
                    
                    // 如果是顶级分类，也更新顶级分类列表
                    if (category.parentId == null) {
                        loadTopLevelCategories()
                    }
                    
                    // 如果当前正在查看父分类的子分类，且更新的分类是其中之一，也更新子分类列表
                    if (_selectedParentCategoryId.value != null && _selectedParentCategoryId.value == category.parentId) {
                        val childIndex = _childCategories.value.indexOfFirst { it.category.id == category.id }
                        if (childIndex != -1) {
                            val updatedChildren = _childCategories.value.toMutableList()
                            updatedChildren[childIndex] = categoryWithIcon
                            _childCategories.value = updatedChildren
                        }
                    }
                    
                    // 如果当前选中的分类被更新，也更新选中状态
                    if (_selectedCategory.value?.category?.id == category.id) {
                        _selectedCategory.value = categoryWithIcon
                    }
                }
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }
    
    /**
     * 检查分类是否有子分类
     */
    suspend fun hasChildCategories(categoryId: Long): Boolean {
        return categoryRepository.hasChildCategories(categoryId)
    }
    
    /**
     * 删除分类
     */
    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            try {
                // 检查是否有子分类
                val hasChildren = categoryRepository.hasChildCategories(id)
                
                if (hasChildren) {
                    // 如果有子分类，递归删除
                    categoryRepository.deleteCategoryWithChildren(id)
                } else {
                    // 使用Repository删除单个分类
                    categoryRepository.deleteCategoryById(id)
                }
                
                // 更新全局分类列表
                loadCategories()
                
                // 如果当前在查看此分类的子分类，则返回上一级
                if (_selectedParentCategoryId.value == id) {
                    navigateToParent()
                }
                
                // 如果删除的是当前选中的分类，清除选中状态
                if (_selectedCategory.value?.category?.id == id) {
                    _selectedCategory.value = null
                }
            } catch (e: Exception) {
                // 错误处理
            }
        }
    }
    
    /**
     * 获取所有可用图标
     */
    fun getAvailableIcons(): List<Pair<ImageVector, Color>> {
        return listOf(
            Pair(Icons.Default.Restaurant, PrimaryDark),
            Pair(Icons.Default.DirectionsBus, CategoryBlue),
            Pair(Icons.Default.SportsEsports, CategoryOrange),
            Pair(Icons.Default.Home, CategoryBlue),
            Pair(Icons.Default.Checkroom, CategoryPink),
            Pair(Icons.Default.AttachMoney, IncomeGreen),
            Pair(Icons.Default.Work, IncomeGreen)
        )
    }
    
    /**
     * 将Category映射为CategoryWithIcon
     */
    private fun mapCategoryToIcon(category: Category): CategoryWithIcon {
        // 基于类别名称或图标属性匹配图标
        val iconPair = when(category.icon) {
            "restaurant" -> Pair(Icons.Default.Restaurant, Color(category.color))
            "directions_bus" -> Pair(Icons.Default.DirectionsBus, Color(category.color))
            "sports_esports" -> Pair(Icons.Default.SportsEsports, Color(category.color))
            "home" -> Pair(Icons.Default.Home, Color(category.color))
            "checkroom" -> Pair(Icons.Default.Checkroom, Color(category.color))
            "attach_money" -> Pair(Icons.Default.AttachMoney, Color(category.color))
            "work" -> Pair(Icons.Default.Work, Color(category.color))
            else -> {
                // 根据是否为收入类型选择默认图标
                if (category.isIncome) {
                    Pair(Icons.Default.AttachMoney, IncomeGreen)
                } else {
                    Pair(Icons.Default.Restaurant, PrimaryDark)
                }
            }
        }
        
        return CategoryWithIcon(category, iconPair.first, iconPair.second)
    }
    
    /**
     * 获取示例分类数据（用于测试或加载失败时显示）
     */
    private fun getSampleCategoriesWithIcons(): List<CategoryWithIcon> {
        return listOf(
            CategoryWithIcon(
                Category(id = 1, name = "餐饮", isIncome = false), 
                Icons.Default.Restaurant, 
                PrimaryDark
            ),
            CategoryWithIcon(
                Category(id = 2, name = "交通", isIncome = false), 
                Icons.Default.DirectionsBus, 
                CategoryBlue
            ),
            CategoryWithIcon(
                Category(id = 3, name = "工资", isIncome = true), 
                Icons.Default.Work, 
                IncomeGreen
            )
        )
    }
}

/**
 * 分类数据模型，包含UI显示所需的图标和颜色
 */
data class CategoryWithIcon(
    val category: Category,
    val icon: ImageVector,
    val color: Color
) 