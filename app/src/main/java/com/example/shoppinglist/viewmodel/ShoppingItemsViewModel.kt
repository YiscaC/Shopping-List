package com.example.shoppinglist.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.models.ShoppingItemEntity
import com.example.shoppinglist.data.repository.ShoppingItemsRepository
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

    fun addItemToFirebase(itemName: String) {
        viewModelScope.launch {
            repository.addItemToFirebase(listId, itemName)
        }
    }

    fun addCommentToItem(itemId: String, comment: String) {
        viewModelScope.launch {
            repository.addCommentToItem(listId, itemId, comment)
        }
    }

    fun updateItemImage(itemId: String, imageUrl: String) {
        viewModelScope.launch {
            repository.updateItemImage(listId, itemId, imageUrl)
        }
    }

    fun uploadImageForItem(itemId: String, imageUri: Uri) {
        val imageRef = storage.child("item_images/$itemId.jpg")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                updateItemImage(itemId, uri.toString())
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Error uploading image: ${it.message}")
        }
    }

    fun uploadImageForItem(itemId: String, imageData: ByteArray) {
        val imageRef = storage.child("item_images/$itemId.jpg")
        val uploadTask = imageRef.putBytes(imageData)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                updateItemImage(itemId, uri.toString())
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Error uploading image: ${it.message}")
        }
    }
    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(listId, itemId)
        }
    }

}
