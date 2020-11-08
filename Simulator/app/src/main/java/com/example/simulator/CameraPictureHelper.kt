package com.example.simulator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.Image
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.lifecycle.LifecycleOwner
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import kotlinx.android.synthetic.main.activity_photo.*
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor

private const val LOG_TAG = "SIMULATOR_TAG"

class CameraPictureHelper(
    var imageCapture: ImageCapture?,
    private var executor: Executor,
    private var context: Context,
    private var firebaseStorageReference: StorageReference,
    var lensFacing: CameraX.LensFacing,
    var previewView: TextureView,
    var lifecycleOwner: LifecycleOwner?
) {

    fun takePicture(soundStream: FileInputStream, decibels: Double) {
        savePictureToMemory(soundStream, decibels)
    }

    private fun savePictureToMemory(soundStream: FileInputStream, decibels: Double) {
        imageCapture?.takePicture(executor,
            object : ImageCapture.OnImageCapturedListener() {
                override fun onError(
                    error: ImageCapture.ImageCaptureError,
                    message: String, exc: Throwable?
                ) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.image_save_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onCaptureSuccess(
                    imageProxy: ImageProxy?,
                    rotationDegrees: Int
                ) {
                    imageProxy?.image?.let {
                        val bitmap = rotateImage(
                            imageToBitmap(it),
                            rotationDegrees.toFloat()
                        )

                        uploadImageToCloud(imageToByteArray(bitmap), soundStream, decibels)
                    }
                    super.onCaptureSuccess(imageProxy, rotationDegrees)
                }
            })
    }

    private fun uploadImageToCloud(
        imageByteArray: ByteArray,
        soundStream: FileInputStream,
        decibels: Double
    ) {
        var downloadSoundUri = ""
        val imageRef =
            firebaseStorageReference.child("simulatorImages/Image_" + System.currentTimeMillis() + ".jpg")
        val uploadImageTask = imageRef.putBytes(imageByteArray)
        uploadImageTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d(LOG_TAG, "Upload image to firebase cloud storage was successful!")
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val downloadImageUri = uri.toString()
                    val soundRef =
                        firebaseStorageReference.child("simulatorSounds/Sound_" + System.currentTimeMillis() + ".mp3")
                    val metadata: StorageMetadata =
                        StorageMetadata.Builder().setContentType("audio/mp3").build()
                    val uploadSoundTask = soundRef.putStream(soundStream, metadata)
                    uploadSoundTask.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d(LOG_TAG, "Upload sound to firebase cloud storage was successful!")
                            soundRef.downloadUrl.addOnSuccessListener { uri ->
                                downloadSoundUri = uri.toString()
                            }.addOnCompleteListener {
                                val formatter: DateFormat =
                                    SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault())
                                val today = formatter.format(Date())
                                writeToFirestore(
                                    Notification(
                                        downloadImageUri,
                                        downloadSoundUri,
                                        today,
                                        decibels
                                    )
                                )
                            }
                        }
                    }
                }
            } else {
                Log.e(LOG_TAG, "Upload image to firebase cloud storage failed!")
            }
        }
    }

    private fun writeToFirestore(notification: Notification) {
        val notebookRef = FirebaseFirestore.getInstance()
            .collection("Notifications")
        notebookRef.add(notification).addOnSuccessListener {
            Log.d(LOG_TAG, "Successfuly uploaded notification to Firestore!")
        }.addOnFailureListener {
            Log.e(LOG_TAG, "Error while uploading notification to Firestore!")
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.capacity())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
    }

    private fun imageToByteArray(imageBitmap: Bitmap): ByteArray {
        val baos = ByteArrayOutputStream()
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        return baos.toByteArray();
    }

    fun startCamera() {
        CameraX.unbindAll()

        val preview = createPreviewUseCase()

        preview.setOnPreviewOutputUpdateListener {
            val parent = previewView.parent as ViewGroup
            parent.removeView(previewView)
            parent.addView(previewView, 0)

            previewView.surfaceTexture = it.surfaceTexture

            updateTransform()
        }

        imageCapture = createCaptureUseCase()

        CameraX.bindToLifecycle(lifecycleOwner, preview, imageCapture)
    }

    private fun createPreviewUseCase(): Preview {
        val previewConfig = PreviewConfig.Builder().apply {
            setLensFacing(lensFacing)
            setTargetRotation(previewView.display.rotation)
        }.build()

        return Preview(previewConfig)
    }

    private fun updateTransform() {
        val matrix = Matrix()

        val centerX = previewView.width / 2f
        val centerY = previewView.height / 2f

        val rotationDegrees = when (previewView.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        previewView.setTransform(matrix)
    }

    private fun createCaptureUseCase(): ImageCapture {
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setLensFacing(lensFacing)
                setTargetRotation(previewView.display.rotation)
                setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
            }
        return ImageCapture(imageCaptureConfig.build())
    }

    fun toggleFrontBackCamera() {
        lensFacing = if (lensFacing == CameraX.LensFacing.BACK) {
            CameraX.LensFacing.FRONT
        } else {
            CameraX.LensFacing.BACK
        }
        previewView.post { startCamera() }
    }
}