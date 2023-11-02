package com.rockstone.mototaxapp.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.toObject
import com.rockstone.mototaxapp.R
import com.rockstone.mototaxapp.databinding.ActivitySearchBinding
import com.rockstone.mototaxapp.models.Booking
import com.rockstone.mototaxapp.models.Driver
import com.rockstone.mototaxapp.models.FCMBody
import com.rockstone.mototaxapp.models.FCMResponse
import com.rockstone.mototaxapp.providers.*
import org.imperiumlabs.geofirestore.callbacks.GeoQueryEventListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchActivity : AppCompatActivity() {

    private  var listenerBooking: ListenerRegistration?=null
    private lateinit var  binding: ActivitySearchBinding

    private var extraOriginName = ""
    private var extraDestinationName=""
    private var extraOriginLat=0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng= 0.0
    private var extraTime =0.0
    private var extraDistance=0.0
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val notificationProvider = NotificationProvider()
    private val driverProvider = DriverProvider()

    private var originLatLng:LatLng?= null
    private var destinationLatLng:LatLng?=null

    private var radius = 0.1
    private var limitRadius = 2.0
    private var driver: Driver? = null
    private var idDriver = ""
    private var isDriverFound = false
    private var driverLatLng:LatLng?=null

    private val bookingProvider = BookingProvider()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)


        extraOriginName = intent.getStringExtra("origin")!!
        extraDestinationName = intent.getStringExtra("destination")!!
        extraOriginLat= intent.getDoubleExtra("origin_lat",0.0)
        extraOriginLng= intent.getDoubleExtra("origin_lng",0.0)
        extraDestinationLat= intent.getDoubleExtra("destination_lat",0.0)
        extraDestinationLng= intent.getDoubleExtra("destination_lng",0.0)
        extraTime = intent.getDoubleExtra("time",0.0)
        extraDistance= intent.getDoubleExtra("distance",0.0)
        originLatLng= LatLng(extraOriginLat,extraOriginLng)
        destinationLatLng = LatLng(extraDestinationLat,extraDestinationLng)

        getClosesDrivers()
        checkIfDriverAccept()
    }

    private fun sendNotification() {

        val map = HashMap<String, String>()
        map.put("title", "SOLICITUD DE VIAJE")
        map.put("body", "UN CLIENTE ESTA SOLICITANDO UN VIAJE a" +
                " ${String.format("%.1f",extraDistance)}Km y ${String.format("%.2f",extraTime)} min")

        map.put("idBooking",authProvider.getId())
        val body = FCMBody(
            to = driver?.token!!,
            priority = "high",
            ttl = "4500s",
            data = map
        )


        notificationProvider.sendNotification(body).enqueue(object: Callback<FCMResponse> {
            override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {
                if (response.body() != null) {

                    if (response.body()!!.success == 1) {
                        Toast.makeText(this@SearchActivity, "Se envio la notificacion", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(this@SearchActivity, "No se pudo enviar la notificacion", Toast.LENGTH_LONG).show()
                    }

                }
                else {
                    Toast.makeText(this@SearchActivity, "hubo un error enviando la notificacion", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<FCMResponse>, t: Throwable) {
                Log.d("NOTIFICATION", "ERROR: ${t.message}")
            }

        })
    }
    private fun checkIfDriverAccept(){
        listenerBooking= bookingProvider.getBooking().addSnapshotListener { snapshot, e ->
         if(e!=null){
             Log.d("FIRESTORE","Error: ${e.message}")
             return@addSnapshotListener
         }
             if(snapshot!=null && snapshot.exists()){
                 val booking= snapshot.toObject(Booking::class.java)
                 if(booking?.status=="accept"){
                     Toast.makeText(this@SearchActivity,"Viaje Aceptado",Toast.LENGTH_LONG).show()
                   listenerBooking?.remove()
                   goToMapTrip()
                 }else if(booking?.status == "cancel"){
                     Toast.makeText(this@SearchActivity,"Viaje Cancelado",Toast.LENGTH_LONG).show()
                    listenerBooking?.remove()
                     goToMap()
                 }
             }

         }
    }

    private fun getDriverInfo() {
     driverProvider.getDriver(idDriver).addOnSuccessListener { document ->
         if(document.exists()){
             driver = document.toObject(Driver::class.java)
             sendNotification()
         }
     }
    }
     private fun goToMapTrip(){
         val i = Intent(this,MapTripActivity::class.java)
         startActivity(i)
     }
    private fun goToMap(){
        val i = Intent(this,MapActivity::class.java)
         i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
    }
    private fun createBooking(idDriver:String){
        val booking = Booking(
            idClient = authProvider.getId(),
            idDriver = idDriver,
            status = "create",
            destination = extraDestinationName,
            origin = extraOriginName,
            time= extraTime,
            km = extraDistance,
            originLat = extraOriginLat,
            originLng = extraOriginLng,
            destinationLat = extraDestinationLat,
            destinationLng = extraDestinationLng
        )
        bookingProvider.create(booking).addOnCompleteListener { it
          if(it.isSuccessful){
              Toast.makeText(this@SearchActivity,"Datos del Viaje creados",Toast.LENGTH_LONG).show()
          }else{
              Toast.makeText(this@SearchActivity,"Error al crear los datos",Toast.LENGTH_LONG).show()

          }
        }
    }

     private fun getClosesDrivers() {
         geoProvider.getNearbyDrivers(originLatLng!!, radius)
             .addGeoQueryEventListener(object : GeoQueryEventListener {
                 // Busqueda Del Conductor
                 override fun onKeyEntered(documentID: String, location: GeoPoint) {
                     if (!isDriverFound) {
                         isDriverFound = true
                         idDriver = documentID
                         getDriverInfo()
                         driverLatLng = LatLng(location.latitude, location.longitude)
                         binding.textViewSearch.text =
                             "CONDUCTOR ENCONTRADO\n ESPERANDO RESPUESTA DEL CONDUCTOR"
                         createBooking(documentID)
                     }
                 }

                 override fun onGeoQueryError(exception: Exception) {

                 }

                 override fun onGeoQueryReady() {
                     if (!isDriverFound) {
                         radius += 0.1
                         if (radius > limitRadius) {
                             binding.textViewSearch.text = "NO SE ENCONTRO NINGUN CONDUCTOR"
                             return
                         } else {
                             getClosesDrivers()
                         }
                     }
                 }


                 override fun onKeyExited(documentID: String) {

                 }

                 override fun onKeyMoved(documentID: String, location: GeoPoint) {

                 }

             }
             )
     }
         override fun onDestroy() {
             super.onDestroy()
             listenerBooking?.remove()
         }
     }







