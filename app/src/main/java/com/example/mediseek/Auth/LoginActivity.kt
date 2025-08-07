package com.example.mediseek
import com.example.mediseek.LoginActivity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    // Request code for Google Sign-In
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Get your web_client_id from google-services.json
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val usernameInput = findViewById<TextInputEditText>(R.id.username_input)
        val passwordInput = findViewById<TextInputEditText>(R.id.password_input)
        val loginButton = findViewById<MaterialButton>(R.id.login_button)
        val signUpText = findViewById<TextView>(R.id.sign_up_text)
        val googleLoginButton = findViewById<MaterialButton>(R.id.google_login_button) // Get reference to Google Sign-In button

        loginButton.setOnClickListener {
            val username = usernameInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                showToast("Please enter username and password")
                return@setOnClickListener
            }

            loginWithUsername(username, password)
        }

        googleLoginButton.setOnClickListener {
            signInWithGoogle()
        }

        signUpText.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                // Google Sign In was successful, authenticate with Firebase
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                // Google Sign In failed, update UI appropriately
                showToast("Google Sign In failed: ${e.statusCode} - ${e.message}")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    val user = auth.currentUser
                    // Check if the user already exists in Firestore's "Users" collection
                    firestore.collection("Users").document(user?.uid ?: "")
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                // User exists, proceed with role-based navigation
                                val role = documentSnapshot.getString("role")
                                val status = documentSnapshot.getString("status")

                                showToast("Login Successful with Google")
                                when (role) {
                                    "Client" -> navigateTo(ClientActivity::class.java)
                                    "Admin" -> navigateTo(AdminActivity::class.java)
                                    "Pharmacy" -> {
                                        if (status == "approved") {
                                            navigateTo(PhamacyActivity::class.java)
                                        } else {
                                            showToast("Your account is pending approval. Please wait for admin approval.")
                                            auth.signOut() // Sign out if not approved
                                        }
                                    }
                                    else -> {
                                        showToast("Role not recognized. Please contact support.")
                                        auth.signOut() // Sign out if role is not recognized
                                    }
                                }
                            } else {
                                // New user signing in with Google.
                                // You might want to prompt them to choose a role or register them with a default role.
                                // For now, let's assume new Google sign-ins are "Client" and add them to Firestore.
                                val newUserRef = firestore.collection("Users").document(user?.uid ?: "")
                                val userData = hashMapOf(
                                    "username" to (user?.displayName ?: user?.email?.split("@")?.get(0)),
                                    "email" to user?.email,
                                    "role" to "Client", // Default role for new Google sign-ins
                                    "status" to "approved" // Default status for new Google sign-ins
                                )
                                newUserRef.set(userData)
                                    .addOnSuccessListener {
                                        showToast("Google Login Successful. Welcome!")
                                        navigateTo(ClientActivity::class.java)
                                    }
                                    .addOnFailureListener { e ->
                                        showToast("Error creating user profile: ${e.message}")
                                        auth.signOut() // Sign out on failure
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            showToast("Error checking user existence: ${e.message}")
                            auth.signOut()
                        }

                } else {
                    // If sign in fails, display a message to the user.
                    showToast("Google Login Failed: ${task.exception?.message}")
                }
            }
    }


    private fun loginWithUsername(username: String, password: String) {
        firestore.collection("Users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showToast("Username not found")
                    return@addOnSuccessListener
                }

                val userDoc = documents.documents[0]
                val email = userDoc.getString("email")

                if (email == null) {
                    showToast("Error: Email not found for this user.")
                    return@addOnSuccessListener
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showToast("Login Successful")
                            val role = userDoc.getString("role")
                            val status = userDoc.getString("status")

                            when (role) {
                                "Client" -> navigateTo(ClientActivity::class.java)
                                "Admin" -> navigateTo(AdminActivity::class.java)

                                "Pharmacy" -> {
                                    if (status == "approved") {
                                        navigateTo(PhamacyActivity::class.java)
                                    } else {
                                        showToast("Your account is pending approval. Please wait for admin approval.")
                                        auth.signOut()
                                    }
                                }

                                else -> showToast("Role not recognized")
                            }
                        } else {
                            showToast("Login Failed: ${task.exception?.message}")
                        }
                    }
            }
            .addOnFailureListener {
                showToast("Error finding user: ${it.message}")
            }
    }

    private fun navigateTo(activity: Class<*>) {
        val intent = Intent(this, activity)
        startActivity(intent)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}