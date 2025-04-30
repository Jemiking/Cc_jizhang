package com.ccjizhang.data.archive

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ccjizhang.data.db.converters.Converters
import com.ccjizhang.data.archive.ArchiveAccountDao
import com.ccjizhang.data.archive.ArchiveCategoryDao
import com.ccjizhang.data.archive.ArchiveTransactionDao
import com.ccjizhang.data.model.Account
import com.ccjizhang.data.model.AccountCategory
import com.ccjizhang.data.model.Category
import com.ccjizhang.data.model.Transaction

/**
 * 归档数据库
 * 用于存储归档的历史数据
 */
@Database(
    entities = [
        Transaction::class,
        Category::class,
        Account::class,
        AccountCategory::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ArchiveDatabase : RoomDatabase() {

    /**
     * 获取归档交易DAO
     */
    abstract fun transactionDao(): ArchiveTransactionDao

    /**
     * 获取归档分类DAO
     */
    abstract fun categoryDao(): ArchiveCategoryDao

    /**
     * 获取归档账户DAO
     */
    abstract fun accountDao(): ArchiveAccountDao
}
