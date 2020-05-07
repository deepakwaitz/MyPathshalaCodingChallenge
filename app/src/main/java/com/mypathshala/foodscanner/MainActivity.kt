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
import com.mypathshala.foodscanner.utils.FlowLayout
import com.mypathshala.foodscanner.utils.Utils
import com.mypathshala.foodscanner.utils.Utils.showSnackBar
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val TAG: String = MainActivity::class.java.simpleName
    companion object {
        private const val RC_SIGN_IN = 123
        private const val FIRESTORE_COLLECTION_ALLERGENS_PROFILE = "allergns_profile"
        private const val FIRESTORE_DOCUMENT_KEY = "allergic"
    }

    private val auth = FirebaseAuth.getInstance()
    private val fireStoreDB = Firebase.firestore
    private var fireStoreDocumentReference: DocumentReference? = null
    private var exceptionLayout: FlowLayout? = null
    private var allergenicList: ArrayList<String>? = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        exceptionLayout = findViewById(R.id.allergen_items_flow_layout)
        checkLoginState()
    }

    private fun checkLoginState() {
        if (auth.currentUser != null) {
            onUserLogin()
        } else {
            onUserLogout()
            createSignInIntent()
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
            showAddDialog()
        }

        scan_button?.setOnClickListener {
            val cameraIntent = Intent(this, CameraActivity::class.java)
            startActivity(cameraIntent)
        }

        getExceptionData()
    }

    private fun onUserLogout() {
        fireStoreDocumentReference = null
        allergenicList?.clear()
        exceptionLayout?.removeAllViews()
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
        exceptionLayout?.removeAllViews()
        if (!allergenicList.isNullOrEmpty()) {
            allergenicList?.forEachIndexed { index, item ->
                val exceptionView = LayoutInflater.from(exceptionLayout?.context)
                    .inflate(R.layout.allergen_item_layout, exceptionLayout, false) as FrameLayout
                val exceptionName = exceptionView.findViewById<TextView>(R.id.allergen_name)
                exceptionName.text = item

                exceptionView.setOnClickListener {
                    showEditDeleteDialog(index, item)
                }

                exceptionLayout?.addView(exceptionView)

            }
        }
    }

    private fun showAddDialog() {
        val exceptionAlertDialog = Builder(this)

        val inputText = EditText(this)
        exceptionAlertDialog.setTitle(getString(R.string.placeholder_exceptions))
        exceptionAlertDialog.setView(inputText)

        exceptionAlertDialog.setPositiveButton(
            getString(R.string.placeholder_add)
        ) { _, _ ->
            val enteredValue = inputText.text.toString()
            if (!TextUtils.isEmpty(enteredValue))
                addExceptionData(enteredValue)
        }

        exceptionAlertDialog.setNegativeButton(
            getString(R.string.placeholder_cancel)
        ) { _, _ ->
        }
        exceptionAlertDialog.show()
    }

    private fun showEditDeleteDialog(index: Int, item: String) {
        val exceptionAlertDialog = Builder(this)

        val inputText = EditText(this)
        inputText.setText(item)
        exceptionAlertDialog.setTitle(getString(R.string.placeholder_modify_exceptions))
        exceptionAlertDialog.setView(inputText)

        exceptionAlertDialog.setPositiveButton(
            getString(R.string.placeholder_update)
        ) { _, _ ->
            val enteredValue = inputText.text.toString()
            if (!TextUtils.isEmpty(enteredValue)) {
                allergenicList?.set(index, enteredValue)
                modifyExceptionData()
            }

        }

        exceptionAlertDialog.setNegativeButton(
            getString(R.string.placeholder_delete)
        ) { _, _ ->
            allergenicList?.removeAt(index)
            modifyExceptionData()
        }
        exceptionAlertDialog.show()
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
                showSnackBar(main_container, getString(R.string.error_msg))
            }
    }

    private fun addExceptionData(enteredValue: String) {
        allergenicList?.add(enteredValue)
        //Firestore has offline support as well, to make sure the value is being updated on offline mode we have to do the following check.
        setFireStoreDBNetworkState()

        val allergic: HashMap<String, List<String>?> = hashMapOf(FIRESTORE_DOCUMENT_KEY to allergenicList)
        fireStoreDocumentReference?.set(allergic)
            ?.addOnSuccessListener {
                showAllergensList()
            }
            ?.addOnFailureListener { e ->
                showSnackBar(main_container, getString(R.string.error_msg))
            }
    }

    private fun setFireStoreDBNetworkState() {
        if (Utils.isConnected(this)) {
            fireStoreDB.enableNetwork()
        } else {
            fireStoreDB.disableNetwork().addOnSuccessListener {
                showAllergensList()
            }.addOnFailureListener {
                showSnackBar(main_container, getString(R.string.error_msg))
            }
        }
    }

    private fun modifyExceptionData() {
        //Firestore has offline support as well, to make sure the value is being updated on offline mode we have to do the following check.
        setFireStoreDBNetworkState()
        val allergic: HashMap<String, List<String>?> = hashMapOf(FIRESTORE_DOCUMENT_KEY to allergenicList)
        fireStoreDocumentReference?.set(allergic)
            ?.addOnSuccessListener {
                showAllergensList()
            }
            ?.addOnFailureListener { e ->
                showSnackBar(main_container, getString(R.string.error_msg))
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
            } else {
                Log.d("Sign in failed - ", " " + response?.error?.message)
            }
        }
    }
}
