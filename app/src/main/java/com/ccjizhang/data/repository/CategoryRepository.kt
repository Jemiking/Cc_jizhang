package com.ccjizhang.data.repository

import android.graphics.Color
import com.ccjizhang.data.db.dao.CategoryDao
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.CategoryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分类数据仓库
 * 负责提供分类数据和处理业务逻辑
 */
@Singleton
class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    // 获取所有分类
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    
    // 按ID获取分类
    fun getCategoryById(id: Long): Flow<Category?> = categoryDao.getCategoryById(id)
    
    // 根据ID列表获取多个分类
    suspend fun getCategoriesByIds(ids: List<Long>): List<Category> = 
        categoryDao.getCategoriesByIds(ids)
    
    // 获取支出分类
    fun getExpenseCategories(): Flow<List<Category>> = 
        categoryDao.getCategoriesByType(CategoryType.EXPENSE)
    
    // 获取收入分类
    fun getIncomeCategories(): Flow<List<Category>> = 
        categoryDao.getCategoriesByType(CategoryType.INCOME)
    
    // 获取自定义分类
    fun getCustomCategories(type: CategoryType): Flow<List<Category>> = 
        categoryDao.getCategoriesByTypeAndCustom(type, true)
    
    // 获取默认分类
    fun getDefaultCategories(type: CategoryType): Flow<List<Category>> = 
        categoryDao.getCategoriesByTypeAndCustom(type, false)
    
    // 获取顶级分类（没有父分类的分类）
    fun getTopLevelCategories(type: CategoryType): Flow<List<Category>> =
        categoryDao.getTopLevelCategories(type)
    
    // 获取指定分类的子分类
    fun getChildCategories(parentId: Long): Flow<List<Category>> =
        categoryDao.getChildCategories(parentId)
    
    // 获取所有子分类（包括子分类的子分类）
    fun getAllChildCategories(parentId: Long): Flow<List<Category>> =
        categoryDao.getAllChildCategories(parentId)
    
    // 获取指定层级的分类
    fun getCategoriesByLevel(level: Int, type: CategoryType): Flow<List<Category>> =
        categoryDao.getCategoriesByLevel(level, type)
    
    // 检查分类是否有子分类
    suspend fun hasChildCategories(categoryId: Long): Boolean =
        categoryDao.hasChildCategories(categoryId) > 0
    
    // 添加分类
    suspend fun addCategory(category: Category): Long {
        return categoryDao.insert(category)
    }
    
    // 添加子分类
    suspend fun addChildCategory(parentId: Long, category: Category): Long {
        // 获取父分类
        val parentCategory = categoryDao.getCategoryById(parentId).first()
        
        // 如果找到父分类，则创建子分类
        if (parentCategory != null) {
            // 子分类层级为父分类层级+1
            val childLevel = parentCategory.level + 1
            // 创建子分类，继承父分类的类型
            val childCategory = category.copy(
                parentId = parentId,
                level = childLevel,
                type = parentCategory.type,
                isIncome = parentCategory.isIncome
            )
            return categoryDao.insert(childCategory)
        }
        return -1 // 父分类不存在
    }
    
    // 更新分类
    suspend fun updateCategory(category: Category) {
        categoryDao.update(category)
    }
    
    // 删除分类
    suspend fun deleteCategory(category: Category) {
        categoryDao.delete(category)
    }
    
    // 按ID删除分类
    suspend fun deleteCategoryById(id: Long) {
        categoryDao.deleteById(id)
    }
    
    // 删除分类及其所有子分类
    suspend fun deleteCategoryWithChildren(categoryId: Long) {
        // 先获取所有子分类
        val childCategories = categoryDao.getAllChildCategories(categoryId).first()
        
        // 删除子分类
        for (child in childCategories) {
            categoryDao.deleteById(child.id)
        }
        
        // 最后删除父分类
        categoryDao.deleteById(categoryId)
    }
    
    // 获取分类数量
    fun getCategoryCount(type: CategoryType): Flow<Int> = 
        categoryDao.getCategoryCountByType(type)
    
    // 初始化默认分类
    suspend fun initDefaultCategories() {
        // 检查分类是否已存在
        val expenseCount = categoryDao.getCategoryCountByType(CategoryType.EXPENSE).first()
        val incomeCount = categoryDao.getCategoryCountByType(CategoryType.INCOME).first()
        
        if (expenseCount == null || expenseCount <= 0) {
            // 添加默认支出分类
            val defaultExpenseCategories = listOf(
                Category(name = "餐饮", type = CategoryType.EXPENSE, icon = "restaurant", color = -10107849), // 等同于Color.parseColor("#5D4037")
                Category(name = "交通", type = CategoryType.EXPENSE, icon = "directions_bus", color = -14642899), // 等同于Color.parseColor("#1976D2")
                Category(name = "购物", type = CategoryType.EXPENSE, icon = "shopping_cart", color = -1673527), // 等同于Color.parseColor("#E64A19")
                Category(name = "娱乐", type = CategoryType.EXPENSE, icon = "sports_esports", color = -8732766), // 等同于Color.parseColor("#7B1FA2")
                Category(name = "居家", type = CategoryType.EXPENSE, icon = "home", color = -12412356), // 等同于Color.parseColor("#388E3C")
                Category(name = "服饰", type = CategoryType.EXPENSE, icon = "checkroom", color = -2617828), // 等同于Color.parseColor("#D81B60")
                Category(name = "医疗", type = CategoryType.EXPENSE, icon = "medical_services", color = -2944769), // 等同于Color.parseColor("#D32F2F")
                Category(name = "教育", type = CategoryType.EXPENSE, icon = "school", color = -16566063), // 等同于Color.parseColor("#0288D1")
                Category(name = "其他", type = CategoryType.EXPENSE, icon = "more_horiz", color = -10395295) // 等同于Color.parseColor("#616161")
            )
            categoryDao.insertAll(defaultExpenseCategories)
        }
        
        if (incomeCount == null || incomeCount <= 0) {
            // 添加默认收入分类
            val defaultIncomeCategories = listOf(
                Category(name = "工资", type = CategoryType.INCOME, icon = "work", color = -16742533), // 等同于Color.parseColor("#00897B")
                Category(name = "奖金", type = CategoryType.INCOME, icon = "attach_money", color = -12245561), // 等同于Color.parseColor("#43A047")
                Category(name = "理财", type = CategoryType.INCOME, icon = "trending_up", color = -16738111), // 等同于Color.parseColor("#00ACC1")
                Category(name = "退款", type = CategoryType.INCOME, icon = "receipt", color = -10642051), // 等同于Color.parseColor("#5E35B1")
                Category(name = "其他", type = CategoryType.INCOME, icon = "more_horiz", color = -11186550) // 等同于Color.parseColor("#546E7A")
            )
            categoryDao.insertAll(defaultIncomeCategories)
        }
    }
} 