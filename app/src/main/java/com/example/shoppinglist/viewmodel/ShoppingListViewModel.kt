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

    // ✅ טעינת הרשימות מהמסד נתונים
    val shoppingLists: LiveData<List<ShoppingListEntity>> = repository.getAllShoppingLists()

    // ✅ יצירת רשימה חדשה
    fun createShoppingList(name: String) {
        viewModelScope.launch {
            repository.createShoppingList(name)
        }
    }

    // ✅ מחיקת רשימה
    fun deleteShoppingList(listId: String) {
        viewModelScope.launch {
            repository.deleteShoppingList(listId)
        }
    }

    // ✅ הוספת משתתף רק אם קיים ב- Realtime Database
    fun addParticipantToList(listId: String, participantEmail: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addParticipant(listId, participantEmail)
            callback(success)
        }
    }
}
