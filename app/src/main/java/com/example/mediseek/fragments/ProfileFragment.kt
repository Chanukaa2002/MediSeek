package com.example.mediseek.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mediseek.LoginActivity
import com.example.mediseek.databinding.FragmentProfileBinding
import com.example.mediseek.service.ImageService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var imageService: ImageService
    private var selectedImageUri: Uri? = null
    private lateinit var currentPhotoPath: String
    private val TAG = "ProfileFragment"

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri?.let { uri ->
                handleSelectedImage(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        imageService = ImageService.getInstance(requireContext())

        if (auth.currentUser == null) {
            showError("Please login first")
            return
        }

        setupViews()
        loadUserData()
    }

    private fun setupViews() {
        binding.profileImageView.setOnClickListener {
            showImageSelectionDialog()
        }

        binding.updateProfileButton.setOnClickListener {
            if (validateInputs()) {
                updateUserProfile()
            }
        }

        binding.logoutButton.setOnClickListener {
            logoutUser()
        }
    }

    private fun logoutUser() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                auth.signOut()
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showImageSelectionDialog() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Profile Photo")
        builder.setItems(options) { dialog, item ->
            when (options[item]) {
                "Take Photo" -> checkCameraPermissionAndOpenCamera()
                "Choose from Gallery" -> openGallery()
                "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 1001)
        } else {
            openCamera()
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        try {
            val photoFile = createImageFile()
            val photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                photoFile
            )
            selectedImageUri = photoUri
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Log.e(TAG, "Camera intent failed: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to open camera", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(Exception::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = requireContext().externalCacheDir
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        selectedImageUri = uri
        binding.profileImageView.setImageURI(uri)
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        setLoading(true)

        db.collection("Users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener

                if (document.exists()) {
                    document.getString("username")?.let {
                        binding.usernameEditText.setText(it)
                    }
                    document.getString("email")?.let {
                        binding.emailEditText.setText(it)
                    }
                    document.getString("mobile")?.let {
                        binding.mobileEditText.setText(it)
                    }
                    document.getString("imgUrl")?.takeIf { it.isNotEmpty() }?.let { imageUrl ->
                        lifecycleScope.launch {
                            val bitmap = imageService.loadImage(imageUrl)
                            bitmap?.let {
                                binding.profileImageView.setImageBitmap(it)
                            }
                        }
                    }
                }
                setLoading(false)
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                showError("Failed to load profile")
                Log.e(TAG, "Error loading user data", e)
                setLoading(false)
            }
    }

    private fun validateInputs(): Boolean {
        return when {
            binding.usernameEditText.text.isNullOrBlank() -> {
                binding.usernameEditText.error = "Username required"
                false
            }
            binding.emailEditText.text.isNullOrBlank() -> {
                binding.emailEditText.error = "Email required"
                false
            }
            else -> true
        }
    }

    private fun updateUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        setLoading(true)

        val updates = hashMapOf<String, Any>(
            "username" to binding.usernameEditText.text.toString().trim(),
            "email" to binding.emailEditText.text.toString().trim(),
            "mobile" to binding.mobileEditText.text.toString().trim()
        )

        db.collection("Users").document(userId).update(updates)
            .addOnSuccessListener {
                selectedImageUri?.let { uri ->
                    uploadProfileImage(userId, uri)
                } ?: run {
                    setLoading(false)
                    showSuccess("Profile updated successfully")
                }
            }
            .addOnFailureListener { e ->
                setLoading(false)
                showError("Failed to update profile: ${e.localizedMessage}")
                Log.e(TAG, "Error updating profile", e)
            }
    }

    private fun uploadProfileImage(userId: String, imageUri: Uri) {
        imageService.uploadProfileImage(
            userId = userId,
            imageUri = imageUri,
            onSuccess = { imageUrl ->
                db.collection("Users").document(userId)
                    .update("imgUrl", imageUrl)
                    .addOnSuccessListener {
                        setLoading(false)
                        showSuccess("Profile and image updated")
                        selectedImageUri = null
                    }
                    .addOnFailureListener { e ->
                        setLoading(false)
                        showError("Profile updated but failed to save image URL")
                        Log.e(TAG, "Error saving image URL", e)
                    }
            },
            onError = { error ->
                setLoading(false)
                showError("Profile updated but image upload failed")
                Log.e(TAG, "Image upload error: $error")
            }
        )
    }

    private fun setLoading(isLoading: Boolean) {
        binding.updateProfileButton.isEnabled = !isLoading
        binding.updateProfileButton.text = if (isLoading) "Saving..." else "Save Changes"
    }

    private fun showSuccess(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
