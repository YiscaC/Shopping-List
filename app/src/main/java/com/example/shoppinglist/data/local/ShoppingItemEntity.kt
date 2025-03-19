package com.example.shoppinglist.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String, // מזהה הרשימה שאליה הפריט שייך
    val name: String,
    val quantity: Int,
    val purchased: Boolean,
    val imageUrl: String? = null
) {
    // ✅ פונקציה להמרה ל-`ShoppingItem`
    fun toShoppingItem(): ShoppingItem {
        return ShoppingItem(id, name, quantity, purchased, imageUrl, expanded = false)
    }
}
