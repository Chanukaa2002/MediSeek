package com.example.mediseek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mediseek.R
import com.example.mediseek.model.ChatItem

class ChatListAdapter(
    private var chatList: List<ChatItem>,
    private val onItemClick: (ChatItem) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.chat_profile_image)
        val username: TextView = view.findViewById(R.id.chat_username)
        val newMessageDot: View = view.findViewById(R.id.new_message_dot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_list, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chatItem = chatList[position]

        holder.username.text = chatItem.username

        Glide.with(holder.itemView.context)
            .load(chatItem.profileImageUrl.ifEmpty { R.drawable.default_profile }) // fallback image
            .circleCrop()
            .into(holder.profileImage)

        holder.newMessageDot.isVisible = chatItem.hasNewMessage

        holder.itemView.setOnClickListener {
            onItemClick(chatItem)
        }
    }

    override fun getItemCount(): Int = chatList.size

    fun updateList(newList: List<ChatItem>) {
        chatList = newList
        notifyDataSetChanged()
    }
}
