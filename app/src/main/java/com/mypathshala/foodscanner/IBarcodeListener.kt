package com.mypathshala.foodscanner

interface IBarcodeListener {
    fun onBarCodeDetected(displayCode: String)
}