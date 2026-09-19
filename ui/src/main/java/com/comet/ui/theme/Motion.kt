package com.comet.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Motion tokens (Part 3.1). Springs for all spatial motion; tweens only for progress,
 * shimmer, and infinite loops. 60fps budget on SD845-class hardware: no animated blur,
 * no per-frame allocations in draw phases.
 */
object Motion {
    val DefaultSpring = spring<Float>(dampingRatio = 0.8f, stiffness = 300f)
    val BounceSpring = spring<Float>(dampingRatio = 0.55f, stiffness = 400f)

    const val MICRO_MS = 150
    const val STAGGER_MS = 30
    const val TRAIL_MS = 1400 // comet trail cycle / indeterminate orbit

    /** Typed helpers — a SpringSpec<Float> can't be reused for Dp/Size animations. */
    fun <T> defaultSpring(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 300f)
    fun <T> bounceSpring(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = 400f)

    fun <T> micro(): androidx.compose.animation.core.TweenSpec<T> = tween(MICRO_MS)
}
