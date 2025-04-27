package com.example.shoppinglist.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.models.ShoppingItemEntity
import com.example.shoppinglist.data.local.models.Message
import com.example.shoppinglist.data.repository.ShoppingItemsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch

class ShoppingItemsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingItemsRepository(application)
    private val storage = FirebaseStorage.getInstance().reference
    private var listId: String = ""

    val itemsList: LiveData<List<ShoppingItemEntity>>
        get() = repository.getShoppingItems(listId)

    fun setListId(id: String) {
        listId = id
        viewModelScope.launch {
            repository.syncShoppingItems(listId)
        }
    }

    fun updateItemPurchased(itemId: String, isChecked: Boolean) {
        viewModelScope.launch {
            repository.updateItemPurchased(listId, itemId, isChecked)
        }
    }

    fun updateItemQuantity(itemId: String, newQuantity: Int) {
        viewModelScope.launch {
            repository.updateItemQuantity(listId, itemId, newQuantity)
        }
    }

    fun updateItemOrder(itemId: String, newOrder: Int) {
        viewModelScope.launch {
            repository.updateItemOrder(listId, itemId, newOrder)
        }
    }
    fun updateMultipleItemsOrder(updatedOrders: List<Pair<String, Int>>) {
        viewModelScope.launch {
            updatedOrders.forEach { (itemId, newOrder) ->
                repository.updateItemOrder(listId, itemId, newOrder)
            }
        }
    }

    fun addItemToFirebase(itemName: String, category: String) {
        viewModelScope.launch {
            repository.addItemToFirebase(listId, itemName, category)
        }
    }

    fun addMessageToItem(itemId: String, text: String) {
        val message = Message(
            senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
            text = text,
            imageUrl = null,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.addMessageToItem(listId, itemId, message)
        }
    }

    fun uploadMessageImageFromUri(itemId: String, uri: Uri) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val timestamp = System.currentTimeMillis()
        val localPath = uri.toString()

        val message = Message(senderId, null, localPath, timestamp)
        viewModelScope.launch {
            repository.addMessageToItem(listId, itemId, message)
        }

        val imageRef = storage.child("messages/${itemId}_$timestamp.jpg")
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                viewModelScope.launch {
                    repository.updateMessageImageUrl(listId, itemId, timestamp, downloadUri.toString())
                }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "\u05e9\u05d2\u05d9\u05d0\u05d4 \u05d1\u05d4\u05e2\u05dc\u05d0\u05ea \u05ea\u05de\u05d5\u05e0\u05d4: \${it.message}")
        }
    }

    fun uploadMessageImageFromBytes(itemId: String, imageData: ByteArray) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val timestamp = System.currentTimeMillis()
        val message = Message(senderId, null, null, timestamp)
        viewModelScope.launch {
            repository.addMessageToItem(listId, itemId, message)
        }

        val imageRef = storage.child("messages/${itemId}_$timestamp.jpg")
        val uploadTask = imageRef.putBytes(imageData)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                viewModelScope.launch {
                    repository.updateMessageImageUrl(listId, itemId, timestamp, downloadUri.toString())
                }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "\u05e9\u05d2\u05d9\u05d0\u05d4 \u05d1\u05d4\u05e2\u05dc\u05d0\u05ea \u05ea\u05de\u05d5\u05e0\u05d4: \${it.message}")
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(listId, itemId)
        }
    }
}
