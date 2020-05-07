package com.mypathshala.foodscanner

import androidx.fragment.app.Fragment

interface FragmentCallBack {
    fun addFragment(fragment: Fragment, addToBack: Boolean)
}