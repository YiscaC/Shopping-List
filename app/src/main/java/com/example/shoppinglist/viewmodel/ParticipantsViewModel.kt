package com.example.shoppinglist.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.UserEntity
import com.example.shoppinglist.data.local.dao.ShoppingListDao
import com.example.shoppinglist.data.local.dao.UserDao
import com.example.shoppinglist.data.local.models.ShoppingList
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParticipantsViewModel(
    private val userDao: UserDao,
    private val shoppingListDao: ShoppingListDao // ⬅ הוספה של הדאו של רשימות קניות!
) : ViewModel() {

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

    fun getParticipantsOnce(uids: List<String>, onComplete: (List<UserEntity>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val users = userDao.getUsersByUidsOnce(uids)
            withContext(Dispatchers.Main) {
                onComplete(users)
            }
        }
    }

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
                 Log.d("ParticipantsViewModel", "✅ המשתתף הוסר מה־Firebase")

                 viewModelScope.launch(Dispatchers.IO) {
                     try {
                         // מוחק מהטבלה של משתמשים
                         userDao.deleteUserById(uidToRemove)

                         // נטפל גם ברשימת הקניות
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
                         Log.e("ParticipantsViewModel", "❌ שגיאה בעדכון Room", e)
                         withContext(Dispatchers.Main) {
                             onComplete(false)
                         }
                     }
                 }
             }
             .addOnFailureListener { e ->
                 Log.e("ParticipantsViewModel", "❌ שגיאה במחיקת משתתף מה־Firebase", e)
                 onComplete(false)
             }
     }





 }
