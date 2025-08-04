package com.example.mediseek.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.mediseek.R
import com.example.mediseek.adapter.ChatAdapter
import com.example.mediseek.databinding.FragmentLivechatBinding
import com.example.mediseek.model.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LiveChatFragment : Fragment() {

    private var _binding: FragmentLivechatBinding? = null
    private val binding get() = _binding!!
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private lateinit var dbRef: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var chatId: String? = null
    private var childEventListener: ChildEventListener? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadImage(uri)
            }
        }
    }

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

        try {
            auth = FirebaseAuth.getInstance()

            // Validate user is authenticated
            if (auth.currentUser == null) {
                Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return
            }

            val pharmacyId = "8dJivB4LICOAzuwwCSTcsy3WkoR2"
            val patientId = auth.currentUser?.uid

            chatId = if (patientId != null && pharmacyId.isNotEmpty()) {
                listOf(patientId, pharmacyId).sorted().joinToString("_")
            } else {
                null
            }

            if (chatId == null) {
                Toast.makeText(context, "Failed to start chat", Toast.LENGTH_SHORT).show()
                findNavController().navigateUp()
                return
            }

            initViews()
            setupDatabase()
            loadInitialMessages()
            setupClickListeners()

        } catch (e: Exception) {
            Log.e("LiveChatFragment", "Initialization error", e)
            Toast.makeText(context, "Error initializing chat", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }
    }

    private fun initViews() {
        chatAdapter = ChatAdapter(messageList)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun setupDatabase() {
        try {
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
        } catch (e: Exception) {
            Log.e("LiveChatFragment", "Database setup error", e)
            Toast.makeText(context, "Database error", Toast.LENGTH_SHORT).show()
        }
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

        binding.attachButton.setOnClickListener {
            openImagePicker()
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun openImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            galleryLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to open gallery", Toast.LENGTH_SHORT).show()
            Log.e("LiveChatFragment", "Image picker error", e)
        }
    }

    private fun uploadImage(imageUri: Uri) {
        try {
            binding.uploadProgressBar.visibility = View.VISIBLE
            binding.sendChatButton.isEnabled = false
            binding.attachButton.isEnabled = false

            val userId = auth.currentUser?.uid ?: return
            val timestamp = System.currentTimeMillis()
            val fileName = "chat_${userId}_$timestamp"

            MediaManager.get().upload(imageUri)
                .option("public_id", fileName)
                .option("folder", "mediseek/chat_images")
                .option("transformation", "w_800,h_800,c_limit,q_auto")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Uploading image...", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        activity?.runOnUiThread {
                            binding.uploadProgressBar.progress = (bytes * 100 / totalBytes).toInt()
                        }
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        activity?.runOnUiThread {
                            binding.uploadProgressBar.visibility = View.GONE
                            binding.sendChatButton.isEnabled = true
                            binding.attachButton.isEnabled = true

                            val imageUrl = resultData["secure_url"] as? String
                            if (imageUrl != null) {
                                sendImageMessage(imageUrl)
                                Toast.makeText(context, "Upload complete", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to get image URL", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        activity?.runOnUiThread {
                            binding.uploadProgressBar.visibility = View.GONE
                            binding.sendChatButton.isEnabled = true
                            binding.attachButton.isEnabled = true
                            Toast.makeText(context,
                                "Upload failed: ${error.description}",
                                Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        activity?.runOnUiThread {
                            Toast.makeText(context, "Upload rescheduled", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
                .dispatch()
        } catch (e: Exception) {
            binding.uploadProgressBar.visibility = View.GONE
            binding.sendChatButton.isEnabled = true
            binding.attachButton.isEnabled = true
            Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
            Log.e("LiveChatFragment", "Image upload error", e)
        }
    }

    private fun sendMessage(text: String) {
        try {
            if (auth.currentUser == null) return

            val message = ChatMessage(
                senderId = auth.currentUser!!.uid,
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
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send message", Toast.LENGTH_SHORT).show()
            Log.e("LiveChatFragment", "Message send error", e)
        }
    }

    private fun sendImageMessage(imageUrl: String) {
        try {
            if (auth.currentUser == null) return

            val message = ChatMessage(
                senderId = auth.currentUser!!.uid,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis(),
                type = "image"
            )

            dbRef.push().setValue(message)
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to send image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to send image", Toast.LENGTH_SHORT).show()
            Log.e("LiveChatFragment", "Image message send error", e)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        try {
            childEventListener?.let { dbRef.removeEventListener(it) }
        } catch (e: Exception) {
            Log.e("LiveChatFragment", "Cleanup error", e)
        }
        _binding = null
    }
}