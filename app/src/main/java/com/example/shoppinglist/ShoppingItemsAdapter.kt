package com.example.shoppinglist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.models.ShoppingItem

class ShoppingItemsAdapter(
    private val items: List<ShoppingItem>,
    private val onItemClick: (ShoppingItem) -> Unit,
    private val onPurchasedChanged: (ShoppingItem, Boolean) -> Unit,
    private val onQuantityChanged: (ShoppingItem, Int) -> Unit,
    private val onCommentAdded: (ShoppingItem, String) -> Unit,
    private val onImageAdded: (ShoppingItem) -> Unit
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
        val itemImage: ImageView = view.findViewById(R.id.itemImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.checkbox.isChecked = item.purchased
        holder.quantityText.setText(item.quantity.toString())

        // ✅ לחיצה על שם המוצר -> פותחת את כל האזורים הנוספים
        holder.name.setOnClickListener {
            item.expanded = !item.expanded
            holder.quantityLayout.visibility = if (item.expanded) View.VISIBLE else View.GONE
            holder.commentsSection.visibility = if (item.expanded) View.VISIBLE else View.GONE
            holder.itemImage.visibility = if (!item.imageUrl.isNullOrEmpty() && item.expanded) View.VISIBLE else View.GONE
            notifyDataSetChanged()
        }

        // ✅ עדכון מצב "נקנה"
        holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
            onPurchasedChanged(item, isChecked)
        }

        // ✅ שינוי כמות
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
    }

    override fun getItemCount(): Int = items.size
}
