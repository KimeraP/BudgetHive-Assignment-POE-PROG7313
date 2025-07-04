package com.example.budgethive

/**
 * A simple holder for the query result of total spent per category.
 */
data class CategoryTotal(
    val categoryName: String,
    val totalSpent: Double
)
