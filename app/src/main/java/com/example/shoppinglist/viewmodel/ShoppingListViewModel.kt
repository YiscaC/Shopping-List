package com.example.shoppinglist.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import com.example.shoppinglist.data.repository.ShoppingListRepository
import kotlinx.coroutines.launch

class ShoppingListViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ShoppingListRepository(application)

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> get() = _isLoading

    val shoppingLists: LiveData<List<ShoppingListEntity>> = repository.getUserShoppingLists()

    fun refreshShoppingLists() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            repository.refreshShoppingLists()
            _isLoading.postValue(false)
        }
    }

    fun createShoppingList(name: String) {
        viewModelScope.launch {
            repository.createShoppingList(name)
        }
    }

    fun addParticipantToList(listId: String, participantEmail: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.addParticipant(listId, participantEmail)
            onResult(success)
        }
    }

    suspend fun getShoppingListById(listId: String): ShoppingListEntity? {
        return repository.getShoppingListById(listId)
    }
}
