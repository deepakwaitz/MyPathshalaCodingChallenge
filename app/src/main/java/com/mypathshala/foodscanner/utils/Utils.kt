package com.mypathshala.foodscanner.utils

import android.content.Context
import android.net.ConnectivityManager
import android.view.View
import com.google.android.material.snackbar.Snackbar

object Utils {
    val TAG: String = Utils::class.java.simpleName

    fun isConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(
                Context
                    .CONNECTIVITY_SERVICE
            ) as ConnectivityManager
            cm.activeNetworkInfo.isConnectedOrConnecting
        } catch (e: Exception) {
            false
        }
    }

    fun showSnackBar(contextView: View, message: String): Snackbar {
        val snackBar = Snackbar.make(contextView, message, Snackbar.LENGTH_SHORT)
        snackBar.show()
        return snackBar
    }

}
