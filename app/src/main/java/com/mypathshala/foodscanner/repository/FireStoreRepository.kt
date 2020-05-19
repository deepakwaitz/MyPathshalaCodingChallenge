package com.mypathshala.foodscanner.repository

import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FireStoreRepository {

    private const val FIRE_STORE_COLLECTION_ALLERGENS_PROFILE = "allergns_profile"
    private const val FIRE_STORE_DOCUMENT_KEY = "allergic"

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStoreDB = Firebase.firestore
    private var fireStoreDocumentReference: DocumentReference? = null
    private val allergenicList: MutableLiveData<ArrayList<String>> = MutableLiveData<ArrayList<String>>()
    private val onSuccessFullUpdate: MutableLiveData<Boolean> = MutableLiveData<Boolean>()

    init {
        getUserDocumentReference()
    }

    /**
     * Creating firestore document with user uid value. So Every user will have the separate document.
     */
    private fun getUserDocumentReference() {
        fireStoreDocumentReference =
            auth.currentUser?.uid?.let {
                fireStoreDB.collection(FIRE_STORE_COLLECTION_ALLERGENS_PROFILE).document(it)
            }
    }

    fun removeUserDocumentReference() {
        fireStoreDocumentReference = null
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun getUserExceptionData(): MutableLiveData<ArrayList<String>> {
        if (fireStoreDocumentReference == null)
            getUserDocumentReference()

        fireStoreDocumentReference?.get()
            ?.addOnSuccessListener { result ->
                if (result?.data?.get(FIRE_STORE_DOCUMENT_KEY) != null) {
                    allergenicList.value = result?.data?.get(FIRE_STORE_DOCUMENT_KEY) as ArrayList<String>
                }
            }
            ?.addOnFailureListener { exception ->

            }

        return allergenicList
    }

    fun onUpdatingExceptionData(updatedAllergenicList: ArrayList<String>): MutableLiveData<Boolean> {
        val allergic: HashMap<String, List<String>?> = hashMapOf(FIRE_STORE_DOCUMENT_KEY to updatedAllergenicList)
        fireStoreDocumentReference?.set(allergic)
            ?.addOnSuccessListener {
                onSuccessFullUpdate.value = true
            }
            ?.addOnFailureListener { e ->
                onSuccessFullUpdate.value = false
            }

        return onSuccessFullUpdate
    }

}