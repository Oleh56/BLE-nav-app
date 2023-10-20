package com.example.attempt228

import android.content.Context
import android.content.Intent

object RedirToActivity {
    fun redirectToNavigationActivity(context: Context) {
        val intent = Intent(context, NavigationActivity::class.java)
        context.startActivity(intent)
    }
}
