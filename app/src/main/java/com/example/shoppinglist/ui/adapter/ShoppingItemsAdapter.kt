package com.example.shoppinglist.ui.adapter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.Context


import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.ShoppingItem
import com.example.shoppinglist.data.local.models.ShoppingListItem
import com.google.firebase.auth.FirebaseAuth

class ShoppingItemsAdapter(
    private var items: List<ShoppingListItem>,
    private val onItemClick: (ShoppingItem) -> Unit,
    private val onPurchasedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onQuantityChanged: (ShoppingItem, Int) -> Unit,
    private val onCommentAdded: (ShoppingItem, String) -> Unit,
    private val onImageAdded: (ShoppingItem) -> Unit,
    private val onGallerySelected: (ShoppingItem) -> Unit,
    private val onItemDeleted: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_CATEGORY = 0
        private const val VIEW_TYPE_PRODUCT = 1
    }

    inner class CategoryHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val categoryName: TextView = view.findViewById(R.id.categoryName)
    }

    inner class ShoppingProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val previewImage: ImageView = view.findViewById(R.id.previewImage)
        val name: TextView = view.findViewById(R.id.itemName)
        val checkbox: CheckBox = view.findViewById(R.id.itemCheckbox)
        val quantityLayout: LinearLayout = view.findViewById(R.id.quantityLayout)
        val commentsSection: LinearLayout = view.findViewById(R.id.commentsSection)
        val quantityText: EditText = view.findViewById(R.id.itemQuantityText)
        val increaseButton: ImageButton = view.findViewById(R.id.increaseQuantity)
        val decreaseButton: ImageButton = view.findViewById(R.id.decreaseQuantity)
        val commentInput: EditText = view.findViewById(R.id.itemComment)
        val sendCommentButton: ImageButton = view.findViewById(R.id.btnSendComment)
        val addImageButton: ImageButton = view.findViewById(R.id.btnAddImage)
        val selectFromGalleryButton: ImageButton = view.findViewById(R.id.btnSelectFromGallery)
        val messagesRecyclerView: RecyclerView = view.findViewById(R.id.messagesRecyclerView)

        var messagesAdapter: MessagesAdapter? = null
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ShoppingListItem.CategoryHeader -> VIEW_TYPE_CATEGORY
            is ShoppingListItem.ShoppingProduct -> VIEW_TYPE_PRODUCT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_shopping, parent, false)
                ShoppingProductViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ShoppingListItem.CategoryHeader -> {
                (holder as CategoryHeaderViewHolder).categoryName.text = item.categoryName
            }
            is ShoppingListItem.ShoppingProduct -> {
                bindProduct(holder as ShoppingProductViewHolder, item.item)
            }
        }
    }

    private fun bindProduct(holder: ShoppingProductViewHolder, item: ShoppingItem) {
        holder.name.text = if (!item.expanded) {
            if (item.quantity > 1) "${item.name} ×${item.quantity}" else item.name
        } else {
            item.name
        }

        holder.checkbox.setOnCheckedChangeListener(null)
        holder.checkbox.isChecked = item.purchased
        holder.quantityText.setText(item.quantity.toString())

        if (item.purchased) {
            holder.itemView.setBackgroundResource(R.drawable.item_background_checked)
            holder.name.paintFlags = holder.name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.itemView.setBackgroundResource(R.drawable.item_unchecked_background)
            holder.name.paintFlags = holder.name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        holder.quantityLayout.visibility = if (item.expanded) View.VISIBLE else View.GONE
        holder.commentsSection.visibility = if (item.expanded) View.VISIBLE else View.GONE
        holder.messagesRecyclerView.visibility = if (item.expanded) View.VISIBLE else View.GONE

        if (item.expanded) {
            if (holder.messagesAdapter == null) {
                holder.messagesAdapter = MessagesAdapter(item.messages, FirebaseAuth.getInstance().currentUser?.uid.orEmpty())
                holder.messagesRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
                holder.messagesRecyclerView.adapter = holder.messagesAdapter
            } else {
                holder.messagesAdapter?.updateMessages(item.messages)
            }
        }

        if (item.previewImageBitmap != null && item.expanded) {
            holder.previewImage.setImageBitmap(item.previewImageBitmap)
            holder.previewImage.visibility = View.VISIBLE
        } else {
            holder.previewImage.visibility = View.GONE
        }

        holder.name.setOnClickListener { onItemClick(item) }
        holder.checkbox.setOnCheckedChangeListener { _, isChecked -> onPurchasedChanged(item, isChecked) }
        holder.increaseButton.setOnClickListener { onQuantityChanged(item, item.quantity + 1) }
        holder.decreaseButton.setOnClickListener { onQuantityChanged(item, maxOf(1, item.quantity - 1)) }

        holder.sendCommentButton.setOnClickListener {
            val commentText = holder.commentInput.text.toString().trim()
            val hasText = commentText.isNotEmpty()
            val hasImage = item.previewImageBitmap != null

            if (!hasText && !hasImage) {
                Toast.makeText(holder.itemView.context, "לא ניתן לשלוח הודעה ריקה", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // שולחים פעם אחת בלבד
            onCommentAdded(item, commentText)

            // ניקוי אחרי שליחה
            item.previewImageBitmap = null
            holder.previewImage.setImageBitmap(null)
            holder.previewImage.visibility = View.GONE
            holder.commentInput.text.clear()

            holder.messagesAdapter?.notifyDataSetChanged()
            notifyItemChanged(holder.adapterPosition)
        }

        holder.addImageButton.setOnClickListener {
            if (isNetworkAvailable(holder.itemView.context)) {
                onImageAdded(item)
            } else {
                Toast.makeText(holder.itemView.context, "אין חיבור לאינטרנט - אי אפשר להוסיף תמונה כרגע", Toast.LENGTH_SHORT).show()
            }
        }

        holder.selectFromGalleryButton.setOnClickListener {
            if (isNetworkAvailable(holder.itemView.context)) {
                onGallerySelected(item)
            } else {
                Toast.makeText(holder.itemView.context, "אין חיבור לאינטרנט - אי אפשר להעלות תמונה כרגע", Toast.LENGTH_SHORT).show()
            }
        }


        holder.selectFromGalleryButton.setOnClickListener {
            if (isNetworkAvailable(holder.itemView.context)) {
                onGallerySelected(item)
            } else {
                Toast.makeText(holder.itemView.context, "אין חיבור לאינטרנט - אי אפשר להעלות תמונה כרגע", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ShoppingListItem>) {
        val expandedMap = items
            .filterIsInstance<ShoppingListItem.ShoppingProduct>()
            .associateBy({ it.item.id }, { it.item.expanded })

        val previewMap = items
            .filterIsInstance<ShoppingListItem.ShoppingProduct>()
            .associateBy({ it.item.id }, { it.item.previewImageBitmap })

        items = newItems.map { item ->
            when (item) {
                is ShoppingListItem.CategoryHeader -> item
                is ShoppingListItem.ShoppingProduct -> item.copy(
                    item = item.item.copy(
                        expanded = expandedMap[item.item.id] ?: false,
                        previewImageBitmap = previewMap[item.item.id]
                    )
                )
            }
        }
        notifyDataSetChanged()
    }

    fun swapItems(fromPosition: Int, toPosition: Int, onItemsReordered: (List<Pair<String, Int>>) -> Unit) {
        if (fromPosition == toPosition) return

        val mutableList = items.toMutableList()
        val fromItem = mutableList.getOrNull(fromPosition)
        val toItem = mutableList.getOrNull(toPosition)

        if (fromItem is ShoppingListItem.ShoppingProduct && toItem is ShoppingListItem.ShoppingProduct) {
            if (fromItem.item.category == toItem.item.category) {
                mutableList.removeAt(fromPosition)
                mutableList.add(toPosition, fromItem)

                items = mutableList
                notifyItemMoved(fromPosition, toPosition)

                // ✅ כאן שולחים עדכון ל-ViewModel עם הסדר החדש
                val updatedOrders = mutableList.mapIndexedNotNull { index, listItem ->
                    if (listItem is ShoppingListItem.ShoppingProduct)
                        listItem.item.id to index
                    else null
                }
                onItemsReordered(updatedOrders)
            }
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = connectivityManager?.activeNetwork
        val capabilities = connectivityManager?.getNetworkCapabilities(network)
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun currentItems(): List<ShoppingListItem> = items
}
