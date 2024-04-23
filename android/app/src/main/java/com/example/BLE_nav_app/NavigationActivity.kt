package com.example.BLE_nav_app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import com.example.BLE_nav_app.databinding.ActivityNavigationBinding
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.common.location.AccuracyLevel
import com.mapbox.common.location.DeviceLocationProvider
import com.mapbox.common.location.IntervalSettings
import com.mapbox.common.location.Location
import com.mapbox.common.location.LocationObserver
import com.mapbox.common.location.LocationProviderRequest
import com.mapbox.common.location.LocationService
import com.mapbox.common.location.LocationServiceFactory
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.plugin.PuckBearing
import com.mapbox.maps.plugin.locationcomponent.createDefault2DPuck
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport

class NavigationActivity : AppCompatActivity(), PermissionsListener {

    private lateinit var mapView: MapView
    private lateinit var permissionsManager: PermissionsManager
    private lateinit var showLocationButton: Button
    private var isLocationButtonPressed = false
    private var locationProvider: DeviceLocationProvider? = null
    private var userHeading: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapView = binding.mapView
        permissionsManager = PermissionsManager(this)

        // Always request location permissions, the result will be handled in onPermissionResult
        permissionsManager.requestLocationPermissions(this)

        val locationService: LocationService = LocationServiceFactory.getOrCreate()

        val request = LocationProviderRequest.Builder()
            .interval(IntervalSettings.Builder().interval(0L).minimumInterval(0L).maximumInterval(0L).build())
            .displacement(1F)
            .accuracy(AccuracyLevel.HIGHEST)
            .build()

        val result = locationService.getDeviceLocationProvider(request)
        if (result.isValue) {
            locationProvider = result.value
        } else {
            Log.e("error", "Failed to get device location provider")
        }

        val locationObserver = object : LocationObserver {
            override fun onLocationUpdateReceived(locations: MutableList<Location>) {
                Log.e("locationUpdate", "Location update received: " + locations)
                if (locations.isNotEmpty()) {
                    userHeading = locations[0].bearing
                }
            }
        }
        locationProvider?.addLocationObserver(locationObserver)


        showLocationButton = findViewById(R.id.btnGeolocation)
        showLocationButton.setOnClickListener {
            // Toggle the boolean flag when the button is pressed
            isLocationButtonPressed = !isLocationButtonPressed
            updateLocationVisibility()
        }

        binding.btnDragomanova.setOnClickListener(){
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(24.02735527411767, 49.83220571281894))
                    .zoom(17.9)
                    .pitch(0.0)
                    .bearing(-50.0)
                    .build()
            )
        }


        // Set pre-coded coordinates when the activity is launched
        setPreCodedCoordinates()

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle back press if needed
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    // Handle the result of the location permission request
    override fun onPermissionResult(granted: Boolean) {
        if (granted) {
            Log.d("Permissions", "Location permission granted!")
            updateLocationVisibility()
        } else {
            Log.d("Permissions", "Location permission denied!")
            RedirToActivity.redirectToLoginActivity(this)
        }
    }

    override fun onExplanationNeeded(permissionsToExplain: List<String>) {

        Log.d("Permissions", "Explanation needed for: $permissionsToExplain")
    }

    // Handle the result of the system's permission request dialog
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Pass the result to the PermissionsManager for further handling
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun updateLocationVisibility() {
        with(mapView) {
            if (isLocationButtonPressed) {
                // Show user's position on the map when the button is pressed
                location.locationPuck = createDefault2DPuck(withBearing = true)
                location.enabled = true
                location.puckBearingEnabled = true // Ensure puckBearingEnabled is set to true

                // Set the puck's bearing to the user's heading
                userHeading?.let { heading ->
                    location.puckBearing = PuckBearing.valueOf(heading.toString())
                }

                viewport.transitionTo(
                    targetState = viewport.makeFollowPuckViewportState(),
                    transition = viewport.makeImmediateViewportTransition()
                )
            } else {
                location.enabled = false
            }
        }
    }

    private fun setPreCodedCoordinates() {
        // Set pre-coded coordinates when the activity is launched
        mapView.mapboxMap.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(24.02735527411767, 49.83220571281894))
                .zoom(17.9)
                .pitch(0.0)
                .bearing(-50.0)
                .build()
        )
    }
}

