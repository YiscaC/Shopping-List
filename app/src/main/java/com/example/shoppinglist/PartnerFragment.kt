package com.example.shoppinglist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth

class PartnerFragment : Fragment() {
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_partner, container, false)

        auth = FirebaseAuth.getInstance()

        // כפתור התנתקות
        val logoutButton = view.findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            auth.signOut()
            val navController = findNavController()
            navController.popBackStack(R.id.loginFragment, false)
            navController.navigate(R.id.loginFragment)
        }

        // כפתור מעבר לפרופיל
        val profileButton = view.findViewById<Button>(R.id.btnProfile)
        profileButton.setOnClickListener {
            findNavController().navigate(R.id.action_partnerFragment_to_profileFragment)
        }

        // ✅ כפתור מעבר לרשימת הקניות
        val shoppingListButton = view.findViewById<Button>(R.id.btnGoToShoppingList)
        shoppingListButton.setOnClickListener {
            findNavController().navigate(R.id.action_partnerFragment_to_shoppingListFragment)
        }

        return view
    }
}
