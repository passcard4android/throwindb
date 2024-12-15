package me.hda.throwindb

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.core.os.postDelayed
import androidx.core.widget.doOnTextChanged
import io.github.g0dkar.qrcode.QRCode
import me.hda.throwindb.databinding.ActivityNewQrcodeBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


open class NewQRCodeActivity : AppCompatActivity() {
    private lateinit var qrCodeData: EditText
    private lateinit var qrCodeImageView: ImageView
    private lateinit var saveButton: Button
    private lateinit var imageCaptureButton: Button
    private lateinit var videoCaptureButton: Button
    private var qrCodeIsValid = false
    private lateinit var viewBinding: ActivityNewQrcodeBinding


    private val handler = Handler(Looper.getMainLooper())


    private var imageCapture: ImageCapture? = null

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    private lateinit var cameraExecutor: ExecutorService


    companion object {
        var photoUrl = "Empty"
        var videoUrl = "Empty"
        const val QRCODE_DATA = "qrCodeData"
        private const val TAG = "CameraXApp"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle Permission granted/rejected
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(
                baseContext, "Permission request denied", Toast.LENGTH_SHORT
            ).show()
        } else {
            startCamera()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityNewQrcodeBinding.inflate(layoutInflater)
        val view = viewBinding.root
        setContentView(view)


        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        imageCaptureButton = findViewById(R.id.image_capture_button)
        videoCaptureButton = findViewById(R.id.video_capture_button)

        // Set up the listeners for take photo and video capture buttons
        imageCaptureButton.setOnClickListener { takePhoto() }
        videoCaptureButton.setOnClickListener { captureVideo() }

        cameraExecutor = Executors.newSingleThreadExecutor()


        qrCodeData = findViewById(R.id.newQRCodeData)
        qrCodeImageView = findViewById(R.id.newQRCodePreviewImage)
        saveButton = findViewById(R.id.newQRCodeBtn)

        qrCodeData.doOnTextChanged { text, _, _, _ ->
            val string = text?.toString()?.trim()

            qrCodeIsValid = if (string.isNullOrBlank()) {
                qrCodeImageView.setImageBitmap(null)

                false
            } else {
                try {
                    handler.removeCallbacksAndMessages(null)
                    handler.postDelayed(250) {
                        val qrCodeBitmap = QRCode(string).render().nativeImage() as Bitmap
                        qrCodeImageView.setImageBitmap(qrCodeBitmap)
                    }

                    true
                } catch (t: Throwable) {
                    Toast.makeText(
                        this@NewQRCodeActivity, R.string.new_qrcode_error_preview, Toast.LENGTH_LONG
                    ).show()

                    qrCodeImageView.setImageBitmap(null)

                    false
                }
            }
        }

        saveButton.setOnClickListener {
            val qrCodeDataText = qrCodeData.text
            if (qrCodeDataText.isBlank() || !qrCodeIsValid) {
                Toast.makeText(baseContext, R.string.short_text, Toast.LENGTH_SHORT).show()
            } else {
                save()
            }
        }
    }

    private fun save() {
        val resultIntent = Intent()
        val uuid = UUID.randomUUID()
        if (photoUrl === "Empty" && videoUrl === "Empty") {
            resultIntent.putExtra(
                QRCODE_DATA,
                "{\"text\":" + '"' + qrCodeData.text + '"' + ",\"id\":" + '"' + uuid + '"' + "}"
            )
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        if (photoUrl === "Empty" && videoUrl !== "Empty") {
            val qrCodeDataText =
                ("{\"videoUrl\":" + '"' + videoUrl + '"' + ",\"text\":" + '"' + qrCodeData.text + '"' + ",\"id\":" + '"' + uuid + '"' + "}")
            resultIntent.putExtra(QRCODE_DATA, qrCodeDataText)
            videoUrl = "Empty"
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        if (videoUrl === "Empty" && photoUrl !== "Empty") {
            val qrCodeDataText =
                ("{\"photoUrl\":" + '"' + photoUrl + '"' + ",\"text\":" + '"' + qrCodeData.text + '"' + ",\"id\":" + '"' + uuid + '"' + "}")

            resultIntent.putExtra(QRCODE_DATA, qrCodeDataText)
            photoUrl = "Empty"
            setResult(RESULT_OK, resultIntent)
            finish()
        }
        if (videoUrl !== "Empty" && photoUrl !== "Empty") {
            val qrCodeDataText =
                ("{\"videoUrl\":" + '"' + videoUrl + '"' + ",\"photoUrl\":" + '"' + photoUrl + '"' + ",\"text\":" + '"' + qrCodeData.text + '"' + ",\"id\":" + '"' + uuid + '"' + "}")
            resultIntent.putExtra(QRCODE_DATA, qrCodeDataText)
            videoUrl = "Empty"
            photoUrl = "Empty"
            setResult(RESULT_OK, resultIntent)
            finish()
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time stamped name and MediaStore entry.
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    photoUrl = "${output.savedUri}"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                }
            })
    }


    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        videoCaptureButton.isEnabled = false

        val curRecording = recording
        if (curRecording != null) {
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions.Builder(
            contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(contentValues).build()
        recording = videoCapture.output.prepareRecording(this, mediaStoreOutputOptions).apply {
            if (PermissionChecker.checkSelfPermission(
                    this@NewQRCodeActivity, Manifest.permission.RECORD_AUDIO
                ) == PermissionChecker.PERMISSION_GRANTED
            ) {
                withAudioEnabled()
            }
        }.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
            when (recordEvent) {
                is VideoRecordEvent.Start -> {
                    videoCaptureButton.apply {
                        background = getDrawable(R.drawable.videocamera_stop)
                        isEnabled = true
                    }
                }

                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        val msg =
                            "Video capture succeeded: " + "${recordEvent.outputResults.outputUri}"
                        videoUrl = "${recordEvent.outputResults.outputUri}"
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                        Log.d(TAG, msg)
                    } else {
                        recording?.close()
                        recording = null
                        Log.e(
                            TAG, "Video capture ends with error: " + "${recordEvent.error}"
                        )
                    }
                    videoCaptureButton.apply {
                        background = getDrawable(R.drawable.videocamera)
                        isEnabled = true
                    }
                }
            }
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
            }
            val recorder = Recorder.Builder().setQualitySelector(
                QualitySelector.from(
                    Quality.HIGHEST, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                )
            ).build()
            videoCapture = VideoCapture.withOutput(recorder)

            imageCapture = ImageCapture.Builder().build()

            /*
            val imageAnalyzer = ImageAnalysis.Builder().build()
                .also {
                    setAnalyzer(
                        cameraExecutor,
                        LuminosityAnalyzer { luma ->
                            Log.d(TAG, "Average luminosity: $luma")
                        }
                    )
                }
            */

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }


    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}

