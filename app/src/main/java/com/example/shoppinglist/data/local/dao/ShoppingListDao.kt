package com.example.shoppinglist.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*

import com.example.shoppinglist.data.local.models.ShoppingListEntity

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists")
    fun getAllShoppingLists(): LiveData<List<ShoppingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity)

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingListEntity)
}
