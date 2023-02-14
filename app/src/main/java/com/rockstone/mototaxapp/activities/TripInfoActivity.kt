package com.rockstone.mototaxapp.activities

import android.content.Intent
import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.rockstone.mototaxapp.R
import com.rockstone.mototaxapp.databinding.ActivityTripInfoBinding

class TripInfoActivity : AppCompatActivity(),OnMapReadyCallback,Listener,DirectionUtil.DirectionCallBack {
   private lateinit var binding:ActivityTripInfoBinding
    private var googleMap: GoogleMap?= null
    private var easyWayLocation: EasyWayLocation?=null
    private var extraOriginName = ""
    private var extraDestinationName=""
    private var extraOriginLat=0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng= 0.0

    private var originLatLng:LatLng?= null
    private var destinationLatLng:LatLng?=null

    private var wayPoints:ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG= "way_point_tag"
    private lateinit var directionUtil:DirectionUtil

    private var markerOrigin : Marker?=null
    private var markerDestination : Marker?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTripInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        //Extras

        extraOriginName = intent.getStringExtra("origin")!!
        extraDestinationName = intent.getStringExtra("destination")!!
        extraOriginLat= intent.getDoubleExtra("origin_lat",0.0)
        extraOriginLng= intent.getDoubleExtra("origin_lng",0.0)
        extraDestinationLat= intent.getDoubleExtra("destination_lat",0.0)
        extraDestinationLng= intent.getDoubleExtra("destination_lng",0.0)

        originLatLng= LatLng(extraOriginLat,extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat,extraDestinationLng)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


        val locationRequest = LocationRequest.create().apply {
            interval= 0
            fastestInterval=0
            priority= Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }
        easyWayLocation = EasyWayLocation(this,locationRequest,false,false,this)

        binding.textViewOrigin.text=extraOriginName
        binding.textViewDestination.text = extraDestinationName

        Log.d("Localizacion", "origin lAT : ${originLatLng?.latitude}")
        Log.d("Localizacion", "origin lNG : ${originLatLng?.longitude}")
        Log.d("Localizacion", "destination lAT : ${destinationLatLng?.latitude}")
        Log.d("Localizacion", "destination lng : ${destinationLatLng?.longitude}")

        binding.btnViewBack.setOnClickListener { finish() }
    }



    private fun addOriginMarker() {
        markerOrigin = googleMap?.addMarker(MarkerOptions().position(originLatLng!!).title("Mi posicion")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person)))
    }

    private fun addDestinationMarker() {
        markerDestination = googleMap?.addMarker(MarkerOptions().position(destinationLatLng!!).title("LLegada")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin)))
    }


    private fun easyDrawRoute() {
        wayPoints.add(originLatLng!!)
        wayPoints.add(originLatLng!!)
        directionUtil = DirectionUtil.Builder()
            .setDirectionKey(resources.getString(R.string.google_maps_key))
            .setOrigin(originLatLng!!)
            .setWayPoints(wayPoints)
            .setGoogleMap(googleMap!!)
            .setPolyLineWidth(10)
            .setPathAnimation(true)
            .setCallback(this)
            .setDestination(destinationLatLng!!)
            .build()

        directionUtil.initPath()
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        googleMap?.moveCamera(
            CameraUpdateFactory.newCameraPosition(
            CameraPosition.builder().target(originLatLng!!).zoom(13f).build()
        ))
         easyDrawRoute()
         addOriginMarker()
         addDestinationMarker()

        try{
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this,R.raw.style)
            )
            if(!success!!){
                Log.d("Mapas","No se pudo encontrar el estilo")
            }

        }catch (e: Resources.NotFoundException){
            Log.d("Mapas","Error: ${e.toString()}")
        }
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location?) {

    }

    override fun locationCancelled() {

    }

    override fun onDestroy() {
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        var distance = polyLineDetailsArray[1].distance.toDouble()
        var time = polyLineDetailsArray[1].time.toDouble()
        distance = if(distance <1000.0) 1000.0 else distance
        time = if(time< 60.0) 60.0 else time


        val timeString = String.format("%.2f",time)
        val distanceString = String.format("%.2f",distance)

        binding.textViewTimeAndDistance.text = "$timeString mins - $distanceString km"



        directionUtil.drawPath(WAY_POINT_TAG)
    }
}