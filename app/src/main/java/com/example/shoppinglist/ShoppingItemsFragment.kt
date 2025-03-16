package com.example.shoppinglist

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentShoppingItemsBinding
import com.example.shoppinglist.models.ShoppingItem
import com.google.firebase.database.*

class ShoppingItemsFragment : Fragment() {

    private lateinit var binding: FragmentShoppingItemsBinding
    private val db: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }
    private val args: ShoppingItemsFragmentArgs by navArgs()
    private val itemsList = mutableListOf<ShoppingItem>()
    private lateinit var adapter: ShoppingItemsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtListName.text = args.listName

        adapter = ShoppingItemsAdapter(
            itemsList,
            onItemClick = { selectedItem ->
                selectedItem.expanded = !selectedItem.expanded
                adapter.notifyDataSetChanged()
            },
            onPurchasedChanged = { selectedItem, isChecked ->
                updateItemPurchased(selectedItem, isChecked)
            },
            onQuantityChanged = { selectedItem, newQuantity ->
                updateItemQuantity(selectedItem, newQuantity)
            },
            onCommentAdded = { selectedItem, comment ->
                addCommentToItem(selectedItem, comment)
            },
            onImageAdded = { selectedItem ->
                uploadImageForItem(selectedItem)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddItem.setOnClickListener {
            showAddItemDialog() // ✅ כפתור "Add Item" יפתח דיאלוג
        }

        loadShoppingItems()
    }

    private fun loadShoppingItems() {
        db.child("shoppingLists").child(args.listId).child("items")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    itemsList.clear()
                    for (itemSnapshot in snapshot.children) {
                        val item = itemSnapshot.getValue(ShoppingItem::class.java)
                        item?.let { itemsList.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load items.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateItemPurchased(item: ShoppingItem, isChecked: Boolean) {
        db.child("shoppingLists").child(args.listId).child("items").child(item.id)
            .child("purchased").setValue(isChecked)
    }

    private fun updateItemQuantity(item: ShoppingItem, newQuantity: Int) {
        db.child("shoppingLists").child(args.listId).child("items").child(item.id)
            .child("quantity").setValue(newQuantity)
    }

    private fun addCommentToItem(item: ShoppingItem, comment: String) {
        db.child("shoppingLists").child(args.listId).child("items").child(item.id)
            .child("comments").push().setValue(comment)
    }

    private fun uploadImageForItem(item: ShoppingItem) {
        // כאן אפשר להוסיף לוגיקה לפתיחת גלריה ולשמור את הקובץ ב-Firebase Storage
    }

    // ✅ דיאלוג להוספת פריט חדש
    private fun showAddItemDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Add Item")

        val input = EditText(requireContext())
        input.hint = "Enter item name"
        dialog.setView(input)

        dialog.setPositiveButton("Add") { _, _ ->
            val itemName = input.text.toString().trim()
            if (itemName.isNotEmpty()) {
                addItemToFirebase(itemName)
            } else {
                Toast.makeText(requireContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    // ✅ הוספת פריט חדש ל-Firebase
    private fun addItemToFirebase(itemName: String) {
        val newItemId = db.child("shoppingLists").child(args.listId).child("items").push().key ?: return

        val newItem = ShoppingItem(
            id = newItemId,
            name = itemName,
            quantity = 1,
            purchased = false
        )

        db.child("shoppingLists").child(args.listId).child("items").child(newItemId).setValue(newItem)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to add item.", Toast.LENGTH_SHORT).show()
            }
    }
}
