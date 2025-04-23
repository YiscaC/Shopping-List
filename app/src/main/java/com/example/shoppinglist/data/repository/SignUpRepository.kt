package com.example.shoppinglist.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class SignUpRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference

    fun register(
        username: String,
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        Log.d("SignUpRepository", "🔄 Attempting to register user: $email")

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                Log.d("SignUpRepository", "✅ User created successfully, UID: $userId")

                if (userId != null) {
                    saveUserToDatabase(userId, username, email, onSuccess, onError)
                } else {
                    onError("❌ Failed to retrieve user ID")
                }
            }
            .addOnFailureListener { e ->
                Log.e("SignUpRepository", "❌ Error registering user: ${e.message}", e)
                val errorMessage = when {
                    e.message?.contains("email address is already in use") == true ->
                        "⚠ This email is already registered. Try logging in instead."
                    e.message?.contains("The given password is invalid") == true ->
                        "⚠ Password should be at least 6 characters long."
                    else -> e.message ?: "⚠ An error occurred."
                }
                onError(errorMessage)
            }
    }

    private fun saveUserToDatabase(
        userId: String,
        username: String,
        email: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = mapOf(
            "userId" to userId,
            "username" to username,
            "email" to email,
            "profileImageUrl" to "" // אפשר לעדכן את זה אחרי בחירת תמונה
        )

        Log.d("SignUpRepository", "💾 Saving user to Firebase Realtime Database with UID: $userId")

        realtimeDb.child("users").child(userId).setValue(user)
            .addOnSuccessListener {
                Log.d("SignUpRepository", "✅ User saved successfully in Realtime Database")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e("SignUpRepository", "❌ Error saving user to database: ${e.message}", e)
                onError(e.message ?: "An error occurred while saving user data.")
            }
    }
}
