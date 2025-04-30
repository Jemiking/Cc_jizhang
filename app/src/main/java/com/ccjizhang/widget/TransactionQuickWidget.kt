package com.ccjizhang.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.ccjizhang.MainActivity
import com.ccjizhang.R

/**
 * 快速交易记录小部件
 * 允许用户在桌面上快速添加交易记录
 */
class TransactionQuickWidget : AppWidgetProvider() {

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

    /**
     * 更新单个小部件
     */
    private fun updateAppWidget(
        context: Context, 
        appWidgetManager: AppWidgetManager, 
        appWidgetId: Int
    ) {
        // 构建小部件视图
        val views = RemoteViews(context.packageName, R.layout.widget_transaction_quick)
        
        // 设置添加支出按钮点击事件
        val intentExpense = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADD_EXPENSE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // 添加独特的URI，确保PendingIntent是唯一的
            data = Uri.parse("widget:$appWidgetId:expense")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentExpense = PendingIntent.getActivity(
            context, 0, intentExpense, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_expense, pendingIntentExpense)
        
        // 设置添加收入按钮点击事件
        val intentIncome = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADD_INCOME
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // 添加独特的URI，确保PendingIntent是唯一的
            data = Uri.parse("widget:$appWidgetId:income")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentIncome = PendingIntent.getActivity(
            context, 1, intentIncome, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_income, pendingIntentIncome)
        
        // 设置转账按钮点击事件
        val intentTransfer = Intent(context, MainActivity::class.java).apply {
            action = ACTION_ADD_TRANSFER
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            // 添加独特的URI，确保PendingIntent是唯一的
            data = Uri.parse("widget:$appWidgetId:transfer")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentTransfer = PendingIntent.getActivity(
            context, 2, intentTransfer, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.btn_add_transfer, pendingIntentTransfer)
        
        // 更新小部件
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
    
    companion object {
        const val ACTION_ADD_EXPENSE = "com.ccjizhang.action.ADD_EXPENSE"
        const val ACTION_ADD_INCOME = "com.ccjizhang.action.ADD_INCOME"
        const val ACTION_ADD_TRANSFER = "com.ccjizhang.action.ADD_TRANSFER"
    }
} 