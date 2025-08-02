package com.example.mediseek.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mediseek.R
import com.example.mediseek.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private var messages: List<ChatMessage>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val VIEW_TYPE_SENT_TEXT = 1
    private val VIEW_TYPE_RECEIVED_TEXT = 2
    private val VIEW_TYPE_SENT_IMAGE = 3
    private val VIEW_TYPE_RECEIVED_IMAGE = 4

    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun setData(newMessages: List<ChatMessage>) {
        messages = newMessages.sortedBy { it.timestamp }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            messages[position].senderId == currentUserUid && messages[position].type == "text" -> VIEW_TYPE_SENT_TEXT
            messages[position].senderId == currentUserUid && messages[position].type == "image" -> VIEW_TYPE_SENT_IMAGE
            messages[position].type == "image" -> VIEW_TYPE_RECEIVED_IMAGE
            else -> VIEW_TYPE_RECEIVED_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_sent, parent, false)
                TextMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED_TEXT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message_received, parent, false)
                TextMessageViewHolder(view)
            }
            VIEW_TYPE_SENT_IMAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_sent, parent, false)
                ImageMessageViewHolder(view)
            }
            VIEW_TYPE_RECEIVED_IMAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image_received, parent, false)
                ImageMessageViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is TextMessageViewHolder -> {
                holder.messageText.text = message.text
                holder.timeText.text = dateFormat.format(Date(message.timestamp))
            }
            is ImageMessageViewHolder -> {
                Glide.with(holder.itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.messageImage)
                holder.timeText.text = dateFormat.format(Date(message.timestamp))

                holder.messageImage.setOnClickListener {
                }
            }
        }
    }

    override fun getItemCount() = messages.size

    class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val timeText: TextView = itemView.findViewById(R.id.message_time)
    }

    class ImageMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageImage: ImageView = itemView.findViewById(R.id.message_image)
        val timeText: TextView = itemView.findViewById(R.id.message_time)
    }
}