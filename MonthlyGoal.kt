// MonthlyGoal.kt
package com.example.budgethive

data class MonthlyGoal(
    var id: String = "",
    var userId: String = "",
    var year: Int = 0,
    var month: Int = 0,
    var minAmount: Double? = null,
    var maxAmount: Double? = null
)
