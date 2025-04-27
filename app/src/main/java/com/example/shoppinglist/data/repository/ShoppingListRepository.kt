
package com.example.shoppinglist.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ShoppingListRepository(private val context: Context) {

    private val dbRef = FirebaseDatabase.getInstance().reference.child("shoppingLists")
    private val usersRef = FirebaseDatabase.getInstance().reference.child("users")
    private val shoppingListDao = AppDatabase.getDatabase(context).shoppingListDao()
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid
    private val repositoryScope = CoroutineScope(Dispatchers.IO)

    fun getUserShoppingLists(): LiveData<List<ShoppingListEntity>> {
        return currentUserId?.let { shoppingListDao.getUserShoppingLists(it) }
            ?: throw IllegalStateException("משתמש לא מחובר")
    }

    suspend fun createShoppingList(name: String) {
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // יוצרים רשימה חדשה עם מזהה זמני (לטובת Room)
        val tempId = System.currentTimeMillis().toString()
        val initialParticipants = mapOf(ownerId to true)
        val newList = ShoppingListEntity(tempId, name, ownerId, initialParticipants)

        // שמירה מיידית ב-ROOM
        withContext(Dispatchers.IO) {
            shoppingListDao.insertShoppingList(newList)
        }

        try {
            // ניסיון שמירה בפיירבייס
            val newListId = dbRef.push().key ?: return
            val firebaseList = newList.copy(id = newListId)

            dbRef.child(newListId).setValue(firebaseList).await()

            // עידכון ה-ROOM עם ה-ID האמיתי מהפיירבייס
            withContext(Dispatchers.IO) {
                shoppingListDao.deleteShoppingList(newList)
                shoppingListDao.insertShoppingList(firebaseList)
            }
        } catch (e: Exception) {
            Log.e("ShoppingListRepository", "\uD83D\uDEAB שגיאה בשמירה לפיירבייס: ${e.message}")
        }
    }

    suspend fun deleteShoppingList(listId: String) {
        // מחיקה מה-ROOM תמיד
        withContext(Dispatchers.IO) {
            val list = shoppingListDao.getListById(listId)
            if (list != null) shoppingListDao.deleteShoppingList(list)
        }

        // מחיקה מפיירבייס אם יש אינטרנט
        try {
            dbRef.child(listId).removeValue().await()
        } catch (e: Exception) {
            Log.e("ShoppingListRepository", "\uD83D\uDEAB שגיאה במחיקת רשימה מפיירבייס: ${e.message}")
        }
    }

    suspend fun checkIfUserExists(email: String): Boolean {
        return try {
            val sanitized = email.replace(".", ",")
            val snap = usersRef.child(sanitized).get().await()
            snap.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun addParticipant(listId: String, participantEmail: String): Boolean {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email ?: return false
        if (participantEmail == currentUserEmail) return false // \u274C לא מוסיפים את עצמנו

        return try {
            val allUsers = usersRef.get().await()
            var participantUid: String? = null

            for (user in allUsers.children) {
                val email = user.child("email").value as? String
                if (email == participantEmail) {
                    participantUid = user.key
                    break
                }
            }

            if (participantUid == null) return false

            withContext(Dispatchers.IO) {
                val list = shoppingListDao.getListById(listId) ?: return@withContext
                val updated = list.participants + (participantUid to true)
                val updatedList = list.copy(participants = updated)

                shoppingListDao.updateList(updatedList)
            }

            dbRef.child(listId).child("participants").child(participantUid).setValue(true).await()
            true

        } catch (e: Exception) {
            Log.e("ShoppingListRepository", "\uD83D\uDEAB שגיאה בהוספת משתתף: ${e.message}")
            false
        }
    }

    fun refreshShoppingLists() {
        repositoryScope.launch {
            fetchShoppingListsFromFirebase()
        }
    }

    private suspend fun fetchShoppingListsFromFirebase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        try {
            val snapshot = dbRef.get().await()
            val shoppingLists = mutableListOf<ShoppingListEntity>()

            for (child in snapshot.children) {
                val list = child.getValue(ShoppingListEntity::class.java)
                if (list != null && (list.ownerId == uid || list.participants.containsKey(uid))) {
                    shoppingLists.add(list)
                }
            }

            // עדכון הנתונים ב-ROOM
            withContext(Dispatchers.IO) {
                shoppingListDao.insertShoppingLists(shoppingLists)
            }

        } catch (e: Exception) {
            Log.e("ShoppingListRepository", "\uD83D\uDEAB שגיאה בטעינת רשימות מהפיירבייס: ${e.message}")
        }
    }
}



