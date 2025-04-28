package com.example.shoppinglist.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.shoppinglist.data.local.models.ShoppingListEntity

@Dao
interface ShoppingListDao {

    // ✅ החזרת רשימות שהמשתמש יצר או שהוא משתתף בהן
    @Query("SELECT * FROM shopping_lists WHERE ownerId = :userId OR participants LIKE '%' || :userId || '%' ")
    fun getUserShoppingLists(userId: String): LiveData<List<ShoppingListEntity>>

    // ✅ הוספת רשימה חדשה
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingListEntity)

    // ✅ מחיקת רשימה
    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingListEntity)

    // ✅ החזרת רשימה לפי מזהה
    @Query("SELECT * FROM shopping_lists WHERE id = :listId")
    suspend fun getListById(listId: String): ShoppingListEntity?

    // ✅ עדכון רשימה קיימת
    @Update
    suspend fun updateList(shoppingList: ShoppingListEntity)

    // ✅ הוספת רשימות מרובות
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingLists(lists: List<ShoppingListEntity>)

    // ✅ מחיקת רשימה לפי מזהה
    @Query("DELETE FROM shopping_lists WHERE id = :listId")
    suspend fun deleteListById(listId: String)
}
