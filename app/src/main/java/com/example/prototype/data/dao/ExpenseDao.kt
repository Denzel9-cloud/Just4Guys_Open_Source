package com.example.prototype.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete
import com.example.prototype.data.entity.Expense
import com.example.prototype.data.model.CategoryTotal
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense): Long

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<Expense>>

    @Query("""
        SELECT c.name as categoryName, SUM(e.amount) as totalAmount, c.color as categoryColor
        FROM expenses e
        JOIN categories c ON e.categoryId = c.id
        WHERE e.userId = :userId AND e.date BETWEEN :startDate AND :endDate
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryTotals(userId: Long, startDate: Date, endDate: Date): List<CategoryTotal>

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpensesForPeriod(userId: Long, startDate: Date, endDate: Date): Double?
}