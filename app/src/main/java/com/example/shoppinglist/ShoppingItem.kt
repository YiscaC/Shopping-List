package com.example.shoppinglist

data class ShoppingItem(
    val name: String,       // שם הפריט
    val quantity: Int,      // כמות הפריט
    val purchased: Boolean, // האם הפריט נקנה
    val imageUrl: String? = null // קישור לתמונה (לא חובה)
)
