package com.example.attempt228

import NetworkRequests
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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

            val isValidEmail = isValidEmail(email)
            val isPasswordValid = password.length >= 5

            if (isValidEmail && isPasswordValid) {
                // Clear any existing errors
                binding.etEmail.error = null
                binding.etPassword.error = null

                postCredentials(email, password)


            } else {
                if (!isValidEmail) {
                    // Set red error text below the email field
                    binding.etEmail.error = "Введіть коректну електронну пошту"
                } else {
                    // Clear the error if email is valid
                    binding.etEmail.error = null
                }

                if (!isPasswordValid) {
                    // Set red error text below the password field
                    binding.etPassword.error =
                        "Пароль повинен складатися щонайменше з восьми символів"
                } else {
                    // Clear the error if password is valid
                    binding.etPassword.error = null
                }
            }

        }

        binding.guestButton.setOnClickListener(){
            RedirToActivity.redirectToNavigationActivity(this@MainActivity)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)\$"
        return email.matches(emailRegex.toRegex())
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
                    val serverResponse = response.body()?.string()
                    Log.i("response", serverResponse.toString())

                    serverResponse?.let {
                        when {
                            it.contains("NO USER") -> {
                                runOnUiThread {
                                    Log.d("Thread", "Running on UI Thread")
                                    Toast.makeText(this@MainActivity, "Користувача не знайдено", Toast.LENGTH_LONG).show()
                                }
                            }
                            it.contains("STUDENT") -> {

                                    RedirToActivity.redirectToNavigationActivity(this@MainActivity)
                            }
                            it.contains("TEACHER") -> {

                                    RedirToActivity.redirectToNavigationActivity(this@MainActivity)

                            }
                            it.contains("ADMIN") -> {

                                    RedirToActivity.redirectToNavigationActivity(this@MainActivity)
                            }
                        }
                    }
                } else {

                    Toast.makeText(this@MainActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                Toast.makeText(this@MainActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }
}


