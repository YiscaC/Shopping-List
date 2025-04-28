package com.example.shoppinglist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
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
    private val onAddParticipantClick: (ShoppingList) -> Unit,
    private val onEditListClick: (ShoppingList) -> Unit,
    private val onDeleteListClick: (ShoppingList) -> Unit,
    private val onLeaveListClick: (ShoppingList) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingListViewHolder>() {

    inner class ShoppingListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.listName)
        val participantsContainer: LinearLayout = view.findViewById(R.id.participantsContainer)
        val btnAddParticipant: ImageButton = view.findViewById(R.id.btnAddParticipant)
        val btnEditList: ImageButton = view.findViewById(R.id.btnEditList)
        val btnDeleteList: ImageButton = view.findViewById(R.id.btnDeleteList)
        val btnLeaveList: ImageButton = view.findViewById(R.id.btnLeaveList)

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


            btnEditList.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditListClick(shoppingLists[position])
                }
            }

            btnDeleteList.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteListClick(shoppingLists[position])
                }
            }

            btnLeaveList.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLeaveListClick(shoppingLists[position])
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
            val density = holder.itemView.context.resources.displayMetrics.density

            val imageView = ShapeableImageView(holder.itemView.context).apply {
                val sizeInDp = 56
                val marginInDp = 6

                layoutParams = LinearLayout.LayoutParams(
                    (sizeInDp * density).toInt(),
                    (sizeInDp * density).toInt()
                ).apply {
                    setMargins(
                        (marginInDp * density).toInt(),
                        (marginInDp * density).toInt(),
                        (marginInDp * density).toInt(),
                        (marginInDp * density).toInt()
                    )
                }

                shapeAppearanceModel = shapeAppearanceModel
                    .toBuilder()
                    .setAllCornerSizes(28f)
                    .build()

                setImageResource(R.drawable.default_profile)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

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
