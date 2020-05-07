package com.mypathshala.foodscanner

import android.app.AlertDialog
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.mypathshala.foodscanner.CameraActivity.Companion.BARCODE_BUNDLE_KEY
import com.mypathshala.foodscanner.MainActivity.Companion.FIRESTORE_COLLECTION_ALLERGENS_PROFILE
import com.mypathshala.foodscanner.utils.Utils
import kotlinx.android.synthetic.main.fragment_product.*

class ProductFragment : Fragment() {
    val TAG: String = ProductFragment::class.java.simpleName

    companion object {
        private const val FIRESTORE_COLLECTION_PRODUCTS = "products"
        private const val FIRESTORE_DOCUMENT_KEY_NAME = "name"
        private const val FIRESTORE_DOCUMENT_KEY_INGREDIENTS = "ingredients"
    }

    private var barCode: String? = null
    private val mContext = this.context

    private val auth = FirebaseAuth.getInstance()
    private val fireStoreDB = Firebase.firestore
    private var fireStoreProductDocReference: DocumentReference? = null
    private var fireStoreAllergenDocReference: DocumentReference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_product, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        barCode = arguments?.getString(BARCODE_BUNDLE_KEY)

        if (!TextUtils.isEmpty(barCode)) {
            random?.text = barCode

            fireStoreAllergenDocReference =
                auth.currentUser?.uid?.let { fireStoreDB.collection(FIRESTORE_COLLECTION_ALLERGENS_PROFILE).document(it) }

            fireStoreProductDocReference = fireStoreDB.collection(FIRESTORE_COLLECTION_PRODUCTS).document(barCode!!)

            checkForProduct()
        }
    }

    private fun addProduct() {
        val ingredients: ArrayList<String>? = arrayListOf("sugar", "nuts")
        val product = hashMapOf(FIRESTORE_DOCUMENT_KEY_NAME to barCode, FIRESTORE_DOCUMENT_KEY_INGREDIENTS to ingredients)
        fireStoreProductDocReference?.set(product)
            ?.addOnSuccessListener {
                Utils.showSnackBar(product_container, getString(R.string.success_msg))
            }
            ?.addOnFailureListener { e ->
                Utils.showSnackBar(product_container, getString(R.string.error_msg))
            }
    }

    private fun checkForProduct() {
        /*Fetching allergic list*/
        var productName: String
        fireStoreProductDocReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(FIRESTORE_DOCUMENT_KEY_NAME) != null) {
                    productName = result?.data?.get(FIRESTORE_DOCUMENT_KEY_NAME) as String
                    Utils.showSnackBar(product_container, productName)
                    checkForAllergens()
                } else {
                    Utils.showSnackBar(product_container, "Product Not Found")
                    showAddProductDialog()
                }

            }
            ?.addOnFailureListener { exception ->

            }
    }

    private fun checkForAllergens() {
        var allergenicList: ArrayList<String>? = arrayListOf()

        /*Fetching allergic list*/
        fireStoreAllergenDocReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(MainActivity.FIRESTORE_DOCUMENT_KEY) != null) {
                    allergenicList = result?.data?.get(MainActivity.FIRESTORE_DOCUMENT_KEY) as ArrayList<String>
                    /*Fetching product's inredients list*/
                    fetchIngredients(allergenicList)
                }
            }
            ?.addOnFailureListener { exception ->
                Utils.showSnackBar(product_container, getString(R.string.error_msg))
            }
    }

    private fun fetchIngredients(allergenicList: ArrayList<String>?) {
        var ingredientsList: ArrayList<String>? = arrayListOf()
        fireStoreProductDocReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(FIRESTORE_DOCUMENT_KEY_INGREDIENTS) != null) {
                    ingredientsList = result?.data?.get(FIRESTORE_DOCUMENT_KEY_INGREDIENTS) as ArrayList<String>
                    isAllergic(allergenicList, ingredientsList)
                }
            }
            ?.addOnFailureListener { exception ->
                Utils.showSnackBar(product_container, getString(R.string.error_msg))
            }
    }

    private fun isAllergic(allergenicList: ArrayList<String>?, ingredientsList: ArrayList<String>?) {
        var isAllergic = false
        if (!allergenicList.isNullOrEmpty() && !ingredientsList.isNullOrEmpty()) {
            for (allergen in allergenicList) {
                if (!isAllergic) {
                    for (ingredient in ingredientsList) {
                        if (allergen.equals(ingredient,true)) {
                            isAllergic = true
                            break
                        }
                    }
                } else
                    break
            }
        }

        if (isAllergic)
            Utils.showSnackBar(product_container, "Is Allergic")
        else
            Utils.showSnackBar(product_container, "Not Allergic")
    }


    private fun showAddProductDialog() {
        val exceptionAlertDialog = AlertDialog.Builder(this.activity)

        exceptionAlertDialog.setTitle(getString(R.string.placeholder_unknown_product))
        exceptionAlertDialog.setMessage(getString(R.string.placeholder_unknown_product_message))

        exceptionAlertDialog.setPositiveButton(
            getString(R.string.placeholder_add)
        ) { _, _ ->
            addProduct()
        }

        exceptionAlertDialog.setNegativeButton(
            getString(R.string.placeholder_cancel)
        ) { _, _ ->
        }
        exceptionAlertDialog.show()
    }
}
