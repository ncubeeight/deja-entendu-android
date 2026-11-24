package com.ncubeeight.dejaentendu.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * iOS-style swipe-to-delete: swiping a row left slides it aside and parks a
 * trash button in view; the row is only deleted when that button is tapped.
 * (Material's SwipeToDismissBox deletes the moment the swipe passes its
 * threshold, so a button behind it can never be tapped.)
 *
 * [content] receives whether the button is currently showing and a function
 * that closes it. Rows should use these for their own tap handling: while the
 * button is showing, tapping the row closes it instead of opening the row.
 *
 * With [enabled] = false the row does not respond to swipes.
 *
 * Callers in a LazyColumn must give their items stable keys so this state
 * follows its own row when rows are added or removed.
 */
@Composable
fun SwipeToRevealDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    buttonShape: Shape = RectangleShape,
    buttonGap: Dp = 0.dp,
    revealWidth: Dp = 80.dp,
    enabled: Boolean = true,
    deleteLabel: String = "Delete",
    closeAfterDelete: Boolean = false,
    content: @Composable (isRevealed: Boolean, close: () -> Unit) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val revealPx = with(LocalDensity.current) { revealWidth.toPx() }
    val offsetX = remember { Animatable(0f) }
    val isRevealed = offsetX.value < -revealPx / 2

    Box(modifier = modifier.fillMaxWidth()) {
        // Only composed once the row has moved, so nothing red peeks out from
        // behind the row's rounded corners while it is at rest.
        if (offsetX.value < 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(start = buttonGap),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .width(revealWidth - buttonGap)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.error, buttonShape)
                        .clickable(onClickLabel = deleteLabel) {
                            if (closeAfterDelete) scope.launch { offsetX.animateTo(0f) }
                            onDelete()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = deleteLabel, tint = Color.White)
                }
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .draggable(
                    enabled = enabled,
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-revealPx, 0f)) }
                    },
                    // A quick flick decides the direction; a slow drag settles on
                    // whichever side of halfway it was released. Either way the
                    // row ends up fully open (button showing) or fully closed.
                    onDragStopped = { velocity ->
                        val open = when {
                            velocity < -FLING_VELOCITY -> true
                            velocity > FLING_VELOCITY -> false
                            else -> offsetX.value < -revealPx / 2
                        }
                        scope.launch { offsetX.animateTo(if (open) -revealPx else 0f) }
                    },
                ),
        ) {
            content(isRevealed) { scope.launch { offsetX.animateTo(0f) } }
        }
    }
}

private const val FLING_VELOCITY = 400f

/** The rounded-card variant Home uses. */
val CardSwipeButtonShape = RoundedCornerShape(18.dp)
