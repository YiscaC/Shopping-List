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
import com.example.shoppinglist.MainActivity
import com.example.shoppinglist.databinding.FragmentShoppingItemsBinding
import com.example.shoppinglist.ui.adapter.ShoppingItemsAdapter
import com.example.shoppinglist.viewmodel.ShoppingItemsViewModel
import java.io.ByteArrayOutputStream

class ShoppingItemsFragment : Fragment() {

    private lateinit var binding: FragmentShoppingItemsBinding
    private val args: ShoppingItemsFragmentArgs by navArgs()
    private val viewModel: ShoppingItemsViewModel by viewModels()
    private lateinit var adapter: ShoppingItemsAdapter
    private var currentItemId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ הגדרת ה־listId ב־ViewModel
        viewModel.setListId(args.listId)

        // ✅ שמירה ב־MainActivity עבור ניווט מהיר
        (activity as? MainActivity)?.apply {
            activeListId = args.listId
            activeListName = args.listName
        }

        binding.txtListName.text = args.listName

        adapter = ShoppingItemsAdapter(
            emptyList(),
            onItemClick = { selectedItem ->
                selectedItem.expanded = !selectedItem.expanded
                adapter.notifyDataSetChanged()
            },
            onPurchasedChanged = { selectedItem, isChecked ->
                viewModel.updateItemPurchased(selectedItem.id, isChecked)
            },
            onQuantityChanged = { selectedItem, newQuantity ->
                viewModel.updateItemQuantity(selectedItem.id, newQuantity)
            },
            onCommentAdded = { selectedItem, comment ->
                viewModel.addCommentToItem(selectedItem.id, comment)
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

        viewModel.itemsList.observe(viewLifecycleOwner) { items ->
            adapter.updateItems(items.map { it.toShoppingItem() })
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera()
        else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { uploadImageToFirebase(it) }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadImageToFirebase(it) }
    }

    private fun uploadImageToFirebase(uri: Uri) {
        currentItemId?.let { itemId ->
            viewModel.uploadImageForItem(itemId, uri)
        }
    }

    private fun uploadImageToFirebase(imageBitmap: Bitmap) {
        currentItemId?.let { itemId ->
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()
            viewModel.uploadImageForItem(itemId, imageData)
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
                    viewModel.addItemToFirebase(itemName)
                } else {
                    Toast.makeText(requireContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
