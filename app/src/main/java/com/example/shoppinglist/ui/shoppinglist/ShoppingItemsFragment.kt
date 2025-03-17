package com.example.shoppinglist.ui.shoppinglist.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentShoppingItemsBinding
import com.example.shoppinglist.viewmodel.ShoppingItemsViewModel
import com.example.shoppinglist.ui.adapter.ShoppingItemsAdapter
import java.io.ByteArrayOutputStream

class ShoppingItemsFragment : Fragment() {

    private lateinit var binding: FragmentShoppingItemsBinding
    private val args: ShoppingItemsFragmentArgs by navArgs()
    private val viewModel: ShoppingItemsViewModel by viewModels()
    private lateinit var adapter: ShoppingItemsAdapter
    private var currentItemId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentShoppingItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtListName.text = args.listName

        adapter = ShoppingItemsAdapter(
            listOf(),
            onItemClick = { selectedItem ->
                selectedItem.expanded = !selectedItem.expanded
                adapter.notifyDataSetChanged()
            },
            onPurchasedChanged = { selectedItem, isChecked ->
                viewModel.updateItemPurchased(args.listId, selectedItem.id, isChecked)
            },
            onQuantityChanged = { selectedItem, newQuantity ->
                viewModel.updateItemQuantity(args.listId, selectedItem.id, newQuantity)
            },
            onCommentAdded = { selectedItem, comment ->
                viewModel.addCommentToItem(args.listId, selectedItem.id, comment)
            },
            onImageAdded = { selectedItem ->
                currentItemId = selectedItem.id
                requestCameraPermission()
            },
            onGallerySelected = { selectedItem ->
                currentItemId = selectedItem.id
                openGallery()
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddItem.setOnClickListener {
            showAddItemDialog()
        }

        viewModel.loadShoppingItems(args.listId)
        viewModel.itemsList.observe(viewLifecycleOwner) { items ->
            adapter.updateItems(items)
        }
    }

    private fun showAddItemDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter item name"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Add Item")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val itemName = editText.text.toString().trim()
                if (itemName.isNotEmpty()) {
                    viewModel.addItemToFirebase(args.listId, itemName)
                } else {
                    Toast.makeText(requireContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // 📸 מצלמה - בקשת הרשאה
    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // 📸 פתיחת מצלמה
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            uploadImageToFirebase(imageBitmap)
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    // 🖼️ פתיחת גלריה
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadImageToFirebase(it)
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    // 🚀 העלאת תמונה ל-Firebase
    private fun uploadImageToFirebase(uri: Uri) {
        currentItemId?.let { itemId ->
            viewModel.uploadImageForItem(args.listId, itemId, uri)
        }
    }

    private fun uploadImageToFirebase(imageBitmap: Bitmap) {
        currentItemId?.let { itemId ->
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            viewModel.uploadImageForItem(args.listId, itemId, imageData)
        }
    }
}
