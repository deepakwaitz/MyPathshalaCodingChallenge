package com.mypathshala.foodscanner

/**
 * Using this interface we can get the scanned data from ImageAnalyzer to CameraActivity
 */
interface IScannerCallBacks {
    fun onBarCodeScanComplete(barcode: String)
    fun onTextScanComplete(text: String)
}