package com.ccjizhang.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import timber.log.Timber
import com.ccjizhang.data.db.AppDatabase
import com.ccjizhang.data.db.dao.AccountDao
import com.ccjizhang.data.db.dao.BudgetDao
import com.ccjizhang.data.db.dao.CategoryDao
import com.ccjizhang.data.db.dao.TransactionDao
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.Budget
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.CategoryType
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope

/**
 * 数据导出导入仓库
 * 负责提供数据备份和恢复功能
 */
@Singleton
class DataExportImportRepository @Inject constructor(
    private val appDatabase: AppDatabase,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val accountDao: AccountDao,
    private val budgetDao: BudgetDao
) {

    companion object {
        private const val TAG = "DataExportImportRepo"
    }

    /**
     * 从JSON导入数据
     */
    suspend fun importDataFromJson(context: Context, uri: Uri): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // 解析JSON文件
                val jsonContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?:
                    return@withContext Result.failure(Exception("无法读取文件"))

                val jsonObject = JSONObject(jsonContent)

                // 不使用runInTransaction，而是直接在协程中执行各个操作
                // 导入分类
                if (jsonObject.has("categories")) {
                    val categoriesJson = jsonObject.getJSONArray("categories").toString()
                    val categoriesType = object : TypeToken<List<Category>>() {}.type
                    val categories = Gson().fromJson<List<Category>>(categoriesJson, categoriesType)
                    categoryDao.insertAll(categories)
                }

                // 导入账户
                if (jsonObject.has("accounts")) {
                    val accountsJson = jsonObject.getJSONArray("accounts").toString()
                    val accountsType = object : TypeToken<List<Account>>() {}.type
                    val accounts = Gson().fromJson<List<Account>>(accountsJson, accountsType)
                    accountDao.insertAll(accounts)
                }

                // 导入预算
                if (jsonObject.has("budgets")) {
                    val budgetsJson = jsonObject.getJSONArray("budgets").toString()
                    val budgetsType = object : TypeToken<List<Budget>>() {}.type
                    val budgets = Gson().fromJson<List<Budget>>(budgetsJson, budgetsType)
                    budgetDao.insertAll(budgets)
                }

                // 导入交易
                if (jsonObject.has("transactions")) {
                    val transactionsJson = jsonObject.getJSONArray("transactions").toString()
                    val transactionsType = object : TypeToken<List<Transaction>>() {}.type
                    val transactions = Gson().fromJson<List<Transaction>>(transactionsJson, transactionsType)
                    transactionDao.insertAll(transactions)
                }

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从CSV导入数据
     */
    suspend fun importDataFromCsv(
        context: Context,
        categoryUri: Uri,
        accountUri: Uri,
        budgetUri: Uri,
        transactionUri: Uri
    ): Result<Unit> {
        return try {
            withContext(Dispatchers.IO) {
                // 不使用runInTransaction，而是直接在协程中执行各个操作
                // 导入分类
                val categories = importCategoriesFromCsv(context, categoryUri)
                categoryDao.insertAll(categories)

                // 导入账户
                val accounts = importAccountsFromCsv(context, accountUri)
                accountDao.insertAll(accounts)

                // 导入预算
                val budgets = importBudgetsFromCsv(context, budgetUri)
                budgetDao.insertAll(budgets)

                // 导入交易
                val transactions = importTransactionsFromCsv(context, transactionUri)
                transactionDao.insertAll(transactions)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 从CSV导入分类数据
     */
    private suspend fun importCategoriesFromCsv(context: Context, uri: Uri): List<Category> = withContext(Dispatchers.IO) {
        val categories = mutableListOf<Category>()

        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            // 跳过CSV头行
            reader.readLine()

            // 读取数据行
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size >= 7) {
                    val category = Category(
                        id = parts[0].toLongOrNull() ?: 0L,
                        name = parts[1],
                        type = CategoryType.valueOf(parts[2]),
                        icon = parts[3],
                        color = parts[4].toIntOrNull() ?: 0,
                        isCustom = parts[5].toBoolean(),
                        sortOrder = parts[6].toIntOrNull() ?: 0
                    )
                    categories.add(category)
                }
            }
        }

        categories
    }

    /**
     * 从CSV导入账户数据
     */
    private suspend fun importAccountsFromCsv(context: Context, uri: Uri): List<Account> = withContext(Dispatchers.IO) {
        val accounts = mutableListOf<Account>()

        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            // 跳过CSV头行
            reader.readLine()

            // 读取数据行
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size >= 6) {
                    val account = Account(
                        id = parts[0].toLongOrNull() ?: 0L,
                        name = parts[1],
                        type = AccountType.valueOf(parts[2]),
                        balance = parts[3].toDoubleOrNull() ?: 0.0,
                        icon = parts[4],
                        color = parts[5].toIntOrNull() ?: 0
                    )
                    accounts.add(account)
                }
            }
        }

        accounts
    }

    /**
     * 从CSV导入预算数据
     */
    private suspend fun importBudgetsFromCsv(context: Context, uri: Uri): List<Budget> = withContext(Dispatchers.IO) {
        val budgets = mutableListOf<Budget>()

        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            // 跳过CSV头行
            reader.readLine()

            // 读取数据行
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size >= 10) {
                    try {
                        val budget = Budget(
                            id = parts[0].toLongOrNull() ?: 0L,
                            name = parts[1],
                            amount = parts[2].toDoubleOrNull() ?: 0.0,
                            startDate = dateFormat.parse(parts[3]) ?: Date(),
                            endDate = dateFormat.parse(parts[4]) ?: Date(),
                            period = parts[5],
                            categories = parts[6].split(";").filter { it.isNotEmpty() }.map { it.toLongOrNull() ?: 0L },
                            isActive = parts[7].toBoolean(),
                            notifyEnabled = parts[8].toBoolean(),
                            notifyThreshold = parts[9].toIntOrNull() ?: 80
                        )
                        budgets.add(budget)
                    } catch (e: Exception) {
                        // 记录错误并继续
                        e.printStackTrace()
                    }
                }
            }
        }

        budgets
    }

    /**
     * 从CSV导入交易数据
     */
    private suspend fun importTransactionsFromCsv(context: Context, uri: Uri): List<Transaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<Transaction>()

        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            // 跳过CSV头行
            reader.readLine()

            // 读取数据行
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line!!.split(",")
                if (parts.size >= 8) {
                    try {
                        val transaction = Transaction(
                            id = parts[0].toLongOrNull() ?: 0L,
                            amount = parts[1].toDoubleOrNull() ?: 0.0,
                            isIncome = parts[2].toBoolean(),
                            categoryId = parts[3].toLongOrNull() ?: 0L,
                            accountId = parts[4].toLongOrNull() ?: 0L,
                            date = dateFormat.parse(parts[5]) ?: Date(),
                            note = parts[6]
                        )
                        transactions.add(transaction)
                    } catch (e: Exception) {
                        // 记录错误并继续
                        e.printStackTrace()
                    }
                }
            }
        }

        transactions
    }

    /**
     * 导出数据为JSON字符串
     */
    suspend fun exportDataToJson(context: Context): String {
        return withContext(Dispatchers.IO) {
            // 获取所有数据
            val categories = categoryDao.getAllCategories().first()
            val accounts = accountDao.getAllAccounts().first()
            val budgets = budgetDao.getAllBudgets().first()
            val transactions = transactionDao.getAllTransactions().first()

            // 创建JSON对象
            val jsonObject = JSONObject()

            // 添加数据
            val gson = GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss").create()
            jsonObject.put("categories", JSONArray(gson.toJson(categories)))
            jsonObject.put("accounts", JSONArray(gson.toJson(accounts)))
            jsonObject.put("budgets", JSONArray(gson.toJson(budgets)))
            jsonObject.put("transactions", JSONArray(gson.toJson(transactions)))

            // 添加元数据
            jsonObject.put("metadata", JSONObject().apply {
                put("exportTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                put("version", "1.0")
            })

            // 返回格式化的JSON字符串
            jsonObject.toString(4) // 缩进4个空格
        }
    }

    /**
     * 导出数据为JSON格式到URI
     */
    suspend fun exportDataToJson(context: Context, uri: Uri): Result<Unit> {
        return exportDataToJsonUri(context, uri)
    }



    /**
     * 导出数据为JSON格式到URI
     * 与exportDataToJson功能相同，提供兼容性
     */
    suspend fun exportDataToJsonUri(context: Context, uri: Uri): Result<Unit> {
        Timber.i("开始导出数据到JSON，目标URI: $uri")
        return try {
            withContext(Dispatchers.IO) {
                val startTime = System.currentTimeMillis()
                Timber.d("开始导出数据到JSON，目标URI: $uri")

                // 获取JSON数据
                val jsonData = try {
                    exportDataToJson(context)
                } catch (e: Exception) {
                    Timber.e(e, "生成JSON数据失败")
                    return@withContext Result.failure(Exception("生成JSON数据失败: ${e.message}", e))
                }

                val jsonGenerationTime = System.currentTimeMillis() - startTime
                Timber.d("成功生成JSON数据，大小: ${jsonData.length} 字节，耗时: ${jsonGenerationTime}ms")

                // 写入文件
                val writeStartTime = System.currentTimeMillis()
                Timber.d("开始写入文件，URI: $uri")
                try {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                    if (outputStream == null) {
                        Timber.e("无法打开输出流，URI: $uri")
                        return@withContext Result.failure(Exception("无法打开输出流"))
                    }

                    outputStream.use { stream ->
                        val writer = OutputStreamWriter(stream)
                        writer.write(jsonData)
                        writer.flush()
                    }

                    val writeTime = System.currentTimeMillis() - writeStartTime
                    val totalTime = System.currentTimeMillis() - startTime
                    Timber.i("数据导出成功，URI: $uri，写入耗时: ${writeTime}ms，总耗时: ${totalTime}ms")
                    Result.success(Unit)
                } catch (e: Exception) {
                    Timber.e(e, "写入文件失败，URI: $uri，异常类型: ${e.javaClass.simpleName}")
                    Result.failure(Exception("写入文件失败: ${e.message}", e))
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "导出数据到JSON失败，异常类型: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    // 已在下面实现了更完整的版本，这里删除重复定义

    /**
     * 导出数据为CSV格式
     */
    suspend fun exportDataToCsv(context: Context, baseUri: Uri): Result<List<Uri>> {
        return try {
            withContext(Dispatchers.IO) {
                val uriList = mutableListOf<Uri>()

                // 创建目录
                val dirUri = DocumentsContract.buildDocumentUriUsingTree(
                    baseUri,
                    DocumentsContract.getTreeDocumentId(baseUri)
                )

                // 导出分类数据
                val categoryData = categoryDao.getAllCategories().first()
                val categoryUri = exportCategoryToCsv(context, categoryData, dirUri)
                categoryUri?.let { uriList.add(it) }

                // 导出账户数据
                val accountData = accountDao.getAllAccounts().first()
                val accountUri = exportAccountToCsv(context, accountData, dirUri)
                accountUri?.let { uriList.add(it) }

                // 导出预算数据
                val budgetData = budgetDao.getAllBudgets().first()
                val budgetUri = exportBudgetToCsv(context, budgetData, dirUri)
                budgetUri?.let { uriList.add(it) }

                // 导出交易数据
                val transactionData = transactionDao.getAllTransactions().first()
                val transactionUri = exportTransactionToCsv(context, transactionData, dirUri)
                transactionUri?.let { uriList.add(it) }

                Result.success(uriList)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 导出分类数据到CSV
     */
    private suspend fun exportCategoryToCsv(context: Context, categories: List<Category>, dirUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val fileName = "categories_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".csv"

        try {
            // 创建文件
            val fileUri = DocumentsContract.createDocument(
                context.contentResolver,
                dirUri,
                "text/csv",
                fileName
            ) ?: return@withContext null

            // 写入数据
            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)

                // 写入表头
                writer.write("id,name,type,icon,color,isCustom,sortOrder\n")

                // 写入数据行
                categories.forEach { category ->
                    writer.write("${category.id},${category.name},${category.type},${category.icon},${category.color},${category.isCustom},${category.sortOrder}\n")
                }

                writer.flush()
            }

            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 导出账户数据到CSV
     */
    private suspend fun exportAccountToCsv(context: Context, accounts: List<Account>, dirUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val fileName = "accounts_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".csv"

        try {
            // 创建文件
            val fileUri = DocumentsContract.createDocument(
                context.contentResolver,
                dirUri,
                "text/csv",
                fileName
            ) ?: return@withContext null

            // 写入数据
            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)

                // 写入表头
                writer.write("id,name,type,balance,icon,color\n")

                // 写入数据行
                accounts.forEach { account ->
                    writer.write("${account.id},${account.name},${account.type},${account.balance},${account.icon},${account.color}\n")
                }

                writer.flush()
            }

            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 导出预算数据到CSV
     */
    private suspend fun exportBudgetToCsv(context: Context, budgets: List<Budget>, dirUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val fileName = "budgets_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".csv"

        try {
            // 创建文件
            val fileUri = DocumentsContract.createDocument(
                context.contentResolver,
                dirUri,
                "text/csv",
                fileName
            ) ?: return@withContext null

            // 写入数据
            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                // 写入表头
                writer.write("id,name,amount,startDate,endDate,period,categories,isActive,notifyEnabled,notifyThreshold\n")

                // 写入数据行
                budgets.forEach { budget ->
                    val categoriesStr = budget.categories.joinToString(";")
                    writer.write("${budget.id},${budget.name},${budget.amount},${dateFormat.format(budget.startDate)},${dateFormat.format(budget.endDate)},${budget.period},\"${categoriesStr}\",${budget.isActive},${budget.notifyEnabled},${budget.notifyThreshold}\n")
                }

                writer.flush()
            }

            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 导出交易数据到CSV
     */
    private suspend fun exportTransactionToCsv(context: Context, transactions: List<Transaction>, dirUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val fileName = "transactions_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".csv"

        try {
            // 创建文件
            val fileUri = DocumentsContract.createDocument(
                context.contentResolver,
                dirUri,
                "text/csv",
                fileName
            ) ?: return@withContext null

            // 写入数据
            context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                val writer = OutputStreamWriter(outputStream)
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                // 写入表头
                writer.write("id,amount,isIncome,categoryId,accountId,date,note\n")

                // 写入数据行
                transactions.forEach { transaction ->
                    writer.write("${transaction.id},${transaction.amount},${transaction.isIncome},${transaction.categoryId},${transaction.accountId},${dateFormat.format(transaction.date)},${transaction.note}\n")
                }

                writer.flush()
            }

            fileUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 生成备份文件名
     */
    fun generateBackupFileName(extension: String): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        return "ccjizhang_backup_${dateFormat.format(Date())}.${extension}"
    }

    /**
     * 导出数据到JSON文件
     * 主要用于自动备份功能
     */
    suspend fun exportDataToJsonFile(context: Context, file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val startTime = System.currentTimeMillis()
            Timber.i("开始导出数据到JSON文件: ${file.absolutePath}")

            // 获取所有数据
            val categories = try {
                categoryDao.getAllCategoriesSync().also {
                    Timber.d("获取分类数据成功，数量: ${it.size}")
                }
            } catch (e: Exception) {
                Timber.e(e, "获取分类数据失败")
                return@withContext Result.failure(Exception("获取分类数据失败: ${e.message}", e))
            }

            val accounts = try {
                accountDao.getAllAccountsSync().also {
                    Timber.d("获取账户数据成功，数量: ${it.size}")
                }
            } catch (e: Exception) {
                Timber.e(e, "获取账户数据失败")
                return@withContext Result.failure(Exception("获取账户数据失败: ${e.message}", e))
            }

            val transactions = try {
                transactionDao.getAllTransactionsSync().also {
                    Timber.d("获取交易数据成功，数量: ${it.size}")
                }
            } catch (e: Exception) {
                Timber.e(e, "获取交易数据失败")
                return@withContext Result.failure(Exception("获取交易数据失败: ${e.message}", e))
            }

            val budgets = try {
                budgetDao.getAllBudgets().first().also {
                    Timber.d("获取预算数据成功，数量: ${it.size}")
                }
            } catch (e: Exception) {
                Timber.e(e, "获取预算数据失败")
                return@withContext Result.failure(Exception("获取预算数据失败: ${e.message}", e))
            }

            // 创建JSON对象
            val jsonCreationStartTime = System.currentTimeMillis()
            Timber.d("开始创建JSON对象")
            val jsonObject = try {
                JSONObject().apply {
                    // 使用JSONArray处理数组数据
                    put("categories", JSONArray(Gson().toJson(categories)))
                    put("accounts", JSONArray(Gson().toJson(accounts)))
                    put("transactions", JSONArray(Gson().toJson(transactions)))
                    put("budgets", JSONArray(Gson().toJson(budgets)))
                    put("metadata", JSONObject().apply {
                        put("exportTime", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                        put("version", "1.0")
                    })
                }
            } catch (e: Exception) {
                Timber.e(e, "创建JSON对象失败")
                return@withContext Result.failure(Exception("创建JSON对象失败: ${e.message}", e))
            }

            val jsonCreationTime = System.currentTimeMillis() - jsonCreationStartTime
            Timber.d("成功创建JSON对象，耗时: ${jsonCreationTime}ms")

            // 写入文件
            val writeStartTime = System.currentTimeMillis()
            Timber.d("开始写入文件: ${file.absolutePath}")
            try {
                // 确保父目录存在
                val parentDir = file.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    Timber.d("父目录不存在，尝试创建: ${parentDir.absolutePath}")
                    val dirCreated = parentDir.mkdirs()
                    if (!dirCreated) {
                        Timber.e("创建目录失败: ${parentDir.absolutePath}")
                        return@withContext Result.failure(Exception("创建目录失败: ${parentDir.absolutePath}"))
                    }
                    Timber.d("父目录创建成功: ${parentDir.absolutePath}")
                }

                // 写入文件
                FileWriter(file).use { writer ->
                    val jsonString = jsonObject.toString(2) // 美化JSON输出
                    writer.write(jsonString)
                    writer.flush()
                    val writeTime = System.currentTimeMillis() - writeStartTime
                    Timber.d("成功写入JSON数据到文件，大小: ${jsonString.length} 字节，耗时: ${writeTime}ms")
                }

                val totalTime = System.currentTimeMillis() - startTime
                Timber.i("数据导出成功，文件路径: ${file.absolutePath}，总耗时: ${totalTime}ms")
                Result.success(Unit)
            } catch (e: Exception) {
                Timber.e(e, "写入文件失败: ${file.absolutePath}，异常类型: ${e.javaClass.simpleName}")
                return@withContext Result.failure(Exception("写入文件失败: ${e.message}", e))
            }
        } catch (e: Exception) {
            Timber.e(e, "导出数据到JSON文件失败，异常类型: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    /**
     * 从文件导入JSON数据
     * 主要用于从备份文件恢复
     */
    suspend fun importDataFromJsonFile(context: Context, file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        return@withContext importDataFromJsonFileInternal(context, file)
    }

    /**
     * 从URI导入JSON数据
     * 与importDataFromJson功能相同，提供兼容性
     */
    suspend fun importDataFromJsonUri(context: Context, uri: Uri): Result<Unit> {
        return importDataFromJson(context, uri)
    }

    /**
     * 从文件导入JSON数据的内部实现
     */
    private suspend fun importDataFromJsonFileInternal(context: Context, file: File): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || !file.canRead()) {
                return@withContext Result.failure(Exception("无法读取文件"))
            }

            // 读取JSON文件
            val jsonContent = file.readText()
            val jsonObject = JSONObject(jsonContent)

            // 首先解析所有数据
            val categories = if (jsonObject.has("categories")) {
                val categoriesJson = jsonObject.getString("categories")
                val categoriesType = object : TypeToken<List<Category>>() {}.type
                Gson().fromJson<List<Category>>(categoriesJson, categoriesType)
            } else null

            val accounts = if (jsonObject.has("accounts")) {
                val accountsJson = jsonObject.getString("accounts")
                val accountsType = object : TypeToken<List<Account>>() {}.type
                Gson().fromJson<List<Account>>(accountsJson, accountsType)
            } else null

            val budgets = if (jsonObject.has("budgets")) {
                val budgetsJson = jsonObject.getString("budgets")
                val budgetsType = object : TypeToken<List<Budget>>() {}.type
                Gson().fromJson<List<Budget>>(budgetsJson, budgetsType)
            } else null

            val transactions = if (jsonObject.has("transactions")) {
                val transactionsJson = jsonObject.getString("transactions")
                val transactionsType = object : TypeToken<List<Transaction>>() {}.type
                Gson().fromJson<List<Transaction>>(transactionsJson, transactionsType)
            } else null

            // 执行导入操作
            // 导入分类
            categories?.let {
                categoryDao.insertAll(it)
            }

            // 导入账户
            accounts?.let {
                accountDao.deleteAllAccounts() // 先清除现有数据
                accountDao.insertAll(it)
            }

            // 导入预算
            budgets?.let {
                budgetDao.deleteAllBudgets() // 先清除现有数据
                budgetDao.insertAll(it)
            }

            // 导入交易
            transactions?.let {
                transactionDao.deleteAllTransactions() // 先清除现有数据
                transactionDao.insertAll(it)
            }

            Result.success(true)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 选择性清理数据
     * @param clearTransactions 是否清除交易记录
     * @param clearCategories 是否清除自定义分类
     * @param clearAccounts 是否清除账户
     * @param clearBudgets 是否清除预算
     * @param beforeDate 清除此日期之前的交易记录（如果为null则清除所有）
     * @return 清理结果
     */
    suspend fun cleanUpData(
        clearTransactions: Boolean,
        clearCategories: Boolean,
        clearAccounts: Boolean,
        clearBudgets: Boolean,
        beforeDate: Date? = null
    ): Result<CleanupStats> = withContext(Dispatchers.IO) {
        try {
            val stats = CleanupStats()

            // 不使用事务，依次执行各操作
            // 清理交易记录
            if (clearTransactions) {
                if (beforeDate != null) {
                    val deletedCount = transactionDao.deleteTransactionsBeforeDate(beforeDate)
                    stats.transactionsDeleted = deletedCount
                } else {
                    val deletedCount = transactionDao.deleteAllTransactions()
                    stats.transactionsDeleted = deletedCount
                }
            }

            // 清理自定义分类（只删除用户自定义的分类）
            if (clearCategories) {
                val deletedCount = categoryDao.deleteCustomCategories()
                stats.categoriesDeleted = deletedCount
            }

            // 清理账户
            if (clearAccounts) {
                val deletedCount = accountDao.deleteAllAccounts()
                stats.accountsDeleted = deletedCount
            }

            // 清理预算
            if (clearBudgets) {
                val deletedCount = budgetDao.deleteAllBudgets()
                stats.budgetsDeleted = deletedCount
            }

            Result.success(stats)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 验证备份文件
     * @param context 上下文
     * @param file 备份文件
     * @return 验证结果
     */
    suspend fun validateBackupFile(context: Context, file: File): Result<ImportValidationResult> = withContext(Dispatchers.IO) {
        try {
            // 读取文件内容
            val jsonContent = file.readText()
            return@withContext validateJsonContent(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }

    /**
     * 验证导入的数据
     * @param context 上下文
     * @param uri 数据文件的URI
     * @return 验证结果
     */
    suspend fun validateImportData(context: Context, uri: Uri): Result<ImportValidationResult> = withContext(Dispatchers.IO) {
        try {
            // 解析JSON文件
            val jsonContent = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?:
                return@withContext Result.failure(Exception("无法读取文件"))

            return@withContext validateJsonContent(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext Result.failure(e)
        }
    }

    /**
     * 验证JSON内容
     * @param jsonContent JSON内容
     * @return 验证结果
     */
    private suspend fun validateJsonContent(jsonContent: String): Result<ImportValidationResult> = withContext(Dispatchers.IO) {
        try {
            val jsonObject = JSONObject(jsonContent)
            val result = ImportValidationResult()

            // 验证数据结构
            if (jsonObject.has("categories")) {
                val categoriesJson = jsonObject.getJSONArray("categories").toString()
                val categoriesType = object : TypeToken<List<Category>>() {}.type
                result.categories = Gson().fromJson<List<Category>>(categoriesJson, categoriesType)
            }

            if (jsonObject.has("accounts")) {
                val accountsJson = jsonObject.getJSONArray("accounts").toString()
                val accountsType = object : TypeToken<List<Account>>() {}.type
                result.accounts = Gson().fromJson<List<Account>>(accountsJson, accountsType)
            }

            if (jsonObject.has("budgets")) {
                val budgetsJson = jsonObject.getJSONArray("budgets").toString()
                val budgetsType = object : TypeToken<List<Budget>>() {}.type
                result.budgets = Gson().fromJson<List<Budget>>(budgetsJson, budgetsType)
            }

            if (jsonObject.has("transactions")) {
                val transactionsJson = jsonObject.getJSONArray("transactions").toString()
                val transactionsType = object : TypeToken<List<Transaction>>() {}.type
                result.transactions = Gson().fromJson<List<Transaction>>(transactionsJson, transactionsType)
            }

            // 检查是否有元数据
            if (jsonObject.has("metadata")) {
                val metadata = jsonObject.getJSONObject("metadata")
                if (metadata.has("exportTime")) {
                    result.exportTime = metadata.getString("exportTime")
                }
                if (metadata.has("version")) {
                    result.version = metadata.getString("version")
                }
            }

            // 检查数据一致性
            result.hasConsistencyIssues = checkDataConsistency(result)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(Exception("数据格式无效: ${e.message}"))
        }
    }

    /**
     * 检查导入数据的一致性
     */
    private suspend fun checkDataConsistency(data: ImportValidationResult): Boolean = withContext(Dispatchers.IO) {
        var hasIssues = false

        // 检查交易引用的分类和账户是否存在
        data.transactions?.forEach { transaction ->
            val categoryExists = data.categories?.any { it.id == transaction.categoryId } ?: false
            val accountExists = data.accounts?.any { it.id == transaction.accountId } ?: false

            if (!categoryExists || !accountExists) {
                hasIssues = true
                return@forEach
            }
        }

        // 检查预算引用的分类是否存在
        data.budgets?.forEach { budget ->
            budget.categories.forEach { categoryId ->
                val categoryExists = data.categories?.any { it.id == categoryId } ?: false
                if (!categoryExists) {
                    hasIssues = true
                    return@forEach
                }
            }
        }

        hasIssues
    }

    /**
     * 数据清理统计结果
     */
    data class CleanupStats(
        var transactionsDeleted: Int = 0,
        var categoriesDeleted: Int = 0,
        var accountsDeleted: Int = 0,
        var budgetsDeleted: Int = 0
    )

    /**
     * 导入数据验证结果
     */
    data class ImportValidationResult(
        var categories: List<Category>? = null,
        var accounts: List<Account>? = null,
        var transactions: List<Transaction>? = null,
        var budgets: List<Budget>? = null,
        var exportTime: String? = null,
        var version: String? = null,
        var hasConsistencyIssues: Boolean = false
    ) {
        val categoryCount: Int get() = categories?.size ?: 0
        val accountCount: Int get() = accounts?.size ?: 0
        val transactionCount: Int get() = transactions?.size ?: 0
        val budgetCount: Int get() = budgets?.size ?: 0
        val isValid: Boolean get() = categoryCount > 0 || accountCount > 0 || transactionCount > 0 || budgetCount > 0
    }
}