package com.ncubeeight.dejaentendu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * Re-runs [onResume] whenever this composable's lifecycle owner resumes —
 * e.g. navigating back to this screen after adding a word from another
 * tab. Mirrors iOS's `.task { ... }`-on-appear pattern, needed here
 * because the JSON-file-backed stores (VocabularyStore,
 * ImportedRecordingStore) aren't reactive.
 */
@Composable
fun OnResume(onResume: () -> Unit) {
    val currentOnResume = rememberUpdatedState(onResume)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentOnResume.value()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
