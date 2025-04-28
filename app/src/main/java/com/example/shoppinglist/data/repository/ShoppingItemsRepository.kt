package com.example.shoppinglist.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.dao.ShoppingListDao
import com.example.shoppinglist.data.local.models.ShoppingItemEntity
import com.example.shoppinglist.data.local.models.ShoppingItem
import com.example.shoppinglist.data.local.models.Message
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                    val id = itemSnapshot.key.orEmpty()
                    val name = itemSnapshot.child("name").getValue(String::class.java).orEmpty()
                    val quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 1
                    val purchased = itemSnapshot.child("purchased").getValue(Boolean::class.java) ?: false
                    val order = itemSnapshot.child("order").getValue(Int::class.java) ?: 0

                    val messagesSnapshot = itemSnapshot.child("messages")
                    val messages = mutableListOf<Message>()
                    for (msgSnapshot in messagesSnapshot.children) {
                        msgSnapshot.getValue(Message::class.java)?.let { messages.add(it) }
                    }
                    val category = itemSnapshot.child("category").getValue(String::class.java).orEmpty()

                    val item = ShoppingItemEntity(
                        id = id,
                        listId = listId,
                        name = name,
                        quantity = quantity,
                        purchased = purchased,
                        order = order,
                        messages = messages,
                        category = category,
                    )
                    item?.let {
                        items.add(it) // פשוט מוסיפים את האייטם המקורי שכבר כולל category!
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

    suspend fun addItemToFirebase(listId: String, itemName: String, category: String,imageUrl: String) {
        val newItemId = db.child(listId).child("items").push().key ?: return
        val newItem = ShoppingItemEntity(
            id = newItemId,
            listId = listId,
            name = itemName,
            quantity = 1,
            purchased = false,
            order = 0,
            messages = emptyList(),
            category = category, // ✅ הוספתי את הקטגוריה פה
            imageUrl = imageUrl
        )

        db.child(listId).child("items").child(newItemId).setValue(newItem)
        withContext(Dispatchers.IO) {
            shoppingItemDao.insertItem(newItem)
        }
    }


    suspend fun addMessageToItem(listId: String, itemId: String, message: Message) {
        db.child(listId).child("items").child(itemId).child("messages").push().setValue(message)
        val current = shoppingItemDao.getItemById(itemId)
        current?.let {
            val updated = it.copy(messages = it.messages + message)
            shoppingItemDao.insertItem(updated)
        }
    }
    suspend fun uploadMessageImage(listId: String, itemId: String, senderId: String, imageUrl: String) {
        val message = Message(
            senderId = senderId,
            text = null,
            imageUrl = imageUrl,
            timestamp = System.currentTimeMillis()
        )
        addMessageToItem(listId, itemId, message)
    }

    suspend fun updateMessageImageUrl(listId: String, itemId: String, timestamp: Long, newUrl: String) {
        val item = shoppingItemDao.getItemById(itemId)
        item?.let {
            val updatedMessages = it.messages.map { msg ->
                if (msg.timestamp == timestamp) msg.copy(imageUrl = newUrl) else msg
            }
            val updatedItem = it.copy(messages = updatedMessages)
            shoppingItemDao.insertItem(updatedItem)

            val firebaseMessageRef = db.child(listId).child("items").child(itemId).child("messages")
            firebaseMessageRef.get().addOnSuccessListener { snapshot ->
                for (msg in snapshot.children) {
                    val msgObj = msg.getValue(Message::class.java)
                    if (msgObj != null && msgObj.timestamp == timestamp) {
                        msg.ref.child("imageUrl").setValue(newUrl)
                        break
                    }
                }
            }
        }
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
