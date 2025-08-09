//package com.example.mediseek.fragments
//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.view.View
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.core.content.ContextCompat
//import androidx.fragment.app.Fragment
//import androidx.fragment.app.setFragmentResult
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.OnMapReadyCallback
//import com.google.android.gms.maps.SupportMapFragment
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.MarkerOptions
//import com.example.mediseek.R
//
//class MapPickerFragment : Fragment(R.layout.fragment_map), OnMapReadyCallback {
//
//    private var googleMap: GoogleMap? = null
//
//    companion object {
//        const val REQUEST_KEY = "map_location_request"
//        const val BUNDLE_KEY_LATITUDE = "map_latitude"
//        const val BUNDLE_KEY_LONGITUDE = "map_longitude"
//    }
//
//    private val locationPermissionRequest = registerForActivityResult(
//        ActivityResultContracts.RequestPermission()
//    ) { isGranted ->
//        if (isGranted) {
//            enableMyLocation()
//        } else {
//            Toast.makeText(requireContext(), "Location permission is required to show your position.", Toast.LENGTH_LONG).show()
//        }
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        val mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
//        mapFragment?.getMapAsync(this)
//    }
//
//    override fun onMapReady(map: GoogleMap) {
//        googleMap = map
//        checkLocationPermission()
//
//        // Set a default location (e.g., a central point in your country)
//        val defaultLocation = LatLng(51.1657, 10.4515) // Germany
//        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 6f))
//
//        // Set up the long-click listener to pick a location
//        googleMap?.setOnMapLongClickListener { latLng ->
//            googleMap?.clear() // Clear previous markers
//            googleMap?.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
//
//            // Return the result to the previous fragment
//            val result = Bundle().apply {
//                putDouble(BUNDLE_KEY_LATITUDE, latLng.latitude)
//                putDouble(BUNDLE_KEY_LONGITUDE, latLng.longitude)
//            }
//            setFragmentResult(REQUEST_KEY, result)
//
//            // Go back to the previous screen
//            parentFragmentManager.popBackStack()
//        }
//    }
//
//    private fun checkLocationPermission() {
//        when {
//            ContextCompat.checkSelfPermission(
//                requireContext(),
//                Manifest.permission.ACCESS_FINE_LOCATION
//            ) == PackageManager.PERMISSION_GRANTED -> {
//                enableMyLocation()
//            }
//            else -> {
//                locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
//            }
//        }
//    }
//
//    private fun enableMyLocation() {
//        try {
//            googleMap?.isMyLocationEnabled = true
//        } catch (e: SecurityException) {
//            // This case should be handled by the permission check, but it's good practice
//        }
//    }
//}
