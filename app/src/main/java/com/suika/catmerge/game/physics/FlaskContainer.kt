package com.suika.catmerge.game.physics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Exact RGB Rainbow Flask Container as requested:
 * - Clean vertical white neck at the top
 * - Large RGB Rainbow Glowing circular chamber at the bottom
 * - Solid physics collision boundaries with zero tunneling
 */
class FlaskContainer {

    var centerX: Float = 0f
        private set
    var circleCenterY: Float = 0f
        private set
    var circleRadius: Float = 0f
        private set
    var neckHalfWidth: Float = 0f
        private set
    var neckTopY: Float = 0f
        private set
    var neckBottomY: Float = 0f
        private set
    var dangerLineY: Float = 0f
        private set

    // Aliases for backwards compatibility
    val bulbCenterY: Float get() = circleCenterY
    val bulbRadius: Float get() = circleRadius
    val shoulderY: Float get() = neckBottomY

    // Intersection geometry
    private var openingHalfAngleDeg: Float = 0f
    private var arcStartAngle: Float = 0f
    private var arcSweepAngle: Float = 0f

    // Rendering Paths & Rects
    private val circleBounds = RectF()
    private val leftNeckPath = Path()
    private val rightNeckPath = Path()
    private val arcPath = Path()
    private val dangerLinePath = Path()

    // Paints
    private val neckGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = Color.parseColor("#35FFFFFF")
        strokeCap = Paint.Cap.ROUND
    }

    private val neckPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.WHITE
        strokeCap = Paint.Cap.ROUND
    }

    private val rainbowGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
    }

    private val rainbowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
    }

    private val dangerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.parseColor("#FF5252")
        pathEffect = DashPathEffect(floatArrayOf(14f, 12f), 0f)
    }

    private val dangerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 12f
        color = Color.parseColor("#44FF5252")
    }

    // RGB Rainbow Shaders
    private var rainbowShader: SweepGradient? = null
    private var rainbowGlowShader: SweepGradient? = null
    private val shaderMatrix = Matrix()
    private var rainbowRotation = 0f
    private var dangerPulseTime = 0f

    private val rainbowColors = intArrayOf(
        Color.parseColor("#FF0055"), // Neon Pink/Red
        Color.parseColor("#FF7700"), // Orange
        Color.parseColor("#FFDD00"), // Yellow
        Color.parseColor("#00FF66"), // Neon Green
        Color.parseColor("#00E5FF"), // Cyan
        Color.parseColor("#3366FF"), // Royal Blue
        Color.parseColor("#CC00FF"), // Purple/Magenta
        Color.parseColor("#FF0055")  // Loop back to Neon Red
    )

    fun setup(viewWidth: Float, viewHeight: Float) {
        centerX = viewWidth / 2f

        // Bottom circular chamber occupies ~88% of screen width
        circleRadius = (viewWidth * 0.44f).coerceAtMost(viewHeight * 0.35f)

        // Neck dimensions: vertical white lines with a natural, proportional neck length
        neckHalfWidth = circleRadius * 0.22f
        val neckLength = circleRadius * 0.65f // Proportional neck (no longer overstretched)

        val dy = sqrt(circleRadius * circleRadius - neckHalfWidth * neckHalfWidth)
        val jarTotalHeight = neckLength + dy + circleRadius

        // Center the jar vertically between top HUD and bottom screen margin
        val topHudSpace = 220f.coerceAtMost(viewHeight * 0.16f)
        val bottomMargin = 70f
        val availableHeight = viewHeight - topHudSpace - bottomMargin

        val verticalOffset = if (availableHeight > jarTotalHeight) {
            topHudSpace + (availableHeight - jarTotalHeight) * 0.45f
        } else {
            topHudSpace
        }

        neckTopY = verticalOffset
        neckBottomY = neckTopY + neckLength
        circleCenterY = neckBottomY + dy

        // Danger line located near top third of neck
        dangerLineY = neckTopY + (neckBottomY - neckTopY) * 0.35f

        // Arc angles: standard 0=East, 90=South, 180=West, 270=North
        val halfAngleRad = asin(neckHalfWidth / circleRadius)
        openingHalfAngleDeg = Math.toDegrees(halfAngleRad.toDouble()).toFloat()

        // Arc starts at right intersection (270 + halfAngle) and sweeps clockwise
        arcStartAngle = -90f + openingHalfAngleDeg
        arcSweepAngle = 360f - 2f * openingHalfAngleDeg

        rebuildPaths()
    }

    private fun rebuildPaths() {
        circleBounds.set(
            centerX - circleRadius,
            circleCenterY - circleRadius,
            centerX + circleRadius,
            circleCenterY + circleRadius
        )

        // Neck lines
        leftNeckPath.reset()
        leftNeckPath.moveTo(centerX - neckHalfWidth, neckTopY)
        leftNeckPath.lineTo(centerX - neckHalfWidth, neckBottomY)

        rightNeckPath.reset()
        rightNeckPath.moveTo(centerX + neckHalfWidth, neckTopY)
        rightNeckPath.lineTo(centerX + neckHalfWidth, neckBottomY)

        // Arc path
        arcPath.reset()
        arcPath.arcTo(circleBounds, arcStartAngle, arcSweepAngle, false)

        // Danger line across neck
        dangerLinePath.reset()
        dangerLinePath.moveTo(centerX - neckHalfWidth + 6f, dangerLineY)
        dangerLinePath.lineTo(centerX + neckHalfWidth - 6f, dangerLineY)

        // Setup RGB sweep gradients
        rainbowShader = SweepGradient(centerX, circleCenterY, rainbowColors, null)
        rainbowGlowShader = SweepGradient(centerX, circleCenterY, rainbowColors, null)

        rainbowPaint.shader = rainbowShader
        rainbowGlowPaint.shader = rainbowGlowShader
        rainbowGlowPaint.alpha = 80
    }

    fun update(deltaTime: Float) {
        // Slowly rotate RGB rainbow around the circle for animated neon effect
        rainbowRotation = (rainbowRotation + deltaTime * 30f) % 360f
        shaderMatrix.setRotate(rainbowRotation, centerX, circleCenterY)
        rainbowShader?.setLocalMatrix(shaderMatrix)
        rainbowGlowShader?.setLocalMatrix(shaderMatrix)

        dangerPulseTime += deltaTime * 5f
    }

    /**
     * Resolves collisions for balls against the neck and the RGB circle chamber.
     */
    fun resolveBallCollision(ball: Ball, restitution: Float = 0.25f, friction: Float = 0.03f): Boolean {
        var collided = false

        val leftX = centerX - neckHalfWidth
        val rightX = centerX + neckHalfWidth

        // 1. Inside Neck vertical walls (only above the chamber opening)
        if (ball.y < neckBottomY) {
            // Left neck wall
            if (ball.x - ball.radius < leftX) {
                val pen = leftX - (ball.x - ball.radius)
                ball.x += pen
                if (ball.vx < 0f) {
                    ball.vx = -ball.vx * restitution
                    ball.angularVelocity = -ball.vy * 0.001f
                }
                collided = true
            }

            // Right neck wall
            if (ball.x + ball.radius > rightX) {
                val pen = (ball.x + ball.radius) - rightX
                ball.x -= pen
                if (ball.vx > 0f) {
                    ball.vx = -ball.vx * restitution
                    ball.angularVelocity = ball.vy * 0.001f
                }
                collided = true
            }
        }

        // 2. Corner junctions where neck meets the circle (smooth sliding entrance)
        val cornerY = neckBottomY

        // Left corner point collision
        val cldx = ball.x - leftX
        val cldy = ball.y - cornerY
        val cldistSq = cldx * cldx + cldy * cldy
        if (cldistSq < ball.radius * ball.radius && ball.x < leftX + ball.radius * 0.5f) {
            val dist = sqrt(cldistSq)
            if (dist > 0.0001f) {
                val nx = cldx / dist
                val ny = cldy / dist
                val pen = ball.radius - dist
                ball.x += nx * pen
                ball.y += ny * pen
                val vn = ball.vx * nx + ball.vy * ny
                if (vn < 0f) {
                    ball.vx -= (1f + restitution) * vn * nx
                    ball.vy -= (1f + restitution) * vn * ny
                }
                collided = true
            }
        }

        // Right corner point collision
        val crdx = ball.x - rightX
        val crdy = ball.y - cornerY
        val crdistSq = crdx * crdx + crdy * crdy
        if (crdistSq < ball.radius * ball.radius && ball.x > rightX - ball.radius * 0.5f) {
            val dist = sqrt(crdistSq)
            if (dist > 0.0001f) {
                val nx = crdx / dist
                val ny = crdy / dist
                val pen = ball.radius - dist
                ball.x += nx * pen
                ball.y += ny * pen
                val vn = ball.vx * nx + ball.vy * ny
                if (vn < 0f) {
                    ball.vx -= (1f + restitution) * vn * nx
                    ball.vy -= (1f + restitution) * vn * ny
                }
                collided = true
            }
        }

        // 3. Inside the Circular Chamber (ignoring the open neck entry corridor)
        val inNeckOpening = (ball.y < circleCenterY) && (kotlin.math.abs(ball.x - centerX) < (neckHalfWidth - 2f))

        if (!inNeckOpening) {
            val dx = ball.x - centerX
            val dy = ball.y - circleCenterY
            val distSq = dx * dx + dy * dy
            val maxAllowedDist = circleRadius - ball.radius

            if (distSq > maxAllowedDist * maxAllowedDist && maxAllowedDist > 0f) {
                val dist = sqrt(distSq)
                if (dist > 0.0001f) {
                    val nx = -dx / dist
                    val ny = -dy / dist
                    val penetration = dist - maxAllowedDist

                    // Push ball inwards
                    ball.x += nx * penetration
                    ball.y += ny * penetration

                    // Reflect velocity
                    val vn = ball.vx * nx + ball.vy * ny
                    if (vn < 0f) {
                        val vtx = ball.vx - vn * nx
                        val vty = ball.vy - vn * ny
                        ball.vx = -vn * restitution * nx + vtx * (1f - friction)
                        ball.vy = -vn * restitution * ny + vty * (1f - friction)
                        ball.angularVelocity += (vtx * ny - vty * nx) * 0.001f
                    }
                    collided = true
                }
            }
        }

        return collided
    }

    /**
     * Renders the white neck and glowing RGB Rainbow circular chamber.
     */
    fun draw(canvas: Canvas, isInDanger: Boolean) {
        // 1. White Vertical Neck (outer glow + inner white stroke)
        canvas.drawPath(leftNeckPath, neckGlowPaint)
        canvas.drawPath(rightNeckPath, neckGlowPaint)
        canvas.drawPath(leftNeckPath, neckPaint)
        canvas.drawPath(rightNeckPath, neckPaint)

        // 2. RGB Rainbow Circular Chamber (outer bloom + inner neon stroke)
        canvas.drawPath(arcPath, rainbowGlowPaint)
        canvas.drawPath(arcPath, rainbowPaint)

        // 3. Danger Line across neck
        if (isInDanger) {
            val pulseAlpha = (130 + 125 * sin(dangerPulseTime.toDouble())).toInt().coerceIn(40, 255)
            dangerLinePaint.alpha = pulseAlpha
            dangerGlowPaint.alpha = (pulseAlpha * 0.45f).toInt()
            canvas.drawPath(dangerLinePath, dangerGlowPaint)
        } else {
            dangerLinePaint.alpha = 80
        }
        canvas.drawPath(dangerLinePath, dangerLinePaint)
    }
}
