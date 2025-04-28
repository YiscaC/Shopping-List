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

    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference.child("shoppingLists")
    private val shoppingListDao = AppDatabase.getDatabase(context).shoppingListDao()

    private val userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    // ✅ מביא רק מה-Room
    fun getUserShoppingLists(): LiveData<List<ShoppingListEntity>> {
        return shoppingListDao.getUserShoppingLists(userId)
    }

    // ✅ מרענן מהפיירבייס ושומר ל-ROOM
    suspend fun refreshShoppingLists() {
        try {
            val snapshot = db.get().await()
            val lists = mutableListOf<ShoppingListEntity>()

            for (listSnapshot in snapshot.children) {
                val id = listSnapshot.key ?: continue
                val name = listSnapshot.child("name").getValue(String::class.java).orEmpty()
                val ownerId = listSnapshot.child("ownerId").getValue(String::class.java).orEmpty()

                val participantsMap = mutableMapOf<String, Boolean>()
                val participantsSnapshot = listSnapshot.child("participants")
                for (participantSnapshot in participantsSnapshot.children) {
                    val participantId = participantSnapshot.key.orEmpty()
                    val isActive = participantSnapshot.getValue(Boolean::class.java) ?: true
                    participantsMap[participantId] = isActive
                }

                val shoppingList = ShoppingListEntity(
                    id = id,
                    name = name,
                    ownerId = ownerId,
                    participants = participantsMap
                )
                lists.add(shoppingList)
            }

            withContext(Dispatchers.IO) {
                shoppingListDao.insertShoppingLists(lists)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ✅ יצירת רשימה חדשה - גם ב-Firebase וגם ב-Room
    suspend fun createShoppingList(name: String) {
        val newListId = db.push().key ?: return
        val newList = ShoppingListEntity(
            id = newListId,
            name = name,
            ownerId = userId,
            participants = mapOf(userId to true)
        )

        withContext(Dispatchers.IO) {
            shoppingListDao.insertShoppingList(newList)
        }

        db.child(newListId).setValue(newList)
    }// ✅ מביא רשימה ספציפית לפי מזהה מ-ROOM
    suspend fun getShoppingListById(listId: String): ShoppingListEntity? {
        return withContext(Dispatchers.IO) {
            shoppingListDao.getListById(listId)
        }
    }


    // ✅ הוספת משתתף
    suspend fun addParticipant(listId: String, participantUid: String): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val list = shoppingListDao.getListById(listId) ?: return@withContext false

                val updatedParticipants = list.participants.toMutableMap().apply {
                    this[participantUid] = true
                }

                val updatedList = list.copy(participants = updatedParticipants)
                shoppingListDao.updateList(updatedList)
            }

            db.child(listId).child("participants").child(participantUid).setValue(true)
            true
        } catch (e: Exception) {
            false
        }
    }
}
