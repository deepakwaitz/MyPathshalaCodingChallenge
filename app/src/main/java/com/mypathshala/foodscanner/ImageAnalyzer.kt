package com.mypathshala.foodscanner

import android.app.Activity
import android.util.Log
import androidx.camera.core.CameraX
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata

class ImageAnalyzer(val activity: Activity) : ImageAnalysis.Analyzer {
    val TAG: String = ImageAnalyzer::class.java.simpleName
    var mBarCodeListener: IBarcodeListener = activity as IBarcodeListener

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
            val detector = FirebaseVision.getInstance()
                .visionBarcodeDetector

            val result = detector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener { barcodes ->
                    if (!barcodes.isNullOrEmpty())
                        for (barcode in barcodes) {
                            Log.d(TAG, "OnSuccess -" + barcode.displayValue)
                            barcode.displayValue?.let { mBarCodeListener.onBarCodeDetected(it) }
                            CameraX.unbindAll()
                            //activity.finish()
                        }
                }
                .addOnFailureListener {
                    Log.d(TAG, "OnFailure -")
                }

        }
    }
}