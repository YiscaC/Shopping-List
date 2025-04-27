package com.example.shoppinglist.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.shoppinglist.data.local.models.ShoppingListEntity

@Dao
interface ShoppingListDao {
    // ✅ החזרת רשימות שהמשתמש יצר או שהוא משתתף בהן
    @Query("SELECT * FROM shopping_lists WHERE ownerId = :userId OR participants LIKE '%' || :userId || '%' ")
    fun getUserShoppingLists(userId: String): LiveData<List<ShoppingListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity)

    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingListEntity)

    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getListById(listId: String): ShoppingListEntity?

    @Update
    suspend fun updateList(shoppingList: ShoppingListEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingLists(lists: List<ShoppingListEntity>)
}
