package com.example.shoppinglist.ui.shoppinglist.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.MainActivity
import com.example.shoppinglist.R
import com.example.shoppinglist.databinding.FragmentShoppingItemsBinding
import com.example.shoppinglist.ui.adapter.ShoppingItemsAdapter
import com.example.shoppinglist.viewmodel.ShoppingItemsViewModel
import java.io.ByteArrayOutputStream
import android.graphics.Matrix
private var pendingImageUri: Uri? = null
private var pendingImageBitmap: Bitmap? = null

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

        viewModel.setListId(args.listId)

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
                val newList = adapter.currentItems().toMutableList().apply {
                    remove(selectedItem)
                    if (isChecked) add(selectedItem) else add(0, selectedItem)
                }
                adapter.updateItems(newList)
                newList.forEachIndexed { index, item ->
                    viewModel.updateItemOrder(item.id, index)
                }
            },
            onQuantityChanged = { selectedItem, newQuantity ->
                viewModel.updateItemQuantity(selectedItem.id, newQuantity)
            },
            onCommentAdded = { selectedItem, comment ->
                currentItemId = selectedItem.id

                if (comment.isNotBlank()) {
                    viewModel.addMessageToItem(selectedItem.id, comment)
                }

                sendPendingImageIfNeeded()

            },

            onImageAdded = { selectedItem ->
                currentItemId = selectedItem.id
                requestCameraPermission()
            },
            onGallerySelected = { selectedItem ->
                currentItemId = selectedItem.id
                openGallery()
            },
            onItemDeleted = { selectedItem ->
                viewModel.deleteItem(selectedItem.id)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.setHasFixedSize(false)
        binding.recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) return false
                adapter.swapItems(fromPos, toPos)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentItems()[position]

                AlertDialog.Builder(requireContext())
                    .setTitle("מחיקת מוצר")
                    .setMessage("האם את/ה בטוח/ה שברצונך למחוק את '${item.name}'?")
                    .setPositiveButton("מחק") { _, _ ->
                        viewModel.deleteItem(item.id)
                    }
                    .setNegativeButton("ביטול") { dialog, _ ->
                        dialog.dismiss()
                        adapter.notifyItemChanged(position)
                    }
                    .setCancelable(false)
                    .show()
            }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                adapter.currentItems().forEachIndexed { index, item ->
                    viewModel.updateItemOrder(item.id, index)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)

                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val itemView = viewHolder.itemView
                    val paint = Paint().apply {
                        color = ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                    }

                    c.drawRect(
                        itemView.right + dX, itemView.top.toFloat(),
                        itemView.right.toFloat(), itemView.bottom.toFloat(), paint
                    )

                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)
                    icon?.let {
                        val iconMargin = (itemView.height - it.intrinsicHeight) / 2
                        val iconTop = itemView.top + iconMargin
                        val iconLeft = itemView.right - iconMargin - it.intrinsicWidth
                        val iconRight = itemView.right - iconMargin
                        val iconBottom = iconTop + it.intrinsicHeight

                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                        it.draw(c)
                    }
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        binding.btnAddItem.setOnClickListener {
            showAddItemDialog()
        }

        viewModel.itemsList.observe(viewLifecycleOwner) { items ->
            val sorted = items.map { it.toShoppingItem() }.sortedBy { it.order }
            adapter.updateItems(sorted)
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
        else Toast.makeText(requireContext(), "יש לאשר הרשאת מצלמה", Toast.LENGTH_SHORT).show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                val rotated = rotateBitmapIfRequired(it)
                pendingImageBitmap = rotated

                // מציגים רק לאחר לחיצה על שליחה – כמו בגלריה
                val currentList = adapter.currentItems().toMutableList()
                val index = currentList.indexOfFirst { item -> item.id == currentItemId }
                if (index != -1) {
                    currentList[index].previewImageBitmap = rotated
                    currentList[index].expanded = true
                    adapter.updateItems(currentList)
                    binding.recyclerView.scrollToPosition(index)
                }

                Toast.makeText(requireContext(), "תמונה מוכנה לשליחה", Toast.LENGTH_SHORT).show()
            }
        }
    }



    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }
    val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            pendingImageUri = it

            val currentList = adapter.currentItems().toMutableList()
            val index = currentList.indexOfFirst { item -> item.id == currentItemId }
            if (index != -1) {
                currentList[index].previewImageBitmap = bitmap
                currentList[index].expanded = true
                adapter.notifyItemChanged(index) // ✅ גם כאן
                binding.recyclerView.scrollToPosition(index)
            }

            Toast.makeText(requireContext(), "תמונה מוכנה לשליחה", Toast.LENGTH_SHORT).show()
        }
    }


    private fun rotateBitmapIfRequired(bitmap: Bitmap, uri: Uri? = null): Bitmap {
        val ei = uri?.let {
            requireContext().contentResolver.openInputStream(it)?.use { input ->
                androidx.exifinterface.media.ExifInterface(input)
            }
        } ?: return bitmap

        val orientation = ei.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )

        val rotation = when (orientation) {
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }

        return if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else bitmap
    }

    private fun sendPendingImageIfNeeded() {
        val itemId = currentItemId ?: return

        pendingImageUri?.let {
            viewModel.uploadMessageImageFromUri(itemId, it)
            pendingImageUri = null
        }

        pendingImageBitmap?.let {
            val baos = ByteArrayOutputStream()
            it.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            viewModel.uploadMessageImageFromBytes(itemId, baos.toByteArray())
            pendingImageBitmap = null
        }
    }


    private fun uploadMessageImage(uri: Uri) {
        currentItemId?.let { itemId ->
            viewModel.uploadMessageImageFromUri(itemId, uri)
        }
    }

    private fun uploadMessageImage(imageBitmap: Bitmap) {
        currentItemId?.let { itemId ->
            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageData = baos.toByteArray()
            viewModel.uploadMessageImageFromBytes(itemId, imageData)
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
