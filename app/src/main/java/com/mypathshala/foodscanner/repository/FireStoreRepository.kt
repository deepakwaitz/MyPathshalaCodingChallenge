package com.mypathshala.foodscanner.repository

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FireStoreRepository {

    private const val FIRE_STORE_COLLECTION_ALLERGENS_PROFILE = "allergns_profile"
    private const val FIRE_STORE_DOCUMENT_KEY = "allergic"

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val fireStoreDB = Firebase.firestore
    private var fireStoreDocumentReference: DocumentReference? = null

    init {
        fireStoreDocumentReference =
            auth.currentUser?.uid?.let {
                fireStoreDB.collection(FIRE_STORE_COLLECTION_ALLERGENS_PROFILE).document(it)
            }
    }

    fun getUserName(): String? {
        return auth.currentUser?.displayName
    }

    fun getUserAvatarURL(): Uri? {
        return auth.currentUser?.photoUrl
    }

}