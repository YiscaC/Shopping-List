package com.example.shoppinglist.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey val id: String, // מזהה הרשימה יהיה המזהה של Firebase
    val name: String,
    val owner: String
)
