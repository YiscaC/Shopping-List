package com.example.shoppinglist.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.shoppinglist.data.local.models.ShoppingListEntity

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists") // 🔹 החזרת כל הרשימות בלי קשר למשתמש
    fun getAllShoppingLists(): LiveData<List<ShoppingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity)

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingListEntity)

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getListById(listId: String): ShoppingListEntity?

    @Update
    suspend fun updateList(shoppingList: ShoppingListEntity)
}
