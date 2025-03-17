package com.example.shoppinglist.ui.login

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import android.widget.Toast
import com.example.shoppinglist.R
import com.example.shoppinglist.viewmodel.LoginViewModel

class LoginFragment : Fragment() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        val emailInput = view.findViewById<EditText>(R.id.etEmail)
        val passwordInput = view.findViewById<EditText>(R.id.etPassword)
        val loginButton = view.findViewById<Button>(R.id.btnLogin)
        val signUpTextView = view.findViewById<TextView>(R.id.tvSignUp)

        // הופך את טקסט ההרשמה ללחיץ
        val spannable = SpannableString("Don't have an account? Sign Up")
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.color = ContextCompat.getColor(requireContext(), R.color.green)
                ds.isUnderlineText = false
            }
        }
        spannable.setSpan(clickableSpan, 23, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        signUpTextView.text = spannable
        signUpTextView.movementMethod = LinkMovementMethod.getInstance()

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (isInternetAvailable()) {
                    viewModel.login(email, password)
                } else {
                    showAlert("No Internet", "No internet connection. Try again later.")
                }
            } else {
                showAlert("Missing Fields", "Please enter all fields.")
            }
        }

        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Login successful", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_partnerFragment)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showAlert("Login Failed", it)
            }
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
