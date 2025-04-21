package com.example.shoppinglist.ui.profile

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.shoppinglist.R
import com.example.shoppinglist.databinding.FragmentProfileBinding
import com.example.shoppinglist.viewmodel.ProfileViewModel
import com.squareup.picasso.Picasso
import java.io.File

class ProfileFragment : Fragment() {


    private lateinit var binding: FragmentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var progressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        progressBar = binding.progressBar
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadUserData()

        binding.cameraIcon.setOnClickListener { selectImage() }
        binding.btnSave.setOnClickListener { saveUserData() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteConfirmationDialog() }

        viewModel.username.observe(viewLifecycleOwner, Observer {
            binding.usernameEditText.setText(it)
        })

        viewModel.profileImageUrl.observe(viewLifecycleOwner, Observer { imagePath ->
            if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                Picasso.get().load(File(imagePath)).into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.default_profile)
            }
        })

        viewModel.updateSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) showToast("Profile updated successfully!")
            else showToast("Failed to update profile")
        })

        viewModel.deleteSuccess.observe(viewLifecycleOwner, Observer { success ->
            if (success) findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            else showToast("Failed to delete account")
        })
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                viewModel.saveProfileImage(uri)
            }
        }
    }

    private fun saveUserData() {
        val newUsername = binding.usernameEditText.text.toString().trim()
        if (newUsername.isNotEmpty()) {
            viewModel.updateUsername(newUsername)
        } else {
            showToast("No changes to update")
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.deleteUserAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
