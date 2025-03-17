package com.example.shoppinglist.data.repository

import com.example.shoppinglist.data.local.models.ShoppingList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ShoppingListRepository {

    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference.child("shoppingLists")
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun getShoppingLists(callback: (List<ShoppingList>) -> Unit) {
        val user = auth.currentUser ?: return
        db.orderByChild("participants/${user.uid}").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lists = mutableListOf<ShoppingList>()
                    for (listSnapshot in snapshot.children) {
                        val list = listSnapshot.getValue(ShoppingList::class.java)
                        list?.let { lists.add(it) }
                    }
                    callback(lists)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    fun createShoppingList(listName: String, onComplete: () -> Unit) {
        val user = auth.currentUser ?: return
        val listId = db.push().key ?: return

        val newList = ShoppingList(
            id = listId,
            name = listName,
            owner = user.uid,
            participants = mapOf(user.uid to true)
        )

        db.child(listId).setValue(newList)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { onComplete() }
    }
}
