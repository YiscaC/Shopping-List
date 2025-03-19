package com.example.shoppinglist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import com.example.shoppinglist.data.repository.ShoppingListRepository
import kotlinx.coroutines.launch

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingListRepository(application)

    // ✅ קבלת רק הרשימות של המשתמש המחובר
    val shoppingLists: LiveData<List<ShoppingListEntity>> = repository.getUserShoppingLists()

    fun createShoppingList(name: String) {
        viewModelScope.launch {
            repository.createShoppingList(name)
        }
    }

    fun deleteShoppingList(listId: String) {
        viewModelScope.launch {
            repository.deleteShoppingList(listId)
        }
    }

    fun addParticipantToList(listId: String, participantEmail: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addParticipant(listId, participantEmail)
            callback(success)
        }
    }
}
