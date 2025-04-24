package com.example.shoppinglist.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.ShoppingItem
import com.google.firebase.auth.FirebaseAuth

class ShoppingItemsAdapter(
    private var items: List<ShoppingItem>,
    private val onItemClick: (ShoppingItem) -> Unit,
    private val onPurchasedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onQuantityChanged: (ShoppingItem, Int) -> Unit,
    private val onCommentAdded: (ShoppingItem, String) -> Unit,
    private val onImageAdded: (ShoppingItem) -> Unit,
    private val onGallerySelected: (ShoppingItem) -> Unit,
    private val onItemDeleted: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<ShoppingItemsAdapter.ShoppingItemViewHolder>() {

    inner class ShoppingItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
        val messagesRecyclerView: RecyclerView = view.findViewById(R.id.messagesRecyclerView)

        var messagesAdapter: MessagesAdapter? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        val item = items[position]

        holder.name.text = if (!item.expanded) {
            if (item.quantity > 1) "${item.name}   ×${item.quantity}" else item.name
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

        // אזור פריסה
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
            holder.messagesRecyclerView.visibility = View.VISIBLE
        } else {
            holder.messagesRecyclerView.visibility = View.GONE
        }


        if (item.previewImageBitmap != null && item.expanded) {
            holder.previewImage.setImageBitmap(item.previewImageBitmap)
            holder.previewImage.visibility = View.VISIBLE
        } else {
            holder.previewImage.visibility = View.GONE
        }

        holder.name.setOnClickListener {
            item.expanded = !item.expanded
            notifyItemChanged(position)
        }

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.purchased = isChecked
            onPurchasedChanged(item, isChecked)

            val mutableList = items.toMutableList().apply {
                remove(item)
                if (isChecked) add(item) else add(0, item)
            }
            updateItems(mutableList)
        }

        holder.increaseButton.setOnClickListener {
            val newQuantity = item.quantity + 1
            onQuantityChanged(item, newQuantity)
            holder.quantityText.setText(newQuantity.toString())
        }

        holder.decreaseButton.setOnClickListener {
            val newQuantity = if (item.quantity > 1) item.quantity - 1 else 1
            onQuantityChanged(item, newQuantity)
            holder.quantityText.setText(newQuantity.toString())
        }

        holder.sendCommentButton.setOnClickListener {
            val commentText = holder.commentInput.text.toString().trim()
            val hasText = commentText.isNotEmpty()
            val hasImage = item.previewImageBitmap != null

            if (!hasText && !hasImage) {
                Toast.makeText(
                    holder.itemView.context,
                    "לא ניתן לשלוח הודעה ריקה",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            item.expanded = true // ✅ שומר את הפריט פתוח

            if (hasText) {
                onCommentAdded(item, commentText)
                holder.commentInput.setText("")
            }

            if (hasImage) {
                onCommentAdded(item, "") // שולח את התמונה בלבד
            }

            item.previewImageBitmap = null
            holder.previewImage.setImageBitmap(null)
            holder.previewImage.visibility = View.GONE

            notifyItemChanged(holder.adapterPosition) // ✅ מרענן את הפריט תוך שמירה על expanded

            // גלילה אוטומטית להודעה האחרונה
            holder.messagesRecyclerView.post {
                holder.messagesRecyclerView.scrollToPosition(
                    holder.messagesAdapter?.itemCount?.minus(1) ?: 0
                )
            }
        }


        holder.addImageButton.setOnClickListener {
            onImageAdded(item)
        }

        holder.selectFromGalleryButton.setOnClickListener {
            onGallerySelected(item)
        }

        holder.messagesAdapter?.updateMessages(item.messages)
    }


    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ShoppingItem>) {
        val expandedMap = items.associateBy({ it.id }, { it.expanded }) // שומר פריסה קיימת
        val previewMap = items.associateBy({ it.id }, { it.previewImageBitmap }) // שומר preview תמונה

        items = newItems.map { item ->
            item.copy(
                expanded = expandedMap[item.id] ?: false,
                previewImageBitmap = previewMap[item.id] // נשמר עד שהמשתמש שולח או מוחק
            )
        }

        notifyDataSetChanged()
    }


    fun swapItems(fromPosition: Int, toPosition: Int) {
        if (fromPosition == toPosition) return
        val mutableList = items.toMutableList()
        val item = mutableList.removeAt(fromPosition)
        mutableList.add(toPosition, item)
        items = mutableList
        notifyItemMoved(fromPosition, toPosition)
    }

    fun sortItemsByChecked() {
        val sorted = items.sortedBy { it.purchased }
        updateItems(sorted)
    }

    fun currentItems(): List<ShoppingItem> = items
}