package com.example.shoppinglist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.ShoppingList

class ShoppingListAdapter(
    private var shoppingLists: List<ShoppingList>,
    private val onItemClick: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingListViewHolder>() {

    inner class ShoppingListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.listName)

        init {
            view.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(shoppingLists[position])
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
    }

    override fun getItemCount(): Int = shoppingLists.size

    fun updateLists(newLists: List<ShoppingList>) {
        shoppingLists = newLists
        notifyDataSetChanged()
    }
}
