package com.example.shoppinglist.ui.user

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.shoppinglist.databinding.FragmentEditProfileBinding
import com.example.shoppinglist.viewmodel.ProfileViewModel
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

class EditProfileFragment : Fragment() {

    private lateinit var binding: FragmentEditProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    private var tempImageUri: Uri? = null
    private var selectedImageUri: Uri? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "נדרשת הרשאה למצלמה", Toast.LENGTH_SHORT).show()
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            selectedImageUri = tempImageUri
            binding.profileImage.setImageURI(tempImageUri)
        } else {
            Toast.makeText(requireContext(), "צילום נכשל", Toast.LENGTH_SHORT).show()
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            binding.profileImage.setImageURI(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.loadUserData()

        viewModel.username.observe(viewLifecycleOwner) {
            binding.editUsername.setText(it)
        }
        viewModel.firstName.observe(viewLifecycleOwner) {
            binding.editFirstName.setText(it)
        }
        viewModel.phone.observe(viewLifecycleOwner) {
            binding.editPhone.setText(it)
        }
        viewModel.email.observe(viewLifecycleOwner) {
            binding.editEmail.setText(it)
        }
        viewModel.profileImageUrl.observe(viewLifecycleOwner) { url ->
            if (!url.isNullOrEmpty()) {
                val file = File(url)
                if (file.exists()) {
                    Picasso.get().load(file).into(binding.profileImage)
                } else {
                    Picasso.get().load(url).into(binding.profileImage)
                }
            }
        }

        binding.btnCamera.setOnClickListener {
            if (!isInternetAvailable(requireContext())) {
                Toast.makeText(requireContext(), "אין אינטרנט – לא ניתן להשתמש במצלמה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val permission = Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                launchCamera()
            } else {
                cameraPermissionLauncher.launch(permission)
            }
        }

        binding.btnGallery.setOnClickListener {
            if (!isInternetAvailable(requireContext())) {
                Toast.makeText(requireContext(), "אין אינטרנט – לא ניתן לבחור תמונה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            galleryLauncher.launch("image/*")
        }

        binding.btnSaveEdit.setOnClickListener {
            val username = binding.editUsername.text.toString().trim()
            val firstName = binding.editFirstName.text.toString().trim()
            val phone = binding.editPhone.text.toString().trim()
            val password = binding.editPassword.text.toString().trim()

            if (username.isEmpty() || firstName.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE

            if (selectedImageUri != null && !isInternetAvailable(requireContext())) {
                Toast.makeText(requireContext(), "אין אינטרנט – לא ניתן להעלות תמונה", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            selectedImageUri?.let { uri ->
                val rotatedBitmap = getRotatedBitmapFromUri(requireContext(), uri)
                rotatedBitmap?.let { bmp ->
                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 70, baos)
                    val bytes = baos.toByteArray()

                    viewModel.saveProfileImage(bytes) { success ->
                        if (success) {
                            viewModel.updateProfile(username, firstName, phone, viewModel.profileImageUrl.value)
                            Toast.makeText(requireContext(), "פרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "שמירת תמונה נכשלה", Toast.LENGTH_SHORT).show()
                        }
                        binding.progressBar.visibility = View.GONE
                    }
                } ?: run {
                    binding.progressBar.visibility = View.GONE
                }
            } ?: run {
                viewModel.updateProfile(username, firstName, phone)
                Toast.makeText(requireContext(), "פרופיל עודכן בהצלחה", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.GONE
            }

            if (password.isNotEmpty()) {
                viewModel.getCurrentUser()?.updatePassword(password)
                    ?.addOnSuccessListener {
                        Toast.makeText(requireContext(), "הסיסמה עודכנה", Toast.LENGTH_SHORT).show()
                    }
                    ?.addOnFailureListener {
                        Toast.makeText(requireContext(), "עדכון הסיסמה נכשל", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun launchCamera() {
        val imageFile = File(requireContext().cacheDir, "temp_profile_image.jpg")
        tempImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
        cameraLauncher.launch(tempImageUri)
    }

    private fun getRotatedBitmapFromUri(context: Context, imageUri: Uri): Bitmap? {
        val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
        val exif = inputStream?.let { ExifInterface(it) }
        val orientation = exif?.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val rotationDegrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = ContextCompat.getSystemService(context, android.net.ConnectivityManager::class.java)
        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
