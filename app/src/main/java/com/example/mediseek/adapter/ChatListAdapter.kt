package com.example.mediseek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mediseek.R
import com.example.mediseek.model.ChatItem

class ChatListAdapter(
    private val onItemClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    // Initialize with an empty list
    private var chatList: List<ChatItem> = emptyList()

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val profileImage: ImageView = view.findViewById(R.id.chat_profile_image)
        private val username: TextView = view.findViewById(R.id.chat_username)
        private val newMessageDot: View = view.findViewById(R.id.new_message_dot)

        fun bind(chatItem: ChatItem) {
            username.text = chatItem.username

            Glide.with(itemView.context)
                .load(chatItem.profileImageUrl.ifEmpty { R.drawable.default_profile }) // Fallback image
                .circleCrop()
                .into(profileImage)

            newMessageDot.isVisible = chatItem.hasNewMessage

            itemView.setOnClickListener {
                onItemClick(chatItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chatList[position])
    }

    override fun getItemCount(): Int = chatList.size

    /**
     * Updates the list using DiffUtil for efficient UI updates and animations.
     */
    fun updateList(newList: List<ChatItem>) {
        val diffCallback = ChatListDiffCallback(this.chatList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.chatList = newList // Update the internal list
        diffResult.dispatchUpdatesTo(this) // Dispatch changes to the adapter
    }
}

/**
 * DiffUtil Callback to calculate differences between two lists of ChatItems.
 * This helps the adapter perform efficient updates.
 */
private class ChatListDiffCallback(
    private val oldList: List<ChatItem>,
    private val newList: List<ChatItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    /**
     * Called to check if two items represent the same object.
     * We use the unique userId for this check.
     */
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].userId == newList[newItemPosition].userId
    }

    /**
     * Called to check if the visual representation of two items is the same.
     * Since ChatItem is a data class, the auto-generated equals() method
     * checks all properties (username, image URL, hasNewMessage).
     */
    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}