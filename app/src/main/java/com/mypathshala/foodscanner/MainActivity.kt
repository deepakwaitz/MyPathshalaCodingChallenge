package com.mypathshala.foodscanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.generic.RoundingParams
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkLoginState()

        // Access a Cloud Firestore instance from your Activity
        /* val db = Firebase.firestore
         val allergic = hashMapOf("allergic" to "fish")

         db.collection("users")
             .add(allergic)
             .addOnSuccessListener { documentReference ->
                 Log.d(
                     "OnSuccessListener",
                     "DocumentSnapshot added with ID: ${documentReference.id}"
                 )
             }
             .addOnFailureListener { e ->
                 Log.w("OnFailureListener", "Error adding document", e)
             }*/
    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser != null) {
            // already signed in
            onUserLogin()
        } else {
            onUserLogout()
        }
    }

    private fun onUserLogout() {
        //Updating the user login status as guest user
        user_status_tv?.text = getString(R.string.placeholder_guest_user)

        //Remove avatar from image view
        user_avatar?.setImageURI("")

        //Updating views visibility
        showSignInView()
    }

    private fun showSignInView() {
        signout_button?.visibility = View.GONE
        signin_button?.visibility = View.VISIBLE

        signin_button?.setOnClickListener {
            traversToLoginScreen()
        }
    }

    private fun onUserLogin() {
        //Updating the user login status with user name
        user_status_tv?.text =
            getString(R.string.placeholder_hi) + auth.currentUser?.displayName + getString(
                R.string.placeholder_welcome
            )

        //Setting user avatar as a circular image
        val roundingParams = RoundingParams.fromCornersRadius(7f)
            .setBorder(resources.getColor(R.color.blue_color), 1.0f)
            .setRoundAsCircle(true)

        user_avatar?.hierarchy = GenericDraweeHierarchyBuilder(resources)
            .setRoundingParams(roundingParams)
            .build()

        if (auth.currentUser?.photoUrl != null)
            user_avatar?.setImageURI(auth.currentUser?.photoUrl)
        //Setting views visibility
        signout_button?.visibility = View.VISIBLE
        signin_button?.visibility = View.GONE

        signout_button?.setOnClickListener {
            signOut()
        }
    }

    private fun checkLoginState() {
        if (auth.currentUser != null) {
            onUserLogin()
        } else {
            // not signed in, navigate to login screen
            traversToLoginScreen()
        }
    }

    private fun signOut() {
        // [START auth_fui_signout]
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                //traversToLoginScreen()
                showSignInView()
            }
    }

    private fun traversToLoginScreen() {
        val loginIntent: Intent = Intent(this, LoginActivity::class.java)
        startActivity(loginIntent)
    }
}
