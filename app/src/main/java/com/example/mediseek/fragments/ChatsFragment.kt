package com.example.mediseek.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mediseek.R
import com.example.mediseek.adapter.ChatListAdapter
import com.example.mediseek.db.ChatListDbHelper
import com.example.mediseek.model.ChatItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class ChatsFragment : Fragment() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var chatAdapter: ChatListAdapter
    private lateinit var tvNoChats: View

    private lateinit var dbHelper: ChatListDbHelper
    private val firestore = FirebaseFirestore.getInstance()
    private val realtimeDb = FirebaseDatabase.getInstance().reference.child("chats")

    // ... (onCreateView and setupRecyclerView are the same) ...
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chats, container, false)
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view)
        tvNoChats = view.findViewById(R.id.tv_no_chats)

        dbHelper = ChatListDbHelper(requireContext())

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadChatsFromDb()
        fetchChatsFromFirebase()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatListAdapter { chatItem ->
            val bundle = Bundle().apply {
                putString("patientId", chatItem.userId)
            }
            findNavController().navigate(R.id.chat, bundle)
        }
        chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        chatRecyclerView.adapter = chatAdapter
    }


    /**
     * CORRECTION: Runs the database query in a background coroutine.
     */
    private fun loadChatsFromDb() {
        lifecycleScope.launch {
            val cachedChats = dbHelper.getAllChats() // This is now a suspend call
            Log.d("ChatsFragment", "Loaded ${cachedChats.size} chats from local DB.")
            updateUI(cachedChats)
        }
    }

    private fun fetchChatsFromFirebase() {
        val currentPharmacyId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        realtimeDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // CORRECTION: Database saving now happens in a background coroutine.
                lifecycleScope.launch {
                    if (!snapshot.exists()) {
                        dbHelper.saveChatList(emptyList())
                        updateUI(emptyList())
                        return@launch
                    }

                    val fetchedChatItems = mutableListOf<ChatItem>()
                    // ... (The rest of the Firebase fetching logic is complex to do in coroutines,
                    // so we can keep it as is, and just save the final result in a coroutine)

                    // The original implementation with counters is fine, but let's simplify for clarity
                    // (Note: a more advanced implementation would use Kotlin Coroutines for Firebase, but this is a direct fix)

                    // Let's stick to the user's existing logic with a small change for saving.
                    val chatsToProcess = snapshot.children.count()
                    var processedChats = 0

                    for (chatSnapshot in snapshot.children) {
                        val chatId = chatSnapshot.key ?: continue
                        val parts = chatId.split("_")
                        if (parts.size != 2 || parts[0] != currentPharmacyId) {
                            processedChats++
                            continue
                        }

                        val clientId = parts[1]
                        firestore.collection("Users").document(clientId)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    fetchedChatItems.add(
                                        ChatItem(
                                            userId = clientId,
                                            username = document.getString("username") ?: "Unknown",
                                            profileImageUrl = document.getString("imgUrl") ?: "",
                                            hasNewMessage = false // Your logic for new messages here
                                        )
                                    )
                                }
                            }
                            .addOnCompleteListener {
                                processedChats++
                                if (processedChats == chatsToProcess) {
                                    // CORRECTION: Save to DB and update UI from a coroutine
                                    lifecycleScope.launch {
                                        Log.d("ChatsFragment", "Fetched ${fetchedChatItems.size} chats from Firebase.")
                                        dbHelper.saveChatList(fetchedChatItems) // suspend call
                                        val newCachedChats = dbHelper.getAllChats() // suspend call
                                        updateUI(newCachedChats)
                                    }
                                }
                            }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatsFragment", "Firebase fetch cancelled: ${error.message}")
            }
        })
    }

    private fun updateUI(chats: List<ChatItem>) {
        if (chats.isEmpty()) {
            chatRecyclerView.visibility = View.GONE
            tvNoChats.visibility = View.VISIBLE
        } else {
            val sortedList = chats.sortedByDescending { it.hasNewMessage }
            chatAdapter.updateList(sortedList)
            chatRecyclerView.visibility = View.VISIBLE
            tvNoChats.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbHelper.close()
    }
}