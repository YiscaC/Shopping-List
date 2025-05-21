package com.example.shoppinglist.ui.shoppinglist.fragment

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.MainActivity
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.Category
import com.example.shoppinglist.data.local.models.ShoppingItem
import com.example.shoppinglist.data.local.models.ShoppingListItem
import com.example.shoppinglist.databinding.FragmentShoppingItemsBinding
import com.example.shoppinglist.ui.adapter.ShoppingItemsAdapter
import com.example.shoppinglist.viewmodel.ShoppingItemsViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.ByteArrayOutputStream
import android.text.TextWatcher
import android.text.Editable
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import org.json.JSONObject


private var pendingImageUri: Uri? = null
private var pendingImageBitmap: Bitmap? = null
private var rememberedListId: String? = null

class ShoppingItemsFragment : Fragment() {

    private lateinit var binding: FragmentShoppingItemsBinding
    private val args: ShoppingItemsFragmentArgs by navArgs()
    private val viewModel: ShoppingItemsViewModel by viewModels()

    private lateinit var adapter: ShoppingItemsAdapter
    private var currentItemId: String? = null
    private var selectedImageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentShoppingItemsBinding.inflate(inflater, container, false)
        val idToUse = args.listId.ifBlank { rememberedListId }
        if (idToUse.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "\u05dc\u05d0 \u05e0\u05d9\u05ea\u05df \u05dc\u05d8\u05e2\u05d5\u05df \u05d0\u05ea \u05d4\u05e8\u05e9\u05d9\u05de\u05d4", Toast.LENGTH_LONG).show()
            return binding.root
        }
        viewModel.setListId(idToUse)
        rememberedListId = idToUse
        return binding.root
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_shopping_items, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
                currentItemId = selectedItem.id
                if (comment.isNotBlank()) {
                    viewModel.addMessageToItem(selectedItem.id, comment)
                }
                sendPendingImageIfNeeded()
                clearInputFields(selectedItem)
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
        binding.recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                adapter.swapItems(viewHolder.adapterPosition, target.adapterPosition) { updatedOrders ->
                    viewModel.updateMultipleItemsOrder(updatedOrders)
                }
                return true
            }
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.currentItems().getOrNull(position)

                if (item is ShoppingListItem.ShoppingProduct) {
                    viewModel.deleteItem(item.item.id)
                }
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)

        viewModel.itemsList.observe(viewLifecycleOwner) { items ->
            try {
                val shoppingItems = items.map { it.toShoppingItem() }

                val notPurchased = shoppingItems.filter { !it.purchased }
                val purchased = shoppingItems.filter { it.purchased }

                val grouped = notPurchased
                    .filter { it.category.isNotBlank() }
                    .groupBy { it.category }

                val displayList = mutableListOf<ShoppingListItem>()

                grouped.forEach { (category, itemsInCategory) ->
                    displayList.add(ShoppingListItem.CategoryHeader(category))
                    itemsInCategory.sortedBy { it.order }.forEach { item ->
                        displayList.add(ShoppingListItem.ShoppingProduct(item))
                    }
                }

                // הוספת כותרת מיוחדת לנקנו
                if (purchased.isNotEmpty()) {
                    displayList.add(ShoppingListItem.CategoryHeader("נקנו"))
                    purchased.sortedBy { it.order }.forEach { item ->
                        displayList.add(ShoppingListItem.ShoppingProduct(item))
                    }
                }

                adapter.updateItems(displayList)
            } catch (e: Exception) {
                Log.e("ShoppingItems", "שגיאה בטעינת פריטים: ${e.message}", e)
            }
        }


        binding.btnAddItem.setOnClickListener { showAddItemDialog() }
    }

    private fun clearInputFields(item: ShoppingItem) {
        val index = adapter.currentItems().indexOfFirst {
            it is ShoppingListItem.ShoppingProduct && it.item.id == item.id
        }
        if (index != -1) {
            (adapter.currentItems()[index] as? ShoppingListItem.ShoppingProduct)?.item?.apply {
                previewImageBitmap = null
            }
            adapter.notifyItemChanged(index)
        }
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_participants -> {
                rememberedListId?.let {
                    val action = ShoppingItemsFragmentDirections.actionShoppingItemsFragmentToParticipantsFragment(it)
                    findNavController().navigate(action)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera()
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
            imageBitmap?.let {
                pendingImageBitmap = rotateBitmapIfRequired(it)
                updatePreviewImage(pendingImageBitmap)
            }
        }
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pendingImageUri = it
            val bitmap = MediaStore.Images.Media.getBitmap(requireContext().contentResolver, it)
            pendingImageBitmap = bitmap
            updatePreviewImage(bitmap)
        }
    }

    private fun updatePreviewImage(bitmap: Bitmap?) {
        val index = adapter.currentItems().indexOfFirst {
            it is ShoppingListItem.ShoppingProduct && it.item.id == currentItemId
        }
        if (index != -1 && bitmap != null) {
            (adapter.currentItems()[index] as? ShoppingListItem.ShoppingProduct)?.item?.previewImageBitmap = bitmap
            adapter.notifyItemChanged(index)
        }
    }

    private fun sendPendingImageIfNeeded() {
        val itemId = currentItemId ?: return

        // אם יש תמונה מהגלריה – נעלה רק אותה
        if (pendingImageUri != null) {
            viewModel.uploadMessageImageFromUri(itemId, pendingImageUri!!)
            pendingImageUri = null
            pendingImageBitmap = null // נוודא שלא תעלה פעמיים
            return
        }

        // אחרת נעלה את ה-Bitmap מהמצלמה
        if (pendingImageBitmap != null) {
            val baos = ByteArrayOutputStream()
            pendingImageBitmap!!.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            viewModel.uploadMessageImageFromBytes(itemId, baos.toByteArray())
            pendingImageBitmap = null
        }
    }


    private fun rotateBitmapIfRequired(bitmap: Bitmap, uri: Uri? = null): Bitmap {
        return bitmap
    }

    private fun showAddItemDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val editTextName = dialogView.findViewById<EditText>(R.id.editTextItemName)

        editTextName.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                if (query.length > 2) { // אחרי 3 אותיות לפחות
                    searchImageFromPexels(query,dialogView)
                }
            }
        })
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        val categories = loadCategoriesFromAssets(requireContext())
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories.map { it.name })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter

        AlertDialog.Builder(requireContext())
            .setTitle("\u05d4\u05d5\u05e1\u05e4\u05ea \u05de\u05d5\u05e6\u05e8 \u05d7\u05d3\u05e9")
            .setView(dialogView)
            .setPositiveButton("\u05d4\u05d5\u05e1\u05e3") { _, _ ->
                val itemName = editTextName.text.toString().trim()
                val selectedCategory = spinnerCategory.selectedItem.toString()

                if (itemName.isNotEmpty()) {
                    viewModel.addItemToFirebase(itemName, selectedCategory, selectedImageUrl?:"")
                } else {
                    Toast.makeText(requireContext(), "\u05e9\u05dd \u05d4\u05de\u05d5\u05e6\u05e8 \u05dc\u05d0 \u05d9\u05db\u05d5\u05dc \u05dc\u05d4\u05d9\u05d5\u05ea \u05e8\u05d9\u05e7", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("\u05d1\u05d9\u05d8\u05d5\u05dc", null)
            .show()
    }

    private fun loadCategoriesFromAssets(context: Context): List<Category> {
        val jsonString = context.assets.open("categories.json").bufferedReader().use { it.readText() }
        val listType = object : TypeToken<List<Category>>() {}.type
        return Gson().fromJson(jsonString, listType)
    }
    private fun searchImageFromPexels(query: String, dialogView: View) {
        val apiKey = "nhBkDpt4ksLOFvFHyI8p2T8mOg9clsEW8x9shY9K6YfEYlIpmDCf9WkM"
        val url = "https://api.pexels.com/v1/search?query=$query&per_page=1"

        // יצירת בקשה ב-Volley
        val request = object : StringRequest(
            Method.GET, url,
            { response ->
                val jsonObject = JSONObject(response)
                val photosArray = jsonObject.getJSONArray("photos")
                if (photosArray.length() > 0) {
                    val firstPhoto = photosArray.getJSONObject(0)
                    val src = firstPhoto.getJSONObject("src")
                    val imageUrl = src.getString("medium")
                    selectedImageUrl=imageUrl
                    // בדוק אם ה-URL לא ריק או null
                    if (imageUrl.isNullOrEmpty()) {
                        Log.e("PEXELS", "תמונה לא נמצאה או ה-URL ריק")
                    } else {
                        loadImage(imageUrl, dialogView)  // אם התמונה נמצאה, נטען אותה
                    }
                } else {
                    Log.e("PEXELS", "לא נמצאו תמונות עבור השאילתה")
                }
            },
            { error ->
                Log.e("PEXELS", "Error fetching image: ${error.message}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] = apiKey
                return headers
            }
        }

        // הוספת הבקשה ל-Volley RequestQueue
        Volley.newRequestQueue(requireContext()).add(request)
    }


    private fun loadImage(url: String, dialogView: View) {
        val productImageView = dialogView.findViewById<ImageView>(R.id.productImageView)
        if (productImageView != null) {
            Glide.with(this)
                .load(url)
                .into(productImageView)
        } else {
            Log.e("LOAD_IMAGE", "productImageView is NULL")
        }
    }
}
