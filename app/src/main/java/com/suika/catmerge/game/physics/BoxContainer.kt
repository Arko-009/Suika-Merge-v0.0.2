package com.suika.catmerge.game.physics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.SweepGradient
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Arcade Rectangular Box container with open top and glowing RGB Rainbow neon borders.
 */
class BoxContainer : GameContainer {

    override val containerType: ContainerType = ContainerType.BOX

    override var centerX: Float = 0f
        private set
    override var circleCenterY: Float = 0f
        private set
    override var circleRadius: Float = 0f
        private set
    override var neckHalfWidth: Float = 0f
        private set
    override var neckTopY: Float = 0f
        private set
    override var neckBottomY: Float = 0f
        private set
    override var dangerLineY: Float = 0f
        private set

    // Box bounds
    var leftX: Float = 0f
        private set
    var rightX: Float = 0f
        private set
    var topY: Float = 0f
        private set
    var bottomY: Float = 0f
        private set
    val cornerRadius: Float = 36f

    // Paths
    private val boxPath = Path()
    private val dangerLinePath = Path()

    // Paints
    private val rainbowGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 20f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val rainbowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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
        Color.parseColor("#00E5FF"), // Cyan
        Color.parseColor("#7B2CBF"), // Royal Purple
        Color.parseColor("#FF0055"), // Neon Red
        Color.parseColor("#FF7700"), // Orange
        Color.parseColor("#FFDD00"), // Yellow
        Color.parseColor("#00FF66"), // Neon Green
        Color.parseColor("#00E5FF")  // Loop back
    )

    override fun setup(viewWidth: Float, viewHeight: Float) {
        centerX = viewWidth / 2f

        val boxWidth = (viewWidth * 0.86f).coerceAtMost(viewHeight * 0.48f)
        val boxHeight = (viewHeight * 0.58f).coerceAtMost(boxWidth * 1.55f)

        bottomY = viewHeight - 90f
        topY = bottomY - boxHeight
        leftX = centerX - boxWidth / 2f
        rightX = centerX + boxWidth / 2f

        neckTopY = topY
        neckBottomY = topY + 45f
        neckHalfWidth = boxWidth * 0.42f // Wide opening across box top

        circleCenterY = topY + boxHeight * 0.55f
        circleRadius = boxWidth / 2f

        dangerLineY = topY + boxHeight * 0.16f

        rebuildPaths()
    }

    private fun rebuildPaths() {
        boxPath.reset()
        // Start at top-left lip
        boxPath.moveTo(leftX, topY)
        // Down left wall
        boxPath.lineTo(leftX, bottomY - cornerRadius)
        // Bottom-left rounded corner
        boxPath.arcTo(
            RectF(leftX, bottomY - 2f * cornerRadius, leftX + 2f * cornerRadius, bottomY),
            180f,
            -90f,
            false
        )
        // Bottom wall
        boxPath.lineTo(rightX - cornerRadius, bottomY)
        // Bottom-right rounded corner
        boxPath.arcTo(
            RectF(rightX - 2f * cornerRadius, bottomY - 2f * cornerRadius, rightX, bottomY),
            90f,
            -90f,
            false
        )
        // Up right wall
        boxPath.lineTo(rightX, topY)

        // Danger line across upper box
        dangerLinePath.reset()
        dangerLinePath.moveTo(leftX + 8f, dangerLineY)
        dangerLinePath.lineTo(rightX - 8f, dangerLineY)

        // Shaders
        rainbowShader = SweepGradient(centerX, circleCenterY, rainbowColors, null)
        rainbowGlowShader = SweepGradient(centerX, circleCenterY, rainbowColors, null)

        rainbowPaint.shader = rainbowShader
        rainbowGlowPaint.shader = rainbowGlowShader
        rainbowGlowPaint.alpha = 80
    }

    override fun update(deltaTime: Float) {
        rainbowRotation = (rainbowRotation + deltaTime * 28f) % 360f
        shaderMatrix.setRotate(rainbowRotation, centerX, circleCenterY)
        rainbowShader?.setLocalMatrix(shaderMatrix)
        rainbowGlowShader?.setLocalMatrix(shaderMatrix)

        dangerPulseTime += deltaTime * 5f
    }

    override fun resolveBallCollision(ball: Ball, restitution: Float, friction: Float): Boolean {
        var collided = false

        // Collision only applies once ball enters top opening
        if (ball.y + ball.radius < topY) return false

        // 1. Left Wall (straight vertical section)
        if (ball.y < bottomY - cornerRadius) {
            if (ball.x - ball.radius < leftX) {
                val pen = leftX - (ball.x - ball.radius)
                ball.x += pen
                if (ball.vx < 0f) {
                    ball.vx = -ball.vx * restitution
                    ball.angularVelocity = -ball.vy * 0.001f
                }
                collided = true
            }
        }

        // 2. Right Wall (straight vertical section)
        if (ball.y < bottomY - cornerRadius) {
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

        // 3. Bottom Wall (straight horizontal section between corners)
        val cornerLeft = leftX + cornerRadius
        val cornerRight = rightX - cornerRadius
        if (ball.x >= cornerLeft && ball.x <= cornerRight) {
            if (ball.y + ball.radius > bottomY) {
                val pen = (ball.y + ball.radius) - bottomY
                ball.y -= pen
                if (ball.vy > 0f) {
                    ball.vy = -ball.vy * restitution
                    ball.vx *= (1f - friction)
                }
                collided = true
            }
        }

        // 4. Bottom-Left Corner
        val blcx = leftX + cornerRadius
        val blcy = bottomY - cornerRadius
        if (ball.x < blcx && ball.y > blcy) {
            val dx = ball.x - blcx
            val dy = ball.y - blcy
            val dist = sqrt(dx * dx + dy * dy)
            val maxDist = cornerRadius - ball.radius
            if (dist > maxDist && maxDist > 0f && dist > 0.0001f) {
                val nx = -dx / dist
                val ny = -dy / dist
                val pen = dist - maxDist
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

        // 5. Bottom-Right Corner
        val brcx = rightX - cornerRadius
        val brcy = bottomY - cornerRadius
        if (ball.x > brcx && ball.y > brcy) {
            val dx = ball.x - brcx
            val dy = ball.y - brcy
            val dist = sqrt(dx * dx + dy * dy)
            val maxDist = cornerRadius - ball.radius
            if (dist > maxDist && maxDist > 0f && dist > 0.0001f) {
                val nx = -dx / dist
                val ny = -dy / dist
                val pen = dist - maxDist
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

        return collided
    }

    override fun draw(canvas: Canvas, isInDanger: Boolean) {
        // Draw U-shaped glowing RGB neon box
        canvas.drawPath(boxPath, rainbowGlowPaint)
        canvas.drawPath(boxPath, rainbowPaint)

        // Draw danger line
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
