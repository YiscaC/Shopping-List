package com.example.shoppinglist.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.UserEntity
import com.example.shoppinglist.data.local.dao.ShoppingListDao
import com.example.shoppinglist.data.local.dao.UserDao
import com.example.shoppinglist.data.local.models.ShoppingList
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParticipantsViewModel(
    private val userDao: UserDao,
    private val shoppingListDao: ShoppingListDao
) : ViewModel() {

    fun getShoppingListById(listId: String, onComplete: (ShoppingList?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val localList = shoppingListDao.getListById(listId)
            if (localList != null) {
                withContext(Dispatchers.Main) {
                    onComplete(localList.toShoppingList())
                }
            } else {
                val ref = FirebaseDatabase.getInstance().getReference("shoppingLists").child(listId)

                ref.get().addOnSuccessListener { snapshot ->
                    try {
                        val list = snapshot.getValue(ShoppingList::class.java)
                        onComplete(list)
                    } catch (e: Exception) {
                        Log.e("ParticipantsViewModel", "\u274C \u05e9\u05d2\u05d9\u05d0\u05d4 \u05d1\u05e4\u05e2\u05e0\u05d5\u05d7 ShoppingList", e)
                        onComplete(null)
                    }
                }.addOnFailureListener { error ->
                    Log.e("ParticipantsViewModel", "\u274C \u05e9\u05d2\u05d9\u05d0\u05d4 \u05d1\u05e9\u05dc\u05d9\u05e4\u05ea \u05d4\u05e0\u05ea\u05d5\u05e0\u05d9\u05dd", error)
                    onComplete(null)
                }
            }
        }
    }

    fun getParticipantsOnce(uids: List<String>, onComplete: (List<UserEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = userDao.getUsersByUidsOnce(uids)
            withContext(Dispatchers.Main) {
                onComplete(users)
            }
        }
    }

    fun getShoppingListFromLocal(listId: String, onComplete: (ShoppingListEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = shoppingListDao.getListById(listId)
            withContext(Dispatchers.Main) {
                onComplete(entity)
            }
        }
    }

    fun fetchUsersFromFirebase(uids: List<String>, onComplete: (List<UserEntity>) -> Unit) {
        val dbRef = FirebaseDatabase.getInstance().getReference("users")
        val usersList = mutableListOf<UserEntity>()

        var completedCount = 0
        if (uids.isEmpty()) {
            onComplete(emptyList())
            return
        }

        uids.forEach { uid ->
            dbRef.child(uid).get().addOnSuccessListener { snapshot ->
                val username = snapshot.child("username").getValue(String::class.java) ?: ""
                val firstName = snapshot.child("firstName").getValue(String::class.java) ?: ""
                val phone = snapshot.child("phone").getValue(String::class.java) ?: ""
                val remoteProfileImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)

                val user = UserEntity(
                    uid = uid,
                    username = username,
                    firstName = firstName,
                    phone = phone,
                    localProfileImagePath = null,
                    remoteProfileImageUrl = remoteProfileImageUrl
                )

                usersList.add(user)

                viewModelScope.launch(Dispatchers.IO) {
                    userDao.insertUser(user)
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

    fun removeParticipant(listId: String, uidToRemove: String, onComplete: (Boolean) -> Unit) {
        val listRef = FirebaseDatabase.getInstance().getReference("shoppingLists").child(listId)

        listRef.child("participants").child(uidToRemove).removeValue()
            .addOnSuccessListener {
                Log.d("ParticipantsViewModel", "\u2705 \u05d4\u05de\u05e9\u05ea\u05ea\u05e3 \u05d4\u05d5\u05e1\u05e8 \u05de\u05d4\u05c0Firebase")

                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        userDao.deleteUserById(uidToRemove)

                        val currentList = shoppingListDao.getListById(listId)

                        if (currentList != null) {
                            val updatedParticipants = currentList.participants.toMutableMap()
                            updatedParticipants.remove(uidToRemove)

                            val updatedList = currentList.copy(participants = updatedParticipants)
                            shoppingListDao.updateList(updatedList)
                        }

                        withContext(Dispatchers.Main) {
                            onComplete(true)
                        }
                    } catch (e: Exception) {
                        Log.e("ParticipantsViewModel", "\u274C \u05e9\u05d2\u05d9\u05d0\u05d4 \u05d1\u05e2\u05d3\u05db\u05d5\u05df Room", e)
                        withContext(Dispatchers.Main) {
                            onComplete(false)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ParticipantsViewModel", "\u274C \u05e9\u05d2\u05d9\u05d0\u05d4 \u05d1\u05de\u05d7\u05d9\u05e7\u05ea \u05de\u05e9\u05ea\u05ea\u05e3 \u05de\u05d4\u05c0Firebase", e)
                onComplete(false)
            }
    }
}

private fun ShoppingListEntity.toShoppingList(): ShoppingList {
    return ShoppingList(
        id = this.id,
        name = this.name,
        ownerId = this.ownerId,
        participants = this.participants
    )
}
