package com.example.mediseek

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mediseek.Auth.LoginActivity
import com.example.mediseek.databinding.ActivityClientBinding
import com.google.firebase.auth.FirebaseAuth

class ClientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityClientBinding
    private lateinit var navController: NavController
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Check if the user is authenticated
        if (firebaseAuth.currentUser == null) {
            // User is not logged in, redirect to the login screen
            redirectToLogin()
            return // Exit onCreate early to prevent UI setup
        }

        // User is authenticated, proceed to set up the UI and navigation
        setupNavigation()
    }

    private fun setupNavigation() {
        try {
            // Find the NavHostFragment from the layout
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController

            // Set up the Toolbar
            setSupportActionBar(binding.toolbar)

            // Define top-level destinations where the Up button should not be shown
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.homeFragment,
                    R.id.clientSearchFragment,
                    R.id.liveChatFragment,
                    R.id.clientProfileFragment
                )
            )

            // Connect the Toolbar with the NavController
            setupActionBarWithNavController(navController, appBarConfiguration)

            // Connect the BottomNavigationView with the NavController
            binding.bottomNavigationView.setupWithNavController(navController)

        } catch (e: Exception) {
            Toast.makeText(this, "Navigation setup failed: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            // Finish the activity if navigation fails to initialize
            finish()
        }
    }

    private fun redirectToLogin() {
        val loginIntent = Intent(this, LoginActivity::class.java).apply {
            // Clear the activity stack to prevent the user from going back to this activity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(loginIntent)
        finish() // Finish the current activity
    }

    /**
     * This function handles the Up button navigation in the toolbar.
     * It's automatically called when the user presses the Up button.
     */
    override fun onSupportNavigateUp(): Boolean {
        // Let the NavController handle the Up navigation.
        // Fallback to the default implementation if NavController can't handle it.
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}