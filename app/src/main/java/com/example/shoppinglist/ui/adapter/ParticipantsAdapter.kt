package com.example.shoppinglist.ui.adapter

import android.app.AlertDialog
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.shoppinglist.R
import com.example.shoppinglist.data.local.UserEntity
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView
import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan

class ParticipantsAdapter(
    var currentOwnerId: String, // הפכנו ל־var כדי שנוכל לעדכן בזמן ריצה
    private val onDeleteClicked: (String) -> Unit
) : ListAdapter<UserEntity, ParticipantsAdapter.ParticipantViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val participant = getItem(position)
        holder.bind(participant, currentOwnerId, onDeleteClicked)
    }

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageProfile: CircleImageView = itemView.findViewById(R.id.imageProfile)
        private val textUsername: TextView = itemView.findViewById(R.id.textUsername)
        private val buttonDelete: ImageView = itemView.findViewById(R.id.buttonDelete)
        fun bind(user: UserEntity, ownerId: String, onDelete: (String) -> Unit) {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

            // ✨ הכנת השם + תוספת (אני) או (מנהל)
            val label = when {
                user.uid == currentUserId -> " (אני)"
                user.uid == ownerId -> " (מנהל)"
                else -> ""
            }

            val fullText = user.username + label
            val spannable = SpannableString(fullText)

            // ✨ הדגשה של שם המשתמש
            spannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                user.username.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // ✨ עיצוב התוספת (אני / מנהל)
            if (label.isNotEmpty()) {
                spannable.setSpan(
                    ForegroundColorSpan(Color.GRAY),
                    fullText.indexOf(label),
                    fullText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    RelativeSizeSpan(0.8f),
                    fullText.indexOf(label),
                    fullText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            textUsername.text = spannable

            // ✨ הצגת תמונת פרופיל
            when {
                !user.localProfileImagePath.isNullOrEmpty() -> {
                    imageProfile.setImageURI(Uri.parse(user.localProfileImagePath))
                }
                !user.remoteProfileImageUrl.isNullOrEmpty() -> {
                    Picasso.get()
                        .load(user.remoteProfileImageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(imageProfile)
                }
                else -> {
                    imageProfile.setImageResource(R.drawable.default_profile)
                }
            }

            // ✨ כפתור מחיקה
            if (currentUserId == ownerId && user.uid != ownerId) {
                buttonDelete.visibility = View.VISIBLE
                buttonDelete.setOnClickListener {
                    // דיאלוג אישור מחיקה בעיצוב מותאם
                    val dialogView = LayoutInflater.from(itemView.context)
                        .inflate(R.layout.dialog_delete_confirmation, null)

                    val titleText = dialogView.findViewById<TextView>(R.id.deleteTitle)
                    val messageText = dialogView.findViewById<TextView>(R.id.deleteMessage)

                    titleText.text = "אישור מחיקה"
                    messageText.text = "האם אתה בטוח שברצונך למחוק את המשתמש: ${user.username}?"

                    AlertDialog.Builder(itemView.context)
                        .setView(dialogView)
                        .setPositiveButton("מחק") { _, _ ->
                            onDelete(user.uid)
                        }
                        .setNegativeButton("בטל", null)
                        .show()
                }
            } else {
                buttonDelete.visibility = View.GONE
            }
        }

    }

    class DiffCallback : DiffUtil.ItemCallback<UserEntity>() {
        override fun areItemsTheSame(oldItem: UserEntity, newItem: UserEntity) = oldItem.uid == newItem.uid
        override fun areContentsTheSame(oldItem: UserEntity, newItem: UserEntity) = oldItem == newItem
    }
}
