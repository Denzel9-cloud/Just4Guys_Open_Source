package com.example.prototype.data.model

import com.example.prototype.data.entity.Expense

data class ExpenseWithCategory(
    val expense: Expense,
    val categoryName: String,
    val categoryColor: Int
)