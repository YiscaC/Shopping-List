package com.example.shoppinglist.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.UserEntity
import com.example.shoppinglist.data.local.dao.UserDao
import com.example.shoppinglist.data.local.models.ShoppingList
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParticipantsViewModel(
    private val userDao: UserDao
) : ViewModel() {

    // מביא את רשימת הקניות מ-Firebase לפי listId
    fun getShoppingListById(listId: String, onComplete: (ShoppingList?) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("shoppingLists").child(listId)

        ref.get().addOnSuccessListener { snapshot ->
            try {
                val list = snapshot.getValue(ShoppingList::class.java)
                onComplete(list)
            } catch (e: Exception) {
                Log.e("ParticipantsViewModel", "❌ שגיאה בפענוח ShoppingList", e)
                onComplete(null)
            }
        }.addOnFailureListener { error ->
            Log.e("ParticipantsViewModel", "❌ שגיאה בשליפת הנתונים", error)
            onComplete(null)
        }
    }

    // מביא את המשתתפים מתוך Room לפי UID
    fun getParticipantsOnce(uids: List<String>, onComplete: (List<UserEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = userDao.getUsersByUidsOnce(uids)
            withContext(Dispatchers.Main) {
                onComplete(users)
            }
        }
    }

    // מביא את המשתמשים מ-Firebase לפי UID ושומר אותם ל-Room
    fun fetchUsersFromFirebase(
        uids: List<String>,
        onComplete: (List<UserEntity>) -> Unit
    ) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        val usersList = mutableListOf<UserEntity>()

        var completedCount = 0
        if (uids.isEmpty()) {
            onComplete(emptyList())
            return
        }

        uids.forEach { uid ->
            dbRef.child(uid).get().addOnSuccessListener { snapshot ->
                val user = snapshot.getValue(UserEntity::class.java)
                if (user != null) {
                    usersList.add(user)

                    // שמירה ל־Room
                    viewModelScope.launch(Dispatchers.IO) {
                        userDao.insertUser(user)
                    }
                }
                completedCount++
                if (completedCount == uids.size) {
                    onComplete(usersList)
                }
            }.addOnFailureListener {
                completedCount++
                if (completedCount == uids.size) {
                    onComplete(usersList)
                }
            }
        }
    }

    // מסיר משתתף מתוך Firebase ואז מפעיל onComplete
    fun removeParticipant(listId: String, uidToRemove: String, onComplete: () -> Unit) {
        val ref = FirebaseDatabase.getInstance()
            .getReference("shoppingLists")
            .child(listId)
            .child("participants")

        ref.child(uidToRemove).removeValue().addOnCompleteListener {
            onComplete()
        }
    }
}
