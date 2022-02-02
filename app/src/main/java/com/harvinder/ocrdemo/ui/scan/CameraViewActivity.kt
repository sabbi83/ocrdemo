package com.harvinder.ocrdemo.ui.scan


import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.firebase.database.DatabaseReference
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.text.FirebaseVisionText
import com.harvinder.ocrdemo.R
import com.harvinder.ocrdemo.constants.Constants.Companion.RATIO_16_9_VALUE
import com.harvinder.ocrdemo.constants.Constants.Companion.RATIO_4_3_VALUE
import com.harvinder.ocrdemo.customview.ProgressDialog
import com.harvinder.ocrdemo.databinding.ActivityCameraViewBinding
import com.snatik.storage.Storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import com.google.firebase.database.FirebaseDatabase
import com.harvinder.ocrdemo.constants.Constants.Companion.SCAN_REF
import com.harvinder.ocrdemo.model.ScanText


class CameraViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityCameraViewBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraControl: CameraControl
    private lateinit var cameraInfo: CameraInfo

    // creating a variable for our
    // Firebase Database.
    var databaseReference:DatabaseReference?=null
    private val executor by lazy {
        Executors.newSingleThreadExecutor()
    }
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_view)
        binding = ActivityCameraViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        progressDialog = ProgressDialog(this, false)
        databaseReference= FirebaseDatabase.getInstance().reference.child(SCAN_REF)

        binding.viewFinder.post {
            startCamera()
        }
        binding.captureImage.setOnClickListener {
            progressDialog.show()
            takePicture()
        }
    }
    @Suppress("SameParameterValue")
    private fun createFile(baseFolder: File, format: String, extension: String) =
        File(
            baseFolder, SimpleDateFormat(format, Locale.US)
                .format(System.currentTimeMillis()) + extension
        )

    private fun takePicture() {

        val file = createFile(
            getOutputDirectory(
                this
            ),
            "yyyy-MM-dd-HH-mm-ss-SSS",
            ".png"
        )
        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(file).build()
        imageCapture.takePicture(
            outputFileOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val savedUri = outputFileResults.savedUri ?: Uri.fromFile(file)
                    try {
                        processImage(savedUri)
                    }catch (e :Exception){
                        e.printStackTrace()
                    }

                }

                override fun onError(exception: ImageCaptureException) {
                    progressDialog.dismiss()
                    Log.e("error", exception.localizedMessage!!)
                }
            })
    }

    fun processImage(bm:Uri) {

            binding.editText.setText("")
            val image = FirebaseVisionImage.fromFilePath(this,bm)
            val detector = FirebaseVision.getInstance().onDeviceTextRecognizer
            detector.processImage(image)
                .addOnSuccessListener { firebaseVisionText ->
                    progressDialog.dismiss()
                    processResultText(firebaseVisionText)
                }
                .addOnFailureListener {
                    progressDialog.dismiss()
                    binding.editText.setText("Failed")
                }


    }


    private fun processResultText(resultText: FirebaseVisionText) {
        if (resultText.textBlocks.size == 0) {
            binding.editText.setText("No Text Found")
            return
        }
        for (block in resultText.textBlocks) {
            val blockText = block.text
            binding.editText.append(blockText + "")
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun startCamera() {


        // Get screen metrics used to setup camera for full screen resolution
        val metrics = DisplayMetrics().also { binding.viewFinder.display.getRealMetrics(it) }

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)

        val rotation = binding.viewFinder.display.rotation


        // Bind the CameraProvider to the LifeCycleOwner
        val cameraSelector =
            CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({

            // CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                // Set initial target rotation
                .setTargetRotation(rotation)

                .build()

            preview.setSurfaceProvider(binding.viewFinder.surfaceProvider)

            // ImageCapture
            imageCapture = initializeImageCapture(screenAspectRatio, rotation)

            // ImageAnalysis


            cameraProvider.unbindAll()

            try {
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
                cameraControl = camera.cameraControl
                cameraInfo = camera.cameraInfo
                cameraControl.setLinearZoom(0.5f)


            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(this))


    }

    private fun getOutputDirectory(context: Context): File {

        val storage = Storage(context)
        val mediaDir = storage.internalCacheDirectory?.let {
            File(it, "Intelligible OCR").apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else context.filesDir
    }


    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }


    private fun initializeImageCapture(
        screenAspectRatio: Int,
        rotation: Int
    ): ImageCapture {
        return ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.saveData ->{
              if(!binding.editText.text.toString().equals("",ignoreCase = true)){
                  val tsLong = System.currentTimeMillis() / 1000
                  val ts = tsLong.toString()
                  val scanText=ScanText(binding.editText.text.toString())
                  databaseReference?.child(ts)?.setValue(scanText)
                  binding.editText.setText("")
                  Toast.makeText(CameraViewActivity@this,"Data Saved",Toast.LENGTH_SHORT).show()

              }else{
                  Toast.makeText(CameraViewActivity@this,"Please Scan the text",Toast.LENGTH_SHORT).show()
              }
            }
        }
        return true
    }
}