package com.example.shoppinglist.ui.signup

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shoppinglist.R
import com.example.shoppinglist.viewmodel.SignUpViewModel

class SignUpFragment : Fragment() {

    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_sign_up, container, false)

        val usernameInput = view.findViewById<EditText>(R.id.etUsername)
        val emailInput = view.findViewById<EditText>(R.id.etEmailSignUp)
        val passwordInput = view.findViewById<EditText>(R.id.etPasswordSignUp)
        val confirmPasswordInput = view.findViewById<EditText>(R.id.etConfirmPassword)
        val signUpButton = view.findViewById<Button>(R.id.btnSignUp)
        val loginTextView = view.findViewById<TextView>(R.id.tvLogin)

        // טקסט מעבר לדף התחברות בעברית
        val spannable = SpannableString("כבר יש לך חשבון? התחבר")
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
        // המילה "התחבר" מתחילה אחרי "כבר יש לך חשבון? " שזה 18 תווים
        spannable.setSpan(clickableSpan, 18, spannable.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        loginTextView.text = spannable
        loginTextView.movementMethod = LinkMovementMethod.getInstance()

        signUpButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            if (username.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password != confirmPassword) {
                    showAlert("סיסמאות לא תואמות", "הסיסמאות אינן זהות. נסה שוב.")
                } else if (isInternetAvailable()) {
                    viewModel.register(username, email, password)
                } else {
                    showAlert("אין חיבור אינטרנט", "אין חיבור לאינטרנט. נסה שוב מאוחר יותר.")
                }
            } else {
                showAlert("שדות חסרים", "אנא מלא את כל השדות.")
            }
        }

        observeViewModel()

        return view
    }

    private fun observeViewModel() {
        viewModel.signUpSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "נרשמת בהצלחה", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_signUpFragment_to_shoppingListFragment)
            }
        }

        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                showAlert("ההרשמה נכשלה", it)
            }
        }
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("אישור") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
