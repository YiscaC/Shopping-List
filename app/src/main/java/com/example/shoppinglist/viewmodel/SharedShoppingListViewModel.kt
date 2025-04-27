package com.example.shoppinglist.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedShoppingListViewModel : ViewModel() {

    private val _refreshShoppingLists = MutableLiveData<Unit>()
    val refreshShoppingLists: LiveData<Unit> = _refreshShoppingLists

    fun notifyRefresh() {
        _refreshShoppingLists.value = Unit
    }
}
