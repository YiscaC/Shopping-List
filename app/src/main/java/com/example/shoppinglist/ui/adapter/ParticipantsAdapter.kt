package com.example.shoppinglist.ui.adapter

import android.net.Uri
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
import de.hdodenhof.circleimageview.CircleImageView

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
            textUsername.text = user.username

            if (!user.localProfileImagePath.isNullOrEmpty()) {
                imageProfile.setImageURI(Uri.parse(user.localProfileImagePath))
            } else {
                imageProfile.setImageResource(R.drawable.ic_person)
            }

            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId == ownerId && user.uid != ownerId) {
                buttonDelete.visibility = View.VISIBLE
                buttonDelete.setOnClickListener { onDelete(user.uid) }
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
