package com.example.mediseek.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.mediseek.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: Location

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
        enableTapToGetLocation()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        googleMap.isMyLocationEnabled = true

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                currentLocation = it
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                addMarkerAt(currentLatLng, "You are here")

                // Example destination
                val destination = LatLng(6.9271, 79.8612) // Colombo
                addMarkerAt(destination, "Destination")
                drawDirection(currentLatLng, destination)
            }
        }
    }

    private fun enableTapToGetLocation() {
        googleMap.setOnMapClickListener { latLng ->
            googleMap.clear()
            addMarkerAt(latLng, "Tapped Location")

            // Update UI
            view?.findViewById<TextView>(R.id.locationInfo)?.text = "Tapped: ${latLng.latitude}, ${latLng.longitude}"

            // Show Toast
            Toast.makeText(requireContext(), "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}", Toast.LENGTH_LONG).show()
        }
    }

    private fun addMarkerAt(position: LatLng, title: String) {
        googleMap.addMarker(MarkerOptions().position(position).title(title))
    }

    private fun drawDirection(origin: LatLng, destination: LatLng) {
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${destination.latitude},${destination.longitude}&key=YOUR_API_KEY_HERE"

        lifecycleScope.launch(Dispatchers.IO) {
            val response = URL(url).readText()
            val jsonObject = JSONObject(response)
            val routes = jsonObject.getJSONArray("routes")
            val overviewPolyline = routes.getJSONObject(0).getJSONObject("overview_polyline")
            val points = overviewPolyline.getString("points")
            val path = decodePolyline(points)

            withContext(Dispatchers.Main) {
                val polylineOptions = PolylineOptions().addAll(path).color(Color.BLUE).width(10f)
                googleMap.addPolyline(polylineOptions)

                val distance = jsonObject.getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0)
                    .getJSONObject("distance")
                    .getString("text")

                view?.findViewById<TextView>(R.id.locationInfo)?.text = "Distance: $distance"

                // Optional: Toast distance too
                Toast.makeText(requireContext(), "Distance: $distance", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if ((result and 1) != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val p = LatLng((lat / 1E5), (lng / 1E5))
            poly.add(p)
        }

        return poly
    }
}



