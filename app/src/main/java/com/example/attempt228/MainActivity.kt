package com.example.attempt228

import NetworkRequests
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.attempt228.databinding.ActivityMainBinding
import java.security.SecureRandom
import java.math.BigInteger
import org.mindrot.jbcrypt.BCrypt
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnConfirm.setOnClickListener() {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                postCredentials(email, password)


            } else Toast.makeText(this, "Input your email and password", Toast.LENGTH_SHORT).show()
        }
    }

    private fun postCredentials(email: String, password: String) {
        val networkUtils = NetworkRequests()

        val jsonObject = JSONObject()
        jsonObject.put("email", email)
        jsonObject.put("password", password)

        val postRequestBody = RequestBody.create(MediaType.parse("application/json"), jsonObject.toString())

        networkUtils.authenticateUser(postRequestBody, object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    var serverResponse = response.body()?.string()
                    Log.i("response", serverResponse.toString())

                } else {


                    Toast.makeText(this@MainActivity, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {


                Toast.makeText(this@MainActivity, "Network error", Toast.LENGTH_SHORT).show()
            }
        })
    }
}


