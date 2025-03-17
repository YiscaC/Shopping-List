package com.example.shoppinglist.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun register(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d("SignUpRepository", "Attempting to register user: $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                Log.d("SignUpRepository", "User created successfully, UID: $userId")

                if (userId != null) {
                    saveUserToFirestore(userId, username, email, onSuccess, onError)
                } else {
                    onError("Failed to retrieve user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e("SignUpRepository", "Error registering user: ${e.message}", e)
                val errorMessage = when {
                    e.message?.contains("email address is already in use") == true ->
                        "This email is already registered. Try logging in instead."
                    e.message?.contains("The given password is invalid") == true ->
                        "Password should be at least 6 characters long."
                    else -> e.message ?: "An error occurred."
                }
                onError(errorMessage)
            }
    }

    private fun saveUserToFirestore(
        userId: String,
        username: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = hashMapOf(
            "username" to username,
            "email" to email
        )

        Log.d("SignUpRepository", "Saving user to Firestore: $userId")

        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                Log.d("SignUpRepository", "User saved to Firestore successfully")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("SignUpRepository", "Error saving user to Firestore: ${e.message}", e)
                onError(e.message ?: "An error occurred while saving user data.")
            }
    }
}
