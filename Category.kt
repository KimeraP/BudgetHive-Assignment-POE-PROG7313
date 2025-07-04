// Category.kt
package com.example.budgethive

data class Category(
    var id: String = "",
    var userId: String = "",
    var name: String = "",
    var colorHex: String? = null
) {
    override fun toString() = name
}
