package com.example.prototype.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.prototype.data.entity.BudgetGoal
import java.util.Date

@Dao
interface BudgetGoalDao {
    @Insert
    suspend fun insertBudgetGoal(goal: BudgetGoal): Long

    @Update
    suspend fun updateBudgetGoal(goal: BudgetGoal)

    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND month = :month AND year = :year")
    suspend fun getBudgetGoal(userId: Long, month: Int, year: Int): BudgetGoal?

    @Query("SELECT * FROM budget_goals WHERE userId = :userId AND year = :year ORDER BY month")
    suspend fun getAllGoalsForYear(userId: Long, year: Int): List<BudgetGoal>
}