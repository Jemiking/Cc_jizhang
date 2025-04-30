package com.ccjizhang.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 日期工具类
 * 
 * 提供Date和LocalDate之间的转换功能，以及日期格式化和解析功能
 */
object DateUtils {
    
    private val dateFormats = mutableMapOf<String, SimpleDateFormat>()
    
    /**
     * Date转LocalDate
     */
    fun Date.toLocalDate(): LocalDate = 
        this.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    
    /**
     * LocalDate转Date
     */
    fun LocalDate.toDate(): Date = 
        Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
    
    /**
     * Date格式化为字符串
     * 
     * @param pattern 日期格式，如"yyyy-MM-dd"
     * @return 格式化后的字符串
     */
    fun Date.format(pattern: String): String = 
        SimpleDateFormat(pattern, Locale.getDefault()).format(this)
    
    /**
     * LocalDate格式化为字符串
     * 
     * @param pattern 日期格式，如"yyyy-MM-dd"
     * @return 格式化后的字符串
     */
    fun LocalDate.format(pattern: String): String = 
        this.format(DateTimeFormatter.ofPattern(pattern))
    
    /**
     * 字符串解析为Date
     * 
     * @param pattern 日期格式，如"yyyy-MM-dd"
     * @return 解析结果，解析失败返回null
     */
    fun String.parseDate(pattern: String): Date? = try {
        SimpleDateFormat(pattern, Locale.getDefault()).parse(this)
    } catch (e: Exception) {
        null
    }
    
    /**
     * 字符串解析为LocalDate
     * 
     * @param pattern 日期格式，如"yyyy-MM-dd"
     * @return 解析结果，解析失败返回null
     */
    fun String.parseLocalDate(pattern: String): LocalDate? = try {
        LocalDate.parse(this, DateTimeFormatter.ofPattern(pattern))
    } catch (e: Exception) {
        null
    }
    
    /**
     * 获取格式化的日期字符串
     *
     * @param date 日期
     * @param pattern 格式模式
     * @return 格式化的日期字符串
     */
    fun formatDate(date: Date, pattern: String): String {
        val format = dateFormats.getOrPut(pattern) {
            SimpleDateFormat(pattern, Locale.getDefault())
        }
        return format.format(date)
    }
    
    /**
     * 检查两个日期是否在指定天数内
     *
     * @param date1 第一个日期
     * @param date2 第二个日期
     * @param days 天数
     * @return 如果两个日期相差不超过指定天数，则返回true
     */
    fun isDatesClose(date1: Date, date2: Date, days: Int): Boolean {
        val diffInMillis = Math.abs(date1.time - date2.time)
        val diffInDays = TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS)
        return diffInDays <= days
    }
    
    /**
     * 获取从当前日期开始的日期范围
     *
     * @param months 月数
     * @return 日期范围，包含开始日期和结束日期
     */
    fun getDateRangeFromNow(months: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        
        calendar.add(Calendar.MONTH, -months)
        val startDate = calendar.time
        
        return Pair(startDate, endDate)
    }
    
    /**
     * 获取月份的开始日期和结束日期
     *
     * @param year 年份
     * @param month 月份（0-11）
     * @return 日期范围，包含开始日期和结束日期
     */
    fun getMonthDateRange(year: Int, month: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        
        // 设置月初
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        // 设置月末
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        return Pair(startDate, endDate)
    }
    
    /**
     * 获取星期的开始日期和结束日期
     *
     * @param date 日期
     * @return 日期范围，包含开始日期和结束日期
     */
    fun getWeekDateRange(date: Date): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        // 设置为一周的第一天
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        // 设置为一周的最后一天
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time
        
        return Pair(startDate, endDate)
    }
    
    /**
     * 获取季度的开始日期和结束日期
     *
     * @param year 年份
     * @param quarter 季度（1-4）
     * @return 日期范围，包含开始日期和结束日期
     */
    fun getQuarterDateRange(year: Int, quarter: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        
        // 设置季度初
        val startMonth = (quarter - 1) * 3
        calendar.set(year, startMonth, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        // 设置季度末
        calendar.add(Calendar.MONTH, 3)
        calendar.add(Calendar.MILLISECOND, -1)
        val endDate = calendar.time
        
        return Pair(startDate, endDate)
    }
    
    /**
     * 获取年的开始日期和结束日期
     *
     * @param year 年份
     * @return 日期范围，包含开始日期和结束日期
     */
    fun getYearDateRange(year: Int): Pair<Date, Date> {
        val calendar = Calendar.getInstance()
        
        // 设置年初
        calendar.set(year, Calendar.JANUARY, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startDate = calendar.time
        
        // 设置年末
        calendar.set(year, Calendar.DECEMBER, 31, 23, 59, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endDate = calendar.time
        
        return Pair(startDate, endDate)
    }
    
    /**
     * 计算两个日期之间的天数
     *
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数
     */
    fun daysBetween(startDate: Date, endDate: Date): Int {
        val diffInMillis = Math.abs(endDate.time - startDate.time)
        return TimeUnit.DAYS.convert(diffInMillis, TimeUnit.MILLISECONDS).toInt()
    }
} 