package com.example.shoppinglist.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
            if (!url.isNullOrEmpty()) {
                val file = File(url)
                binding.profileImage.setImageDrawable(null)
                if (file.exists()) {
                    Picasso.get().load(file).into(binding.profileImage)
                } else {
                    Picasso.get().load(url).into(binding.profileImage) // אולי זה URL מ-Firebase
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
            AlertDialog.Builder(requireContext())
                .setTitle("מחיקת משתמש")
                .setMessage("האם את בטוחה שברצונך למחוק את המשתמש?")
                .setPositiveButton("מחק") { _, _ ->
                    viewModel.deleteUserAccount()
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
