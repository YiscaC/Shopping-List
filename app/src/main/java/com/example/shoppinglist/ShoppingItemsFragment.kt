package com.example.shoppinglist

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.shoppinglist.databinding.FragmentShoppingItemsBinding
import com.example.shoppinglist.models.ShoppingItem
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class ShoppingItemsFragment : Fragment() {

    private lateinit var binding: FragmentShoppingItemsBinding
    private val db: DatabaseReference by lazy { FirebaseDatabase.getInstance().reference }
    private val storage = FirebaseStorage.getInstance().reference
    private val args: ShoppingItemsFragmentArgs by navArgs()
    private val itemsList = mutableListOf<ShoppingItem>()
    private lateinit var adapter: ShoppingItemsAdapter
    private var currentItem: ShoppingItem? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentShoppingItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtListName.text = args.listName

        adapter = ShoppingItemsAdapter(
            itemsList,
            onItemClick = { selectedItem ->
                selectedItem.expanded = !selectedItem.expanded
                adapter.notifyDataSetChanged()
            },
            onPurchasedChanged = { selectedItem, isChecked ->
                updateItemPurchased(selectedItem, isChecked)
            },
            onQuantityChanged = { selectedItem, newQuantity ->
                updateItemQuantity(selectedItem, newQuantity)
            },
            onCommentAdded = { selectedItem, comment ->
                addCommentToItem(selectedItem, comment)
            },
            onImageAdded = { selectedItem ->
                requestCameraPermission(selectedItem)
            },
            onGallerySelected = { selectedItem ->
                selectImageFromGalleryForItem(selectedItem)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        binding.btnAddItem.setOnClickListener {
            showAddItemDialog()
        }

        loadShoppingItems()
    }

    private fun loadShoppingItems() {
        db.child("shoppingLists").child(args.listId).child("items")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    itemsList.clear()
                    for (itemSnapshot in snapshot.children) {
                        val item = itemSnapshot.getValue(ShoppingItem::class.java)
                        item?.let { itemsList.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load items.", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun updateItemPurchased(item: ShoppingItem, isChecked: Boolean) {
        db.child("shoppingLists").child(args.listId).child("items").child(item.id)
            .child("purchased").setValue(isChecked)
    }

    private fun updateItemQuantity(item: ShoppingItem, newQuantity: Int) {
        db.child("shoppingLists").child(args.listId).child("items").child(item.id)
            .child("quantity").setValue(newQuantity)
    }

    private fun addCommentToItem(item: ShoppingItem, comment: String) {
        db.child("shoppingLists").child(args.listId).child("items").child(item.id)
            .child("comments").push().setValue(comment)
    }

    private fun showAddItemDialog() {
        val editText = EditText(requireContext()).apply {
            hint = "Enter item name"
            textSize = 16f
            setPadding(32, 16, 32, 16)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Add Item")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val itemName = editText.text.toString().trim()
                if (itemName.isNotEmpty()) {
                    addItemToFirebase(itemName)
                } else {
                    Toast.makeText(requireContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addItemToFirebase(itemName: String) {
        val newItemId = db.child("shoppingLists").child(args.listId).child("items").push().key ?: return

        val newItem = ShoppingItem(
            id = newItemId,
            name = itemName,
            quantity = 1,
            purchased = false
        )

        db.child("shoppingLists").child(args.listId).child("items").child(newItemId).setValue(newItem)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Item added!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to add item.", Toast.LENGTH_SHORT).show()
            }
    }

    // ✅ בקשת הרשאה למצלמה
    private fun requestCameraPermission(item: ShoppingItem) {
        currentItem = item
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

    // ✅ פתיחת מצלמה
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            uploadImageToFirebase(imageBitmap)
        } else {
            Toast.makeText(requireContext(), "צילום בוטל", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    // ✅ פתיחת בורר גלריה
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            uploadImageToFirebase(it)
        }
    }

    fun selectImageFromGalleryForItem(item: ShoppingItem) {
        currentItem = item
        galleryLauncher.launch("image/*")
    }

    private fun uploadImageToFirebase(uri: Uri) {
        currentItem?.let { item ->
            val imageRef = storage.child("item_images/${item.id}.jpg")
            val uploadTask = imageRef.putFile(uri)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    updateItemImageInDatabase(item, downloadUri.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "שגיאה בהעלאת התמונה", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImageToFirebase(imageBitmap: Bitmap) {
        currentItem?.let { item ->
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()

            val imageRef = storage.child("item_images/${item.id}.jpg")
            val uploadTask = imageRef.putBytes(imageData)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateItemImageInDatabase(item, uri.toString())
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "שגיאה בהעלאת התמונה", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateItemImageInDatabase(item: ShoppingItem, imageUrl: String) {
        db.child("shoppingLists").child(args.listId).child("items").child(item.id).child("imageUrl").setValue(imageUrl)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "התמונה נשמרה!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Log.e("Firebase", "Failed to update item image: ${it.message}")
            }
    }
}
