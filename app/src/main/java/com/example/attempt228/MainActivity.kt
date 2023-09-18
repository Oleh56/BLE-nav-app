package com.example.attempt228

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.attempt228.databinding.ActivityMainBinding
import java.security.SecureRandom
import java.math.BigInteger
import org.mindrot.jbcrypt.BCrypt

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
                val hashedPassword = hashPassword(password)
                Log.i("Password hashing",  "Hashed password $hashedPassword")


            } else Toast.makeText(this, "Input your email and password", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateRandomSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)

        return salt
    }

    private fun ByteArray.toHexString(): String {
        val bigInt = BigInteger(1, this)
        return String.format("%0${this.size * 2}x", bigInt)
    }

    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }

    fun verifyPassword(inputPassword: String, hashedPassword: String): Boolean {
        return BCrypt.checkpw(inputPassword, hashedPassword)
    }
}
