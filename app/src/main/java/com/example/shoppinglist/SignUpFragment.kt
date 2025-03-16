package com.example.shoppinglist

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val usernameInput = view.findViewById<EditText>(R.id.etUsername)
        val emailInput = view.findViewById<EditText>(R.id.etEmailSignUp)
        val passwordInput = view.findViewById<EditText>(R.id.etPasswordSignUp)
        val confirmPasswordInput = view.findViewById<EditText>(R.id.etConfirmPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnSignUp)
        val loginTextView = view.findViewById<TextView>(R.id.tvLogin)

        // טקסט מעבר לדף התחברות
        val spannable = SpannableString("Already have an account? Login")
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
            }
            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireContext(), R.color.green)
                ds.isUnderlineText = false
            }
        }
        spannable.setSpan(clickableSpan, 25, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        loginTextView.text = spannable
        loginTextView.movementMethod = LinkMovementMethod.getInstance()

        signUpButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password != confirmPassword) {
                    showAlert("Password Mismatch", "Passwords do not match. Please try again.")
                } else if (isInternetAvailable()) {
                    register(username, email, password)
                } else {
                    showAlert("No Internet", "No internet connection. Try again later.")
                }
            } else {
                showAlert("Missing Fields", "Please enter all fields.")
            }
        }

        return view
    }

    private fun register(username: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val userId = result.user?.uid
                if (userId != null) {
                    saveUserToFirestore(userId, username, email)
                }
            }
            .addOnFailureListener { e ->
                when {
                    e.message?.contains("email address is already in use") == true ->
                        showAlert("Email Exists", "This email is already registered. Try logging in instead.")
                    e.message?.contains("The given password is invalid") == true ->
                        showAlert("Weak Password", "Password should be at least 6 characters long.")
                    else -> showAlert("Registration Failed", e.message ?: "An error occurred.")
                }
            }
    }

    private fun saveUserToFirestore(userId: String, username: String, email: String) {
        val user = hashMapOf(
            "username" to username,
            "email" to email
        )
        db.collection("users").document(userId).set(user)
            .addOnSuccessListener {
                Log.d("Firestore", "User added successfully")
                Toast.makeText(requireContext(), "User registered successfully", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_signUpFragment_to_partnerFragment)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding user", e)
                showAlert("Firestore Error", e.message ?: "An error occurred while saving user data.")
            }
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
