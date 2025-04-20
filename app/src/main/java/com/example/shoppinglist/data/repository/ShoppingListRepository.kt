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

    // ✅ מחזיר רק את הרשימות של המשתמש המחובר
    fun getUserShoppingLists(): LiveData<List<ShoppingListEntity>> {
        return currentUserId?.let { shoppingListDao.getUserShoppingLists(it) }
            ?: throw IllegalStateException("User not logged in")
    }

    // ✅ יצירת רשימה עם userId כ- ownerId
    suspend fun createShoppingList(name: String) {
        val newListId = db.push().key ?: return
        val ownerId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val newList = ShoppingListEntity(newListId, name, ownerId = ownerId)

        db.child(newListId).setValue(newList)
        withContext(Dispatchers.IO) {
            shoppingListDao.insertShoppingList(newList)
        }
    }

    // ✅ מחיקת רשימה
    suspend fun deleteShoppingList(listId: String) {
        db.child(listId).removeValue()
        withContext(Dispatchers.IO) {
            val listEntity = shoppingListDao.getListById(listId)
            if (listEntity != null) {
                shoppingListDao.deleteShoppingList(listEntity)
            }
        }
    }

    // ✅ בדיקת קיום משתמש ב-Firebase Realtime Database
    suspend fun checkIfUserExists(email: String): Boolean {
        return try {
            val sanitizedEmail = email.replace(".", ",")
            val snapshot = usersRef.child(sanitizedEmail).get().await()
            snapshot.exists()
        } catch (e: Exception) {
            false
        }
    }

    // ✅ הוספת משתתף
    suspend fun addParticipant(listId: String, participantEmail: String): Boolean {
        val sanitizedEmail = participantEmail.replace(".", ",")

        val snapshot = usersRef.child(sanitizedEmail).get().await()
        if (!snapshot.exists()) return false

        val participantUid = snapshot.child("userId").value as? String ?: return false

        withContext(Dispatchers.IO) {
            val list = shoppingListDao.getListById(listId) ?: return@withContext
            val updatedParticipants = list.participants + (participantUid to true)
            val updatedList = list.copy(participants = updatedParticipants)

            shoppingListDao.updateList(updatedList)
            db.child(listId).child("participants").child(participantUid).setValue(true)
        }

        return true
    }


}
