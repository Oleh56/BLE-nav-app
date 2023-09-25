package com.example.attempt228

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.attempt228.databinding.ActivityXyetablyadBinding
import retrofit2.HttpException
import java.io.IOException

const val TAG = "xyetablyad"

class xyetablyad : AppCompatActivity() {

    private lateinit var binding: ActivityXyetablyadBinding

    private lateinit var todoAdapter: ToDoAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityXyetablyadBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()

        lifecycleScope.launchWhenCreated {
            binding.progressBar.isVisible = true
            val response = try {
                RetrofitInstance.api.getTodos()
            } catch (e: IOException) {
                Log.e(TAG, "IOException, you might not have internet connection")
                binding.progressBar.isVisible = false

                return@launchWhenCreated

            } catch (e: HttpException){
                Log.e(TAG, "HTTPException, unexpected response")
                binding.progressBar.isVisible = false
                return@launchWhenCreated
            }
            if(response.isSuccessful && response.body() != null) {
                todoAdapter.todos = response.body()!!
            } else {
                Log.e(TAG, "Response not successful")
            }
            binding.progressBar.isVisible = false
        }

    }

    private fun setupRecyclerView() = binding.rvTodos.apply {
        todoAdapter = ToDoAdapter()
        adapter = todoAdapter
        layoutManager = LinearLayoutManager(this@xyetablyad)
    }
}