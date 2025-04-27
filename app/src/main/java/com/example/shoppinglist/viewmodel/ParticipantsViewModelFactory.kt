package com.example.shoppinglist.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.shoppinglist.data.local.AppDatabase

class ParticipantsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ParticipantsViewModel::class.java)) {
            val db = AppDatabase.getDatabase(context)
            val userDao = db.userDao()
            val shoppingListDao = db.shoppingListDao()
            return ParticipantsViewModel(userDao, shoppingListDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
