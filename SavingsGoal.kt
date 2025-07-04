
package com.example.budgethive



data class SavingsGoal(
    var id: String          = "",
    var userId: String      = "",
    var name: String        = "",
    var targetAmount: Double= 0.0,
    var deadline: Long      = 0L,
    var completed: Boolean= false    // ← add this!
)
