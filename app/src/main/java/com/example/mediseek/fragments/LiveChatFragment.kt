package com.example.mediseek.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.ChatAdapter
import com.example.mediseek.model.ChatMessage
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LiveChatFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()
    private lateinit var messageInput: EditText
    private lateinit var sendButton: MaterialButton

    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var chatId: String? = null
    private var childEventListener: ChildEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()
        val pharmacyId = "Demo_PID"
        val patientId = auth.currentUser?.uid

        chatId = if (patientId != null && pharmacyId != null) {
            listOf(patientId, pharmacyId).sorted().joinToString("_")
        } else null

        return inflater.inflate(R.layout.fragment_livechat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (chatId == null) {
            Toast.makeText(context, "Authentication required", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        initViews(view)
        setupDatabase()
        loadInitialMessages()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view)
        messageInput = view.findViewById(R.id.chat_message_input)
        sendButton = view.findViewById(R.id.send_chat_button)

        chatAdapter = ChatAdapter(messageList)
        chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupDatabase() {
        dbRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId!!)

        childEventListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(ChatMessage::class.java)
                message?.let {
                    if (!messageList.any { m -> m.timestamp == it.timestamp }) {
                        messageList.add(it)
                        updateMessages()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveChat", "Database error: ${error.message}")
            }
        }
        childEventListener?.let { dbRef.addChildEventListener(it) }
    }

    private fun loadInitialMessages() {
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val loadedMessages = snapshot.children.mapNotNull {
                    it.getValue(ChatMessage::class.java)
                }
                messageList.clear()
                messageList.addAll(loadedMessages)
                updateMessages()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveChat", "Initial load failed: ${error.message}")
            }
        })
    }

    private fun updateMessages() {
        messageList.sortBy { it.timestamp }
        chatAdapter.setData(messageList)
        chatRecyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener { sendMessage() }
        view?.findViewById<ImageButton>(R.id.back_button)?.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun sendMessage() {
        val messageText = messageInput.text.toString().trim()
        if (messageText.isNotEmpty() && auth.currentUser != null) {
            val message = ChatMessage(
                senderId = auth.currentUser!!.uid,
                text = messageText,
                timestamp = System.currentTimeMillis()
            )

            dbRef.push().setValue(message)
                .addOnSuccessListener {
                    messageInput.setText("")
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        childEventListener?.let { dbRef.removeEventListener(it) }
    }
}
