import com.example.attempt228.retrofitInterface
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkRequests {
    private val BASE_URL = "http://192.168.1.10:3333/"
    private val retrofit: Retrofit

    init {
        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    fun authenticateUser(requestBody: RequestBody, callback: Callback<ResponseBody>) {
        val authService = retrofit.create(retrofitInterface::class.java)
        //val requestBody = RequestBody.create(MediaType.parse("text/plain"), postRequestBody)
        val call = authService.authenticateUser(requestBody)
        call.enqueue(callback)
    }
}