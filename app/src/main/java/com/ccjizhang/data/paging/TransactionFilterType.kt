package com.ccjizhang.data.paging

/**
 * 交易筛选类型枚举
 * 用于分页查询时指定筛选条件
 */
enum class TransactionFilterType {
    /**
     * 所有交易
     */
    ALL,
    
    /**
     * 仅收入交易
     */
    INCOME,
    
    /**
     * 仅支出交易
     */
    EXPENSE
}
