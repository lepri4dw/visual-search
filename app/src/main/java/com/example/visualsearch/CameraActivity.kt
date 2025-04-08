package com.example.visualsearch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var captureButton: FloatingActionButton
    private val executor: Executor = Executors.newSingleThreadExecutor()

    private var scanLine: View? = null
    private var scanFrame: View? = null
    private var scanAnimation: Animation? = null
    private var captureButtonAnimation: Animation? = null
    private var buttonClose: FloatingActionButton? = null
    private var loadingOverlay: View? = null
    private var scanTip: TextView? = null

    private fun startScanAnimation() {
        scanLine?.let { line ->
            scanAnimation?.let { animation ->
                line.startAnimation(animation)
            }
        }
        
        // Apply pulse animation to the scan frame
        scanFrame?.let { frame ->
            val pulseAnimation = AnimationUtils.loadAnimation(this, R.anim.scan_pulse_animation)
            frame.startAnimation(pulseAnimation)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        try {
            // Initialize UI components
            viewFinder = findViewById(R.id.viewFinder)
            captureButton = findViewById(R.id.capture_button)
            buttonClose = findViewById(R.id.button_close)
            progressBar = findViewById(R.id.progressBar)
            statusText = findViewById(R.id.status_text)
            scanTip = findViewById(R.id.scanTip)
            loadingOverlay = findViewById(R.id.loadingOverlay)

            // Initialize scanning line and animation
            scanLine = findViewById(R.id.scanLine)
            scanFrame = findViewById(R.id.scanFrame)
            scanAnimation = AnimationUtils.loadAnimation(this, R.anim.scan_line_animation)
            captureButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.pulse_capture_button)

            // Apply animations
            captureButton.startAnimation(captureButtonAnimation)

            // Check permissions
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
            }

            // Set up capture button
            captureButton.setOnClickListener { takePhoto() }
            
            // Set up close button
            buttonClose?.setOnClickListener {
                finish()
            }

            // Start scan animation
            startScanAnimation()

        } catch (e: Exception) {
            Log.e(TAG, "Error during initialization: ", e)
            Toast.makeText(this, "Initialization error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startCamera() {
        try {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    bindPreview(cameraProvider)
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting camera: ", e)
                    Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (e: Exception) {
            Log.e(TAG, "Error starting camera: ", e)
            Toast.makeText(this, "Error starting camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        try {
            // Set up preview
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(viewFinder.surfaceProvider)

            // Set up camera selector (use back camera)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            // Set up image capture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // Remove all bindings before adding new ones
            cameraProvider.unbindAll()

            // Bind camera to lifecycle
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            statusText.text = "Наведите камеру на товар"
        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera: ", e)
            Toast.makeText(this, "Camera initialization error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: run {
            Toast.makeText(this, "Camera not initialized", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            // Stop scanning animation
            scanLine?.let {
                it.clearAnimation()
                it.visibility = View.INVISIBLE
            }

            // Show progress overlay
            loadingOverlay?.visibility = View.VISIBLE
            statusText.text = "Обработка изображения..."
            scanTip?.visibility = View.INVISIBLE

            // Create directory if needed
            val directory = filesDir
            if (!directory.exists()) {
                directory.mkdirs()
            }

            // Create temporary file for photo with unique name
            val photoFile = File(directory, "waste_${System.currentTimeMillis()}.jpg")
            Log.d(TAG, "Saving photo to: ${photoFile.absolutePath}")

            // Create output options
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            // Take photo
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        try {
                            statusText.text = "Processing image..."

                            // Check if file exists
                            if (!photoFile.exists() || photoFile.length() == 0L) {
                                Log.e(TAG, "File was not created or is empty: ${photoFile.absolutePath}")
                                loadingOverlay?.visibility = View.GONE
                                Toast.makeText(this@CameraActivity, "Error saving image", Toast.LENGTH_SHORT).show()
                                return
                            }

                            // Create Bitmap from file
                            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (bitmap != null) {
                                // Return result to MainActivity
                                returnImageResult(bitmap)
                            } else {
                                Log.e(TAG, "Failed to create bitmap from file")
                                loadingOverlay?.visibility = View.GONE
                                Toast.makeText(this@CameraActivity, "Error processing image", Toast.LENGTH_SHORT).show()
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Error after saving image: ", e)
                            loadingOverlay?.visibility = View.GONE
                            Toast.makeText(this@CameraActivity, "Image processing error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        loadingOverlay?.visibility = View.GONE

                        // Restart animation in case of error
                        scanLine?.let {
                            it.visibility = View.VISIBLE
                            startScanAnimation()
                        }

                        Log.e(TAG, "Error saving photo: ", exception)
                        Toast.makeText(
                            this@CameraActivity,
                            "Error taking photo: ${exception.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        statusText.text = "Наведите камеру на товар"
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error taking photo: ", e)
            loadingOverlay?.visibility = View.GONE

            // Restart animation in case of error
            scanLine?.let {
                it.visibility = View.VISIBLE
                startScanAnimation()
            }

            Toast.makeText(this, "Error taking photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun returnImageResult(bitmap: Bitmap) {
        // Create temporary file to store Bitmap
        try {
            val cacheDir = cacheDir
            val outputFile = File(cacheDir, "scanned_image.jpg")

            // Write Bitmap to file
            outputFile.outputStream().use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }

            // Create Intent with result
            val resultIntent = Intent()
            resultIntent.putExtra("image_path", outputFile.absolutePath)
            setResult(RESULT_OK, resultIntent)

            // Finish activity
            finish()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving result: ", e)
            Toast.makeText(this, "Error saving result", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}