package com.mypathshala.foodscanner

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mypathshala.foodscanner.CameraActivity.Companion.BARCODE_BUNDLE_KEY
import com.mypathshala.foodscanner.MainActivity.Companion.FIRESTORE_COLLECTION_ALLERGENS_PROFILE
import com.mypathshala.foodscanner.utils.Utils
import com.mypathshala.foodscanner.utils.Utils.showSnackBar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_product.*

class ProductActivity : AppCompatActivity() {
    val TAG: String = ProductActivity::class.java.simpleName

    companion object {
        private const val FIRESTORE_COLLECTION_PRODUCTS = "products"
        private const val FIRESTORE_DOCUMENT_KEY_NAME = "name"
        private const val FIRESTORE_DOCUMENT_KEY_INGREDIENTS = "ingredients"
    }

    private var barCode: String? = null

    private val auth = FirebaseAuth.getInstance()
    private val fireStoreDB = Firebase.firestore
    private var fireStoreProductDocReference: DocumentReference? = null
    private var fireStoreAllergenDocReference: DocumentReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product)

        barCode = intent.getStringExtra(BARCODE_BUNDLE_KEY)

        if (!TextUtils.isEmpty(barCode)) {
            fireStoreAllergenDocReference =
                auth.currentUser?.uid?.let { fireStoreDB.collection(FIRESTORE_COLLECTION_ALLERGENS_PROFILE).document(it) }

            fireStoreProductDocReference = fireStoreDB.collection(FIRESTORE_COLLECTION_PRODUCTS).document(barCode!!)

            checkForProduct()
        }
    }

    private fun addUpdateProduct(productName: String, ingredients: String) {
        val product = hashMapOf(FIRESTORE_DOCUMENT_KEY_NAME to productName, FIRESTORE_DOCUMENT_KEY_INGREDIENTS to ingredients)

        if (Utils.isConnected(this)) {
            fireStoreDB.enableNetwork()
        } else {
            fireStoreDB.disableNetwork().addOnSuccessListener {
                finish()
            }.addOnFailureListener {
                showSnackBar(main_container, getString(R.string.error_msg))
            }
        }

        fireStoreProductDocReference?.set(product)
            ?.addOnSuccessListener {
                finish()
            }
            ?.addOnFailureListener { e ->
                showSnackBar(product_container, getString(R.string.error_msg))
            }
    }

    private fun checkForProduct() {
        /*Fetching allergic list*/
        fireStoreProductDocReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(FIRESTORE_DOCUMENT_KEY_NAME) != null) {
                    onProductFound(result)
                    checkForAllergens()
                } else {
                    showSnackBar(product_container, "Product Not Found")
                    showAddProductDialog()
                }
            }
            ?.addOnFailureListener { exception ->

            }
    }

    private fun onProductFound(result: DocumentSnapshot) {
        page_title?.text = getString(R.string.title_product_found)

        val productName = result.data?.get(FIRESTORE_DOCUMENT_KEY_NAME) as String
        val ingredients = result.data?.get(FIRESTORE_DOCUMENT_KEY_INGREDIENTS) as String

        product_name_edit_text?.setText(productName)
        product_ingredient_edit_text?.setText(ingredients)

        add_update_product_button?.text = getString(R.string.placeholder_update)

        add_update_product_button?.setOnClickListener {
            val updatedProductName = product_name_edit_text?.text.toString()
            val updatedIngredients = product_ingredient_edit_text?.text.toString()
            addUpdateProduct(updatedProductName, updatedIngredients)
        }
    }

    private fun checkForAllergens() {
        var allergenicList: ArrayList<String>? = arrayListOf()

        /*Fetching allergic list*/
        fireStoreAllergenDocReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(MainActivity.FIRESTORE_DOCUMENT_KEY) != null) {
                    allergenicList = result?.data?.get(MainActivity.FIRESTORE_DOCUMENT_KEY) as ArrayList<String>
                    /*Fetching product's inredients*/
                    fetchIngredients(allergenicList)
                }
            }
            ?.addOnFailureListener { exception ->
                showSnackBar(product_container, getString(R.string.error_msg))
            }
    }

    private fun fetchIngredients(allergenicList: ArrayList<String>?) {
        var ingredients: String
        fireStoreProductDocReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(FIRESTORE_DOCUMENT_KEY_INGREDIENTS) != null) {
                    ingredients = result.data?.get(FIRESTORE_DOCUMENT_KEY_INGREDIENTS) as String
                    isAllergic(allergenicList, ingredients)
                }
            }
            ?.addOnFailureListener { exception ->
                showSnackBar(product_container, getString(R.string.error_msg))
            }
    }

    private fun isAllergic(allergenicList: ArrayList<String>?, ingredients: String?) {
        var isAllergic = false
        var allergicIngredients: String = ""
        if (!allergenicList.isNullOrEmpty() && !TextUtils.isEmpty(ingredients)) {
            for (allergen in allergenicList) {
                if (ingredients.toString().contains(allergen, true)) {
                    isAllergic = true
                    allergicIngredients += allergen
                }
            }
        }

        if (isAllergic) {
            allergen_note_card_view?.visibility = View.VISIBLE
            allergic_warning_layout?.setBackgroundColor(resources.getColor(R.color.light_red_color))
            allergic_note_tv?.text = getString(R.string.allergic_warning) + allergicIngredients
            ok_button?.setOnClickListener {
                finish()
            }
        } else {
            allergen_note_card_view?.visibility = View.VISIBLE
            allergic_warning_layout?.setBackgroundColor(resources.getColor(R.color.border_color))
            allergic_note_tv?.text = getString(R.string.no_allergic_warning)
            allergic_note_tv?.setTextColor(resources.getColor(R.color.colorPrimaryDark))
            ok_button?.setOnClickListener {
                finish()
            }
        }

    }


    private fun showAddProductDialog() {
        val exceptionAlertDialog = AlertDialog.Builder(this)

        exceptionAlertDialog.setTitle(getString(R.string.placeholder_unknown_product))
        exceptionAlertDialog.setMessage(getString(R.string.placeholder_unknown_product_message))

        exceptionAlertDialog.setPositiveButton(
            getString(R.string.placeholder_add)
        ) { _, _ ->

            onAddProduct()

        }

        exceptionAlertDialog.setNegativeButton(
            getString(R.string.placeholder_cancel)
        ) { _, _ ->
            finish()
        }
        exceptionAlertDialog.show()
    }

    private fun onAddProduct() {
        page_title?.text = getString(R.string.title_new_product)

        add_update_product_button?.setOnClickListener {
            val productName = product_name_edit_text?.text.toString()
            val ingredients = product_ingredient_edit_text?.text.toString()
            addUpdateProduct(productName, ingredients)
        }
    }
}
