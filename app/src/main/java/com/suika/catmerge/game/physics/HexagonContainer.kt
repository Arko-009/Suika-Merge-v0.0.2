package com.suika.catmerge.game.physics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.SweepGradient
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Cyber Funnel / Hexagonal chamber with sloped sliding neon walls and wide top opening.
 */
class HexagonContainer : GameContainer {

    override val containerType: ContainerType = ContainerType.HEXAGON

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

    // Geometry points
    private var topLeftX = 0f
    private var topRightX = 0f
    private var midLeftX = 0f
    private var midRightX = 0f
    private var botLeftX = 0f
    private var botRightX = 0f
    private var topY = 0f
    private var midY = 0f
    private var botY = 0f

    // Paths
    private val chamberPath = Path()
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
        Color.parseColor("#00FF66"), // Neon Green
        Color.parseColor("#00E5FF"), // Cyan
        Color.parseColor("#7B2CBF"), // Royal Purple
        Color.parseColor("#FF0055"), // Neon Red
        Color.parseColor("#FFDD00"), // Yellow
        Color.parseColor("#00FF66")  // Loop back
    )

    override fun setup(viewWidth: Float, viewHeight: Float) {
        centerX = viewWidth / 2f

        val chamberWidth = (viewWidth * 0.88f).coerceAtMost(viewHeight * 0.48f)
        val chamberHeight = (viewHeight * 0.58f).coerceAtMost(chamberWidth * 1.55f)

        botY = viewHeight - 90f
        topY = botY - chamberHeight
        midY = topY + chamberHeight * 0.40f

        topLeftX = centerX - chamberWidth * 0.48f
        topRightX = centerX + chamberWidth * 0.48f

        midLeftX = centerX - chamberWidth * 0.48f
        midRightX = centerX + chamberWidth * 0.48f

        botLeftX = centerX - chamberWidth * 0.28f
        botRightX = centerX + chamberWidth * 0.28f

        neckTopY = topY
        neckBottomY = topY + 45f
        neckHalfWidth = chamberWidth * 0.42f

        circleCenterY = topY + chamberHeight * 0.55f
        circleRadius = chamberWidth / 2f

        dangerLineY = topY + chamberHeight * 0.16f

        rebuildPaths()
    }

    private fun rebuildPaths() {
        chamberPath.reset()
        // Top-left opening
        chamberPath.moveTo(topLeftX, topY)
        // Straight upper left wall
        chamberPath.lineTo(midLeftX, midY)
        // Sloped lower left wall
        chamberPath.lineTo(botLeftX, botY)
        // Bottom flat floor
        chamberPath.lineTo(botRightX, botY)
        // Sloped lower right wall
        chamberPath.lineTo(midRightX, midY)
        // Straight upper right wall
        chamberPath.lineTo(topRightX, topY)

        // Danger line
        dangerLinePath.reset()
        dangerLinePath.moveTo(topLeftX + 8f, dangerLineY)
        dangerLinePath.lineTo(topRightX - 8f, dangerLineY)

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
        if (ball.y + ball.radius < topY) return false

        var collided = false

        // 1. Upper Left Vertical Wall (topY to midY)
        if (ball.y >= topY && ball.y <= midY) {
            if (ball.x - ball.radius < midLeftX) {
                val pen = midLeftX - (ball.x - ball.radius)
                ball.x += pen
                if (ball.vx < 0f) ball.vx = -ball.vx * restitution
                ball.angularVelocity = -ball.vy * 0.001f
                collided = true
            }
        }

        // 2. Upper Right Vertical Wall (topY to midY)
        if (ball.y >= topY && ball.y <= midY) {
            if (ball.x + ball.radius > midRightX) {
                val pen = (ball.x + ball.radius) - midRightX
                ball.x -= pen
                if (ball.vx > 0f) ball.vx = -ball.vx * restitution
                ball.angularVelocity = ball.vy * 0.001f
                collided = true
            }
        }

        // 3. Lower Left Sloped Wall (midLeft -> botLeft)
        if (collideSegment(ball, midLeftX, midY, botLeftX, botY, restitution, friction)) {
            collided = true
        }

        // 4. Lower Right Sloped Wall (botRight -> midRight)
        if (collideSegment(ball, botRightX, botY, midRightX, midY, restitution, friction)) {
            collided = true
        }

        // 5. Bottom Floor (botLeft -> botRight)
        if (ball.x >= botLeftX - ball.radius * 0.2f && ball.x <= botRightX + ball.radius * 0.2f) {
            if (ball.y + ball.radius > botY) {
                val pen = (ball.y + ball.radius) - botY
                ball.y -= pen
                if (ball.vy > 0f) ball.vy = -ball.vy * restitution
                ball.vx *= (1f - friction)
                collided = true
            }
        }

        return collided
    }

    private fun collideSegment(
        ball: Ball,
        ax: Float, ay: Float,
        bx: Float, by: Float,
        restitution: Float,
        friction: Float
    ): Boolean {
        val abx = bx - ax
        val aby = by - ay
        val abLenSq = abx * abx + aby * aby
        if (abLenSq < 1e-5f) return false

        val t = (((ball.x - ax) * abx + (ball.y - ay) * aby) / abLenSq).coerceIn(0f, 1f)
        val cx = ax + t * abx
        val cy = ay + t * aby

        val dx = ball.x - cx
        val dy = ball.y - cy
        val distSq = dx * dx + dy * dy

        if (distSq < ball.radius * ball.radius) {
            val dist = sqrt(distSq)
            if (dist > 0.0001f) {
                val nx = dx / dist
                val ny = dy / dist
                val pen = ball.radius - dist

                ball.x += nx * pen
                ball.y += ny * pen

                val vn = ball.vx * nx + ball.vy * ny
                if (vn < 0f) {
                    val vtx = ball.vx - vn * nx
                    val vty = ball.vy - vn * ny
                    ball.vx = -vn * restitution * nx + vtx * (1f - friction)
                    ball.vy = -vn * restitution * ny + vty * (1f - friction)
                    ball.angularVelocity += (vtx * ny - vty * nx) * 0.001f
                }
                return true
            }
        }
        return false
    }

    override fun draw(canvas: Canvas, isInDanger: Boolean) {
        // Draw cyber hexagonal chamber
        canvas.drawPath(chamberPath, rainbowGlowPaint)
        canvas.drawPath(chamberPath, rainbowPaint)

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

