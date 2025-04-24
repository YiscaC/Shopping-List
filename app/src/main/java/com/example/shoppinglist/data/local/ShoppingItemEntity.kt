package com.example.shoppinglist.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.shoppinglist.data.local.converters.MessagesConverter

@Entity(tableName = "shopping_items")
@TypeConverters(MessagesConverter::class)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val listId: String, // מזהה הרשימה שאליה הפריט שייך
    val name: String,
    val quantity: Int,
    val purchased: Boolean,
    val imageUrl: String? = null,
    val order: Int = 0, // ✅ שדה חדש לציון סדר הפריטים
    val messages: List<Message> = emptyList() // ✅ שדה חדש של הודעות
) {
    fun toShoppingItem(): ShoppingItem {
        return ShoppingItem(
            id = id,
            name = name,
            quantity = quantity,
            purchased = purchased,
            expanded = false,
            order = order,
            messages = messages // ✅ הוספה להמרה
        )
    }
}
