package com.example.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import java.io.File
import java.io.IOException

/**
 * AudioRecorderHelper records high-quality spoken memos (AAC/M4A)
 * to be transcribed by Gemini for CRM client context.
 */
class AudioRecorderHelper(private val context: Context) {
    private val tag = "AudioRecorderHelper"

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var startTimeMs: Long = 0L

    val isRecording: Boolean
        get() = mediaRecorder != null

    val recordingDurationMs: Long
        get() = if (isRecording && startTimeMs > 0L) {
            System.currentTimeMillis() - startTimeMs
        } else {
            0L
        }

    val maxAmplitude: Int
        get() = try {
            mediaRecorder?.maxAmplitude ?: 0
        } catch (e: Exception) {
            0
        }

    fun startRecording(targetFile: File? = null): Result<File> {
        return try {
            if (isRecording) {
                cancelRecording()
            }

            val file = targetFile ?: File(
                context.cacheDir,
                "voice_memo_${System.currentTimeMillis()}.m4a"
            )
            if (file.exists()) {
                file.delete()
            }

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            currentOutputFile = file
            startTimeMs = System.currentTimeMillis()
            Log.d(tag, "Recording started -> ${file.absolutePath}")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(tag, "Failed to start audio recording", e)
            releaseRecorder()
            Result.failure(e)
        }
    }

    fun stopRecording(): Result<File> {
        return try {
            val recorder = mediaRecorder ?: return Result.failure(IllegalStateException("No active recording session"))
            val file = currentOutputFile ?: return Result.failure(IllegalStateException("No output file specified"))

            try {
                recorder.stop()
            } catch (e: RuntimeException) {
                Log.w(tag, "stop() called right after start() or interrupted", e)
            }

            releaseRecorder()
            Log.d(tag, "Recording stopped successfully. File size: ${file.length()} bytes")
            Result.success(file)
        } catch (e: Exception) {
            Log.e(tag, "Error stopping audio recording", e)
            releaseRecorder()
            Result.failure(e)
        }
    }

    fun cancelRecording() {
        try {
            mediaRecorder?.let {
                try {
                    it.stop()
                } catch (ignored: Exception) {}
            }
            currentOutputFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        } finally {
            releaseRecorder()
        }
    }

    private fun releaseRecorder() {
        try {
            mediaRecorder?.release()
        } catch (ignored: Exception) {}
        mediaRecorder = null
        currentOutputFile = null
        startTimeMs = 0L
    }
}
