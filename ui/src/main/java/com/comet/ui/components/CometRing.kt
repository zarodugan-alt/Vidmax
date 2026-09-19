package com.comet.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.comet.ui.theme.AccentCyan
import com.comet.ui.theme.AccentCyanDim
import com.comet.ui.theme.AccentViolet
import com.comet.ui.theme.AccentVioletDim
import com.comet.ui.theme.BounceSpring
import com.comet.ui.theme.Danger
import com.comet.ui.theme.Motion
import com.comet.ui.theme.Success
import com.comet.ui.theme.Warning
import kotlin.math.cos
import kotlin.math.sin

enum class RingMode { VIDEO, AUDIO }
enum class RingState { ACTIVE, PAUSED, FAILED, COMPLETE }

/**
 * Comet Progress Ring — the app's visual identity (Part 3.2).
 *
 * - Track: 3dp ring, AccentCyanDim (video) / AccentVioletDim (audio).
 * - Comet head: 6dp glowing dot at the progress angle (layered radial glow, no allocations).
 * - Trail: arc sweeping behind the head, alpha 0.8 -> 0.0, stroke tapering 3dp -> 0.5dp.
 * - Determinate: head at progress x 360deg; progress changes animate via DefaultSpring —
 *   bars never jump.
 * - Indeterminate (analyzing): comet orbits continuously, 1400ms/revolution, trail 120deg.
 * - Paused: head stops, trail collapses to 20deg, alpha 0.5, Warning tint.
 * - Failed: ring flashes Danger red, comet scatters (6 particles, 400ms), settles dim red.
 * - Complete: ring fills Success green, checkmark pops center (scale 0 -> 1.15 -> 1, BounceSpring).
 * - Center content slot: percentage (Telemetry) or status icon.
 */
@Composable
fun CometRing(
    progress: Float?,
    modifier: Modifier = Modifier,
    mode: RingMode = RingMode.VIDEO,
    state: RingState = RingState.ACTIVE,
    ringSize: Dp = 56.dp,
    strokeWidth: Dp = 3.dp,
    dim: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val dimFactor = if (dim) 0.55f else 1f

    val headColor = when (state) {
        RingState.PAUSED -> Warning
        RingState.FAILED -> Danger
        RingState.COMPLETE -> Success
        RingState.ACTIVE -> if (mode == RingMode.AUDIO) AccentViolet else AccentCyan
    }
    val trackColor = when (state) {
        RingState.PAUSED -> Warning.copy(alpha = 0.2f * dimFactor)
        RingState.FAILED -> Danger.copy(alpha = 0.2f * dimFactor)
        RingState.COMPLETE -> Success.copy(alpha = 0.22f * dimFactor)
        RingState.ACTIVE ->
            if (mode == RingMode.AUDIO) {
                AccentVioletDim.copy(alpha = if (dim) 0.6f else 1f)
            } else {
                AccentCyanDim.copy(alpha = if (dim) 0.6f else 1f)
            }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = (progress ?: 0f).coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Motion.DefaultSpring.dampingRatio,
            stiffness = Motion.DefaultSpring.stiffness,
        ),
        label = "cometRingProgress",
    )

    val indeterminate = progress == null && state == RingState.ACTIVE
    val orbit = rememberInfiniteTransition(label = "cometOrbit")
    val orbitAngle by orbit.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(Motion.TRAIL_MS, easing = LinearEasing)),
        label = "orbitAngle",
    )

    // Failure burst: 0 -> 1 over 400ms.
    var failureT by remember { mutableFloatStateOf(1f) }
    LaunchedEffect(state) {
        if (state == RingState.FAILED) {
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(400),
            ) { v, _ -> failureT = v }
        }
    }

    // Checkmark pop: BounceSpring overshoots past 1 (about 1.15) and settles at 1.
    val checkScale by animateFloatAsState(
        targetValue = if (state == RingState.COMPLETE) 1f else 0f,
        animationSpec = spring(
            dampingRatio = BounceSpring.dampingRatio,
            stiffness = BounceSpring.stiffness,
        ),
        label = "checkPop",
    )

    val checkPath = remember { Path() }

    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f + 1.dp.toPx()
            val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
            val topLeft = Offset(inset, inset)
            val arcCenter = Offset(inset + arcSize.width / 2f, inset + arcSize.height / 2f)
            val radius = arcSize.minDimension / 2f

            // ---- 1. Track ----
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(strokePx, cap = StrokeCap.Round),
            )

            // ---- 2. Resolve comet geometry ----
            val headAngle: Float
            val trailSweep: Float
            val trailAlpha: Float
            when {
                indeterminate -> {
                    headAngle = orbitAngle
                    trailSweep = 120f
                    trailAlpha = 0.8f * dimFactor
                }

                state == RingState.PAUSED -> {
                    headAngle = -90f + animatedProgress * 360f
                    trailSweep = 20f
                    trailAlpha = 0.5f * dimFactor
                }

                else -> {
                    headAngle = -90f + animatedProgress * 360f
                    trailSweep = 60f
                    trailAlpha = 0.8f * dimFactor
                }
            }

            val headRad = Math.toRadians(headAngle.toDouble())
            val headPos = Offset(
                arcCenter.x + (radius * cos(headRad)).toFloat(),
                arcCenter.y + (radius * sin(headRad)).toFloat(),
            )

            // ---- 3. Trail: tapered, fading arc behind the head ----
            if (trailSweep > 0.5f) {
                val segments = 16
                val minStroke = 0.5.dp.toPx()
                for (i in 0 until segments) {
                    val f1 = (i + 1).toFloat() / segments
                    val start = headAngle - trailSweep + ((i).toFloat() / segments) * trailSweep
                    val sweep = (trailSweep / segments) + 0.6f // slight overlap hides gaps
                    val alpha = (trailAlpha * f1 * f1).coerceIn(0f, 1f)
                    val width = minStroke + f1 * (strokePx - minStroke)
                    drawArc(
                        color = headColor.copy(alpha = alpha),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width, cap = StrokeCap.Butt),
                    )
                }
            }

            // ---- 4. Head: glowing dot (layered radial approximation, zero allocations) ----
            val headRadius = (strokeWidth * 2f).toPx() / 2f // 6dp dot for a 3dp ring
            if (state != RingState.COMPLETE) {
                drawCircle(headColor.copy(alpha = 0.10f * dimFactor), headRadius * 2.4f, headPos)
                drawCircle(headColor.copy(alpha = 0.24f * dimFactor), headRadius * 1.6f, headPos)
                drawCircle(headColor.copy(alpha = 0.95f * dimFactor), headRadius, headPos)
            }

            // ---- 5. State dressing ----
            when (state) {
                RingState.FAILED -> {
                    // Flash bright red while scattering, then settle dim.
                    val flash = 0.45f + 0.55f * (1f - failureT)
                    drawArc(
                        color = Danger.copy(alpha = flash),
                        startAngle = -90f,
                        sweepAngle = if (progress != null) animatedProgress * 360f else 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokePx, cap = StrokeCap.Round),
                    )
                    if (failureT < 1f) {
                        val burstRadius = radius * (0.55f + failureT * 1.05f)
                        repeat(6) { i ->
                            val a = Math.toRadians((i * 60 + 15).toDouble())
                            val p = Offset(
                                arcCenter.x + (burstRadius * cos(a)).toFloat(),
                                arcCenter.y + (burstRadius * sin(a)).toFloat(),
                            )
                            drawCircle(
                                color = Danger.copy(alpha = (1f - failureT) * 0.9f),
                                radius = 2.dp.toPx() * (1f - 0.5f * failureT),
                                center = p,
                            )
                        }
                    }
                }

                RingState.COMPLETE -> {
                    drawArc(
                        color = Success,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(strokePx, cap = StrokeCap.Round),
                    )
                    if (checkScale > 0.01f) {
                        drawCheck(checkPath, checkScale, Success, arcCenter, size.minDimension)
                    }
                }

                else -> Unit
            }
        }
        content()
    }
}

private fun DrawScope.drawCheck(
    path: Path,
    scale: Float,
    color: Color,
    center: Offset,
    dimension: Float,
) {
    path.reset()
    path.moveTo(center.x - dimension * 0.18f, center.y + dimension * 0.02f)
    path.lineTo(center.x - dimension * 0.05f, center.y + dimension * 0.15f)
    path.lineTo(center.x + dimension * 0.20f, center.y - dimension * 0.12f)
    withTransform({ scale(scale, scale, pivot = center) }) {
        // Compensate stroke width so the pop doesn't thicken the check.
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = (2.5.dp.toPx()) / scale.coerceAtLeast(0.4f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
