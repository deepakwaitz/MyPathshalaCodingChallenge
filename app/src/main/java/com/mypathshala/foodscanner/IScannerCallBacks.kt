package com.mypathshala.foodscanner

interface IScannerCallBacks {
    fun onBarCodeScanComplete(barcode: String)
    fun onTextScanComplete(text: String)
}