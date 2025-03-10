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
    ): View? {
        val view = inflater.inflate(R.layout.fragment_partner, container, false)

        auth = FirebaseAuth.getInstance()

        val logoutButton = view.findViewById<Button>(R.id.btnLogout)
        logoutButton.setOnClickListener {
            // יוצא מהמשתמש
            auth.signOut()

            // ננקה את ה-Back Stack ונעבור לדף הלוגין
            val navController = findNavController()
            navController.popBackStack(R.id.loginFragment, false) // מנקה את ה-back stack עד לעמוד הלוגין
            navController.navigate(R.id.loginFragment) // נעבור לדף הלוגין
        }

        return view
    }
}
