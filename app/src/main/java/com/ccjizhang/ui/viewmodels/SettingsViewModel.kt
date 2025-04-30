package com.ccjizhang.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ccjizhang.data.repository.DataExportImportRepository
import com.ccjizhang.data.repository.UserPreferencesRepository
import com.ccjizhang.utils.DatabaseRepairTool
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * 设置页面状态数据类
 */
data class SettingsState(
    val isDarkModeEnabled: Boolean = false,
    val themeMode: String = "system",
    val isLoading: Boolean = false,
    val appVersion: String = "",
    val appBuildNumber: String = "",
    val databaseVersion: Int = 0
)

/**
 * 设置项数据类
 */
data class SettingItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val icon: String,
    val category: String,
    val route: String?,
    val action: (() -> Unit)? = null
)

/**
 * 设置页面的ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dataExportImportRepository: DataExportImportRepository,
    private val databaseRepairTool: DatabaseRepairTool,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // 设置状态
    private val _settingsState = MutableStateFlow(SettingsState())
    val settingsState: StateFlow<SettingsState> = _settingsState.asStateFlow()

    // 操作结果
    private val _operationResult = MutableSharedFlow<String>()
    val operationResult: SharedFlow<String> = _operationResult.asSharedFlow()

    // 所有设置项列表
    private val allSettingItems = mutableListOf<SettingItem>()

    // 设置项缓存，按类别分组
    private val settingItemsCache = mutableMapOf<String, List<SettingItem>>()

    // 设置项是否已初始化
    private var isSettingItemsInitialized = false

    init {
        loadSettings()
        loadAppInfo()

        // 在后台线程中初始化设置项
        viewModelScope.launch(Dispatchers.IO) {
            initializeSettingItems()
        }
    }

    /**
     * 加载设置
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }

                // 加载深色模式设置
                val darkModeEnabled = userPreferencesRepository.getDarkModeEnabled()
                val themeMode = userPreferencesRepository.getThemeMode()

                _settingsState.update {
                    it.copy(
                        isDarkModeEnabled = darkModeEnabled,
                        themeMode = themeMode,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "加载设置失败")
                _settingsState.update { it.copy(isLoading = false) }
                _operationResult.emit("加载设置失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 加载应用信息
     */
    private fun loadAppInfo() {
        viewModelScope.launch {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val versionName = packageInfo.versionName
                val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode.toString()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toString()
                }

                _settingsState.update {
                    it.copy(
                        appVersion = versionName,
                        appBuildNumber = versionCode
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "加载应用信息失败")
            }
        }
    }

    /**
     * 切换深色模式
     */
    fun toggleDarkMode() {
        viewModelScope.launch {
            try {
                val currentValue = _settingsState.value.isDarkModeEnabled
                userPreferencesRepository.setDarkModeEnabled(!currentValue)

                // 更新主题模式
                val newThemeMode = if (!currentValue) "dark" else "light"
                userPreferencesRepository.setThemeMode(newThemeMode)

                _settingsState.update {
                    it.copy(
                        isDarkModeEnabled = !currentValue,
                        themeMode = newThemeMode
                    )
                }

                _operationResult.emit("主题已切换")
            } catch (e: Exception) {
                Timber.e(e, "切换深色模式失败")
                _operationResult.emit("切换主题失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 设置主题模式
     */
    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setThemeMode(mode)

                // 如果设置为深色或浅色模式，同时更新深色模式开关
                val darkModeEnabled = mode == "dark"
                if (mode != "system") {
                    userPreferencesRepository.setDarkModeEnabled(darkModeEnabled)
                }

                _settingsState.update {
                    it.copy(
                        themeMode = mode,
                        isDarkModeEnabled = if (mode != "system") darkModeEnabled else it.isDarkModeEnabled
                    )
                }

                _operationResult.emit("主题模式已设置为: ${getThemeModeDisplayName(mode)}")
            } catch (e: Exception) {
                Timber.e(e, "设置主题模式失败")
                _operationResult.emit("设置主题模式失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 清除所有数据
     */
    fun clearAllData() {
        viewModelScope.launch {
            try {
                _settingsState.update { it.copy(isLoading = true) }

                // 清除所有数据
                val result = dataExportImportRepository.cleanUpData(
                    clearTransactions = true,
                    clearCategories = true,
                    clearAccounts = true,
                    clearBudgets = true
                )

                if (result.isSuccess) {
                    val stats = result.getOrNull()
                    val message = "已清除 ${stats?.transactionsDeleted ?: 0} 条交易记录, " +
                            "${stats?.categoriesDeleted ?: 0} 个分类, " +
                            "${stats?.accountsDeleted ?: 0} 个账户, " +
                            "${stats?.budgetsDeleted ?: 0} 个预算"

                    _operationResult.emit("数据清除成功\n$message")
                } else {
                    val error = result.exceptionOrNull()
                    _operationResult.emit("清除数据失败: ${error?.localizedMessage}")
                }

                _settingsState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "清除数据失败")
                _settingsState.update { it.copy(isLoading = false) }
                _operationResult.emit("清除数据失败: ${e.localizedMessage}")
            }
        }
    }

    /**
     * 获取主题模式显示名称
     */
    private fun getThemeModeDisplayName(mode: String): String {
        return when (mode) {
            "system" -> "跟随系统"
            "dark" -> "深色模式"
            "light" -> "浅色模式"
            else -> mode
        }
    }

    /**
     * 初始化设置项列表
     */
    private fun initializeSettingItems() {
        if (isSettingItemsInitialized) return

        // 临时列表，用于按类别分组
        val tempItems = mutableListOf<SettingItem>()

        // 数据管理类别
        tempItems.add(
            SettingItem(
                id = "category_management",
                title = "分类管理",
                subtitle = "管理收入和支出分类",
                icon = "Category",
                category = "数据管理",
                route = "category_management"
            )
        )

        tempItems.add(
            SettingItem(
                id = "tag_management",
                title = "标签管理",
                subtitle = "管理交易标签",
                icon = "Tag",
                category = "数据管理",
                route = "tag_management"
            )
        )

        tempItems.add(
            SettingItem(
                id = "account_management",
                title = "账户管理",
                subtitle = "管理您的账户和余额",
                icon = "AccountBalance",
                category = "数据管理",
                route = "account_management"
            )
        )

        tempItems.add(
            SettingItem(
                id = "data_backup",
                title = "数据备份与恢复",
                subtitle = "备份、恢复和自动备份设置",
                icon = "Backup",
                category = "数据管理",
                route = "data_backup"
            )
        )

        tempItems.add(
            SettingItem(
                id = "security_settings",
                title = "安全设置",
                subtitle = "数据库加密和密码管理",
                icon = "Security",
                category = "数据管理",
                route = "security_settings"
            )
        )

        tempItems.add(
            SettingItem(
                id = "clear_data",
                title = "清除数据",
                subtitle = "清除所有账单数据",
                icon = "Delete",
                category = "数据管理",
                route = null
            )
        )

        // 外观设置类别
        tempItems.add(
            SettingItem(
                id = "theme_settings",
                title = "主题设置",
                subtitle = "设置深色模式和应用主题颜色",
                icon = "Palette",
                category = "外观设置",
                route = "theme_settings"
            )
        )

        tempItems.add(
            SettingItem(
                id = "currency_settings",
                title = "货币设置",
                subtitle = "设置基准货币和汇率",
                icon = "Language",
                category = "外观设置",
                route = "currency_settings"
            )
        )

        tempItems.add(
            SettingItem(
                id = "notification_settings",
                title = "通知设置",
                subtitle = "管理应用通知和提醒",
                icon = "Notifications",
                category = "外观设置",
                route = "notification_settings"
            )
        )

        // 高级功能类别
        tempItems.add(
            SettingItem(
                id = "saving_goals",
                title = "目标储蓄计划",
                subtitle = "设定和跟踪您的储蓄目标",
                icon = "Star",
                category = "高级功能",
                route = "saving_goals"
            )
        )

        tempItems.add(
            SettingItem(
                id = "recurring_transactions",
                title = "定期交易自动化",
                subtitle = "设置周期性自动记账",
                icon = "AttachMoney",
                category = "高级功能",
                route = "recurring_transactions"
            )
        )

        tempItems.add(
            SettingItem(
                id = "family_members",
                title = "家庭共享记账",
                subtitle = "与家人共享账本和管理",
                icon = "Person",
                category = "高级功能",
                route = "family_members"
            )
        )

        tempItems.add(
            SettingItem(
                id = "investments",
                title = "理财产品跟踪",
                subtitle = "管理和分析您的投资产品",
                icon = "AccountBalance",
                category = "高级功能",
                route = "investments"
            )
        )

        tempItems.add(
            SettingItem(
                id = "financial_reports",
                title = "财务报告生成",
                subtitle = "生成详细的财务状况报告",
                icon = "ImportExport",
                category = "高级功能",
                route = "financial_reports"
            )
        )

        // 开发者工具类别
        tempItems.add(
            SettingItem(
                id = "nav_analytics_report",
                title = "导航分析报告",
                subtitle = "查看用户导航行为分析",
                icon = "Analytics",
                category = "开发者工具",
                route = "nav_analytics_report"
            )
        )

        tempItems.add(
            SettingItem(
                id = "nav_performance_report",
                title = "导航性能报告",
                subtitle = "查看导航性能指标和警告",
                icon = "Speed",
                category = "开发者工具",
                route = "nav_performance_report"
            )
        )

        tempItems.add(
            SettingItem(
                id = "log_viewer",
                title = "日志查看器",
                subtitle = "查看和分享应用日志",
                icon = "Article",
                category = "开发者工具",
                route = "log_viewer"
            )
        )

        // 关于类别
        tempItems.add(
            SettingItem(
                id = "app_info",
                title = "应用信息",
                subtitle = "查看应用版本和信息",
                icon = "Info",
                category = "关于",
                route = null
            )
        )

        tempItems.add(
            SettingItem(
                id = "feedback",
                title = "反馈",
                subtitle = "提供意见和反馈",
                icon = "Feedback",
                category = "关于",
                route = null
            )
        )

        // 将所有设置项添加到allSettingItems列表
        allSettingItems.addAll(tempItems)

        // 按类别分组并缓存
        val groupedItems = tempItems.groupBy { it.category }
        settingItemsCache.putAll(groupedItems)

        // 标记为已初始化
        isSettingItemsInitialized = true

        Timber.d("设置项初始化完成，共 ${allSettingItems.size} 项，${settingItemsCache.size} 个类别")
    }

    /**
     * 获取指定类别的设置项
     */
    fun getSettingItemsByCategory(category: String): List<SettingItem> {
        // 确保设置项已初始化
        if (!isSettingItemsInitialized) {
            initializeSettingItems()
        }

        // 从缓存中获取
        return settingItemsCache[category] ?: emptyList()
    }

    /**
     * 获取所有设置项类别
     */
    fun getAllCategories(): List<String> {
        // 确保设置项已初始化
        if (!isSettingItemsInitialized) {
            initializeSettingItems()
        }

        return settingItemsCache.keys.toList()
    }

    /**
     * 清除设置项缓存
     */
    fun clearSettingItemsCache() {
        settingItemsCache.clear()
        allSettingItems.clear()
        isSettingItemsInitialized = false

        // 重新初始化
        viewModelScope.launch(Dispatchers.IO) {
            initializeSettingItems()
        }
    }

    /**
     * 获取设置项通过ID
     */
    fun getSettingItemById(id: String): SettingItem? {
        return allSettingItems.find { it.id == id }
    }
}
