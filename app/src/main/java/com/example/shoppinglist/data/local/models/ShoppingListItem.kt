package com.example.shoppinglist.data.local.models
sealed class ShoppingListItem {
    data class CategoryHeader(val categoryName: String) : ShoppingListItem()
    data class ShoppingProduct(val item: ShoppingItem) : ShoppingListItem()
}
