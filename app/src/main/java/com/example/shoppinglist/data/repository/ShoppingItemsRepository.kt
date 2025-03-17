package com.example.shoppinglist.data.repository

import com.example.shoppinglist.data.local.models.ShoppingItem
import com.google.firebase.database.*

class ShoppingItemsRepository {

    private val db: DatabaseReference = FirebaseDatabase.getInstance().reference.child("shoppingLists")

    fun loadShoppingItems(listId: String, callback: (List<ShoppingItem>) -> Unit) {
        db.child(listId).child("items").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = mutableListOf<ShoppingItem>()
                for (itemSnapshot in snapshot.children) {
                    val item = itemSnapshot.getValue(ShoppingItem::class.java)
                    item?.let { items.add(it) }
                }
                callback(items)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun updateItemPurchased(listId: String, itemId: String, isChecked: Boolean) {
        db.child(listId).child("items").child(itemId).child("purchased").setValue(isChecked)
    }

    fun updateItemQuantity(listId: String, itemId: String, newQuantity: Int) {
        db.child(listId).child("items").child(itemId).child("quantity").setValue(newQuantity)
    }

    fun addCommentToItem(listId: String, itemId: String, comment: String) {
        db.child(listId).child("items").child(itemId).child("comments").push().setValue(comment)
    }

    fun updateItemImage(listId: String, itemId: String, imageUrl: String) {
        db.child(listId).child("items").child(itemId).child("imageUrl").setValue(imageUrl)
    }

    fun addItemToFirebase(listId: String, itemName: String) {
        val newItemId = db.child(listId).child("items").push().key ?: return
        val newItem = ShoppingItem(id = newItemId, name = itemName, quantity = 1, purchased = false)
        db.child(listId).child("items").child(newItemId).setValue(newItem)
    }
}
