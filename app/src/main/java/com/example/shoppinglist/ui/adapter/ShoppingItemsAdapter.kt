package com.example.shoppinglist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.ShoppingItem

class ShoppingItemsAdapter(
    private var items: List<ShoppingItem>,
    private val onItemClick: (ShoppingItem) -> Unit,
    private val onPurchasedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onQuantityChanged: (ShoppingItem, Int) -> Unit,
    private val onCommentAdded: (ShoppingItem, String) -> Unit,
    private val onImageAdded: (ShoppingItem) -> Unit,
    private val onGallerySelected: (ShoppingItem) -> Unit
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

        // ✅ הצגת שם הפריט
        holder.name.text = item.name
        holder.checkbox.isChecked = item.purchased
        holder.quantityText.setText(item.quantity.toString())

        // ✅ פתיחת אזורי כמות, הערות ותמונה בעת לחיצה על הפריט
        holder.name.setOnClickListener {
            item.expanded = !item.expanded
            holder.quantityLayout.visibility = if (item.expanded) View.VISIBLE else View.GONE
            holder.commentsSection.visibility = if (item.expanded) View.VISIBLE else View.GONE
            holder.itemImage.visibility = if (!item.imageUrl.isNullOrEmpty() && item.expanded) View.VISIBLE else View.GONE
            notifyItemChanged(position)
        }

        // ✅ עדכון אם פריט נקנה
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onPurchasedChanged(item, isChecked)
        }

        // ✅ שינוי כמות הפריט
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

        // ✅ שליחת תגובה לפריט
        holder.sendCommentButton.setOnClickListener {
            val commentText = holder.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                onCommentAdded(item, commentText)
                holder.commentInput.setText("")
            } else {
                Toast.makeText(holder.itemView.context, "לא ניתן לשלוח הודעה ריקה", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ פתיחת המצלמה להוספת תמונה
        holder.addImageButton.setOnClickListener {
            onImageAdded(item)
        }

        // ✅ פתיחת הגלריה לבחירת תמונה
        holder.selectFromGalleryButton.setOnClickListener {
            onGallerySelected(item)
        }

        // ✅ הצגת תמונה אם קיימת
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.itemImage.visibility = View.VISIBLE
            // אפשר להשתמש ב-Glide או Picasso כדי לטעון תמונה
            // Glide.with(holder.itemView.context).load(item.imageUrl).into(holder.itemImage)
        } else {
            holder.itemImage.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<ShoppingItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
