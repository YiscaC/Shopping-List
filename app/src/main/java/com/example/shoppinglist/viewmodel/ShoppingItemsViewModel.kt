package com.example.shoppinglist.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.shoppinglist.data.local.models.ShoppingItem
import com.example.shoppinglist.data.repository.ShoppingItemsRepository
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class ShoppingItemsViewModel : ViewModel() {

    private val repository = ShoppingItemsRepository()
    private val storage = FirebaseStorage.getInstance().reference
    private val _itemsList = MutableLiveData<List<ShoppingItem>>()
    val itemsList: LiveData<List<ShoppingItem>> get() = _itemsList

    fun loadShoppingItems(listId: String) {
        repository.loadShoppingItems(listId) { items ->
            _itemsList.value = items
        }
    }

    fun updateItemPurchased(listId: String, itemId: String, isChecked: Boolean) {
        repository.updateItemPurchased(listId, itemId, isChecked)
    }

    fun updateItemQuantity(listId: String, itemId: String, newQuantity: Int) {
        repository.updateItemQuantity(listId, itemId, newQuantity)
    }

    fun addItemToFirebase(listId: String, itemName: String) {
        repository.addItemToFirebase(listId, itemName)
    }

    fun addCommentToItem(listId: String, itemId: String, comment: String) {
        repository.addCommentToItem(listId, itemId, comment)
    }

    fun updateItemImage(listId: String, itemId: String, imageUrl: String) {
        repository.updateItemImage(listId, itemId, imageUrl)
    }

    // 📸 העלאת תמונה מהמצלמה או הגלריה (URI)
    fun uploadImageForItem(listId: String, itemId: String, imageUri: Uri) {
        val imageRef = storage.child("item_images/$itemId.jpg")
        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                updateItemImage(listId, itemId, uri.toString())
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Error uploading image: ${it.message}")
        }
    }

    // 📸 העלאת תמונה מהמצלמה (Bitmap)
    fun uploadImageForItem(listId: String, itemId: String, imageData: ByteArray) {
        val imageRef = storage.child("item_images/$itemId.jpg")
        val uploadTask = imageRef.putBytes(imageData)

        uploadTask.addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                updateItemImage(listId, itemId, uri.toString())
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Error uploading image: ${it.message}")
        }
    }
}
