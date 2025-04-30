package com.ccjizhang.util

/**
 * 枚举工具类
 * 
 * 提供枚举与整数之间的类型转换函数
 */
object EnumUtils {
    
    /**
     * 将整数转换为枚举值
     * 
     * @param value 整数值
     * @param defaultValue 默认值，当转换失败时返回
     * @return 对应的枚举值，如果找不到对应值则返回默认值
     */
    inline fun <reified T : Enum<T>> fromInt(value: Int, defaultValue: T): T {
        return try {
            enumValues<T>().getOrElse(value) { defaultValue }
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * 将枚举值转换为整数
     * 
     * @return 枚举的序号
     */
    fun <T : Enum<T>> toInt(enum: T): Int = enum.ordinal
    
    /**
     * 将字符串转换为枚举值
     * 
     * @param value 字符串
     * @param ignoreCase 是否忽略大小写，默认为true
     * @param defaultValue 默认值，当转换失败时返回
     * @return 对应的枚举值，如果找不到对应值则返回默认值
     */
    inline fun <reified T : Enum<T>> fromString(
        value: String, 
        ignoreCase: Boolean = true,
        defaultValue: T
    ): T {
        return try {
            enumValues<T>().find { 
                it.name.equals(value, ignoreCase) 
            } ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * 安全地处理可能为null的枚举值
     * 
     * @param action 对枚举值执行的操作
     */
    inline fun <T : Enum<T>> T?.ifPresent(action: (T) -> Unit) {
        this?.let(action)
    }
} 