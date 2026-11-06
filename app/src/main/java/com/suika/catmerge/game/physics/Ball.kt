package com.suika.catmerge.game.physics

import kotlin.math.PI
import kotlin.math.max

/**
 * Represents a circular meme cat ball with physical properties and visual animation state.
 */
class Ball(
    val id: Long,
    var x: Float,
    var y: Float,
    val level: Int,
    baseRadius: Float
) {
    // Velocity
    var vx: Float = 0f
    var vy: Float = 0f

    // Visual rotation
    var angle: Float = 0f // In radians
    var angularVelocity: Float = 0f

    // Radius calculation according to level progression
    val radius: Float = calculateRadius(level, baseRadius)

    // Mass proportional to area (r^2)
    val mass: Float = (radius * radius * 0.005f).coerceAtLeast(1.0f)
    val invMass: Float = 1.0f / mass

    // Spawn / Merge bounce animation scale
    var visualScale: Float = 1.35f
    private var scaleVelocity: Float = -2.5f

    // Merge lifecycle flags
    var isMerging: Boolean = false
    var isMarkedForDeletion: Boolean = false

    // Overflow / Danger tracking
    var timeAboveDanger: Float = 0f

    // Score earned when this level is created through merging
    val scoreValue: Int = calculateScore(level)

    fun update(deltaTime: Float) {
        // Update position
        x += vx * deltaTime
        y += vy * deltaTime

        // Update rotation
        angle += angularVelocity * deltaTime
        // Angular damping
        angularVelocity *= 0.985f

        // Spring-damper for visual scale (elastic pop)
        if (visualScale != 1.0f) {
            val displacement = visualScale - 1.0f
            val springForce = -displacement * 65f
            val dampingForce = -scaleVelocity * 10f
            scaleVelocity += (springForce + dampingForce) * deltaTime
            visualScale += scaleVelocity * deltaTime
            if (kotlin.math.abs(displacement) < 0.01f && kotlin.math.abs(scaleVelocity) < 0.05f) {
                visualScale = 1.0f
                scaleVelocity = 0f
            }
        }
    }

    fun triggerPopEffect(startScale: Float = 1.35f) {
        visualScale = startScale
        scaleVelocity = -2.0f
    }

    companion object {
        private val RADIUS_MULTIPLIERS = floatArrayOf(
            1.00f, // Level 0: Kitten with bonnet
            1.16f, // Level 1: Golden kitten
            1.34f, // Level 2: Tabby cat
            1.54f, // Level 3: Polite Tuxedo cat
            1.76f, // Level 4: White smug cat
            2.00f, // Level 5: Grey cat
            2.26f, // Level 6: Smiling black cat
            2.55f, // Level 7: Ginger gigachad
            2.86f, // Level 8: Tuxedo gigachad
            3.20f, // Level 9: White gigachad
            3.56f, // Level 10: Grey gigachad
            3.95f, // Level 11: Beard cat
            4.38f, // Level 12: Brown gigachad
            4.85f  // Level 13: Ultra buff bodybuilder cat
        )

        fun calculateRadius(level: Int, baseRadius: Float): Float {
            val idx = level.coerceIn(0, RADIUS_MULTIPLIERS.size - 1)
            return baseRadius * RADIUS_MULTIPLIERS[idx]
        }

        fun calculateScore(level: Int): Int {
            // Level 1 = 10 pts, Level 2 = 20, Level 3 = 40, ..., Level 13 = 40960
            if (level <= 0) return 0
            return (1 shl (level - 1)) * 10
        }

        val MAX_LEVEL = 13
    }
}

