package com.example.shoppinglist.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.shoppinglist.data.local.AppDatabase
import com.example.shoppinglist.data.local.models.ShoppingListEntity
import com.example.shoppinglist.data.local.models.ShoppingList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShoppingListRepository(context: Context) {

    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference.child("shoppingLists")
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val shoppingListDao = AppDatabase.getDatabase(context).shoppingListDao()

    // ✅ משיכת הנתונים מ-Room (אם אין אינטרנט, עדיין ניתן לראות את הנתונים השמורים)
    fun getShoppingLists(): LiveData<List<ShoppingListEntity>> {
        return shoppingListDao.getAllShoppingLists()
    }

    // ✅ יצירת רשימה חדשה ושמירה גם ב-Room וגם ב-Firebase
    suspend fun createShoppingList(listName: String) {
        val user = auth.currentUser ?: return
        val listId = db.push().key ?: return

        val newList = ShoppingList(
            id = listId,
            name = listName,
            owner = user.uid,
            participants = mapOf(user.uid to true)
        )

        db.child(listId).setValue(newList)

        // ✅ שמירה ב-Room
        withContext(Dispatchers.IO) {
            shoppingListDao.insertShoppingList(ShoppingListEntity(listId, listName, user.uid))
        }
    }

    // ✅ סנכרון הנתונים מ-Firebase ל-Room (פעם אחת בהפעלת האפליקציה)
    fun syncShoppingLists() {
        val user = auth.currentUser ?: return
        db.orderByChild("participants/${user.uid}").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lists = mutableListOf<ShoppingListEntity>()
                    for (listSnapshot in snapshot.children) {
                        val list = listSnapshot.getValue(ShoppingList::class.java)
                        list?.let { lists.add(ShoppingListEntity(it.id, it.name, it.owner)) }
                    }

                    // ✅ שמירה ב-Room בתוך קורוטינה
                    CoroutineScope(Dispatchers.IO).launch {
                        lists.forEach { shoppingListDao.insertShoppingList(it) }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }
}
