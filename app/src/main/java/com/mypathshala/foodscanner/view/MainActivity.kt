package com.mypathshala.foodscanner.view

import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.Context
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mypathshala.foodscanner.R
import com.mypathshala.foodscanner.utils.FlowLayout
import com.mypathshala.foodscanner.utils.Utils
import com.mypathshala.foodscanner.utils.Utils.showSnackBar
import com.mypathshala.foodscanner.viewModel.HomeScreenViewModel
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    val TAG: String = MainActivity::class.java.simpleName

    companion object {
        private const val RC_SIGN_IN = 123
        const val FIRESTORE_COLLECTION_ALLERGENS_PROFILE = "allergns_profile"
        const val FIRESTORE_DOCUMENT_KEY = "allergic"
        const val IS_BARCODE_SCANNER = "is_barcode_scanner"
    }

    private val fireStoreDB = Firebase.firestore
    private var exceptionLayout: FlowLayout? = null
    private var allergenicList: ArrayList<String>? = arrayListOf()
    private lateinit var homeScreenViewModel: HomeScreenViewModel
    private lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContext = this.baseContext
        exceptionLayout = findViewById(R.id.allergen_items_flow_layout)
        homeScreenViewModel = ViewModelProviders.of(this).get(HomeScreenViewModel::class.java)

        checkLoginState()
    }


    /**
     * To check the login state, eight the user is logged or not. Only logged in users can access the app.
     */
    private fun checkLoginState() {
        if (homeScreenViewModel.isUserLoggedIn()) {
            onUserLogin()
        } else {
            onUserLogout()
            createSignInIntent()
        }
    }

    private fun onUserLogin() {
        //Updating the user login status with UserName
        user_status_tv?.text = homeScreenViewModel.getUserStatus(mContext)

        //Setting user avatar
        user_avatar?.setImageURI(homeScreenViewModel.getUserAvatarURL())


        //Setting views visibility
        signout_button?.visibility = View.VISIBLE
        signin_button?.visibility = View.GONE

        signout_button?.setOnClickListener {
            signOut()
        }

        allergen_profile_card_view?.visibility = View.VISIBLE
        scanner_card_view?.visibility = View.VISIBLE

        add_allergen_button?.setOnClickListener {
            showAddDialog()
        }

        // To scan the barcode
        scan_button?.setOnClickListener {
            val cameraIntent = Intent(this, CameraActivity::class.java)
            cameraIntent.putExtra(IS_BARCODE_SCANNER, true)
            startActivity(cameraIntent)
        }

        // Here we fetch the collection 'allergy_profile' and the respective document of the user. After fetching we show the items.
        homeScreenViewModel.getUserExceptionalList()?.observe(this, Observer {
            if (it != null && it.size > 0) {
                allergenicList = it
                showAllergensList()
            } //else
            // showSnackBar(main_container, getString(R.string.error_msg))
        })
    }

    /**
     * Update the UI and reset the user specific values.
     */
    private fun onUserLogout() {
        homeScreenViewModel.removeUserDocumentReference()
        allergenicList?.clear()
        exceptionLayout?.removeAllViews()

        //Updating the user login status with GuestUser
        user_status_tv?.text = homeScreenViewModel.getUserStatus(mContext)

        //Remove avatar from image view
        user_avatar?.setImageURI(homeScreenViewModel.getUserAvatarURL())

        //Updating views visibility
        signout_button?.visibility = View.GONE
        allergen_profile_card_view?.visibility = View.GONE
        scanner_card_view?.visibility = View.GONE
        signin_button?.visibility = View.VISIBLE

        signin_button?.setOnClickListener {
            createSignInIntent()
        }
    }

    /**
     * Display's the specific user's allergic profile by fetching the data from firestore.
     */
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

    /**
     * AlertDialog to add the exceptions(Allergens).
     */
    private fun showAddDialog() {
        val exceptionAlertDialog = Builder(this)

        val inputText = EditText(this)
        exceptionAlertDialog.setTitle(getString(R.string.placeholder_exceptions))
        exceptionAlertDialog.setView(inputText)

        exceptionAlertDialog.setPositiveButton(
            getString(R.string.placeholder_add)
        ) { _, _ ->
            val enteredValue = inputText.text.toString()
            if (!TextUtils.isEmpty(enteredValue)) {
                allergenicList?.add(enteredValue)
                updateExceptionData()
            }
        }

        exceptionAlertDialog.setNegativeButton(
            getString(R.string.placeholder_cancel)
        ) { _, _ ->
        }
        exceptionAlertDialog.show()
    }

    /**
     * On Tap of exception items user can update or delete the value.
     */
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
                //modifyExceptionData()
                updateExceptionData()
            }

        }

        exceptionAlertDialog.setNegativeButton(
            getString(R.string.placeholder_delete)
        ) { _, _ ->
            allergenicList?.removeAt(index)
            //modifyExceptionData()
            updateExceptionData()
        }
        exceptionAlertDialog.show()
    }

    /**
     * Here we add/modify/delete the exceptions to the FireStore db. Also works in offline
     */
    private fun updateExceptionData() {
        //Firestore has offline support as well, to make sure the value is being updated on offline mode we have to do the following check.
        setFireStoreDBNetworkState()

        allergenicList?.let {
            homeScreenViewModel.onUpdatingExceptionalData(it).observe(this, Observer { isSuccessFull ->
                if (isSuccessFull)
                    showAllergensList()
                else {
                    showSnackBar(main_container, getString(R.string.error_msg))
                }
            })
        }
    }

    /**
     * This is to update the firestore listeners based on the network state.
     */
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

    /**
     * Signout the user from firebase auth
     */
    private fun signOut() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                onUserLogout()
            }
    }

    /**
     * With the help of firebase auth-ui we are loading the login screen.
     */
    private fun createSignInIntent() {
        // Authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.FacebookBuilder().build(),//TODO Facebook login is misbehaving, have to fit it.
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
