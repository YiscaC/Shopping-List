package com.example.shoppinglist.ui.shoppinglist.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentParticipantsBinding
import com.example.shoppinglist.ui.adapter.ParticipantsAdapter
import com.example.shoppinglist.viewmodel.ParticipantsViewModel
import com.example.shoppinglist.viewmodel.ParticipantsViewModelFactory

class ParticipantsFragment : Fragment() {

    private lateinit var binding: FragmentParticipantsBinding
    private val args: ParticipantsFragmentArgs by navArgs()
    private val viewModel: ParticipantsViewModel by viewModels {
        ParticipantsViewModelFactory(requireContext())
    }

    private lateinit var adapter: ParticipantsAdapter
    private lateinit var listId: String
    private lateinit var ownerId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentParticipantsBinding.inflate(inflater, container, false)
        listId = args.listId
        Log.d("ParticipantsFragment", "✅ listId = $listId")

        setupRecyclerView()
        loadParticipants()

        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = ParticipantsAdapter(
            currentOwnerId = "", // נעדכן את זה מאוחר יותר
            onDeleteClicked = { uid ->
                viewModel.removeParticipant(listId, uid) {
                    Toast.makeText(requireContext(), "🗑️ המשתתף הוסר", Toast.LENGTH_SHORT).show()
                    loadParticipants() // טען מחדש אחרי מחיקה
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
            ownerId = shoppingList.owner

            // נעדכן את האדפטר עם ה־owner
            adapter.currentOwnerId = ownerId

            // נשלוף את המשתמשים מה־Firebase ונשמור ל־Room
            viewModel.fetchUsersFromFirebase(uids) { users ->
                adapter.submitList(users)
            }
        }
    }
}
