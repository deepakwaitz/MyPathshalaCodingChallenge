package com.mypathshala.foodscanner.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot
import com.mypathshala.foodscanner.repository.FireStoreRepository

class ProductViewModel : ViewModel() {

    fun getProduct(productCode: String): MutableLiveData<DocumentSnapshot> {
        return FireStoreRepository.getProduct(productCode)
    }

    fun getProductIngredients(productCode: String): MutableLiveData<String> {
        return FireStoreRepository.getProductIngredients(productCode)
    }

    fun getUserExceptionalList(): MutableLiveData<ArrayList<String>> {
        return FireStoreRepository.getUserExceptionData()
    }

    fun onAddUpdateProduct(productCode: String, productName: String, ingredients: String): MutableLiveData<Boolean> {
        return FireStoreRepository.onAddUpdateProduct(productCode, productName, ingredients)
    }

}