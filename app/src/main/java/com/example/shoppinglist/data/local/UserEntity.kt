package com.example.shoppinglist.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val username: String,
    val firstName: String,
    val phone: String,
    val localProfileImagePath: String? = null
)
