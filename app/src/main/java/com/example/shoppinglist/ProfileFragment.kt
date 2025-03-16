package com.example.shoppinglist

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.shoppinglist.databinding.FragmentProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseDatabase by lazy { FirebaseDatabase.getInstance() }
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
        Log.d("ProfileFragment", "ProfileFragment Created")

        loadUserData()

        binding.cameraIcon.setOnClickListener { selectImage() }
        binding.btnSave.setOnClickListener { saveUserData() }
        binding.btnDeleteAccount.setOnClickListener { showDeleteConfirmationDialog() }
    }

    private fun loadUserData() {
        val user = auth.currentUser ?: return
        binding.emailTextView.text = user.email

        db.getReference("users").child(user.uid).child("profileImageUrl").get()
            .addOnSuccessListener { snapshot ->
                val imagePath = snapshot.value as? String
                if (!imagePath.isNullOrEmpty() && File(imagePath).exists()) {
                    Log.d("ProfileFragment", "Loaded image from local path: $imagePath")
                    Picasso.get().load(File(imagePath)).into(binding.profileImage)
                } else {
                    binding.profileImage.setImageResource(R.drawable.default_profile)
                }
            }
            .addOnFailureListener {
                Log.e("ProfileFragment", "Failed to load image from Firebase")
                binding.profileImage.setImageResource(R.drawable.default_profile)
            }
    }

    private fun selectImage() {
        Log.d("ProfileFragment", "Opening gallery...")
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                Log.d("ProfileFragment", "Image selected: $uri")
                saveImageLocally(uri)
            }
        }
    }

    private fun saveImageLocally(uri: Uri) {
        val user = auth.currentUser ?: return
        try {
            val inputStream: InputStream? = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().cacheDir, "profile_image_${user.uid}_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)

            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val savedPath = file.absolutePath
            Log.d("ProfileFragment", "Image saved locally: $savedPath")

            val userRef = db.getReference("users").child(user.uid).child("profileImageUrl")

            // 🔥 **עכשיו זה פשוט מעדכן ישירות בלי למחוק קודם**
            userRef.setValue(savedPath)
                .addOnSuccessListener {
                    Log.d("ProfileFragment", "Updated image path in Firebase: $savedPath")
                    Picasso.get().invalidate(File(savedPath))
                    Picasso.get().load(File(savedPath)).into(binding.profileImage)
                }
                .addOnFailureListener {
                    Log.e("ProfileFragment", "Failed to update image path in Firebase")
                }

        } catch (e: Exception) {
            Log.e("ProfileFragment", "Failed to save image locally", e)
        }
    }


    private fun saveUserData() {
        val user = auth.currentUser ?: return
        val username = binding.usernameEditText.text.toString().trim()

        val userRef = db.getReference("users").child(user.uid)

        val updates = mutableMapOf<String, Any>()
        if (username.isNotEmpty()) {
            updates["username"] = username
        }

        progressBar.visibility = View.VISIBLE

        if (updates.isNotEmpty()) {
            userRef.updateChildren(updates)
                .addOnSuccessListener {
                    showToast("Profile updated successfully!")
                }
                .addOnFailureListener {
                    showToast("Failed to update profile")
                }
                .addOnCompleteListener {
                    progressBar.visibility = View.GONE
                }
        } else {
            progressBar.visibility = View.GONE
            showToast("No changes to update")
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> deleteAccount() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        val user = auth.currentUser ?: return
        progressBar.visibility = View.VISIBLE

        db.getReference("users").child(user.uid).removeValue()
        user.delete().addOnCompleteListener {
            progressBar.visibility = View.GONE
            if (it.isSuccessful) {
                findNavController().navigate(R.id.action_profileFragment_to_loginFragment)
            } else {
                showToast("Failed to delete account")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
