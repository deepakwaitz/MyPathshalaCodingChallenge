package com.mypathshala.foodscanner

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata

class ImageAnalyzer(val activity: Activity, private val isBarCodeScanner: Boolean) : ImageAnalysis.Analyzer {
    val TAG: String = ImageAnalyzer::class.java.simpleName
    var scannerCallBack: IScannerCallBacks = activity as IScannerCallBacks
    var alreadyDetected = false

    private fun degreesToFirebaseRotation(degrees: Int): Int = when (degrees) {
        0 -> FirebaseVisionImageMetadata.ROTATION_0
        90 -> FirebaseVisionImageMetadata.ROTATION_90
        180 -> FirebaseVisionImageMetadata.ROTATION_180
        270 -> FirebaseVisionImageMetadata.ROTATION_270
        else -> throw Exception("Rotation must be 0, 90, 180, or 270.")
    }

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val mediaImage = image.image
        val imageRotation = degreesToFirebaseRotation(rotationDegrees)
        if (mediaImage != null) {
            val firebaseVisionImage = FirebaseVisionImage.fromMediaImage(mediaImage, imageRotation)

            if (isBarCodeScanner)
                scanBarCode(firebaseVisionImage)
            else
                scanText(firebaseVisionImage)
        }
    }

    private fun scanText(firebaseVisionImage: FirebaseVisionImage) {
        val textRecognizer = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

        textRecognizer.processImage(firebaseVisionImage)
            .addOnSuccessListener { firebaseVisionText ->
                if (!TextUtils.isEmpty(firebaseVisionText.text) && !alreadyDetected) {
                    Log.d(TAG, "### " + firebaseVisionText.text)
                    scannerCallBack.onScanComplete(firebaseVisionText.text)
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "OnFailure -")
            }
    }

    private fun scanBarCode(firebaseVisionImage: FirebaseVisionImage) {
        val barCodedDetector = FirebaseVision.getInstance()
            .visionBarcodeDetector


        barCodedDetector.detectInImage(firebaseVisionImage)
            .addOnSuccessListener { barcodes ->
                if (!barcodes.isNullOrEmpty() && !alreadyDetected) {
                    alreadyDetected = true
                    barcodes[0].displayValue?.let { scannerCallBack.onScanComplete(it) }
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "OnFailure -")
            }
    }
}