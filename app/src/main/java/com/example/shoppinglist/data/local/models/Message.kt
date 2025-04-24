package com.example.shoppinglist.data.local.models

data class Message(
    val senderId: String = "",
    val text: String? = null,
    val imageUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)