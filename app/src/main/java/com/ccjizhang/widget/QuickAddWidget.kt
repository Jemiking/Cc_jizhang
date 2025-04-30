package com.ccjizhang.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ccjizhang.MainActivity
import com.ccjizhang.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import com.ccjizhang.data.repository.AccountRepository

/**
 * 桌面快速添加交易的小部件
 */
@AndroidEntryPoint
class QuickAddWidget : AppWidgetProvider() {

    @Inject
    lateinit var accountRepository: AccountRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 为每个小部件实例更新UI
        appWidgetIds.forEach { appWidgetId ->
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // 当添加第一个小部件实例时调用
    }

    override fun onDisabled(context: Context) {
        // 当删除最后一个小部件实例时调用
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        // 处理自定义广播操作
        when (intent.action) {
            ACTION_ADD_EXPENSE -> {
                // 打开添加支出的界面
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_OPEN_ADD_TRANSACTION, true)
                    putExtra(EXTRA_TRANSACTION_TYPE, "expense")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(mainIntent)
            }
            ACTION_ADD_INCOME -> {
                // 打开添加收入的界面
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra(EXTRA_OPEN_ADD_TRANSACTION, true)
                    putExtra(EXTRA_TRANSACTION_TYPE, "income")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(mainIntent)
            }
            ACTION_REFRESH -> {
                // 刷新小部件显示的账户余额
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    intent.component
                )
                onUpdate(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        // 创建RemoteViews对象
        val views = RemoteViews(context.packageName, R.layout.widget_quick_add)

        // 设置点击事件
        setOnClickListeners(context, views)

        // 异步加载账户总余额
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val totalBalance = accountRepository.getTotalBalance().first() ?: 0.0
                
                // 在主线程中更新UI
                views.setTextViewText(
                    R.id.tv_account_balance,
                    "总余额: ¥%.2f".format(totalBalance)
                )
                
                // 更新小部件
                appWidgetManager.updateAppWidget(appWidgetId, views)
            } catch (e: Exception) {
                // 处理错误
                views.setTextViewText(R.id.tv_account_balance, "总余额: ¥--.--")
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

    private fun setOnClickListeners(context: Context, views: RemoteViews) {
        // 添加支出按钮
        val expenseIntent = Intent(context, QuickAddWidget::class.java).apply {
            action = ACTION_ADD_EXPENSE
        }
        val expensePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            expenseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_expense, expensePendingIntent)

        // 添加收入按钮
        val incomeIntent = Intent(context, QuickAddWidget::class.java).apply {
            action = ACTION_ADD_INCOME
        }
        val incomePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            incomeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_income, incomePendingIntent)

        // 小部件标题点击打开APP
        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            2,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_title, openAppPendingIntent)
    }

    companion object {
        const val ACTION_ADD_EXPENSE = "com.ccjizhang.widget.ACTION_ADD_EXPENSE"
        const val ACTION_ADD_INCOME = "com.ccjizhang.widget.ACTION_ADD_INCOME"
        const val ACTION_REFRESH = "com.ccjizhang.widget.ACTION_REFRESH"
        
        const val EXTRA_OPEN_ADD_TRANSACTION = "open_add_transaction"
        const val EXTRA_TRANSACTION_TYPE = "transaction_type"
    }
} 