package com.example.shoppinglist.ui.shoppinglist.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentParticipantsBinding
import com.example.shoppinglist.ui.adapter.ParticipantsAdapter
import com.example.shoppinglist.viewmodel.ParticipantsViewModel
import com.example.shoppinglist.viewmodel.ParticipantsViewModelFactory
import com.example.shoppinglist.viewmodel.SharedShoppingListViewModel

class ParticipantsFragment : Fragment() {

    private lateinit var binding: FragmentParticipantsBinding
    private val args: ParticipantsFragmentArgs by navArgs()
    private val sharedViewModel: SharedShoppingListViewModel by activityViewModels()
    private val viewModel: ParticipantsViewModel by viewModels {
        ParticipantsViewModelFactory(requireContext())
    }

    private lateinit var adapter: ParticipantsAdapter
    private lateinit var listId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentParticipantsBinding.inflate(inflater, container, false)
        listId = args.listId
        setupRecyclerView()
        loadParticipants()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ParticipantsAdapter(
            currentOwnerId = "",
            onDeleteClicked = { uid ->
                viewModel.removeParticipant(listId, uid) { success ->
                    if (success) {
                        Toast.makeText(requireContext(), "🗑️ המשתתף הוסר", Toast.LENGTH_SHORT).show()
                        sharedViewModel.notifyRefresh()
                        loadParticipants()
                    } else {
                        Toast.makeText(requireContext(), "⚠️ שגיאה במחיקה", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        binding.participantsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.participantsRecyclerView.adapter = adapter
    }


    private fun loadParticipants() {
        viewModel.getShoppingListById(listId) { shoppingList ->
            if (shoppingList == null) {
                Toast.makeText(requireContext(), "⚠️ הרשימה לא נמצאה", Toast.LENGTH_SHORT).show()
                return@getShoppingListById
            }

            val uids = shoppingList.participants.keys.toList()
            val ownerId = shoppingList.ownerId

            adapter.currentOwnerId = ownerId
            adapter.notifyDataSetChanged()

            // קודם מציגים מה שיש ברום
            viewModel.getParticipantsOnce(uids) { localUsers ->
                adapter.submitList(localUsers)

                // ואז תמיד מנסים לעדכן מפיירבייס
                viewModel.fetchUsersFromFirebase(uids) { remoteUsers ->
                    adapter.submitList(remoteUsers)
                }
            }
        }
    }


}
