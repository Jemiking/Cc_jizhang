package com.ccjizhang.util

/**
 * 集合工具类
 * 
 * 提供类型安全的集合操作函数
 */
object CollectionUtils {
    
    /**
     * 针对List<T>的明确类型forEach
     * 
     * 解决forEach调用时的类型歧义问题
     * 
     * @param action 对每个元素执行的操作
     */
    inline fun <reified T> Iterable<*>.forEachTyped(action: (T) -> Unit) {
        this.forEach { item ->
            if (item is T) {
                action(item)
            }
        }
    }
    
    /**
     * 针对Map<K, V>的明确类型forEach
     * 
     * 解决forEach调用时的类型歧义问题
     * 
     * @param action 对每个键值对执行的操作
     */
    inline fun <reified K, reified V> Map<*, *>.forEachTyped(action: (key: K, value: V) -> Unit) {
        this.forEach { (key, value) ->
            if (key is K && value is V) {
                action(key, value)
            }
        }
    }
    
    /**
     * 安全转换List并执行forEach
     * 
     * @param action 在确认类型后对List执行的操作
     */
    inline fun <reified T> Any?.asListOfType(action: (List<T>) -> Unit) {
        if (this is List<*>) {
            @Suppress("UNCHECKED_CAST")
            action(this as List<T>)
        }
    }
    
    /**
     * 安全转换Map并执行操作
     * 
     * @param action 在确认类型后对Map执行的操作
     */
    inline fun <reified K, reified V> Any?.asMapOfType(action: (Map<K, V>) -> Unit) {
        if (this is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            action(this as Map<K, V>)
        }
    }
    
    /**
     * 对可能为null的集合执行forEach操作
     * 
     * @param action 对每个元素执行的操作
     */
    inline fun <T> Collection<T>?.forEachSafely(action: (T) -> Unit) {
        this?.forEach(action)
    }
} 