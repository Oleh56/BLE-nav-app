package com.example.BLE_nav_app

import android.content.Context
import android.content.Intent

object RedirToActivity {
    fun redirectToNavigationActivity(context: Context) {
        val intent = Intent(context, NavigationActivity::class.java)
        context.startActivity(intent)
    }
    fun redirectToBleScanActivity(context: Context) {
        val intent = Intent(context, bleScanActivity::class.java)
        context.startActivity(intent)
    }
    fun redirectToCompassActivity(context: Context){
        val intent = Intent(context, compassActivity::class.java)
        context.startActivity(intent)
    }
    fun redirectToLoginActivity(context: Context){
        val intent = Intent(context, LoginActivity::class.java)
        context.startActivity(intent)
    }

}
