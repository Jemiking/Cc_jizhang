package com.ccjizhang.data.service

import com.ccjizhang.data.model.Category
import com.ccjizhang.data.repository.CategoryRepository
import com.ccjizhang.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 分类建议服务
 * 负责基于历史数据提供智能分类建议
 */
@Singleton
class CategorySuggestionService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) {
    
    /**
     * 基于交易描述提供分类建议
     * @param description 交易描述
     * @param isIncome 是否为收入
     * @return 建议的分类列表，按匹配度排序
     */
    suspend fun suggestCategoriesForDescription(
        description: String,
        isIncome: Boolean
    ): List<Category> {
        if (description.isBlank()) return emptyList()
        
        // 获取相关分类
        val relevantCategories = if (isIncome) {
            categoryRepository.getIncomeCategories().first()
        } else {
            categoryRepository.getExpenseCategories().first()
        }
        
        // 用于存储每个分类的匹配分数
        val categoryScores = mutableMapOf<Long, Double>()
        
        // 获取最近的交易记录（最多100条）
        val recentTransactions = transactionRepository.getRecentTransactions(100).first()
        
        // 对每个交易进行分析
        for (transaction in recentTransactions) {
            // 跳过与当前类型不匹配的交易
            if (transaction.isIncome != isIncome) continue
            
            // 跳过没有分类的交易
            val categoryId = transaction.categoryId ?: continue
            
            // 计算描述相似度（简单实现，可以用更复杂的算法如TF-IDF或词向量）
            val similarity = calculateSimilarity(description, transaction.note)
            
            // 更新分类得分
            val currentScore = categoryScores.getOrDefault(categoryId, 0.0)
            categoryScores[categoryId] = currentScore + similarity
        }
        
        // 没有找到匹配时，返回所有相关分类
        if (categoryScores.isEmpty()) {
            return relevantCategories
        }
        
        // 按分数排序分类
        return relevantCategories
            .filter { categoryScores.containsKey(it.id) }
            .sortedByDescending { categoryScores[it.id] }
            .ifEmpty { relevantCategories }
    }
    
    /**
     * 计算两个字符串的相似度
     * 这里使用简单的词语匹配算法，可以替换为更复杂的算法
     */
    private fun calculateSimilarity(str1: String, str2: String): Double {
        if (str1.isBlank() || str2.isBlank()) return 0.0
        
        // 将字符串转换为词组集合
        val words1 = str1.lowercase().split(Regex("\\s+")).toSet()
        val words2 = str2.lowercase().split(Regex("\\s+")).toSet()
        
        // 计算重叠词数量
        val intersection = words1.intersect(words2)
        
        // Jaccard相似度: 交集大小 / 并集大小
        val union = words1.union(words2)
        return if (union.isEmpty()) 0.0 else intersection.size.toDouble() / union.size
    }
    
    /**
     * 根据交易金额范围提供分类建议
     * @param amount 交易金额
     * @param isIncome 是否为收入
     * @return 建议的分类列表，按匹配度排序
     */
    suspend fun suggestCategoriesForAmount(
        amount: Double,
        isIncome: Boolean
    ): List<Category> {
        // 获取相关分类
        val relevantCategories = if (isIncome) {
            categoryRepository.getIncomeCategories().first()
        } else {
            categoryRepository.getExpenseCategories().first()
        }
        
        // 用于存储每个分类的匹配次数
        val categoryFrequency = mutableMapOf<Long, Int>()
        
        // 获取最近的交易记录（最多100条）
        val recentTransactions = transactionRepository.getRecentTransactions(100).first()
        
        // 定义金额相似的范围（比如10%以内）
        val lowerBound = amount * 0.9
        val upperBound = amount * 1.1
        
        // 对每个交易进行分析
        for (transaction in recentTransactions) {
            // 跳过与当前类型不匹配的交易
            if (transaction.isIncome != isIncome) continue
            
            // 跳过没有分类的交易
            val categoryId = transaction.categoryId ?: continue
            
            // 检查金额是否在相似范围内
            if (transaction.amount in lowerBound..upperBound) {
                // 更新分类频率
                val currentFreq = categoryFrequency.getOrDefault(categoryId, 0)
                categoryFrequency[categoryId] = currentFreq + 1
            }
        }
        
        // 没有找到匹配时，返回所有相关分类
        if (categoryFrequency.isEmpty()) {
            return relevantCategories
        }
        
        // 按频率排序分类
        return relevantCategories
            .filter { categoryFrequency.containsKey(it.id) }
            .sortedByDescending { categoryFrequency[it.id] }
            .ifEmpty { relevantCategories }
    }
} 