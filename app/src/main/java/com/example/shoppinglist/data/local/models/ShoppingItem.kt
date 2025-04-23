package com.example.shoppinglist.data.local.models


data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val quantity: Int = 1,
    var purchased: Boolean = false,
    val imageUrl: String? = null,
    var expanded: Boolean = false ,// ✅ האם הפריט פתוח או סגור
    var order: Int = 0

)
