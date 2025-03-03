package com.example.shoppinglist

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.navigation.fragment.findNavController
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class LoginFragment : Fragment() {

    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        // אתחול Firestore
        db = FirebaseFirestore.getInstance()
        db.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false) // מונע אחסון נתונים במטמון ומכריח שליחה מיידית לשרת
            .build()

        val emailInput = view.findViewById<EditText>(R.id.etEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etPassword)
        val registerButton = view.findViewById<Button>(R.id.btnLogin)

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (isInternetAvailable()) {
                    registerUser(email, password)
                } else {
                    Toast.makeText(requireContext(), "No internet connection. Try again later.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please enter all fields", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }

    private fun registerUser(email: String, password: String) {
        val user = hashMapOf(
            "email" to email,
            "password" to password
        )

        val userRef = db.collection("users").document(email) // יצירת מסמך עם ID קבוע לפי email

        userRef.set(user) // שימוש ב-set במקום add להימנע מעיכוב יצירת ID
            .addOnSuccessListener {
                Log.d("Firestore", "User added successfully")
                Toast.makeText(requireContext(), "User registered successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_partnerFragment) // ניווט רק לאחר הצלחה
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding user", e)
                Toast.makeText(requireContext(), "Registration failed", Toast.LENGTH_SHORT).show()
            }
    }

    // פונקציה לבדוק אם יש חיבור לאינטרנט לפני שליחה
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
