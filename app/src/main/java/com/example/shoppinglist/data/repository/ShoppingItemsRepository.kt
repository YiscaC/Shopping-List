package com.example.shoppinglist.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.dao.ShoppingListDao
import com.example.shoppinglist.data.local.models.ShoppingItemEntity
import com.example.shoppinglist.data.local.models.ShoppingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.firebase.database.*

class ShoppingItemsRepository(context: Context) {

    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference.child("shoppingLists")
    private val shoppingItemDao = AppDatabase.getDatabase(context).shoppingItemDao()
    private val shoppingListDao: ShoppingListDao = AppDatabase.getDatabase(context).shoppingListDao()

    fun getShoppingItems(listId: String): LiveData<List<ShoppingItemEntity>> {
        return shoppingItemDao.getItemsByListId(listId)
    }

    fun syncShoppingItems(listId: String) {
        db.child(listId).child("items").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ShoppingItemEntity>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(ShoppingItem::class.java)
                    item?.let {
                        items.add(
                            ShoppingItemEntity(
                                it.id,
                                listId,
                                it.name,
                                it.quantity,
                                it.purchased,
                                it.imageUrl,
                                it.order
                            )
                        )
                    }
                }
                CoroutineScope(Dispatchers.IO).launch {
                    items.forEach { shoppingItemDao.insertItem(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    suspend fun updateItemPurchased(listId: String, itemId: String, isChecked: Boolean) {
        db.child(listId).child("items").child(itemId).child("purchased").setValue(isChecked)
        shoppingItemDao.updateItemPurchased(itemId, isChecked)
    }

    suspend fun updateItemQuantity(listId: String, itemId: String, newQuantity: Int) {
        db.child(listId).child("items").child(itemId).child("quantity").setValue(newQuantity)
        shoppingItemDao.updateItemQuantity(itemId, newQuantity)
    }

    suspend fun updateItemOrder(listId: String, itemId: String, newOrder: Int) {
        db.child(listId).child("items").child(itemId).child("order").setValue(newOrder)
        shoppingItemDao.updateItemOrder(itemId, newOrder)
    }

    suspend fun addItemToFirebase(listId: String, itemName: String) {
        val newItemId = db.child(listId).child("items").push().key ?: return
        val newItem = ShoppingItemEntity(newItemId, listId, itemName, 1, false, null, order = 0)

        db.child(listId).child("items").child(newItemId).setValue(newItem)
        withContext(Dispatchers.IO) {
            shoppingItemDao.insertItem(newItem)
        }
    }

    suspend fun addCommentToItem(listId: String, itemId: String, comment: String) {
        db.child(listId).child("items").child(itemId).child("comments").push().setValue(comment)
    }

    suspend fun updateItemImage(listId: String, itemId: String, imageUrl: String) {
        db.child(listId).child("items").child(itemId).child("imageUrl").setValue(imageUrl)
    }

    suspend fun addParticipant(listId: String, participantName: String) {
        withContext(Dispatchers.IO) {
            val list = shoppingListDao.getListById(listId) ?: return@withContext
            val updatedParticipants = list.participants + (participantName to true)
            val updatedList = list.copy(participants = updatedParticipants)

            shoppingListDao.updateList(updatedList)
            db.child("shoppingLists").child(listId).child("participants").child(participantName).setValue(true)
        }
    }
    suspend fun deleteItem(listId: String, itemId: String) {
        db.child(listId).child("items").child(itemId).removeValue()
        shoppingItemDao.deleteById(itemId)
    }

}
