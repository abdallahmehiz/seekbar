package live.mehiz.seekbar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlin.math.abs
import kotlin.math.min

/**
 * Represents a segment of the seekbar with a starting position and optional styling.
 *
 * @param start The starting position of the segment (0.0 to 1.0 for normalized progress, or actual value for ranged seekbar)
 * @param color Optional color for the segment. If null, uses default progress color
 * @param title Optional title/description for the segment
 */
data class SeekBarSegment(
    val start: Float,
    val title: String? = null,
    val color: Color? = null,
)

/**
 * Represents a marker point on the seekbar with customizable appearance and content.
 *
 * @param value The value/position of the marker (0.0 to 1.0 for normalized progress, or actual value for ranged seekbar)
 * @param color Color of the marker indicator
 * @param size Size of the marker
 * @param overlayContent Optional composable content displayed as overlay at marker position
 * @param content Optional composable content displayed at marker position
 */
data class SeekBarMarker(
    val value: Float,
    val color: Color = Color.Red,
    val size: Dp = 4.dp,
    val overlayContent: (@Composable (value: Float, marker: SeekBarMarker) -> Unit)? = null,
    val content: (@Composable (value: Float, marker: SeekBarMarker) -> Unit)? = null
)

/**
 * Configuration for haptic feedback during seekbar interactions.
 *
 * @param enabled Whether haptic feedback is enabled
 * @param onSeekStart Haptic feedback type when seeking starts
 * @param onSeekEnd Haptic feedback type when seeking ends
 * @param onSegmentCross Haptic feedback type when crossing segment boundaries
 * @param onMarkerCross Haptic feedback type when crossing marker positions
 */
data class HapticConfig(
    val enabled: Boolean = true,
    val onSeekStart: HapticFeedbackType = HapticFeedbackType.LongPress,
    val onSeekEnd: HapticFeedbackType = HapticFeedbackType.LongPress,
    val onSegmentCross: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    val onMarkerCross: HapticFeedbackType = HapticFeedbackType.TextHandleMove
)

/**
 * An advanced, customizable seekbar component with support for segments, markers, read-ahead progress,
 * haptic feedback, and thumb overlay content.
 *
 * Features:
 * - Segmented progress with gaps and custom colors
 * - Markers with custom content and overlays
 * - Read-ahead progress (useful for media buffering)
 * - Haptic feedback on interactions
 * - Custom thumb overlay content
 * - Smooth animations
 *
 * @param value Current value of the seekbar
 * @param onValueChange Callback invoked when the value changes
 * @param valueRange The range of values this seekbar can represent
 * @param modifier Modifier to be applied to the seekbar
 * @param enabled Whether the seekbar is enabled for interaction
 * @param segments List of segments to display on the seekbar
 * @param markers List of markers to display on the seekbar
 * @param readAheadValue Value representing read-ahead progress (e.g., buffered content in media)
 * @param hapticConfig Configuration for haptic feedback
 * @param colors Color scheme for the seekbar
 * @param dimensions Size configuration for the seekbar
 * @param onSeekStart Callback invoked when seeking starts with the starting position
 * @param onSeekEnd Callback invoked when seeking ends with the ending position
 * @param onHapticEvent Callback invoked when haptic events occur
 * @param thumbOverlay Custom composable overlay content positioned relative to the thumb
 */
@Composable
fun SeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    segments: List<SeekBarSegment> = emptyList(),
    markers: List<SeekBarMarker> = emptyList(),
    readAheadValue: Float = value,
    hapticConfig: HapticConfig = HapticConfig(),
    colors: SeekBarColors = SeekBarDefaults.colors(),
    dimensions: SeekBarDimensions = SeekBarDefaults.dimensions(),
    onSeekStart: (Float) -> Unit = {},
    onSeekEnd: (Float) -> Unit = {},
    onHapticEvent: (HapticFeedbackType) -> Unit = {},
    thumbOverlay: @Composable ((value: Float, isDragging: Boolean) -> Unit)? = null
) {
    require(value in valueRange) { "Value must be within the specified range" }
    val hapticFeedback = LocalHapticFeedback.current
    var isDragging by remember { mutableStateOf(false) }
    var lastCrossedSegment by remember { mutableStateOf(-1) }
    var lastCrossedMarker by remember { mutableStateOf(-1) }

    val range = valueRange.endInclusive - valueRange.start
    val progress = if (range > 0f) (value - valueRange.start) / range else 0f
    val readAheadProgress = if (range > 0f) (readAheadValue - valueRange.start) / range else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = if (isDragging) 0 else 150),
        label = "progress"
    )

    val progressSegments = segments.sortedBy { it.start }.map { segment ->
        val startProgress = if (range > 0f) (segment.start - valueRange.start) / range else 0f
        SeekBarSegment(
            start = startProgress,
            color = segment.color,
            title = segment.title
        )
    }

    val progressMarkers = markers.map { marker ->
        SeekBarMarker(
            value = if (range > 0f) (marker.value - valueRange.start) / range else 0f,
            color = marker.color,
            size = marker.size,
            overlayContent = marker.overlayContent,
            content = marker.content?.let { originalContent ->
                { _, _ -> originalContent(marker.value, marker) }
            }
        )
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensions.trackHeight + dimensions.thumbSize)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            onSeekStart(value)

                            val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                            val newValue = valueRange.start + (newProgress * range)
                            onValueChange(
                                newValue.coerceIn(
                                    valueRange.start,
                                    valueRange.endInclusive
                                )
                            )

                            if (hapticConfig.enabled) {
                                hapticFeedback.performHapticFeedback(
                                    hapticConfig.onSeekStart
                                )
                                onHapticEvent(hapticConfig.onSeekStart)
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekEnd(value)
                            lastCrossedSegment = -1
                            lastCrossedMarker = -1

                            if (hapticConfig.enabled) {
                                hapticFeedback.performHapticFeedback(
                                    hapticConfig.onSeekEnd
                                )
                                onHapticEvent(hapticConfig.onSeekEnd)
                            }
                        },
                        onDrag = { change, _ ->
                            val currentX = change.position.x
                            val newProgress = (currentX / size.width).coerceIn(0f, 1f)
                            val newValue = valueRange.start + (newProgress * range)
                            onValueChange(
                                newValue.coerceIn(valueRange.start, valueRange.endInclusive)
                            )

                            if (hapticConfig.enabled && progressSegments.isNotEmpty()) {
                                val currentSegment = progressSegments.indexOfLast {
                                    it.start <= newProgress
                                }
                                if (currentSegment != lastCrossedSegment && currentSegment >= 0) {
                                    lastCrossedSegment = currentSegment
                                    hapticFeedback.performHapticFeedback(
                                        hapticConfig.onSegmentCross
                                    )
                                    onHapticEvent(hapticConfig.onSegmentCross)
                                }
                            }

                            // Check marker crossings
                            if (hapticConfig.enabled && progressMarkers.isNotEmpty()) {
                                val nearestMarker = progressMarkers.indexOfFirst {
                                    abs(newProgress - it.value) < 0.02f
                                }
                                if (nearestMarker != lastCrossedMarker && nearestMarker >= 0) {
                                    lastCrossedMarker = nearestMarker
                                    hapticFeedback.performHapticFeedback(
                                        hapticConfig.onMarkerCross
                                    )
                                    onHapticEvent(hapticConfig.onMarkerCross)
                                }
                            }
                        }
                    )
                }
        ) {
            drawSeekBar(
                progress = animatedProgress,
                readAheadProgress = readAheadProgress,
                segments = progressSegments,
                markers = progressMarkers,
                colors = colors,
                dimensions = dimensions,
                isDragging = isDragging,
            )
        }

        progressMarkers.forEachIndexed { index, marker ->
            marker.overlayContent?.let { overlayContent ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.trackHeight + dimensions.thumbSize)
                ) {
                    val markerX = marker.value * maxWidth
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .offset(x = markerX - (marker.size / 2), y = 0.dp)
                    ) {
                        overlayContent(markers[index].value, markers[index])
                    }
                }
            }

            marker.content?.let { content ->
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(dimensions.trackHeight + dimensions.thumbSize)
                ) {
                    val markerX = marker.value * maxWidth
                    Box(
                        modifier = Modifier
                            .wrapContentSize()
                            .offset(
                                x = markerX - (marker.size / 2),
                                y = (dimensions.trackHeight + dimensions.thumbSize) / 2 - marker.size / 2
                            )
                    ) {
                        content(marker.value, marker)
                    }
                }
            }
        }

        // Draw thumb overlay if provided
        thumbOverlay?.let { overlay ->
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(dimensions.trackHeight + dimensions.thumbSize)
            ) {
                val thumbX = animatedProgress * maxWidth
                Box(
                    modifier = Modifier
                        .wrapContentSize()
                        .offset(
                            x = thumbX - (dimensions.thumbSize / 2),
                            y = (dimensions.trackHeight + dimensions.thumbSize) / 2 - dimensions.thumbSize / 2
                        )
                ) {
                    overlay(value, isDragging)
                }
            }
        }
    }
}

private fun DrawScope.drawSeekBar(
    progress: Float,
    readAheadProgress: Float,
    segments: List<SeekBarSegment>,
    markers: List<SeekBarMarker>,
    colors: SeekBarColors,
    dimensions: SeekBarDimensions,
    isDragging: Boolean,
) {
    val trackY = size.height / 2f
    val trackWidth = size.width
    val trackHeight = dimensions.trackHeight.toPx()
    val thumbRadius = dimensions.thumbSize.toPx() / 2f
    val gapWidth = 8.dp.toPx()

    if (segments.isNotEmpty()) {
        val segmentsWithEnds = segments.mapIndexed { index, segment ->
            val endProgress = if (index < segments.size - 1) {
                val nextSegmentStart = segments[index + 1].start
                val gapSizeInProgress = gapWidth / trackWidth
                (nextSegmentStart - gapSizeInProgress).coerceAtLeast(segment.start + 0.01f)
            } else {
                1f
            }
            segment to endProgress
        }

        segmentsWithEnds.forEach { (segment, endValue) ->
            val startX = segment.start * trackWidth
            val endX = endValue * trackWidth
            val segmentColor = segment.color?.copy(alpha = .3f) ?: colors.trackColor

            drawLine(
                color = segmentColor,
                start = Offset(startX, trackY),
                end = Offset(endX, trackY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }

        segmentsWithEnds.forEach { (segment, endValue) ->
            val startX = segment.start * trackWidth
            val endX = endValue * trackWidth
            val segmentColor = segment.color ?: colors.progressColor
            val clampedEnd = min(endX, progress * trackWidth)

            if (clampedEnd > startX) {
                drawLine(
                    color = segmentColor,
                    start = Offset(startX, trackY),
                    end = Offset(clampedEnd, trackY),
                    strokeWidth = trackHeight,
                    cap = StrokeCap.Round
                )
            }
        }

        if (readAheadProgress > progress) {
            segmentsWithEnds.forEach { (segment, endValue) ->
                val segmentStartX = segment.start * trackWidth
                val segmentEndX = endValue * trackWidth
                val readAheadStartX = maxOf(segmentStartX, progress * trackWidth)
                val readAheadEndX = minOf(segmentEndX, readAheadProgress * trackWidth)

                if (readAheadEndX > readAheadStartX && readAheadStartX < segmentEndX) {
                    drawLine(
                        color = colors.readAheadColor,
                        start = Offset(readAheadStartX, trackY),
                        end = Offset(readAheadEndX, trackY),
                        strokeWidth = trackHeight,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        for (i in 0 until segmentsWithEnds.size - 1) {
            val currentSegmentEnd = segmentsWithEnds[i].second * trackWidth
            val nextSegmentStart = segmentsWithEnds[i + 1].first.start * trackWidth
            val gapStart = currentSegmentEnd
            val gapEnd = nextSegmentStart

            if (gapEnd > gapStart) {
                drawLine(
                    color = Color.Transparent,
                    start = Offset(gapStart, trackY),
                    end = Offset(gapEnd, trackY),
                    strokeWidth = trackHeight + 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    } else {
        drawLine(
            color = colors.trackColor,
            start = Offset(0f, trackY),
            end = Offset(trackWidth, trackY),
            strokeWidth = trackHeight,
            cap = StrokeCap.Round
        )

        if (readAheadProgress > progress) {
            val readAheadStartX = progress * trackWidth
            val readAheadEndX = readAheadProgress * trackWidth

            drawLine(
                color = colors.readAheadColor,
                start = Offset(readAheadStartX, trackY),
                end = Offset(readAheadEndX, trackY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }

        val progressEndX = progress * trackWidth
        if (progressEndX > 0f) {
            drawLine(
                color = colors.progressColor,
                start = Offset(0f, trackY),
                end = Offset(progressEndX, trackY),
                strokeWidth = trackHeight,
                cap = StrokeCap.Round
            )
        }
    }

    markers.forEach { marker ->
        val markerX = marker.value * trackWidth

        drawLine(
            color = marker.color,
            start = Offset(markerX, trackY - trackHeight / 2f),
            end = Offset(markerX, trackY + trackHeight / 2f),
            strokeWidth = marker.size.toPx(),
            cap = StrokeCap.Round
        )
    }

    val thumbX = progress * trackWidth
    val thumbColor = if (isDragging) colors.thumbColorPressed else colors.thumbColor
    val currentThumbRadius = if (isDragging) thumbRadius * 1.2f else thumbRadius

    drawCircle(
        color = colors.thumbShadowColor,
        radius = currentThumbRadius + 2.dp.toPx(),
        center = Offset(thumbX, trackY)
    )

    drawCircle(
        color = thumbColor,
        radius = currentThumbRadius,
        center = Offset(thumbX, trackY)
    )
}

/**
 * Color scheme for the seekbar components.
 *
 * @param trackColor Color of the background track
 * @param progressColor Color of the progress indicator
 * @param readAheadColor Color of the read-ahead progress indicator
 * @param thumbColor Color of the thumb in normal state
 * @param thumbColorPressed Color of the thumb when pressed/dragging
 * @param thumbShadowColor Color of the thumb shadow
 */
@Immutable
data class SeekBarColors(
    val trackColor: Color,
    val progressColor: Color,
    val readAheadColor: Color,
    val thumbColor: Color,
    val thumbColorPressed: Color,
    val thumbShadowColor: Color
)

/**
 * Size configuration for the seekbar components.
 *
 * @param trackHeight Height of the track
 * @param thumbSize Size of the thumb
 */
@Immutable
data class SeekBarDimensions(
    val trackHeight: Dp,
    val thumbSize: Dp
)

/**
 * Default values and styling for the seekbar.
 */
object SeekBarDefaults {
    /**
     * Creates a [SeekBarColors] with default Material 3 colors.
     */
    @Composable
    fun colors(
        trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        progressColor: Color = MaterialTheme.colorScheme.primary,
        readAheadColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
        thumbColor: Color = MaterialTheme.colorScheme.primary,
        thumbColorPressed: Color = MaterialTheme.colorScheme.primary,
        thumbShadowColor: Color = Color.Black.copy(alpha = 0.2f)
    ): SeekBarColors = SeekBarColors(
        trackColor = trackColor,
        progressColor = progressColor,
        readAheadColor = readAheadColor,
        thumbColor = thumbColor,
        thumbColorPressed = thumbColorPressed,
        thumbShadowColor = thumbShadowColor
    )

    /**
     * Creates a [SeekBarDimensions] with default sizes.
     */
    fun dimensions(
        trackHeight: Dp = 6.dp,
        thumbSize: Dp = 20.dp
    ): SeekBarDimensions = SeekBarDimensions(
        trackHeight = trackHeight,
        thumbSize = thumbSize
    )
}