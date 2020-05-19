package com.mypathshala.foodscanner.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mypathshala.foodscanner.IScannerCallBacks
import com.mypathshala.foodscanner.ImageAnalyzer
import com.mypathshala.foodscanner.R
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity(), IScannerCallBacks {
    val TAG: String = CameraActivity::class.java.simpleName
    private val activity: Activity = this
    private lateinit var preview: Preview
    private var isBarCodeScanner = true
    private var barCode: String? = null
    private var productName: String? = null


    // This is an array of all the permission specified in the manifest.
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        viewFinder = findViewById(R.id.view_finder)

        isBarCodeScanner = intent.getBooleanExtra(MainActivity.IS_BARCODE_SCANNER, true)
        barCode = intent.getStringExtra(BARCODE_BUNDLE_KEY)
        productName = intent.getStringExtra(PRODUCT_NAME_BUNDLE_KEY)

        // Request camera permissions
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Every time the provided texture view changes, recompute layout
        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView

    private fun startCamera() {
        // Create configuration object for the viewfinder use case
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(640, 480))
        }.build()


        // Build the viewfinder use case
        preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)

            viewFinder.surfaceTexture = it.surfaceTexture
            updateTransform()
        }

        // Setup image analysis pipeline that computes average pixel luminance
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            // In our analysis, we care more about the latest image than
            // analyzing *every* image
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }.build()

        // Build the image analysis use case and instantiate our analyzer
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, ImageAnalyzer(activity, isBarCodeScanner))
        }

        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        // Compute the center of the view finder
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        // Correct preview output to account for display rotation
        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        // Finally, apply transformations to our TextureView
        viewFinder.setTransform(matrix)
    }

    /**
     * Process result from permission request dialog box, has the request
     * been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    /**
     * Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        const val BARCODE_BUNDLE_KEY = "barcode"
        const val PRODUCT_NAME_BUNDLE_KEY = "product_name"
        const val INGREDIENTS_TEXT_BUNDLE_KEY = "ingredients"
        const val IS_TEXT_SCAN = "text_scan"
    }

    override fun onBarCodeScanComplete(barcodeID: String) {
        CameraX.unbindAll()
        /*Calling ProductActivity*/
        val productIntent = Intent(this, ProductActivity::class.java)
        productIntent.putExtra(BARCODE_BUNDLE_KEY, barcodeID)
        productIntent.putExtra(INGREDIENTS_TEXT_BUNDLE_KEY, "")
        productIntent.putExtra(IS_TEXT_SCAN, false)
        startActivity(productIntent)
        finish()
    }

    override fun onTextScanComplete(text: String) {
        CameraX.unbindAll()
        /*Calling ProductActivity*/
        val productIntent = Intent(this, ProductActivity::class.java)
        productIntent.putExtra(BARCODE_BUNDLE_KEY, barCode)
        productIntent.putExtra(INGREDIENTS_TEXT_BUNDLE_KEY, text)
        productIntent.putExtra(PRODUCT_NAME_BUNDLE_KEY, productName)
        productIntent.putExtra(IS_TEXT_SCAN, true)
        startActivity(productIntent)
        finish()
    }
}
