package com.example.shoppinglist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.models.ShoppingItem

class ShoppingItemsAdapter(
    private val items: List<ShoppingItem>,
    private val onItemClick: (ShoppingItem) -> Unit, // ✅ מאזין ללחיצות
    private val onPurchasedChanged: (ShoppingItem, Boolean) -> Unit // ✅ עדכון קנייה
) : RecyclerView.Adapter<ShoppingItemsAdapter.ShoppingItemViewHolder>() {

    inner class ShoppingItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.itemName)
        val quantity: TextView = view.findViewById(R.id.itemQuantity)
        val image: ImageView = view.findViewById(R.id.itemImage)
        val purchasedCheckBox: CheckBox = view.findViewById(R.id.itemPurchased)
        val detailsLayout: LinearLayout = view.findViewById(R.id.itemDetailsLayout) // ✅ פרטי מוצר

        init {
            name.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    items[position].expanded = !items[position].expanded // ✅ הפוך את מצב הפריט
                    notifyItemChanged(position)
                }
            }

            purchasedCheckBox.setOnCheckedChangeListener { _, isChecked ->
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onPurchasedChanged(items[position], isChecked) // ✅ עדכון Firebase
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return ShoppingItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingItemViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.quantity.text = "Quantity: ${item.quantity}"
        holder.purchasedCheckBox.isChecked = item.purchased
        holder.detailsLayout.visibility = if (item.expanded) View.VISIBLE else View.GONE // ✅ הצגת פרטים רק אם פתוח
    }

    override fun getItemCount(): Int = items.size
}
