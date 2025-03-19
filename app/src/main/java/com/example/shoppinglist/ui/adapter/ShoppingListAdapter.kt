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

class ShoppingListAdapter(
    private var shoppingLists: List<ShoppingList>,
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
                onAddParticipantClick(shoppingLists[adapterPosition])
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

        // ניקוי משתתפים קודמים
        holder.participantsContainer.removeAllViews()

        // הצגת המשתתפים
        shoppingList.participants.keys.forEach { participant ->
            val textView = TextView(holder.itemView.context).apply {
                text = participant.first().toString() // הצגת האות הראשונה
                setPadding(8, 8, 8, 8)
                setBackgroundResource(R.drawable.circle_background)
            }
            holder.participantsContainer.addView(textView)
        }
    }

    override fun getItemCount(): Int = shoppingLists.size

    fun updateLists(newLists: List<ShoppingList>) {
        shoppingLists = newLists
        notifyDataSetChanged()
    }
}
