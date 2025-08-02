package com.example.mediseek.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private var messages: List<ChatMessage>) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    private val VIEW_TYPE_SENT = 1
    private val VIEW_TYPE_RECEIVED = 2
    private val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun setData(newMessages: List<ChatMessage>) {
        messages = newMessages.sortedBy { it.timestamp }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].senderId == currentUserUid) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layout = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        Log.d("ChatAdapter", "Binding message: ${message.text}")
        holder.messageText.text = message.text
        holder.timeText.text = dateFormat.format(Date(message.timestamp))
    }

    override fun getItemCount() = messages.size

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val timeText: TextView = itemView.findViewById(R.id.message_time)
    }
}
