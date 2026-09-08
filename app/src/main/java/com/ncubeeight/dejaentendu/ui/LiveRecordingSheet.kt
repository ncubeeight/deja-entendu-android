package com.ncubeeight.dejaentendu.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ncubeeight.dejaentendu.audio.AudioIngestion
import com.ncubeeight.dejaentendu.audio.ImportedRecording
import com.ncubeeight.dejaentendu.audio.LiveAudioRecorder
import com.ncubeeight.dejaentendu.audio.LiveAudioRecorderException
import com.ncubeeight.dejaentendu.transcription.SupportedLanguage
import com.ncubeeight.dejaentendu.ui.theme.AppColors
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Records audio directly in the app — for a user who wants to speak a
 * practice sentence right now rather than importing an existing file.
 * Saved through the same AudioIngestion/ImportedRecordingStore path as any
 * other audio sample, so it's indistinguishable once imported. Mirrors
 * iOS's LiveRecordingView.swift (AVAudioRecorder).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveRecordingSheet(
    language: SupportedLanguage,
    onDismiss: () -> Unit,
    onFinish: (ImportedRecording) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val recorder = remember { LiveAudioRecorder(context.cacheDir) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }
    var recordedFile by remember { mutableStateOf<java.io.File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { if (isRecording) recorder.cancel() }
    }

    LaunchedEffect(isRecording) {
        if (!isRecording) return@LaunchedEffect
        val start = System.currentTimeMillis()
        while (isRecording) {
            elapsedMillis = System.currentTimeMillis() - start
            delay(100)
        }
    }

    fun startRecording() {
        errorMessage = null
        try {
            recorder.start()
            isRecording = true
            elapsedMillis = 0L
        } catch (e: LiveAudioRecorderException) {
            errorMessage = e.message
        }
    }

    fun stopRecording() {
        recorder.stop()
        isRecording = false
        recordedFile = recorder.outputFile
    }

    fun cancelAndDismiss() {
        recorder.cancel()
        onDismiss()
    }

    fun finish() {
        val file = recordedFile ?: return
        try {
            val copy = AudioIngestion.copyIntoAppStorage(context, Uri.fromFile(file), ImportedRecording.Source.LIVE_RECORDING, language)
            file.delete()
            val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault())
            val label = "Recording — ${formatter.format(Instant.ofEpochMilli(copy.importedAtEpochMillis))}"
            onFinish(copy.copy(originalFilename = label))
            onDismiss()
        } catch (e: Exception) {
            errorMessage = e.message ?: e.toString()
        }
    }

    val statusText = when {
        !hasPermission -> "Microphone access is required to record. Enable it in Settings."
        isRecording -> "Recording…"
        recordedFile != null -> "Tap Use Recording to save, or record again to redo it."
        else -> "Tap to start recording."
    }

    ModalBottomSheet(onDismissRequest = { cancelAndDismiss() }, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text("Record — ${language.displayName}", color = AppColors.ink)

            Text(
                text = timeString(elapsedMillis),
                fontSize = 48.sp,
                color = AppColors.ink,
            )

            IconButton(
                onClick = { if (isRecording) stopRecording() else startRecording() },
                enabled = hasPermission,
                modifier = Modifier.size(84.dp),
            ) {
                Icon(
                    if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isRecording) "Stop recording" else "Start recording",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else AppColors.coral,
                    modifier = Modifier.size(64.dp),
                )
            }

            Text(statusText, color = AppColors.inkSoft)

            errorMessage?.let { message ->
                Text(message, color = MaterialTheme.colorScheme.error)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { finish() },
                    enabled = recordedFile != null && !isRecording,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Use Recording")
                }
                TextButton(onClick = { cancelAndDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun timeString(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
