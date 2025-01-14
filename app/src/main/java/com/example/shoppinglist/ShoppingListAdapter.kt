package com.example.shoppinglist
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
//import com.example.shoppinglist.R

class ShoppingListAdapter(
    private val items: List<ShoppingItem> // רשימת הפריטים להצגה
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingViewHolder>() {

    // ViewHolder מייצג פריט בודד ב-RecyclerView
    inner class ShoppingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.itemNameTextView)
        val quantity: TextView = view.findViewById(R.id.itemQuantityTextView)
        val purchased: TextView = view.findViewById(R.id.itemPurchasedTextView)
        val image: ImageView = view.findViewById(R.id.itemImageView)
    }

    // יוצרים ViewHolder חדש (לכל פריט)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping, parent, false)
        return ShoppingViewHolder(view)
    }

    // ממלאים את המידע בפריט (binding)
    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val item = items[position] // הפריט הנוכחי
        holder.name.text = item.name
        holder.quantity.text = "Quantity: ${item.quantity}"
        holder.purchased.text = if (item.purchased) "Purchased" else "Not Purchased"

        // טיפול בתמונה (באמצעות Glide או Placeholder)
        if (item.imageUrl != null) {
            // דוגמה לשימוש ב-Glide:
            // Glide.with(holder.image.context).load(item.imageUrl).into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.placeholder_image)
        }
    }

    // מספר הפריטים ברשימה
    override fun getItemCount(): Int = items.size
}
