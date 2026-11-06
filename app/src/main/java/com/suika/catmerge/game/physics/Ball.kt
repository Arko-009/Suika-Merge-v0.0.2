package com.suika.catmerge.game.physics

/**
 * Represents a circular meme cat ball with physical properties and visual state.
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

    // Previous position for Position-Based Dynamics (PBD)
    var prevX: Float = x
    var prevY: Float = y

    // Visual rotation
    var angle: Float = 0f // In radians
    var angularVelocity: Float = 0f

    // Radius calculation according to level progression
    val radius: Float = calculateRadius(level, baseRadius)

    // Mass proportional to area (r^2)
    val mass: Float = (radius * radius * 0.005f).coerceAtLeast(1.0f)
    val invMass: Float = 1.0f / mass

    // Visual pop scale starts at 1.0f (exact 1:1 match with physics hitbox)
    var visualScale: Float = 1.0f

    // Overflow / Danger tracking
    var timeAboveDanger: Float = 0f

    // Score earned when this level is created through merging
    val scoreValue: Int = calculateScore(level)

    fun update(deltaTime: Float) {
        // Smoothly decay visual pop scale back to exact 1.0f
        if (visualScale > 1.0f) {
            visualScale = (visualScale - deltaTime * 1.8f).coerceAtLeast(1.0f)
        }
    }

    fun triggerPopEffect(startScale: Float = 1.15f) {
        visualScale = startScale
    }

    companion object {
        private val RADIUS_MULTIPLIERS = floatArrayOf(
            1.00f, // Level 0: Bonnet Kitten
            1.16f, // Level 1: Golden Kitten
            1.34f, // Level 2: Tabby cat
            1.54f, // Level 3: Polite Tuxedo cat
            1.76f, // Level 4: Smug White cat
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
            if (level <= 0) return 0
            return (1 shl (level - 1)) * 10
        }

        const val MAX_LEVEL = 13
    }
}
