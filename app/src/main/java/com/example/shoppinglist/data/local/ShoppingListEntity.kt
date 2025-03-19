package com.example.shoppinglist.data.local.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.shoppinglist.data.local.converters.ParticipantsConverter

@Entity(tableName = "shopping_lists")
@TypeConverters(ParticipantsConverter::class)
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val ownerId: String, // ✅ שמירת ה- UID של היוזר המחובר
    val participants: Map<String, Boolean> = emptyMap()
)
