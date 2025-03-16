package com.example.shoppinglist

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentShoppingListBinding
import com.example.shoppinglist.models.ShoppingList
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ShoppingListFragment : Fragment() {

    private lateinit var binding: FragmentShoppingListBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference.child("shoppingLists") }
    private val shoppingLists = mutableListOf<ShoppingList>()
    private lateinit var adapter: ShoppingListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ShoppingListAdapter(shoppingLists) { selectedList ->
            val action = ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingItemsFragment(
                    listId = selectedList.id,
                    listName = selectedList.name
                )
            findNavController().navigate(action) // ✅ נווט למסך הפריטים
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddList.setOnClickListener {
            showAddListDialog()
        }

        loadShoppingLists()
    }

    private fun showAddListDialog() {
        val dialog = AlertDialog.Builder(requireContext())
        dialog.setTitle("Create Shopping List")

        val input = EditText(requireContext())
        input.hint = "Enter list name"
        dialog.setView(input)

        dialog.setPositiveButton("Create") { _, _ ->
            val listName = input.text.toString().trim()
            if (listName.isNotEmpty()) {
                createShoppingList(listName)
            } else {
                Toast.makeText(requireContext(), "List name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }

    private fun createShoppingList(listName: String) {
        val user = auth.currentUser ?: return
        val listId = db.push().key ?: return

        val newList = ShoppingList(
            id = listId,
            name = listName,
            owner = user.uid,
            participants = mapOf(user.uid to true)
        )

        db.child(listId).setValue(newList)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "List created!", Toast.LENGTH_SHORT).show()
                loadShoppingLists()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to create list.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadShoppingLists() {
        val user = auth.currentUser ?: return
        db.orderByChild("participants/${user.uid}").equalTo(true)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    shoppingLists.clear()
                    for (listSnapshot in snapshot.children) {
                        val list = listSnapshot.getValue(ShoppingList::class.java)
                        list?.let { shoppingLists.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load lists.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
