package com.example.simulator

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import kotlin.math.log10

private const val LOG_TAG = "SIMULATOR_TAG"

class MediaRecorderHelper(
    private var fileName: String = "",
    private var fileNameCopy: String = "",
    private var context: Context,
    private var cameraPictureHelper: CameraPictureHelper
) {
    var recorder: MediaRecorder? = null

    fun startRecording() {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(fileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
        recorder?.maxAmplitude
    }

    fun stopRecording() {
        val amplitude = recorder?.maxAmplitude
        val decibels = amplitude?.div(1.0)?.let { 20 * log10(it) }
        recorder?.apply {
            stop()
            release()
        }
        context.toast("Number of decibels: " + decibels.toString())
        if (decibels != null) {
            if (decibels > 70) {
                cameraPictureHelper.takePicture(
                    FileInputStream(File(fileName).copyTo(File(fileNameCopy), true)),
                    decibels
                )
            }
        }
        recorder = null
    }

    private fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}