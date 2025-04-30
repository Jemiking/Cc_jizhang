package com.ccjizhang.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ccjizhang.MainActivity
import com.ccjizhang.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通知管理工具类
 */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // 通知渠道ID
    companion object {
        // 通知渠道
        const val CHANNEL_ID_BUDGET = "budget_notification"
        const val CHANNEL_ID_TRANSACTION_REMINDER = "transaction_reminder"
        const val CHANNEL_ID_SYSTEM = "system_notification"
        const val CHANNEL_ID_CREDIT_CARD = "credit_card_notification"

        // 通知ID
        const val NOTIFICATION_ID_BUDGET_WARNING = 1001
        const val NOTIFICATION_ID_BUDGET_EXCEEDED = 1002
        const val NOTIFICATION_ID_TRANSACTION_REMINDER = 2001
        const val NOTIFICATION_ID_BACKUP_SUCCESS = 3001
        const val NOTIFICATION_ID_BACKUP_FAILURE = 3002
        const val NOTIFICATION_ID_BACKUP_REMINDER = 3003
        const val NOTIFICATION_ID_CREDIT_CARD_PAYMENT = 4001

        // 通知动作
        const val ACTION_OPEN_BUDGET_SCREEN = "com.ccjizhang.action.OPEN_BUDGET_SCREEN"
        const val ACTION_OPEN_ADD_TRANSACTION = "com.ccjizhang.action.OPEN_ADD_TRANSACTION"
        const val ACTION_OPEN_SETTINGS = "com.ccjizhang.action.OPEN_SETTINGS"
        const val ACTION_OPEN_ACCOUNTS = "com.ccjizhang.action.OPEN_ACCOUNTS"
    }

    init {
        createNotificationChannels()
    }

    /**
     * 创建通知渠道（Android 8.0及以上版本需要）
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // 预算通知渠道
            val budgetChannel = NotificationChannel(
                CHANNEL_ID_BUDGET,
                "预算提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "预算警告和超支提醒"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
            }

            // 交易提醒通知渠道
            val transactionChannel = NotificationChannel(
                CHANNEL_ID_TRANSACTION_REMINDER,
                "交易提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "定期交易和还款提醒"
                enableLights(true)
                lightColor = Color.BLUE
            }

            // 系统通知渠道
            val systemChannel = NotificationChannel(
                CHANNEL_ID_SYSTEM,
                "系统通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "备份、同步等系统级通知"
            }

            // 信用卡通知渠道
            val creditCardChannel = NotificationChannel(
                CHANNEL_ID_CREDIT_CARD,
                "信用卡提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "信用卡还款日和额度提醒"
                enableLights(true)
                lightColor = Color.YELLOW
                enableVibration(true)
            }

            // 注册通知渠道
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannels(listOf(budgetChannel, transactionChannel, systemChannel, creditCardChannel))
        }
    }

    /**
     * 显示预算警告通知（接近预算上限）
     */
    fun showBudgetWarningNotification(budgetName: String, usedPercentage: Int, remainingAmount: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_BUDGET_SCREEN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle("预算提醒")
            .setContentText("\"$budgetName\"预算已使用 $usedPercentage%, 剩余 ¥%.2f".format(remainingAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BUDGET_WARNING, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 显示预算超支通知
     */
    fun showBudgetExceededNotification(budgetName: String, exceededAmount: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_BUDGET_SCREEN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle("预算超支警告")
            .setContentText("\"$budgetName\"预算已超支 ¥%.2f".format(exceededAmount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(Color.RED)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BUDGET_EXCEEDED, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 显示定期交易提醒通知
     */
    fun showTransactionReminderNotification(title: String, description: String, amount: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ADD_TRANSACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSACTION_REMINDER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText("$description - ¥%.2f".format(amount))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_TRANSACTION_REMINDER, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 显示信用卡还款提醒通知
     */
    fun showCreditCardPaymentNotification(cardName: String, dueDate: String, amount: Double) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ACCOUNTS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 添加还款按钮
        val paymentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ADD_TRANSACTION
            putExtra("TRANSACTION_TYPE", "EXPENSE")
            putExtra("TRANSACTION_AMOUNT", amount)
            putExtra("TRANSACTION_DESCRIPTION", "$cardName 信用卡还款")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val paymentPendingIntent = PendingIntent.getActivity(
            context, 1, paymentIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_CREDIT_CARD)
            .setSmallIcon(R.drawable.ic_credit_card)
            .setContentTitle("信用卡还款提醒")
            .setContentText("$cardName 还款日：$dueDate，需还款：¥%.2f".format(amount))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_payment, "立即还款",
                paymentPendingIntent
            )
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_CREDIT_CARD_PAYMENT, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 显示备份成功通知
     */
    fun showBackupSuccessNotification(destination: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
            .setSmallIcon(R.drawable.ic_backup)
            .setContentTitle("备份成功")
            .setContentText("数据已成功备份至 $destination")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BACKUP_SUCCESS, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 显示备份失败通知
     */
    fun showBackupFailureNotification(error: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle("备份失败")
            .setContentText("备份失败：$error")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BACKUP_FAILURE, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 取消指定ID的通知
     */
    fun cancelNotification(notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    /**
     * 显示备份提醒通知
     */
    fun showBackupReminderNotification(title: String, message: String, intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
            .setSmallIcon(R.drawable.ic_backup)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BACKUP_REMINDER, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 显示数据恢复完成通知
     */
    fun showRestoreCompletedNotification(title: String, message: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_SETTINGS
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SYSTEM)
            .setSmallIcon(R.drawable.ic_backup)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        with(NotificationManagerCompat.from(context)) {
            try {
                notify(NOTIFICATION_ID_BACKUP_SUCCESS, notification)
            } catch (e: SecurityException) {
                // 用户可能没有授予通知权限
            }
        }
    }

    /**
     * 取消所有通知
     */
    fun cancelAllNotifications() {
        NotificationManagerCompat.from(context).cancelAll()
    }
}