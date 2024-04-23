package com.example.BLE_nav_app
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST


interface retrofitInterface {
    @POST("auth")
    fun authenticateUser(@Body requestBody: RequestBody): Call<ResponseBody>
}