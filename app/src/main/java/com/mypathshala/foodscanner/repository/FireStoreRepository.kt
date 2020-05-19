package com.mypathshala.foodscanner.repository

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
        getAllergicDocumentReference()
    }

    /**
     * Creating firestore document with user uid value. So Every user will have the separate document.
     */
    private fun getAllergicDocumentReference() {
        fireStoreAllergicDocumentReference =
            auth.currentUser?.uid?.let {
                fireStoreDB.collection(FIRE_STORE_COLLECTION_ALLERGENS_PROFILE).document(it)
            }
    }

    fun removeAllergicDocumentReference() {
        fireStoreAllergicDocumentReference = null
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

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