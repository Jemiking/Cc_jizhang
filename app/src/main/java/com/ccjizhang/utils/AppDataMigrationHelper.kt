package com.ccjizhang.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.repository.AccountRepository
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.TransactionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * 应用间数据迁移助手
 * 负责从其他记账应用导入数据的功能
 */
@Singleton
class AppDataMigrationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    companion object {
        private const val TAG = "AppDataMigrationHelper"
        
        // 支持的第三方记账应用格式
        enum class AppType {
            MONEY_MANAGER, // Money Manager 应用
            MONEY_LOVER,   // Money Lover 应用
            AND_MONEY,     // AndMoney 应用
            UNKNOWN        // 未知格式
        }
    }
    
    /**
     * 从第三方应用导入数据
     * @param uri 导入文件的URI
     * @param appType 第三方应用类型
     * @return 导入结果
     */
    suspend fun importFromThirdPartyApp(uri: Uri, appType: AppType): MigrationResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext MigrationResult.Error("无法打开文件")
            
            val result = when (appType) {
                AppType.MONEY_MANAGER -> importFromMoneyManager(inputStream)
                AppType.MONEY_LOVER -> importFromMoneyLover(inputStream)
                AppType.AND_MONEY -> importFromAndMoney(inputStream)
                AppType.UNKNOWN -> detectAndImport(inputStream)
            }
            
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "导入数据失败", e)
            return@withContext MigrationResult.Error("导入失败: ${e.localizedMessage}")
        }
    }
    
    /**
     * 检测文件格式并导入
     */
    private suspend fun detectAndImport(inputStream: InputStream): MigrationResult {
        try {
            // 读取文件内容
            val content = inputStream.bufferedReader().use { it.readText() }
            
            // 尝试解析为JSON
            if (content.trim().startsWith("{") || content.trim().startsWith("[")) {
                // 尝试检测JSON格式
                if (content.contains("\"transactions\"") && content.contains("\"accounts\"")) {
                    return importFromMoneyManager(content.byteInputStream())
                } else if (content.contains("\"walletId\"") || content.contains("\"moneyLover\"")) {
                    return importFromMoneyLover(content.byteInputStream())
                }
            }
            
            // 尝试解析为CSV（AndMoney通常使用CSV格式）
            if (content.contains(",") && (
                    content.contains("Date,Amount") || 
                    content.contains("Transaction,Category")
                )) {
                return importFromAndMoney(content.byteInputStream())
            }
            
            return MigrationResult.Error("无法识别文件格式")
        } catch (e: Exception) {
            Log.e(TAG, "检测文件格式失败", e)
            return MigrationResult.Error("文件格式检测失败: ${e.localizedMessage}")
        }
    }
    
    /**
     * 从Money Manager应用导入数据
     */
    private suspend fun importFromMoneyManager(inputStream: InputStream): MigrationResult {
        try {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonContent = reader.readText()
            val jsonObject = JSONObject(jsonContent)
            
            // 导入账户
            val accountsJson = jsonObject.optJSONArray("accounts") ?: JSONArray()
            val importedAccounts = importMoneyManagerAccounts(accountsJson)
            
            // 导入分类
            val categoriesJson = jsonObject.optJSONArray("categories") ?: JSONArray()
            val importedCategories = importMoneyManagerCategories(categoriesJson)
            
            // 导入交易
            val transactionsJson = jsonObject.optJSONArray("transactions") ?: JSONArray()
            val importedTransactions = importMoneyManagerTransactions(transactionsJson)
            
            return MigrationResult.Success(
                accounts = importedAccounts,
                categories = importedCategories,
                transactions = importedTransactions
            )
        } catch (e: Exception) {
            Log.e(TAG, "从Money Manager导入失败", e)
            return MigrationResult.Error("从Money Manager导入失败: ${e.localizedMessage}")
        }
    }
    
    /**
     * 导入Money Manager的账户数据
     */
    private suspend fun importMoneyManagerAccounts(accountsJson: JSONArray): Int {
        var importedCount = 0
        
        for (i in 0 until accountsJson.length()) {
            try {
                val accountJson = accountsJson.getJSONObject(i)
                
                val name = accountJson.optString("name", "导入账户")
                val balance = accountJson.optDouble("balance", 0.0)
                val accountType = parseMoneyManagerAccountType(accountJson.optString("type", ""))
                
                val account = Account(
                    name = name,
                    balance = balance,
                    type = accountType,
                    icon = "account_balance_wallet", // 默认图标
                    color = 0xFF2196F3.toInt(),      // 默认颜色
                    isDefault = false,
                    includeInTotal = true
                )
                
                accountRepository.addAccount(account)
                importedCount++
            } catch (e: Exception) {
                Log.e(TAG, "导入账户失败", e)
            }
        }
        
        return importedCount
    }
    
    /**
     * 解析Money Manager的账户类型
     */
    private fun parseMoneyManagerAccountType(typeStr: String): com.ccjizhang.data.model.AccountType {
        return when (typeStr.toLowerCase(Locale.ROOT)) {
            "cash" -> com.ccjizhang.data.model.AccountType.CASH
            "bank", "checking" -> com.ccjizhang.data.model.AccountType.DEBIT_CARD
            "credit" -> com.ccjizhang.data.model.AccountType.CREDIT_CARD
            "investment" -> com.ccjizhang.data.model.AccountType.OTHER
            "alipay" -> com.ccjizhang.data.model.AccountType.ALIPAY
            "wechat" -> com.ccjizhang.data.model.AccountType.WECHAT
            else -> com.ccjizhang.data.model.AccountType.OTHER
        }
    }
    
    /**
     * 导入Money Manager的分类数据
     */
    private suspend fun importMoneyManagerCategories(categoriesJson: JSONArray): Int {
        var importedCount = 0
        
        for (i in 0 until categoriesJson.length()) {
            try {
                val categoryJson = categoriesJson.getJSONObject(i)
                
                val name = categoryJson.optString("name", "导入分类")
                val isIncome = categoryJson.optBoolean("isIncome", false)
                
                val category = Category(
                    name = name,
                    isIncome = isIncome,
                    icon = if (isIncome) "attach_money" else "money_off", // 默认图标
                    color = if (isIncome) 0xFF4CAF50.toInt() else 0xFFF44336.toInt() // 默认颜色
                )
                
                categoryRepository.addCategory(category)
                importedCount++
            } catch (e: Exception) {
                Log.e(TAG, "导入分类失败", e)
            }
        }
        
        return importedCount
    }
    
    /**
     * 导入Money Manager的交易数据
     */
    private suspend fun importMoneyManagerTransactions(transactionsJson: JSONArray): Int {
        var importedCount = 0
        
        // 获取默认账户和分类，用于找不到对应数据时
        val accounts = accountRepository.getAllAccountsSync()
        val defaultAccount = accounts.find { it.isDefault } ?: accounts.firstOrNull()
        
        // 获取分类
        val expenseCategories = categoryRepository.getExpenseCategories().first()
        val incomeCategories = categoryRepository.getIncomeCategories().first()
        val defaultExpenseCategory = expenseCategories.firstOrNull()
        val defaultIncomeCategory = incomeCategories.firstOrNull()
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        
        for (i in 0 until transactionsJson.length()) {
            try {
                val transactionJson = transactionsJson.getJSONObject(i)
                
                val amount = transactionJson.optDouble("amount", 0.0)
                val isIncome = transactionJson.optBoolean("isIncome", false)
                val note = transactionJson.optString("note", "")
                val dateStr = transactionJson.optString("date", "")
                val date = try {
                    dateFormat.parse(dateStr) ?: Date()
                } catch (e: Exception) {
                    Date()
                }
                
                // 查找或使用默认账户
                val accountName = transactionJson.optString("accountName", "")
                val account = if (accountName.isNotEmpty()) {
                    accounts.find { acc: Account -> acc.name == accountName }
                } else null ?: defaultAccount
                
                // 查找或使用默认分类
                val categoryName = transactionJson.optString("categoryName", "")
                val category = if (categoryName.isNotEmpty()) {
                    val categories = if (isIncome) incomeCategories else expenseCategories
                    categories.find { cat: Category -> cat.name == categoryName }
                } else null ?: if (isIncome) defaultIncomeCategory else defaultExpenseCategory
                
                if (account != null && category != null) {
                    val transaction = Transaction(
                        amount = amount,
                        isIncome = isIncome,
                        date = date,
                        note = note,
                        accountId = account.id,
                        categoryId = category.id
                    )
                    
                    transactionRepository.addTransaction(transaction)
                    importedCount++
                }
            } catch (e: Exception) {
                Log.e(TAG, "导入交易失败", e)
            }
        }
        
        return importedCount
    }
    
    /**
     * 从Money Lover应用导入数据
     */
    private suspend fun importFromMoneyLover(inputStream: InputStream): MigrationResult {
        // TODO: 实现Money Lover导入逻辑
        // 由于Money Lover格式较为复杂，此处仅作为示例
        return MigrationResult.Error("Money Lover导入功能尚未完全实现")
    }
    
    /**
     * 从AndMoney应用导入数据
     */
    private suspend fun importFromAndMoney(inputStream: InputStream): MigrationResult {
        // TODO: 实现AndMoney导入逻辑
        // AndMoney通常使用CSV格式，需要解析CSV
        return MigrationResult.Error("AndMoney导入功能尚未完全实现")
    }
    
    /**
     * 迁移结果
     */
    sealed class MigrationResult {
        /**
         * 成功结果
         */
        data class Success(
            val accounts: Int = 0,
            val categories: Int = 0,
            val transactions: Int = 0
        ) : MigrationResult()
        
        /**
         * 错误结果
         */
        data class Error(val message: String) : MigrationResult()
    }
} 