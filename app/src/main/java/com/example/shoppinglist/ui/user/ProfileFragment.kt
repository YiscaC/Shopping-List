package com.example.shoppinglist.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.shoppinglist.R
import com.example.shoppinglist.databinding.FragmentProfileBinding
import com.example.shoppinglist.viewmodel.ProfileViewModel
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.io.File

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.loadUserData()

        viewModel.username.observe(viewLifecycleOwner) { name ->
            binding.helloUsernameText.text = "שלום, ${name}!"
        }

        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            binding.profileImage.setImageDrawable(null)

            if (!url.isNullOrEmpty()) {
                val file = File(url)
                val isLocal = file.exists() && url.startsWith("/data")

                if (isLocal) {
                    Picasso.get().load(file).into(binding.profileImage)
                } else {
                    Picasso.get().load(url).into(binding.profileImage)
                }
            } else {
                binding.profileImage.setImageResource(R.drawable.default_profile)
            }
        }

        binding.btnEditProfile.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.loginFragment)
        }

        binding.btnDeleteAccount.setOnClickListener {
            val email = viewModel.email.value
            if (email.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "שגיאה: לא נמצא אימייל", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val input = EditText(requireContext())
            input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

            AlertDialog.Builder(requireContext())
                .setTitle("מחיקת משתמש")
                .setMessage("אנא הזן את הסיסמה שלך לאימות")
                .setView(input)
                .setPositiveButton("אישור") { _, _ ->
                    val password = input.text.toString().trim()
                    if (password.isNotEmpty()) {
                        viewModel.deleteUserAccountWithReAuth(email, password)
                    } else {
                        Toast.makeText(requireContext(), "חובה להזין סיסמה", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("ביטול", null)
                .show()
        }

        viewModel.deleteSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigate(R.id.loginFragment)
            } else {
                Toast.makeText(requireContext(), "מחיקה נכשלה", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
