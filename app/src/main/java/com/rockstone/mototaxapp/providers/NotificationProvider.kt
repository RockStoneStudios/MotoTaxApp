package com.rockstone.mototaxapp.providers

import com.rockstone.mototaxapp.api.IFCMApi
import com.rockstone.mototaxapp.api.RetrofitClient
import com.rockstone.mototaxapp.models.FCMBody
import com.rockstone.mototaxapp.models.FCMResponse
import retrofit2.Call

class NotificationProvider {

    private val URL = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody): Call<FCMResponse> {
        return RetrofitClient.getClient(URL).create(IFCMApi::class.java).send(body)
    }

}