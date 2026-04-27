package com.example.prototype.data

import android.content.Context
import com.example.prototype.data.dao.*
import com.example.prototype.data.entity.*
import com.example.prototype.data.model.CategoryTotal
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date

class PaymentPlannerRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val categoryDao = database.categoryDao()
    private val expenseDao = database.expenseDao()
    private val budgetGoalDao = database.budgetGoalDao()

    // User operations
    suspend fun login(username: String, password: String): User? = userDao.login(username, password)
    suspend fun createUser(username: String, password: String): Long = userDao.insertUser(User(username = username, password = password))
    suspend fun getUserByUsername(username: String): User? = userDao.getUserByUsername(username)

    // Category operations
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategories()
    suspend fun addCategory(category: Category): Long = categoryDao.insertCategory(category)
    suspend fun updateCategory(category: Category) = categoryDao.updateCategory(category)
    suspend fun deleteCategory(category: Category) = categoryDao.deleteCategory(category)

    // Expense operations
    suspend fun addExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    fun getExpensesByDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(userId, startDate, endDate)
    suspend fun getCategoryTotals(userId: Long, startDate: Date, endDate: Date): List<CategoryTotal> =
        expenseDao.getCategoryTotals(userId, startDate, endDate)
    suspend fun getTotalExpensesForPeriod(userId: Long, startDate: Date, endDate: Date): Double =
        expenseDao.getTotalExpensesForPeriod(userId, startDate, endDate) ?: 0.0

    // Budget goals
    suspend fun getBudgetGoalForCurrentMonth(userId: Long): BudgetGoal? {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        return budgetGoalDao.getBudgetGoal(userId, month, year)
    }

    suspend fun saveBudgetGoal(userId: Long, minGoal: Double, maxGoal: Double, targetSavings: Double) {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        val existingGoal = budgetGoalDao.getBudgetGoal(userId, month, year)

        if (existingGoal != null) {
            budgetGoalDao.updateBudgetGoal(existingGoal.copy(minGoal = minGoal, maxGoal = maxGoal, targetSavings = targetSavings))
        } else {
            budgetGoalDao.insertBudgetGoal(BudgetGoal(
                userId = userId,
                month = month,
                year = year,
                minGoal = minGoal,
                maxGoal = maxGoal,
                targetSavings = targetSavings
            ))
        }
    }
}