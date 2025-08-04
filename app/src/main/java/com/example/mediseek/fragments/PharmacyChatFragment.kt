package com.example.mediseek.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediseek.adapter.ChatAdapter
import com.example.mediseek.databinding.FragmentLivechatBinding
import com.example.mediseek.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class PharmacyChatFragment : Fragment() {

    private var _binding: FragmentLivechatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var chatId: String? = null
    private var childEventListener: ChildEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLivechatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        // Check if pharmacy is logged in
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        // Receive patientId from arguments
        val patientId = arguments?.getString("patientId") ?: ""
        val pharmacyId = auth.currentUser?.uid ?: ""

        chatId = if (patientId.isNotEmpty() && pharmacyId.isNotEmpty()) {
            listOf(patientId, pharmacyId).sorted().joinToString("_")
        } else null

        if (chatId == null) {
            Toast.makeText(context, "Invalid chat session", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        initViews()
        setupDatabase()
        loadInitialMessages()
        setupClickListeners()
    }

    private fun initViews() {
        chatAdapter = ChatAdapter(messageList)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }

        // Hide attach button since pharmacy doesn't upload images
        binding.attachButton.visibility = View.GONE
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
                Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Failed to load messages", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateMessages() {
        messageList.sortBy { it.timestamp }
        chatAdapter.setData(messageList)
        binding.chatRecyclerView.scrollToPosition(messageList.size - 1)
    }

    private fun setupClickListeners() {
        binding.sendChatButton.setOnClickListener {
            val messageText = binding.chatMessageInput.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            }
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun sendMessage(text: String) {
        val currentUser = auth.currentUser ?: return

        val message = ChatMessage(
            senderId = currentUser.uid,
            text = text,
            timestamp = System.currentTimeMillis(),
            type = "text"
        )

        dbRef.push().setValue(message)
            .addOnSuccessListener {
                binding.chatMessageInput.setText("")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to send: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        childEventListener?.let { dbRef.removeEventListener(it) }
        _binding = null
    }
}
