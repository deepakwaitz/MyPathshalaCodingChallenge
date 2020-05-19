package com.mypathshala.foodscanner.repository

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

/**
 * Repository for both Home screen and product screen.
 */
object FireStoreRepository {

    private const val FIRE_STORE_COLLECTION_ALLERGENS_PROFILE = "allergns_profile"
    private const val FIRE_STORE_DOCUMENT_KEY_ALLERGIC = "allergic"

    private const val FIRE_STORE_COLLECTION_PRODUCTS = "products"
    const val FIRE_STORE_DOCUMENT_KEY_NAME = "name"
    const val FIRE_STORE_DOCUMENT_KEY_INGREDIENTS = "ingredients"

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStoreDB = Firebase.firestore

    private var fireStoreAllergicDocumentReference: DocumentReference? = null

    init {
        /*If the user is logged in fetch create the FireStore document reference. So that it can be used when ever we need.*/
        getAllergicDocumentReference()
    }

    /**
     * Creating firestore document with user uid value. So Every user will have the separate document.
     * Allergens is the separate Collection will have a separate document for each user.
     */
    private fun getAllergicDocumentReference() {
        fireStoreAllergicDocumentReference =
            auth.currentUser?.uid?.let {
                fireStoreDB.collection(FIRE_STORE_COLLECTION_ALLERGENS_PROFILE).document(it)
            }
    }

    /* On User logout Don't forgot to null the doc reference. Because this is specific for each user. */
    fun removeAllergicDocumentReference() {
        fireStoreAllergicDocumentReference = null
    }

    /*
    * To get the current logged in user.
    * */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * To Fetch the allergen list of the logged in user.
     */
    fun getUserExceptionData(): MutableLiveData<ArrayList<String>> {
        val allergenicList: MutableLiveData<ArrayList<String>> = MutableLiveData<ArrayList<String>>()
        if (fireStoreAllergicDocumentReference == null)
            getAllergicDocumentReference()

        fireStoreAllergicDocumentReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(FIRE_STORE_DOCUMENT_KEY_ALLERGIC) != null) {
                    allergenicList.value = result?.data?.get(FIRE_STORE_DOCUMENT_KEY_ALLERGIC) as ArrayList<String>
                }
            }
            ?.addOnFailureListener { exception ->

            }

        return allergenicList
    }

    /**
     * To update(Add, Modify, Delete) the allergens.
     * @return boolean value either the operation is success or not.
     */
    fun onUpdatingExceptionData(updatedAllergenicList: ArrayList<String>): MutableLiveData<Boolean> {
        val onSuccessFullUpdate: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
        val allergic: HashMap<String, List<String>?> = hashMapOf(FIRE_STORE_DOCUMENT_KEY_ALLERGIC to updatedAllergenicList)
        fireStoreAllergicDocumentReference?.set(allergic)
            ?.addOnSuccessListener {
                onSuccessFullUpdate.value = true
            }
            ?.addOnFailureListener { e ->
                onSuccessFullUpdate.value = false
            }

        return onSuccessFullUpdate
    }

    /**
     * To update(Add, Modify) the product.
     * @return boolean value either the operation is success or not.
     * Products are created under the separate collection, Each product is a separate document.
     *
     * @param productCode is the barcode of the product.
     */
    fun onAddUpdateProduct(productCode: String, productName: String, ingredients: String): MutableLiveData<Boolean> {
        val onSuccessFullUpdate: MutableLiveData<Boolean> = MutableLiveData<Boolean>()
        val fireStoreProductDocumentReference = fireStoreDB.collection(FIRE_STORE_COLLECTION_PRODUCTS).document(productCode)
        val product = hashMapOf(FIRE_STORE_DOCUMENT_KEY_NAME to productName, FIRE_STORE_DOCUMENT_KEY_INGREDIENTS to ingredients)
        fireStoreProductDocumentReference.set(product)
            .addOnSuccessListener {
                onSuccessFullUpdate.value = true
            }
            .addOnFailureListener { e ->
                onSuccessFullUpdate.value = false
            }

        return onSuccessFullUpdate
    }

    /**
     * To check whether the product is already added or not.
     */
    fun getProduct(productCode: String): MutableLiveData<DocumentSnapshot> {
        val fireStoreProductDocumentReference = fireStoreDB.collection(FIRE_STORE_COLLECTION_PRODUCTS).document(productCode)
        val productItem: MutableLiveData<DocumentSnapshot> = MutableLiveData<DocumentSnapshot>()

        fireStoreProductDocumentReference?.get()?.addOnSuccessListener { result ->
            if (result?.data?.get(FIRE_STORE_DOCUMENT_KEY_NAME) != null) {
                productItem.value = result
            } else
                productItem.value = null
        }
        return productItem
    }

    /**
     * To fetch the ingredients of the product.
     */
    fun getProductIngredients(productCode: String): MutableLiveData<String> {
        val productIngredients: MutableLiveData<String> = MutableLiveData<String>()
        val fireStoreProductDocumentReference = fireStoreDB.collection(FIRE_STORE_COLLECTION_PRODUCTS).document(productCode)
        fireStoreProductDocumentReference?.get()?.addOnSuccessListener { result ->
            if (result?.data?.get(FIRE_STORE_DOCUMENT_KEY_INGREDIENTS) != null) {
                productIngredients.value = result.data?.get(FIRE_STORE_DOCUMENT_KEY_INGREDIENTS) as String
            }
        }
        return productIngredients
    }
}