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

        adapter = ShoppingListAdapter(
            listOf(),
            onItemClick = { selectedList ->
                val action = ShoppingListFragmentDirections
                    .actionShoppingListFragmentToShoppingItemsFragment(
                        listId = selectedList.id,
                        listName = selectedList.name
                    )
                findNavController().navigate(action)
            },
            onAddParticipantClick = { selectedList ->
                showAddParticipantDialog(selectedList.id)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddList.setOnClickListener {
            showAddListDialog()
        }

        viewModel.shoppingLists.observe(viewLifecycleOwner) { lists ->
            adapter.updateLists(lists.map { ShoppingList(it.id, it.name, it.owner, it.participants) })
        }
    }

    // ✅ דיאלוג להוספת רשימה חדשה
    private fun showAddListDialog() {
        val input = EditText(requireContext())
        input.hint = "Enter list name"

        AlertDialog.Builder(requireContext())
            .setTitle("Create Shopping List")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val listName = input.text.toString().trim()
                if (listName.isNotEmpty()) {
                    viewModel.createShoppingList(listName)
                } else {
                    Toast.makeText(requireContext(), "⚠ List name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ✅ דיאלוג להוספת משתתף
    private fun showAddParticipantDialog(listId: String) {
        val input = EditText(requireContext())
        input.hint = "Enter participant email"

        AlertDialog.Builder(requireContext())
            .setTitle("Add Participant")
            .setView(input)
            .setPositiveButton("Add") { _, _ ->
                val participantEmail = input.text.toString().trim()
                if (participantEmail.isNotEmpty()) {
                    viewModel.addParticipantToList(listId, participantEmail) { success ->
                        requireActivity().runOnUiThread {
                            if (success) {
                                Toast.makeText(requireContext(), "✅ Participant added!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "❌ User not found!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "⚠ Participant email cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
