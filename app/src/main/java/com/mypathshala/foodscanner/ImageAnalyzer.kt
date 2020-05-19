package com.mypathshala.foodscanner

import android.app.Activity
import android.text.TextUtils
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata

/**
 * Here we have two types of image detectors, 'onDeviceTextRecognizer' for scanning the ingredients from the product, and 'visionBarcodeDetector' to scan the product barcode.
 */
class ImageAnalyzer(activity: Activity, private val isBarCodeScanner: Boolean) : ImageAnalysis.Analyzer {
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

            /*Based on the boolean we choose the detector.*/
            if (isBarCodeScanner)
                scanBarCode(firebaseVisionImage)
            else
                scanText(firebaseVisionImage)
        }
    }

    /**
     * Recognize the text from the image. The result will not be accurate always, we have to edit it before uploading to db.
     */
    private fun scanText(firebaseVisionImage: FirebaseVisionImage) {
        val textRecognizer = FirebaseVision.getInstance()
            .onDeviceTextRecognizer

        //The 'analyze' function will be triggered multiple times. To avoid duplicate values we are maintaining the boolean 'alreadyDetected'.
        textRecognizer.processImage(firebaseVisionImage)
            .addOnSuccessListener { firebaseVisionText ->
                if (!TextUtils.isEmpty(firebaseVisionText.text) && !alreadyDetected) {
                    alreadyDetected = true
                    Log.d(TAG, "### " + firebaseVisionText.text)
                    scannerCallBack.onTextScanComplete(firebaseVisionText.text)
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "OnFailure -")
            }
    }

    /**
     * Scan's the barcode of any format and give the result as array of Barcode. Assuming we always scan one at a time we are always looking for the first barcode object.
     */
    private fun scanBarCode(firebaseVisionImage: FirebaseVisionImage) {
        val barCodedDetector = FirebaseVision.getInstance()
            .visionBarcodeDetector

        barCodedDetector.detectInImage(firebaseVisionImage)
            .addOnSuccessListener { barcodes ->
                if (!barcodes.isNullOrEmpty() && !alreadyDetected) {
                    alreadyDetected = true
                    barcodes[0].displayValue?.let { scannerCallBack.onBarCodeScanComplete(it) }
                }
            }
            .addOnFailureListener {
                Log.d(TAG, "OnFailure -")
            }
    }
}