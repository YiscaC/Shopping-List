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
    val shoppingLists: LiveData<List<ShoppingListEntity>> = repository.getShoppingLists()

    init {
        viewModelScope.launch {
            repository.syncShoppingLists() // ✅ מבצע סנכרון בעת פתיחת האפליקציה
        }
    }

    fun createShoppingList(listName: String) {
        viewModelScope.launch {
            repository.createShoppingList(listName)
        }
    }
}
