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

    // רשימת הקניות של המשתמש (מתעדכנת אוטומטית מה-ROOM)
    val shoppingLists: LiveData<List<ShoppingListEntity>> = repository.getUserShoppingLists()

    // יצירת רשימה חדשה
    fun createShoppingList(name: String) {
        viewModelScope.launch {
            repository.createShoppingList(name)
            // אין צורך לקרוא ל-refresh, כי LiveData מתעדכן לבד מ-ROOM
        }
    }

    // מחיקת רשימה
    fun deleteShoppingList(listId: String) {
        viewModelScope.launch {
            repository.deleteShoppingList(listId)
            // אין צורך לקרוא ל-refresh, כי LiveData מתעדכן לבד מ-ROOM
        }
    }

    // הוספת משתתף לרשימה קיימת
    fun addParticipantToList(listId: String, participantEmail: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addParticipant(listId, participantEmail)
            // אין צורך לקרוא ל-refresh, כי אנו מעדכנים את ROOM ישירות
            callback(success)
        }
    }

    // רענון רשימות מהפיירבייס (לפי דרישה)
    fun refreshShoppingLists() {
        viewModelScope.launch {
            repository.refreshShoppingLists()
        }
    }
}


