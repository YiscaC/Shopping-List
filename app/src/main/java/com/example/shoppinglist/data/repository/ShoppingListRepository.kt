package com.example.shoppinglist.data.repository

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ShoppingListRepository(context: Context) {

    private val db = FirebaseDatabase.getInstance().reference.child("shoppingLists")
    private val usersRef = FirebaseDatabase.getInstance().reference.child("users") // 🔹 הוספנו נתיב למשתמשים
    private val shoppingListDao = AppDatabase.getDatabase(context).shoppingListDao()

    // ✅ החזרת כל הרשימות מה-ROOM
    fun getAllShoppingLists(): LiveData<List<ShoppingListEntity>> {
        return shoppingListDao.getAllShoppingLists()
    }

    // ✅ יצירת רשימה
    suspend fun createShoppingList(name: String) {
        val newListId = db.push().key ?: return
        val newList = ShoppingListEntity(newListId, name, owner = "Admin")

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

    // ✅ בדיקת קיום משתמש ב- Firebase **Realtime Database**
    suspend fun checkIfUserExists(email: String): Boolean {
        return try {
            val sanitizedEmail = email.replace(".", ",") // 🔹 טיפול במיילים
            Log.d("FirebaseAuthCheck", "🔍 Checking if user exists: $email")

            val snapshot = usersRef.child(sanitizedEmail).get().await()
            val exists = snapshot.exists()

            Log.d("FirebaseAuthCheck", "✅ User exists: $exists ($email)")
            exists
        } catch (e: Exception) {
            Log.e("FirebaseAuthCheck", "❌ Error checking user: ${e.message}")
            false
        }
    }

    // ✅ הוספת משתתף לרשימה רק אם הוא קיים **ב-Realtime Database**
    suspend fun addParticipant(listId: String, participantEmail: String): Boolean {
        val sanitizedEmail = participantEmail.replace(".", ",") // 🔹 טיפול במיילים

        if (!checkIfUserExists(participantEmail)) {
            Log.e("FirebaseAuthCheck", "❌ User not found: $participantEmail")
            return false
        }

        withContext(Dispatchers.IO) {
            val list = shoppingListDao.getListById(listId) ?: return@withContext
            val updatedParticipants = list.participants + (participantEmail to true)
            val updatedList = list.copy(participants = updatedParticipants)

            shoppingListDao.updateList(updatedList)
            db.child(listId).child("participants").child(sanitizedEmail).setValue(true)
        }
        return true
    }
}
