package com.mypathshala.foodscanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkLoginState()
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            // already signed in
            user_name?.text = auth.currentUser?.displayName
            signout_button?.visibility = View.VISIBLE
            signout_button?.setOnClickListener {
                signOut()
            }
        } else {
            user_name?.text = "Guest User"
            signout_button?.visibility = View.GONE
        }
    }

    private fun checkLoginState() {
        if (auth.currentUser != null) {
            // already signed in
            user_name?.text = auth.currentUser?.displayName
            signout_button?.visibility = View.VISIBLE
            signout_button?.setOnClickListener {
                signOut()
            }
        } else {
            // not signed in, navigate to login screen
            val loginIntent: Intent = Intent(this, LoginActivity::class.java)
            startActivity(loginIntent)
        }
    }

    private fun signOut() {
        // [START auth_fui_signout]
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                val loginIntent: Intent = Intent(this, LoginActivity::class.java)
                startActivity(loginIntent)
            }
        // [END auth_fui_signout]
    }
}
