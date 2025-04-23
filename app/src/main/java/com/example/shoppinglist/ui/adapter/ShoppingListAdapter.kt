package com.example.shoppinglist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.ShoppingList
import com.google.android.material.imageview.ShapeableImageView
import com.squareup.picasso.Picasso

class ShoppingListAdapter(
    private var shoppingLists: List<ShoppingList>,
    private var participantImages: Map<String, String>,
    private val onItemClick: (ShoppingList) -> Unit,
    private val onAddParticipantClick: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingListViewHolder>() {

    inner class ShoppingListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.listName)
        val participantsContainer: LinearLayout = view.findViewById(R.id.participantsContainer)
        val btnAddParticipant: ImageButton = view.findViewById(R.id.btnAddParticipant)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(shoppingLists[position])
                }
            }

            btnAddParticipant.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onAddParticipantClick(shoppingLists[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ShoppingListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        val shoppingList = shoppingLists[position]
        holder.name.text = shoppingList.name

        // ניקוי המשתתפים הקודמים
        holder.participantsContainer.removeAllViews()

        // הצגת תמונות משתתפים לפי UID
        shoppingList.participants.keys.forEach { participantUid ->
            val imageView = ShapeableImageView(holder.itemView.context).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    setMargins(8, 8, 8, 8)
                }
                // ברירת מחדל לתמונה דיפולטית
                setImageResource(R.drawable.default_profile)
            }

            // אם יש תמונה למשתמש – נטען אותה
            val imageUrl = participantImages[participantUid]
            if (!imageUrl.isNullOrEmpty()) {
                Picasso.get().load(imageUrl).placeholder(R.drawable.default_profile).into(imageView)
            }

            holder.participantsContainer.addView(imageView)
        }
    }

    override fun getItemCount(): Int = shoppingLists.size

    fun updateLists(newLists: List<ShoppingList>, newImages: Map<String, String>) {
        shoppingLists = newLists
        participantImages = newImages
        notifyDataSetChanged()
    }
}
