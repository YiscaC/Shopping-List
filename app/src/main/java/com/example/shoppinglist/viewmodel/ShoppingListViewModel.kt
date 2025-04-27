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

    private val _shoppingLists = repository.getUserShoppingLists()
    val shoppingLists: LiveData<List<ShoppingListEntity>> get() = _shoppingLists

    fun createShoppingList(name: String) {
        viewModelScope.launch {
            repository.createShoppingList(name)
            refreshShoppingLists() // ✅ ישר לרענן אחרי יצירה
        }
    }

    fun deleteShoppingList(listId: String) {
        viewModelScope.launch {
            repository.deleteShoppingList(listId)
            refreshShoppingLists() // ✅ ישר לרענן אחרי מחיקה
        }
    }

    fun addParticipantToList(listId: String, participantEmail: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addParticipant(listId, participantEmail)
            refreshShoppingLists() // ✅ ישר לרענן אחרי הוספה
            callback(success)
        }
    }

    fun refreshShoppingLists() {
        viewModelScope.launch {
            repository.refreshShoppingLists()
        }
    }
}
