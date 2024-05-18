package com.example.BLE_nav_app

import NetworkRequests
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.BLE_nav_app.databinding.ActivityLoginBinding
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
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
            RedirToActivity.redirectToBleScanActivity(this@LoginActivity)
        }

        binding.btCompass.setOnClickListener(){
            RedirToActivity.redirectToCompassActivity(this@LoginActivity)
        }
        binding.btNavigation.setOnClickListener(){
            RedirToActivity.redirectToNavigationActivity(this@LoginActivity)
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

        val postRequestBody =
            jsonObject.toString().toRequestBody("application/json".toMediaTypeOrNull())

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
                                    Toast.makeText(this@LoginActivity, "Користувача не знайдено", Toast.LENGTH_LONG).show()
                                }
                            }
                            it.contains("STUDENT") -> {

                                    RedirToActivity.redirectToNavigationActivity(this@LoginActivity)
                            }
                            it.contains("TEACHER") -> {

                                    RedirToActivity.redirectToNavigationActivity(this@LoginActivity)

                            }
                            it.contains("ADMIN") -> {

                                    RedirToActivity.redirectToNavigationActivity(this@LoginActivity)
                            }
                        }
                    }
                } else {

                    Toast.makeText(this@LoginActivity, "Server Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

                Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })

    }

}


