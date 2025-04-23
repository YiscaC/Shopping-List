package com.example.shoppinglist.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.shoppinglist.data.local.models.ShoppingItemEntity

@Dao
interface ShoppingItemDao {
    @Query("SELECT * FROM shopping_items WHERE listId = :listId")
    fun getItemsByListId(listId: String): LiveData<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingItemEntity)

    @Query("UPDATE shopping_items SET purchased = :isChecked WHERE id = :itemId")
    suspend fun updateItemPurchased(itemId: String, isChecked: Boolean)

    @Query("UPDATE shopping_items SET quantity = :newQuantity WHERE id = :itemId")
    suspend fun updateItemQuantity(itemId: String, newQuantity: Int)

    @Query("UPDATE shopping_items SET imageUrl = :imageUrl WHERE id = :itemId")
    suspend fun updateItemImage(itemId: String, imageUrl: String)

    @Query("UPDATE shopping_items SET `order` = :newOrder WHERE id = :itemId")
    suspend fun updateItemOrder(itemId: String, newOrder: Int)

    @Query("DELETE FROM shopping_items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)

}
