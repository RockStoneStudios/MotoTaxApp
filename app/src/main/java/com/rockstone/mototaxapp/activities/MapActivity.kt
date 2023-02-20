package com.rockstone.mototaxapp.activities


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.firebase.firestore.GeoPoint
import com.google.maps.android.SphericalUtil
import com.rockstone.mototaxapp.R
import com.rockstone.mototaxapp.databinding.ActivityMapBinding
import com.rockstone.mototaxapp.models.DriverLocation
import com.rockstone.mototaxapp.providers.AuthProvider
import com.rockstone.mototaxapp.providers.BookingProvider
import com.rockstone.mototaxapp.providers.GeoProvider
import com.rockstone.mototaxapp.utils.CarMoveAnim
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener

class MapActivity : AppCompatActivity(),OnMapReadyCallback,Listener {
    private lateinit var binding: ActivityMapBinding
    private var googleMap:GoogleMap?= null
    private var easyWayLocation:EasyWayLocation?=null
    private var myLocationLatLng : LatLng?=null

    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()
    //Google Places
    private var places:PlacesClient?=null
    private var autocompleteOrigin:AutocompleteSupportFragment?=null
    private var autocompleteDestination:AutocompleteSupportFragment?=null
    private var originName= ""
    private var destinationName = ""
    private var originLatLng:LatLng?=null
    private var destinationLatLng : LatLng?=null

    private var isLocationEnabled = false
    private val driverMarkers = ArrayList<Marker>();
    private val driversLocation = ArrayList<DriverLocation>();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val locationRequest = LocationRequest.create().apply {
            interval= 0
            fastestInterval=0
            priority= Priority.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 1f
        }
        easyWayLocation = EasyWayLocation(this,locationRequest,false,false,this)
        locationPermission.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
        startGooglePlaces()
        removeBooking()
        binding.btnRequestTrip.setOnClickListener { goToTripInfo() }

    }
    val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){ permission ->
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            when{
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION,false) -> {
                    Log.d("LOCALIZACION","Permiso concedido")
                       easyWayLocation?.startLocation()

                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,false) -> {
                    Log.d("LOCALIZACION","Permiso concedido con limitacion")
                     easyWayLocation?.startLocation()
                }
                else -> {
                    Log.d("LOCALIZACION","Permiso No concedido")
                }

            }

        }
    }

    private fun removeBooking(){
        bookingProvider?.remove()
    }

    private fun getPositionDriver(id:String):Int{
       var position =0
        for(i in driversLocation.indices){
            if(id === driversLocation[i].id){
                position=i
                break;
            }
        }
        return position
    }

    private fun goToTripInfo() {
        if(originLatLng !=null && destinationLatLng !=null){
            val i =  Intent(this,TripInfoActivity::class.java)
            i.putExtra("origin",originName)
            i.putExtra("destination",destinationName)
            i.putExtra("origin_lat",originLatLng?.latitude)
            i.putExtra("origin_lng",originLatLng?.longitude)
            i.putExtra("destination_lat",destinationLatLng?.latitude)
            i.putExtra("destination_lng",destinationLatLng?.longitude)
            startActivity(i)
        }else {
            Toast.makeText(this,"Debes Seleccionar el Origen y el destino",Toast.LENGTH_LONG).show()
        }


    }
    private fun getMarkerFromDrawable(drawable : Drawable) : BitmapDescriptor{
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            130,
            152,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0,0,130,152)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

   private fun getNearbyDrivers(){
       if(myLocationLatLng== null) return
       geoProvider.getNearbyDrivers(myLocationLatLng!!,3.5).addGeoQueryEventListener(object :GeoQueryEventListener{
           override fun onKeyEntered(documentID: String, location: GeoPoint) {
              for (marker in driverMarkers){
                  if(marker.tag != null){
                      if(marker.tag == documentID){
                          return
                      }
                  }
              }
              // Creamos un nuevo markador para el conductor conectado
               val drawable = ContextCompat.getDrawable(applicationContext,R.drawable.mototax)
               val markerIcon = getMarkerFromDrawable(drawable!!)
               val driverLatLng = LatLng(location.latitude,location.longitude)
               val  marker = googleMap?.addMarker(
                  MarkerOptions().position(driverLatLng).title("Conductor disponible").icon(markerIcon)

               )
               marker?.tag= documentID
               driverMarkers.add(marker!!)
               val dl = DriverLocation()
               dl.id = documentID
               driversLocation.add(dl)
           }


           override fun onKeyExited(documentID: String) {
               for(marker in driverMarkers){
                   if(marker.tag != null){
                       if(marker.tag == documentID){
                           marker.remove()
                           driverMarkers.remove(marker)
                           driversLocation.removeAt(getPositionDriver(documentID))
                           return

                       }
                   }
               }
           }

           override fun onKeyMoved(documentID: String, location: GeoPoint) {
               for(marker in driverMarkers){
                   val start = LatLng(location.latitude,location.longitude)
                   var end: LatLng? =null
                   val position = getPositionDriver(marker.tag.toString())
                   if(marker.tag !=null){
                       if(marker.tag == documentID){
                           marker.position = LatLng(location.latitude,location.longitude)

                           if(driversLocation[position].latlng != null){
                               end = driversLocation[position].latlng
                           }
                           driversLocation[position].latlng = LatLng(location.latitude,location.longitude)
                           if(end != null){
                               CarMoveAnim.carAnim(marker,end,start)
                           }
                       }
                   }
               }
           }

           override fun onGeoQueryError(exception: Exception) {

           }

           override fun onGeoQueryReady() {

           }






       })
   }

    private fun onCameraMove(){
        googleMap?.setOnCameraIdleListener {
            try{
               val geocoder = Geocoder(this)
                originLatLng = googleMap?.cameraPosition?.target
                val addressList = geocoder.getFromLocation(originLatLng?.latitude!!,originLatLng?.longitude!!,1)
                val city = addressList?.get(0)?.locality
                val country = addressList?.get(0)?.countryName
                val address = addressList?.get(0)?.getAddressLine(0)
                originName = "$address $city"
                autocompleteOrigin?.setText("$address $city")

            }catch (e:Exception){
                Log.d("Error","Mensaje Error: ${e.message}")
            }
        }
    }
   private fun startGooglePlaces(){
       if(!Places.isInitialized()){
           Places.initialize(applicationContext,resources.getString(R.string.google_maps_key))
       }
       places = Places.createClient(this)
       instanceAutocompleteOrigin()
       instanceAutocompleteDestination()
   }

    private fun limitSearch(){
        val northSide = SphericalUtil.computeOffset(myLocationLatLng,30.0,0.0)
        val southSide = SphericalUtil.computeOffset(myLocationLatLng,30.0,180.0)

        autocompleteOrigin?.setLocationBias(RectangularBounds.newInstance(southSide,northSide))
        autocompleteDestination?.setLocationBias(RectangularBounds.newInstance(southSide,northSide))
    }


    private fun instanceAutocompleteOrigin(){
        autocompleteOrigin = supportFragmentManager.findFragmentById(R.id.placesAutocompleteOrigin)  as AutocompleteSupportFragment
        autocompleteOrigin?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
        )
        autocompleteOrigin?.setHint("Lugar de recogida")

        autocompleteOrigin?.setCountry("CO")
        autocompleteOrigin?.setOnPlaceSelectedListener(object:PlaceSelectionListener{
            override fun onPlaceSelected(place: Place) {
               originName= place.name!!
                originLatLng= place.latLng
                Log.d("Places","Address: $originName")
                Log.d("Places","LAT: ${originLatLng?.latitude}")
                Log.d("Places","LNG: ${originLatLng?.latitude}")

            }

            override fun onError(p0: Status) {

            }
        })
    }



    private fun instanceAutocompleteDestination(){
        autocompleteDestination = supportFragmentManager.findFragmentById(R.id.placesAutocompleteDestination)  as AutocompleteSupportFragment
        autocompleteDestination?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
        )
        autocompleteDestination?.setHint("Destino")

        autocompleteDestination?.setCountry("CO")
        autocompleteDestination?.setOnPlaceSelectedListener(object:PlaceSelectionListener{
            override fun onPlaceSelected(place: Place) {
                destinationName= place.name!!
                destinationLatLng= place.latLng
                Log.d("Places","Address: ${destinationName}Name")
                Log.d("Places","LAT: ${destinationLatLng?.latitude}")
                Log.d("Places","LNG: ${destinationLatLng?.latitude}")

            }

            override fun onError(p0: Status) {

            }
        })
    }

    override fun onResume() {
        super.onResume()

    }

    override fun onDestroy() { //Cierra Aplicacion o Pasamos a otra actividad
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }



    override fun onMapReady(map: GoogleMap){
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        onCameraMove()
        // easyWayLocation?.startLocation()
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        googleMap?.isMyLocationEnabled= false
        try{
            val success = googleMap?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(this,R.raw.style)
            )
            if(!success!!){
                Log.d("Mapas","No se pudo encontrar el estilo")
            }

        }catch (e:Resources.NotFoundException){
            Log.d("Mapas","Error: ${e.toString()}")
        }
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) { //Actualiza la posicion en tiempo real
        myLocationLatLng = LatLng(location.latitude,location.longitude) //Lat y long de la posicion actual

        if(!isLocationEnabled){
            isLocationEnabled = true
            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(myLocationLatLng!!).zoom(15f).build()
            ))
            getNearbyDrivers()
            limitSearch()
        }

    }

    override fun locationCancelled() {

    }

}