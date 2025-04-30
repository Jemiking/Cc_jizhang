package com.ccjizhang.data.db.converters

import androidx.room.TypeConverter
import com.ccjizhang.data.model.Investment
import com.ccjizhang.data.model.RecurringTransactionFrequency
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * Room数据库类型转换器
 * 用于在数据库类型和Java/Kotlin类型之间进行转换
 */
class Converters {
    private val gson = Gson()
    
    // Date 转换
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    // String List 转换
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type)
    }
    
    // Long List 转换
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        if (value == null) return null
        val type = object : TypeToken<List<Long>>() {}.type
        return gson.fromJson(value, type)
    }
    
    // Investment.Type 枚举转换
    @TypeConverter
    fun fromInvestmentType(value: Investment.Type?): Int? {
        return value?.ordinal
    }
    
    @TypeConverter
    fun toInvestmentType(value: Int?): Investment.Type? {
        return value?.let { Investment.Type.values()[it] }
    }
    
    // RecurringTransactionFrequency 枚举转换
    @TypeConverter
    fun fromRecurringFrequency(value: RecurringTransactionFrequency?): Int? {
        return value?.ordinal
    }
    
    @TypeConverter
    fun toRecurringFrequency(value: Int?): RecurringTransactionFrequency? {
        return value?.let { RecurringTransactionFrequency.values()[it] }
    }
    
    // Map<String, Any> 转换 (用于通用JSON数据存储)
    @TypeConverter
    fun fromMap(value: Map<String, Any>?): String? {
        return value?.let { gson.toJson(it) }
    }
    
    @TypeConverter
    fun toMap(value: String?): Map<String, Any>? {
        if (value == null) return null
        val type = object : TypeToken<Map<String, Any>>() {}.type
        return gson.fromJson(value, type)
    }
} 