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
import com.example.shoppinglist.viewmodel.SharedShoppingListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class ShoppingListFragment : Fragment() {

    private lateinit var binding: FragmentShoppingListBinding
    private val viewModel: ShoppingListViewModel by viewModels()
    private val sharedViewModel: SharedShoppingListViewModel by viewModels()

    private lateinit var adapter: ShoppingListAdapter

    private val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val usersRef = FirebaseDatabase.getInstance().reference.child("users")
    private val participantImages = mutableMapOf<String, String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(requireContext(), "⚠ אין חיבור משתמש. אנא התחבר.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(com.example.shoppinglist.R.id.action_shoppingListFragment_to_loginFragment)
            return
        }

        setupRecyclerView()
        observeShoppingLists()
        viewModel.refreshShoppingLists()
    }

    private fun setupRecyclerView() {
        adapter = ShoppingListAdapter(
            listOf(),
            participantImages,
            onItemClick = { selectedList ->
                val action = ShoppingListFragmentDirections
                    .actionShoppingListFragmentToShoppingItemsFragment(
                        listId = selectedList.id,
                        listName = selectedList.name
                    )
                findNavController().navigate(action)
            },
            onAddParticipantClick = { selectedList ->
                if (hasInternetConnection()) {
                    showAddParticipantDialog(selectedList.id)
                } else {
                    Toast.makeText(requireContext(), "⚠ אין חיבור לאינטרנט. לא ניתן להוסיף משתתף.", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddList.setOnClickListener {
            showAddListDialog()
        }
    }

    private fun observeShoppingLists() {
        viewModel.shoppingLists.observe(viewLifecycleOwner) { lists ->
            val userLists = lists.filter {
                it.ownerId == currentUserId || it.participants.containsKey(currentUserId)
            }

            val shoppingModels = userLists.map {
                ShoppingList(it.id, it.name, it.ownerId, it.participants)
            }

            adapter.updateLists(shoppingModels, participantImages)

            if (hasInternetConnection()) {
                val uidsToFetch = userLists.flatMap { it.participants.keys }.toSet()
                fetchParticipantImages(uidsToFetch) { images ->
                    participantImages.clear()
                    participantImages.putAll(images)
                    adapter.updateLists(shoppingModels, participantImages)
                }
            }
        }
    }

    private fun showAddListDialog() {
        val input = EditText(requireContext())
        input.hint = "הכנס שם רשימה"

        AlertDialog.Builder(requireContext())
            .setTitle("יצירת רשימת קניות")
            .setView(input)
            .setPositiveButton("צור") { _, _ ->
                val listName = input.text.toString().trim()
                if (listName.isNotEmpty()) {
                    viewModel.createShoppingList(listName)
                } else {
                    Toast.makeText(requireContext(), "⚠ שם הרשימה לא יכול להיות ריק", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    private fun showAddParticipantDialog(listId: String) {
        val input = EditText(requireContext())
        input.hint = "הכנס כתובת מייל של משתתף"

        AlertDialog.Builder(requireContext())
            .setTitle("הוספת משתתף")
            .setView(input)
            .setPositiveButton("הוסף") { _, _ ->
                val participantEmail = input.text.toString().trim()
                if (participantEmail.isNotEmpty()) {
                    viewModel.addParticipantToList(listId, participantEmail) { success ->
                        requireActivity().runOnUiThread {
                            if (success) {
                                Toast.makeText(requireContext(), "✅ משתתף נוסף בהצלחה", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "❌ לא נמצא משתמש עם כתובת המייל הזו", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "⚠ כתובת מייל לא יכולה להיות ריקה", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    private fun fetchParticipantImages(uids: Set<String>, callback: (Map<String, String>) -> Unit) {
        val imagesMap = mutableMapOf<String, String>()
        val tasksToWait = uids.size

        if (tasksToWait == 0) {
            callback(imagesMap)
            return
        }

        var completed = 0

        uids.forEach { uid ->
            usersRef.child(uid).child("profileImageUrl").get()
                .addOnSuccessListener { snapshot ->
                    val url = snapshot.value as? String
                    if (!url.isNullOrBlank()) {
                        imagesMap[uid] = url
                    }
                }
                .addOnCompleteListener {
                    completed++
                    if (completed == tasksToWait) {
                        callback(imagesMap)
                    }
                }
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = context?.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
