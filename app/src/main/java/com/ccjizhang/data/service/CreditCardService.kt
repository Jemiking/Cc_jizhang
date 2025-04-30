package com.ccjizhang.data.service

import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountType
import com.ccjizhang.data.model.BillingCycleType
import com.ccjizhang.data.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 信用卡服务
 * 处理信用卡相关的业务逻辑，包括账单周期、还款日计算等
 */
@Singleton
class CreditCardService @Inject constructor(
    private val accountRepository: AccountRepository
) {
    /**
     * 更新所有信用卡的账单和还款日期
     */
    suspend fun updateAllCreditCardDates() = withContext(Dispatchers.IO) {
        val accounts = accountRepository.getAllAccountsSync()
        accounts.filter { it.type == AccountType.CREDIT_CARD }.forEach { account ->
            updateNextBillingAndDueDate(account)
        }
    }
    
    /**
     * 更新指定信用卡的下一个账单日和还款日
     * @param account 信用卡账户
     */
    suspend fun updateNextBillingAndDueDate(account: Account) {
        if (account.type != AccountType.CREDIT_CARD) return
        
        val updatedAccount = account.copy(
            nextBillingDate = calculateNextBillingDate(account),
            nextDueDate = calculateNextDueDate(account)
        )
        
        accountRepository.updateAccount(updatedAccount)
    }
    
    /**
     * 计算下一个账单日期
     * @param account 信用卡账户
     * @return 下一个账单日期
     */
    private fun calculateNextBillingDate(account: Account): Date {
        val calendar = Calendar.getInstance()
        
        // 如果已经有下一个账单日期且该日期在未来，则直接返回
        account.nextBillingDate?.let { nextDate ->
            if (nextDate.after(Date())) {
                return nextDate
            }
        }
        
        // 获取当前月份的账单日
        calendar.set(Calendar.DAY_OF_MONTH, account.billingDay)
        
        // 如果设置的日期大于当月最大天数，则使用当月最后一天
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (account.billingDay > maxDay) {
            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
        }
        
        // 如果当前日期已经过了本月账单日，则设置为下个月的账单日
        if (calendar.time.before(Date())) {
            calendar.add(Calendar.MONTH, 1)
            
            // 检查下个月的天数
            val nextMonthMaxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            if (account.billingDay > nextMonthMaxDay) {
                calendar.set(Calendar.DAY_OF_MONTH, nextMonthMaxDay)
            } else {
                calendar.set(Calendar.DAY_OF_MONTH, account.billingDay)
            }
        }
        
        // 设置时间为当天的23:59:59
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        
        return calendar.time
    }
    
    /**
     * 计算下一个还款日期
     * @param account 信用卡账户
     * @return 下一个还款日期
     */
    private fun calculateNextDueDate(account: Account): Date {
        val calendar = Calendar.getInstance()
        
        // 如果已有下一个还款日期且该日期在未来，则直接返回
        account.nextDueDate?.let { nextDate ->
            if (nextDate.after(Date())) {
                return nextDate
            }
        }
        
        // 获取下一个账单日
        val nextBillingDate = account.nextBillingDate ?: calculateNextBillingDate(account)
        calendar.time = nextBillingDate
        
        // 设置为对应的还款日
        val dueDay = account.dueDay
        calendar.add(Calendar.MONTH, 1) // 通常还款日在下个月
        
        // 如果设置的日期大于该月最大天数，则使用当月最后一天
        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (dueDay > maxDay) {
            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
        } else {
            calendar.set(Calendar.DAY_OF_MONTH, dueDay)
        }
        
        // 设置时间为当天的23:59:59
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        
        return calendar.time
    }
    
    /**
     * 获取距离下一个账单日的天数
     * @param account 信用卡账户
     * @return 距离下一个账单日的天数
     */
    fun getDaysUntilNextBillingDate(account: Account): Int {
        if (account.type != AccountType.CREDIT_CARD) return -1
        
        val nextBillingDate = account.nextBillingDate ?: return -1
        return getDaysBetween(Date(), nextBillingDate)
    }
    
    /**
     * 获取距离下一个还款日的天数
     * @param account 信用卡账户
     * @return 距离下一个还款日的天数
     */
    fun getDaysUntilNextDueDate(account: Account): Int {
        if (account.type != AccountType.CREDIT_CARD) return -1
        
        val nextDueDate = account.nextDueDate ?: return -1
        return getDaysBetween(Date(), nextDueDate)
    }
    
    /**
     * 计算两个日期之间的天数
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数差
     */
    private fun getDaysBetween(startDate: Date, endDate: Date): Int {
        val millisecondsPerDay = 1000 * 60 * 60 * 24
        val diff = endDate.time - startDate.time
        return (diff / millisecondsPerDay).toInt()
    }
} 