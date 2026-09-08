package com.ncubeeight.dejaentendu.audio

import android.media.MediaRecorder
import java.io.File
import java.io.IOException
import java.util.UUID

class LiveAudioRecorderException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Records straight to an AAC/.m4a file — the Android analog of iOS's
 * LiveRecordingView (AVAudioRecorder). One recorder per recording; call
 * [start] then [stop], and read [outputFile] once stopped.
 */
class LiveAudioRecorder(private val cacheDir: File) {
    private var recorder: MediaRecorder? = null
    var outputFile: File? = null
        private set

    fun start() {
        val file = File(cacheDir, "${UUID.randomUUID()}.m4a")
        // No-arg constructor rather than MediaRecorder(Context) (API 31+)
        // — this app's minSdk is 26.
        @Suppress("DEPRECATION")
        val newRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44_100)
            setOutputFile(file.absolutePath)
        }
        try {
            newRecorder.prepare()
            newRecorder.start()
        } catch (e: IOException) {
            newRecorder.release()
            throw LiveAudioRecorderException(e.message ?: "Couldn't start recording", e)
        } catch (e: IllegalStateException) {
            newRecorder.release()
            throw LiveAudioRecorderException(e.message ?: "Couldn't start recording", e)
        }
        recorder = newRecorder
        outputFile = file
    }

    /** Stops and releases the recorder. Safe to call even if [start] never succeeded. */
    fun stop() {
        val current = recorder ?: return
        try {
            current.stop()
        } catch (e: RuntimeException) {
            // stop() throws if called too soon after start() with no audio
            // captured yet — the output file is unusable either way, so
            // this is surfaced to the caller via a missing/invalid file
            // rather than a crash.
        } finally {
            current.release()
            recorder = null
        }
    }

    fun cancel() {
        stop()
        outputFile?.delete()
        outputFile = null
    }
}
