package com.example.shoppinglist.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ShoppingListRepository(context: Context) {

    private val db = FirebaseDatabase.getInstance().reference.child("shoppingLists")
    private val usersRef = FirebaseDatabase.getInstance().reference.child("users")
    private val shoppingListDao = AppDatabase.getDatabase(context).shoppingListDao()
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    fun getUserShoppingLists(): LiveData<List<ShoppingListEntity>> {
        return currentUserId?.let { shoppingListDao.getUserShoppingLists(it) }
            ?: throw IllegalStateException("User not logged in")
    }

    suspend fun createShoppingList(name: String) {
        val newListId = db.push().key ?: return
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val initialParticipants = mapOf(ownerId to true) // ✅ הבעלים הוא גם משתתף
        val newList = ShoppingListEntity(newListId, name, ownerId, initialParticipants)

        db.child(newListId).setValue(newList)
        withContext(Dispatchers.IO) {
            shoppingListDao.insertShoppingList(newList)
        }
    }

    suspend fun deleteShoppingList(listId: String) {
        db.child(listId).removeValue()
        withContext(Dispatchers.IO) {
            val list = shoppingListDao.getListById(listId)
            if (list != null) shoppingListDao.deleteShoppingList(list)
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
        if (participantEmail == currentUserEmail) return false // ❌ לא מוסיפים את עצמנו

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
            db.child(listId).child("participants").child(participantUid).setValue(true)
        }

        return true
    }
}
