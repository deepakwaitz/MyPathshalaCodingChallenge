package com.mypathshala.foodscanner

import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val RC_SIGN_IN = 123
        private const val FIRESTORE_COLLECTION_ALLERGENS_PROFILE = "allergns_profile"
        private const val FIRESTORE_DOCUMENT_KEY = "allergic"

    }

    private val auth = FirebaseAuth.getInstance()
    private val fireStoreDB = Firebase.firestore
    private var fireStoreDocumentReference: DocumentReference? = null
    private var tagsLayout: FlowLayout? = null
    private var allergenicList: ArrayList<String>? = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tagsLayout = findViewById(R.id.allergen_items_flow_layout)

        checkLoginState()
    }

    private fun addExceptionData(enteredValue: String) {
        allergenicList?.add(enteredValue)

        val allergic: HashMap<String, List<String>?> = hashMapOf(FIRESTORE_DOCUMENT_KEY to allergenicList)
        fireStoreDocumentReference?.set(allergic)
            ?.addOnSuccessListener {
                showAllergensList()
            }
            ?.addOnFailureListener { e ->
                Log.w("OnFailureListener", "Error adding document", e)
            }


    }

    private fun showDialog() {
        val alert = Builder(this)

        val edittext = EditText(this)
        alert.setMessage("Enter Your Message")
        alert.setTitle("Enter Your Title")

        alert.setView(edittext)

        alert.setPositiveButton(
            "Add"
        ) { dialog, whichButton -> //What ever you want to do with the value
            val enteredValue = edittext.text.toString()
            if (!TextUtils.isEmpty(enteredValue))
                addExceptionData(enteredValue)
        }

        alert.setNegativeButton(
            "Cancel"
        ) { dialog, whichButton ->
            // what ever you want to do with No option.
        }

        alert.show()
    }

    private fun getExceptionData() {
        fireStoreDocumentReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(FIRESTORE_DOCUMENT_KEY) != null) {
                    allergenicList = result?.data?.get(FIRESTORE_DOCUMENT_KEY) as ArrayList<String>
                    showAllergensList()
                }
            }
            ?.addOnFailureListener { exception ->
                Log.w("OnFailureListener", "Error getting documents.", exception)
            }
    }


    private fun onUserLogout() {
        fireStoreDocumentReference = null
        allergenicList?.clear()
        tagsLayout?.removeAllViews()
        //Updating the user login status as guest user
        user_status_tv?.text = getString(R.string.placeholder_guest_user)

        //Remove avatar from image view
        user_avatar?.setImageURI("")

        //Updating views visibility
        signout_button?.visibility = View.GONE
        allergen_profile_card_view?.visibility = View.GONE
        signin_button?.visibility = View.VISIBLE

        signin_button?.setOnClickListener {
            createSignInIntent()
        }
    }

    private fun showAllergensList() {
        tagsLayout?.removeAllViews()
        if (!allergenicList.isNullOrEmpty()) {
            for (item in allergenicList!!) {
                val tagView = LayoutInflater.from(tagsLayout?.context)
                    .inflate(R.layout.allergen_item_layout, tagsLayout, false) as FrameLayout
                val tvTagName = tagView.findViewById<TextView>(R.id.allergen_name)
                tvTagName.text = item

                tagsLayout?.addView(tagView)
            }
        }
    }

    private fun onUserLogin() {
        fireStoreDocumentReference =
            auth.currentUser?.uid?.let {
                fireStoreDB.collection(FIRESTORE_COLLECTION_ALLERGENS_PROFILE).document(
                    it
                )
            }
        //Updating the user login status with user name
        user_status_tv?.text =
            getString(R.string.placeholder_hi) + auth.currentUser?.displayName + getString(
                R.string.placeholder_welcome
            )

        //Setting user avatar
        if (auth.currentUser?.photoUrl != null)
            user_avatar?.setImageURI(auth.currentUser?.photoUrl)
        //Setting views visibility
        signout_button?.visibility = View.VISIBLE
        signin_button?.visibility = View.GONE

        signout_button?.setOnClickListener {
            signOut()
        }

        allergen_profile_card_view?.visibility = View.VISIBLE

        add_allergen_button?.setOnClickListener {
            showDialog()
        }

        getExceptionData()
    }

    private fun checkLoginState() {
        if (auth.currentUser != null) {
            onUserLogin()
        } else {
            onUserLogout()
            createSignInIntent()
        }
    }

    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                onUserLogout()
            }
    }

    private fun createSignInIntent() {
        // Authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build(),
            AuthUI.IdpConfig.TwitterBuilder().build()
        )

        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .setLogo(R.drawable.ic_food_scanner_logo)
                .setTosAndPrivacyPolicyUrls(
                    getString(R.string.toc_url),
                    getString(R.string.privacy_policy_url)
                )
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                onUserLogin()
                val user = FirebaseAuth.getInstance().currentUser
                // ...
            } else {
                //showSnackbar(R.string.unknown_error);
                Log.d("Sign in failed - ", " " + response?.error?.message)
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }


    private fun delete() {
        // [START auth_fui_delete]
        AuthUI.getInstance()
            .delete(this)
            .addOnCompleteListener {
                // ...
            }
        // [END auth_fui_delete]
    }
}
