package com.example.mediseek

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mediseek.databinding.ActivityClientBinding
import com.example.mediseek.databinding.ActivityPhamacyBinding

class PhamacyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPhamacyBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhamacyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_pharmacy) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigationViewPharmacy.setupWithNavController(navController)

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}