package com.mypathshala.foodscanner.viewModel

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mypathshala.foodscanner.R
import com.mypathshala.foodscanner.repository.FireStoreRepository

class HomeScreenViewModel : ViewModel() {

    fun isUserLoggedIn(): Boolean {
        return FireStoreRepository.getCurrentUser() != null
    }

    fun getUserStatus(context: Context): String {
        val userName = FireStoreRepository.getCurrentUser()?.displayName
        return if (!TextUtils.isEmpty(userName))
            context.getString(R.string.placeholder_hi) + userName + context.getString(R.string.placeholder_welcome)
        else
            context.getString(R.string.placeholder_guest_user)
    }

    fun getUserAvatarURL(): Uri? {
        return FireStoreRepository.getCurrentUser()?.photoUrl
    }

    fun getUserExceptionalList(): MutableLiveData<ArrayList<String>> {
        return FireStoreRepository.getUserExceptionData()
    }

    fun onUpdatingExceptionalData(updatedAllergenicList: ArrayList<String>): MutableLiveData<Boolean> {
        return FireStoreRepository.onUpdatingExceptionData(updatedAllergenicList)
    }

    fun removeUserDocumentReference() {
        FireStoreRepository.removeUserDocumentReference()
    }
}