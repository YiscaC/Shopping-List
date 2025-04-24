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

    fun addItemToFirebase(itemName: String) {
        viewModelScope.launch {
            repository.addItemToFirebase(listId, itemName)
        }
    }

    // ✅ שליחת הודעת טקסט
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

    // ✅ העלאת תמונה כהודעה מסוג תמונה
    fun uploadMessageImage(itemId: String, imageUri: Uri) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val imageRef = storage.child("message_images/${itemId}_${System.currentTimeMillis()}.jpg")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                viewModelScope.launch {
                    repository.uploadMessageImage(listId, itemId, senderId, uri.toString())
                }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "שגיאה בהעלאת תמונה: ${it.message}")
        }
    }

    fun uploadMessageImage(itemId: String, imageData: ByteArray) {
        val senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
        val imageRef = storage.child("message_images/${itemId}_${System.currentTimeMillis()}.jpg")
        val uploadTask = imageRef.putBytes(imageData)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                viewModelScope.launch {
                    repository.uploadMessageImage(listId, itemId, senderId, uri.toString())
                }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "שגיאה בהעלאת תמונה: ${it.message}")
        }
    }
    fun uploadMessageImageFromUri(itemId: String, uri: Uri) {
        val imageRef = storage.child("messages/${itemId}_${System.currentTimeMillis()}.jpg")
        val uploadTask = imageRef.putFile(uri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val message = Message(
                    senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                    imageUrl = downloadUri.toString(),
                    timestamp = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    repository.addMessageToItem(listId, itemId, message)
                }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "שגיאה בהעלאת תמונה מהגלריה: ${it.message}")
        }
    }

    fun uploadMessageImageFromBytes(itemId: String, imageData: ByteArray) {
        val imageRef = storage.child("messages/${itemId}_${System.currentTimeMillis()}.jpg")
        val uploadTask = imageRef.putBytes(imageData)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                val message = Message(
                    senderId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                    imageUrl = downloadUri.toString(),
                    timestamp = System.currentTimeMillis()
                )
                viewModelScope.launch {
                    repository.addMessageToItem(listId, itemId, message)
                }
            }
        }.addOnFailureListener {
            Log.e("Firebase", "שגיאה בהעלאת תמונה מהמצלמה: ${it.message}")
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            repository.deleteItem(listId, itemId)
        }
    }
}
