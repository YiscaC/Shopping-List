package com.example.shoppinglist.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.models.Message
import com.squareup.picasso.Picasso

class MessagesAdapter(

    private var messages: List<Message>,
    private val currentUserId: String,


) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.messageText)
        val image: ImageView = view.findViewById(R.id.messageImage)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserId) 1 else 0
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == 1) R.layout.item_message_mine else R.layout.item_message_other
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {

        val message = messages[position]
        if (message.text.isNullOrEmpty() && message.imageUrl.isNullOrEmpty()) {
            holder.text.visibility = View.GONE
            holder.image.visibility = View.GONE
            return
        }

        // טקסט
        if (!message.text.isNullOrEmpty()) {
            holder.text.visibility = View.VISIBLE
            holder.text.text = message.text
        } else {
            holder.text.visibility = View.GONE
        }

        // תמונה
        if (!message.imageUrl.isNullOrEmpty()) {
            holder.image.visibility = View.VISIBLE
            val imageUrl = message.imageUrl

            // תמונה מקומית מ־content://
            if (imageUrl.startsWith("content://")) {
                holder.image.setImageURI(android.net.Uri.parse(imageUrl))
            }
            // תמונה זמנית שנטענה מ־ByteArray
            else if (imageUrl == "local_byte_image") {
                // לא נטען כלום (זה רק לייצוג פנימי, כבר מוצגת מראש בפריט)
                holder.image.visibility = View.GONE
            }
            // אחרת – נטען מ־Firebase
            if (messages.isNullOrEmpty()) return

            else {
                try {
                    if (imageUrl.startsWith("http")) {
                        Picasso.get().load(imageUrl).into(holder.image)
                    } else {
                        holder.image.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    holder.image.visibility = View.GONE
                    e.printStackTrace()
                }
            }

        } else {
            holder.image.visibility = View.GONE
        }
    }


    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }

    class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            // אם יש לך מזהה ייחודי להודעה אפשר להשתמש בו כאן
            return oldList[oldItemPosition].timestamp == newList[newItemPosition].timestamp
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

    }
}
