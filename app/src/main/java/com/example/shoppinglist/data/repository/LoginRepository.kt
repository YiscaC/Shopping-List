package com.example.shoppinglist.data.repository

import com.google.firebase.auth.FirebaseAuth

class LoginRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                val errorMessage = when {
                    e.message?.contains("no user record") == true -> "No account found with this email."
                    e.message?.contains("password is invalid") == true -> "The password you entered is incorrect."
                    else -> e.message ?: "An error occurred."
                }
                onError(errorMessage)
            }
    }
}
