package com.ccjizhang.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.ccjizhang.MainActivity
import com.ccjizhang.R
import com.ccjizhang.data.model.Transaction
import com.ccjizhang.data.repository.TransactionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

/**
 * 最近交易记录小部件
 */
@AndroidEntryPoint
class RecentTransactionsWidget : AppWidgetProvider() {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    companion object {
        const val ACTION_REFRESH = "com.ccjizhang.widget.ACTION_REFRESH_RECENT"
        const val ACTION_ADD_EXPENSE = "com.ccjizhang.widget.ACTION_ADD_EXPENSE_RECENT"
        const val ACTION_ADD_INCOME = "com.ccjizhang.widget.ACTION_ADD_INCOME_RECENT"
        const val ACTION_OPEN_APP = "com.ccjizhang.widget.ACTION_OPEN_APP_RECENT"
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
        
        // 手动刷新最近交易小部件
        fun refreshWidget(context: Context) {
            val intent = Intent(context, RecentTransactionsWidget::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 更新所有的小部件实例
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            ACTION_REFRESH -> {
                // 刷新所有小部件
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    ComponentName(context, RecentTransactionsWidget::class.java)
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
            ACTION_ADD_EXPENSE -> {
                // 打开添加支出的界面
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_TRANSACTION_TYPE, "expense")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(mainIntent)
            }
            ACTION_ADD_INCOME -> {
                // 打开添加收入的界面
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_TRANSACTION_TYPE, "income")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(mainIntent)
            }
            ACTION_OPEN_APP -> {
                // 打开应用程序主界面
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(mainIntent)
            }
        }
    }

    fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 创建RemoteViews对象
        val views = RemoteViews(context.packageName, R.layout.widget_recent_transactions)

        // 设置点击事件
        setOnClickListeners(context, views)

        // 首先显示加载状态
        views.setViewVisibility(R.id.tv_empty, View.VISIBLE)
        views.setTextViewText(R.id.tv_empty, "正在加载...")

        // 隐藏所有交易项目
        for (i in 1..3) {
            views.setViewVisibility(context.resources.getIdentifier("transaction_item_$i", "id", context.packageName), View.GONE)
        }

        // 更新小部件初始状态
        appWidgetManager.updateAppWidget(appWidgetId, views)

        // 异步加载最近交易记录
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 获取最近3条交易记录
                val recentTransactions = transactionRepository.getRecentTransactions(3).first()

                // 在UI线程中更新小部件
                if (recentTransactions.isEmpty()) {
                    // 没有交易记录
                    views.setViewVisibility(R.id.tv_empty, View.VISIBLE)
                    views.setTextViewText(R.id.tv_empty, "暂无交易记录")
                } else {
                    // 有交易记录，隐藏空视图
                    views.setViewVisibility(R.id.tv_empty, View.GONE)

                    // 显示每条交易记录
                    updateTransactionItems(views, recentTransactions, context)
                }

                // 更新小部件
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                // 处理错误
                views.setViewVisibility(R.id.tv_empty, View.VISIBLE)
                views.setTextViewText(R.id.tv_empty, "加载失败")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun updateTransactionItems(views: RemoteViews, transactions: List<Transaction>, context: Context) {
        // 显示每条交易记录
        transactions.forEachIndexed { index, transaction ->
            if (index < 3) { // 最多显示3条
                val itemId = context.resources.getIdentifier("transaction_item_${index + 1}", "id", context.packageName)
                val categoryId = context.resources.getIdentifier("tv_category_${index + 1}", "id", context.packageName)
                val amountId = context.resources.getIdentifier("tv_amount_${index + 1}", "id", context.packageName)

                // 显示交易项目
                views.setViewVisibility(itemId, View.VISIBLE)

                // 设置分类名称，修复categoryName访问问题
                views.setTextViewText(categoryId, getCategoryNameFromTransaction(transaction))

                // 设置金额，根据类型使用不同的格式
                val formattedAmount = getFormattedAmount(transaction)
                views.setTextViewText(amountId, formattedAmount)

                // 设置金额颜色
                val textColorResId = getAmountColorResId(transaction)
                views.setTextColor(amountId, context.resources.getColor(textColorResId, null))

                // 设置点击事件，打开交易详情
                val openDetailIntent = Intent(context, MainActivity::class.java).apply {
                    action = "com.ccjizhang.action.VIEW_TRANSACTION"
                    putExtra("transaction_id", transaction.id)
                    data = Uri.parse("transaction://${transaction.id}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pendingIntent = PendingIntent.getActivity(
                    context, transaction.id.toInt(), openDetailIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(itemId, pendingIntent)
            }
        }
    }

    // 获取交易记录的分类名称
    private fun getCategoryNameFromTransaction(transaction: Transaction): String {
        // Transaction 类中没有 categoryName 属性，只能返回一个默认值
        // 在实际应用中，可能需要通过 categoryId 查询分类名称
        return "记账"
    }

    // 获取格式化的金额字符串
    private fun getFormattedAmount(transaction: Transaction): String {
        return when {
            transaction.isIncome -> "+¥%.2f".format(transaction.amount)
            else -> "-¥%.2f".format(transaction.amount)
        }
    }

    // 获取金额颜色资源ID
    private fun getAmountColorResId(transaction: Transaction): Int {
        return when {
            transaction.isIncome -> android.R.color.holo_green_light
            else -> android.R.color.holo_red_light
        }
    }

    private fun setOnClickListeners(context: Context, views: RemoteViews) {
        // 刷新按钮
        val refreshIntent = Intent(context, RecentTransactionsWidget::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context, 0, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_refresh, refreshPendingIntent)

        // 添加支出按钮
        val expenseIntent = Intent(context, RecentTransactionsWidget::class.java).apply {
            action = ACTION_ADD_EXPENSE
        }
        val expensePendingIntent = PendingIntent.getBroadcast(
            context, 1, expenseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_expense, expensePendingIntent)

        // 添加收入按钮
        val incomeIntent = Intent(context, RecentTransactionsWidget::class.java).apply {
            action = ACTION_ADD_INCOME
        }
        val incomePendingIntent = PendingIntent.getBroadcast(
            context, 2, incomeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_income, incomePendingIntent)

        // 更多按钮点击打开APP
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context, 3, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_more, openAppPendingIntent)

        // 标题点击打开APP
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
    }
} 