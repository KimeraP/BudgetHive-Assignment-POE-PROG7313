// ExpenseEntry.kt
package com.example.budgethive

data class ExpenseEntry(
    var id: String = "",
    var userId: String = "",
    var categoryId: String? = null,
    var date: Long = 0L,
    var startTime: Long = 0L,
    var endTime: Long = 0L,
    var description: String = "",
    var photoUri: String? = null,
    var amount: Double = 0.0
)
