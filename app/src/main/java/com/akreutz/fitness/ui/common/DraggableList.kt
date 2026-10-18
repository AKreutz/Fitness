package com.akreutz.fitness.ui.common

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * Renders [items] in order, each draggable by a leading handle to reorder within the list.
 * Dragging past a neighbor's midpoint swaps their positions live; [onMove] is called once the
 * drag ends, with the item's start and final index. [itemContent] renders the rest of each row
 * (after the drag handle), given the item's current index into [items].
 */
@Composable
fun <T> DraggableList(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    itemContent: @Composable (index: Int, item: T) -> Unit,
) {
    DraggableListWithHandle(
        items = items,
        onMove = onMove,
        modifier = modifier,
        rowSpacing = 0.dp,
        itemContent = { index, item, handle ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                handle(Modifier.padding(end = 4.dp))
                itemContent(index, item)
            }
        },
    )
}

/**
 * Like [DraggableList], but [itemContent] places the drag handle itself: it receives a `handle`
 * composable (taking the modifier to apply to the handle icon) to put wherever suits the row,
 * e.g. inside a card. Rows are separated by [rowSpacing].
 */
@Composable
fun <T> DraggableListWithHandle(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    rowSpacing: androidx.compose.ui.unit.Dp = 0.dp,
    itemContent: @Composable (
        index: Int,
        item: T,
        handle: @Composable (Modifier) -> Unit,
    ) -> Unit,
) {
    // The order shown while a drag is in progress, as a list of original `items` indices; kept in
    // sync with `items` otherwise, so reordering during a drag is instant instead of waiting for
    // the parent's state to round-trip.
    var displayOrder by remember(items) { mutableStateOf(items.indices.toList()) }
    // The original item index being dragged, and its current position within `displayOrder`.
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeightPx = remember { mutableStateMapOf<Int, Int>() }

    val spacingPx = with(LocalDensity.current) { rowSpacing.toPx() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(rowSpacing),
    ) {
        displayOrder.forEach { itemIndex ->
            val isDragged = itemIndex == draggedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowHeightPx[itemIndex] = it.height }
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragged) dragOffsetY else 0f
                    },
            ) {
                val handle: @Composable (Modifier) -> Unit = { handleModifier ->
                    Icon(
                    imageVector = Icons.Filled.DragHandle,
                    contentDescription = "Drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = handleModifier
                        .pointerInput(items) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedIndex = itemIndex
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    val dragged = draggedIndex
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                    if (dragged != null) {
                                        val finalPosition = displayOrder.indexOf(dragged)
                                        onMove(dragged, finalPosition)
                                    }
                                },
                                onDragCancel = {
                                    draggedIndex = null
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val dragged = draggedIndex ?: return@detectDragGestures
                                    dragOffsetY += dragAmount.y
                                    val rowHeight = rowHeightPx[dragged] ?: return@detectDragGestures
                                    val threshold = rowHeight / 2
                                    val position = displayOrder.indexOf(dragged)
                                    if (dragOffsetY > threshold && position < displayOrder.lastIndex) {
                                        displayOrder = displayOrder.toMutableList().apply {
                                            add(position + 1, removeAt(position))
                                        }
                                        dragOffsetY -= rowHeight + spacingPx
                                    } else if (dragOffsetY < -threshold && position > 0) {
                                        displayOrder = displayOrder.toMutableList().apply {
                                            add(position - 1, removeAt(position))
                                        }
                                        dragOffsetY += rowHeight + spacingPx
                                    }
                                },
                            )
                        },
                    )
                }
                itemContent(itemIndex, items[itemIndex], handle)
            }
        }
    }
}
