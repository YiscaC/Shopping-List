package com.example.shoppinglist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.shoppinglist.data.local.models.ShoppingList
import com.example.shoppinglist.data.repository.ShoppingListRepository

class ShoppingListViewModel : ViewModel() {

    private val repository = ShoppingListRepository()
    private val _shoppingLists = MutableLiveData<List<ShoppingList>>()
    val shoppingLists: LiveData<List<ShoppingList>> get() = _shoppingLists

    fun loadShoppingLists() {
        repository.getShoppingLists { lists ->
            _shoppingLists.value = lists
        }
    }

    fun createShoppingList(listName: String) {
        repository.createShoppingList(listName) {
            loadShoppingLists()
        }
    }
}
