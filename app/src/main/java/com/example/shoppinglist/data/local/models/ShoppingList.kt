package com.example.shoppinglist.data.local.models

data class ShoppingList(
    val id: String = "", // מזהה ייחודי
    val name: String = "", // שם הרשימה
    val ownerId: String = "", // בעל הרשימה
    val participants: Map<String, Boolean> = emptyMap() // משתמשים שמשתתפים ברשימה
)
