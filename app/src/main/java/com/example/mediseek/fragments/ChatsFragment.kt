package com.example.mediseek.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.ChatListAdapter
import com.example.mediseek.model.ChatItem
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class ChatsFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatListAdapter
    private lateinit var tvNoChats: View
    private val chatList = mutableListOf<ChatItem>()

    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference.child("chats")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view)
        tvNoChats = view.findViewById(R.id.tv_no_chats)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatAdapter = ChatListAdapter(chatList) { chatItem ->
            val bundle = Bundle().apply {
                putString("patientId", chatItem.userId)
            }
            findNavController().navigate(R.id.chat, bundle)
        }


        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter

        loadChatList()
    }

    private fun loadChatList() {
        realtimeDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()

                if (!snapshot.exists()) {
                    // Show "No chats" UI
                    chatRecyclerView.visibility = View.GONE
                    tvNoChats.visibility = View.VISIBLE
                    return
                }

                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue
                    val userId = chatId.substringAfter("_") // Extract userId from Demo_PID_xxx

                    firestore.collection("Users").document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val username = document.getString("username") ?: "Unknown"
                                val profileImage = document.getString("imgUrl") ?: ""
                                val hasNewMessage = chatSnapshot.children.any {
                                    it.child("isRead").value == false
                                }

                                val chatItem = ChatItem(
                                    userId = userId,
                                    username = username,
                                    profileImageUrl = profileImage,
                                    hasNewMessage = hasNewMessage
                                )
                                chatList.add(chatItem)
                                chatAdapter.updateList(chatList)

                                // Update visibility
                                chatRecyclerView.visibility = View.VISIBLE
                                tvNoChats.visibility = View.GONE
                            }
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
