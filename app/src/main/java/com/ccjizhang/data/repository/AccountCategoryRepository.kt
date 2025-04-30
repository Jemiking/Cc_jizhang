package com.ccjizhang.data.repository

import com.ccjizhang.data.db.dao.AccountCategoryDao
import com.ccjizhang.data.model.AccountCategory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 账户分类数据仓库
 * 负责提供账户分类数据和处理业务逻辑
 */
@Singleton
class AccountCategoryRepository @Inject constructor(
    private val accountCategoryDao: AccountCategoryDao
) {
    /**
     * 获取所有账户分类
     */
    fun getAllCategories(): Flow<List<AccountCategory>> = accountCategoryDao.getAllCategories()

    /**
     * 获取所有账户分类（同步方法）
     */
    suspend fun getAllCategoriesSync(): List<AccountCategory> = accountCategoryDao.getAllCategoriesSync()

    /**
     * 按ID获取账户分类
     */
    fun getCategoryById(id: Long): Flow<AccountCategory?> = accountCategoryDao.getCategoryById(id)

    /**
     * 按ID获取账户分类（同步方法）
     */
    suspend fun getCategoryByIdSync(id: Long): AccountCategory? = accountCategoryDao.getCategoryByIdSync(id)

    /**
     * 获取默认账户分类
     */
    fun getDefaultCategory(): Flow<AccountCategory?> = accountCategoryDao.getDefaultCategory()

    /**
     * 获取默认账户分类（同步方法）
     */
    suspend fun getDefaultCategorySync(): AccountCategory? = accountCategoryDao.getDefaultCategorySync()

    /**
     * 添加账户分类
     */
    suspend fun addCategory(category: AccountCategory): Long {
        // 如果是默认分类，确保其他分类不是默认的
        if (category.isDefault) {
            clearDefaultCategory()
        }
        return accountCategoryDao.insert(category)
    }

    /**
     * 更新账户分类
     */
    suspend fun updateCategory(category: AccountCategory) {
        // 如果是默认分类，确保其他分类不是默认的
        if (category.isDefault) {
            clearDefaultCategory()
        }
        accountCategoryDao.update(category)
    }

    /**
     * 删除账户分类
     */
    suspend fun deleteCategory(category: AccountCategory) {
        accountCategoryDao.delete(category)
    }

    /**
     * 按ID删除账户分类
     */
    suspend fun deleteCategoryById(id: Long) {
        accountCategoryDao.deleteById(id)
    }

    /**
     * 清除所有默认账户分类标记
     */
    private suspend fun clearDefaultCategory() {
        val categories = accountCategoryDao.getAllCategoriesSync()
        for (category in categories) {
            if (category.isDefault) {
                accountCategoryDao.update(category.copy(isDefault = false))
            }
        }
    }

    /**
     * 初始化默认账户分类
     */
    suspend fun initDefaultCategories() {
        val categories = accountCategoryDao.getAllCategoriesSync()
        if (categories.isEmpty()) {
            val defaultCategories = listOf(
                AccountCategory(
                    name = "现金账户",
                    icon = "account_balance_wallet",
                    color = 0xFF43A047.toInt(),
                    sortOrder = 0,
                    isDefault = true
                ),
                AccountCategory(
                    name = "银行卡",
                    icon = "credit_card",
                    color = 0xFF1976D2.toInt(),
                    sortOrder = 1,
                    isDefault = false
                ),
                AccountCategory(
                    name = "电子钱包",
                    icon = "payment",
                    color = 0xFF039BE5.toInt(),
                    sortOrder = 2,
                    isDefault = false
                ),
                AccountCategory(
                    name = "信用卡",
                    icon = "credit_card",
                    color = 0xFFE53935.toInt(),
                    sortOrder = 3,
                    isDefault = false
                ),
                AccountCategory(
                    name = "投资账户",
                    icon = "trending_up",
                    color = 0xFFFF9800.toInt(),
                    sortOrder = 4,
                    isDefault = false
                )
            )
            
            for (category in defaultCategories) {
                accountCategoryDao.insert(category)
            }
        }
    }
}
