package com.example.mediseek.service
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ImageService private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ImageService? = null

        fun getInstance(context: Context): ImageService {
            return INSTANCE ?: synchronized(this) {
                val instance = ImageService(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }

    fun uploadProfileImage(
        userId: String,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        MediaManager.get().upload(imageUri)
            .callback(object : UploadCallback {
                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val imageUrl = resultData["secure_url"] as? String
                    if (imageUrl != null) {
                        onSuccess(imageUrl)
                    } else {
                        onError("Upload succeeded but URL was null")
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    onError("Image upload failed: ${error.description}")
                }

                override fun onStart(requestId: String) {}
                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                override fun onReschedule(requestId: String, error: ErrorInfo) {}
            })
            .dispatch()
    }

    suspend fun loadImage(imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrEmpty()) return null

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                BitmapFactory.decodeStream(connection.inputStream)
            } catch (e: Exception) {
                null
            }
        }
    }
}