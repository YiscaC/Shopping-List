package com.example.shoppinglist.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.ShoppingItem
import java.util.*

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

        holder.name.setOnClickListener {
            item.expanded = !item.expanded
            holder.quantityLayout.visibility = if (item.expanded) View.VISIBLE else View.GONE
            holder.commentsSection.visibility = if (item.expanded) View.VISIBLE else View.GONE
            holder.itemImage.visibility =
                if (!item.imageUrl.isNullOrEmpty() && item.expanded) View.VISIBLE else View.GONE
            notifyItemChanged(position)
        }

        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            item.purchased = isChecked
            onPurchasedChanged(item, isChecked)

            if (isChecked) {
                holder.itemView.setBackgroundResource(R.drawable.item_background_checked)
                holder.name.paintFlags = holder.name.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                holder.itemView.setBackgroundResource(R.drawable.item_unchecked_background)
                holder.name.paintFlags = holder.name.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }

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
            if (commentText.isNotEmpty()) {
                onCommentAdded(item, commentText)
                holder.commentInput.setText("")
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "לא ניתן לשלוח הודעה ריקה",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        holder.addImageButton.setOnClickListener { onImageAdded(item) }
        holder.selectFromGalleryButton.setOnClickListener { onGallerySelected(item) }
        holder.itemImage.visibility =
            if (!item.imageUrl.isNullOrEmpty()) View.VISIBLE else View.GONE
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ShoppingItem>) {
        items = newItems
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
