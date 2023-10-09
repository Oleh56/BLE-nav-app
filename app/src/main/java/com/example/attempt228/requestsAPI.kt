package com.example.attempt228

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface requestsAPI {

    @GET("/todos")
  suspend fun getTodos(): Response<List<Todo>>


}