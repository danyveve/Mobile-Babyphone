package com.example.simulator

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraX
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_photo.*
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val LOG_TAG = "SIMULATOR_TAG"

class PhotoActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO
        )
    }

    private lateinit var firebaseStorageRefference: StorageReference

    private lateinit var context: Context

    private lateinit var mediaRecorderHelper: MediaRecorderHelper
    private lateinit var cameraPictureHelper: CameraPictureHelper

    private var handler: Handler? = null
    private lateinit var myRunnable: Runnable

    private val executor: Executor by lazy { Executors.newSingleThreadExecutor() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_photo)
        setClickListeners()
        requestPermissions()

        FirebaseApp.initializeApp(this)
        firebaseStorageRefference = Firebase.storage.reference
        context = this.applicationContext

        val fileName = "${externalCacheDir?.absolutePath}/simulatorRecorder.mp3"
        val fileNameCopy = "${externalCacheDir?.absolutePath}/simulatorRecorderCopy.mp3"

        cameraPictureHelper = CameraPictureHelper(
            null,
            executor,
            context,
            firebaseStorageRefference,
            CameraX.LensFacing.BACK,
            previewView,
            this
        )

        mediaRecorderHelper =
            MediaRecorderHelper(fileName, fileNameCopy, context, cameraPictureHelper)

        mediaRecorderHelper.startRecording()
        handler = Handler(Looper.getMainLooper())
        myRunnable = object : Runnable {
            override fun run() {
                mediaRecorderHelper.stopRecording()
                mediaRecorderHelper.startRecording()
                handler?.postDelayed(this, 5000)
            }
        }
        handler?.postDelayed(myRunnable, 5000)
    }

    private fun setClickListeners() {
        toggleCameraLens.setOnClickListener { cameraPictureHelper.toggleFrontBackCamera() }
    }

    private fun requestPermissions() {
        if (allPermissionsGranted()) {
            previewView.post { cameraPictureHelper.startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                previewView.post { cameraPictureHelper.startCamera() }
            } else {
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    override fun onStop() {
        super.onStop()
        mediaRecorderHelper.recorder?.release()
        mediaRecorderHelper.recorder = null
        handler?.removeCallbacks(myRunnable)
        handler = null
    }
}
