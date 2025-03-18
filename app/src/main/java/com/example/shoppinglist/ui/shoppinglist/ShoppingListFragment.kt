package com.example.shoppinglist.ui.shoppinglist.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentShoppingListBinding
import com.example.shoppinglist.ui.adapter.ShoppingListAdapter
import com.example.shoppinglist.viewmodel.ShoppingListViewModel
import com.example.shoppinglist.data.local.models.ShoppingList

class ShoppingListFragment : Fragment() {

    private lateinit var binding: FragmentShoppingListBinding
    private val viewModel: ShoppingListViewModel by viewModels()
    private lateinit var adapter: ShoppingListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ShoppingListAdapter(listOf()) { selectedList ->
            val action = ShoppingListFragmentDirections
                .actionShoppingListFragmentToShoppingItemsFragment(
                    listId = selectedList.id,
                    listName = selectedList.name
                )
            findNavController().navigate(action)
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddList.setOnClickListener {
            showAddListDialog()
        }

        // ✅ הצגת הנתונים מ-Room, והם יתעדכנו כש-Firebase מסתנכרן
        viewModel.shoppingLists.observe(viewLifecycleOwner) { lists ->
            val shoppingLists = lists.map { ShoppingList(it.id, it.name, it.owner, emptyMap()) }
            adapter.updateLists(shoppingLists) }
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
                viewModel.createShoppingList(listName)
            } else {
                Toast.makeText(requireContext(), "List name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.setNegativeButton("Cancel", null)
        dialog.show()
    }
}
