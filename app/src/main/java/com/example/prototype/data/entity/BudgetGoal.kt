package com.example.prototype.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "budget_goals")
data class BudgetGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val month: Int, // 1-12
    val year: Int,
    val minGoal: Double,
    val maxGoal: Double,
    val targetSavings: Double // From screenshots: R10000
)