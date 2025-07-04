// User.kt
package com.example.budgethive

data class User(
    var id: String = "",
    var name: String = "",
    var surname: String = "",
    var cellNumber: String = "",
    var password: String ="",
    var email: String = "",
    var currentBalance: Double = 0.0
)
