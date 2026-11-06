package com.suika.catmerge.game.physics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Geometric representation and collision model of the chemical flask container.
 * Features:
 * - Flared mouth at the top for smooth ball drops
 * - Vertical neck channel
 * - Smooth curved shoulders
 * - Large circular bottom bulb chamber
 * - Danger line with pulse animation
 */
class FlaskContainer {

    var centerX: Float = 0f
        private set
    var bulbCenterY: Float = 0f
        private set
    var bulbRadius: Float = 0f
        private set
    var neckHalfWidth: Float = 0f
        private set
    var neckTopY: Float = 0f
        private set
    var shoulderY: Float = 0f
        private set
    var dangerLineY: Float = 0f
        private set

    // Collision boundary segments: Pair of (start, end)
    val wallSegments = ArrayList<Pair<Vector2D, Vector2D>>()

    // Drawing paths and paints (lazily initialized for drawing to keep physics decoupled from Android UI)
    private var pathsInitialized = false
    private val flaskOutlinePath by lazy { Path() }
    private val flaskFillPath by lazy { Path() }
    private val dangerLinePath by lazy { Path() }

    private val outlinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 10f
            color = Color.parseColor("#4DEEEA") // Neon Cyan Glass
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    private val outerGlowPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 22f
            color = Color.parseColor("#224DEEEA")
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }

    private val glassHighlightPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = Color.parseColor("#80FFFFFF")
            strokeCap = Paint.Cap.ROUND
        }
    }

    private val glassInnerFillPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    private val dangerLinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.parseColor("#FF5252")
            pathEffect = DashPathEffect(floatArrayOf(16f, 12f), 0f)
        }
    }

    private val dangerGlowPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 12f
            color = Color.parseColor("#44FF5252")
        }
    }

    private var dangerPulseTime = 0f

    /**
     * Initializes the flask geometry adapted to screen dimensions.
     */
    fun setup(viewWidth: Float, viewHeight: Float) {
        centerX = viewWidth / 2f
        // Bottom bulb sits in lower half of screen
        bulbRadius = (viewWidth * 0.44f).coerceAtMost(viewHeight * 0.28f)
        bulbCenterY = viewHeight - bulbRadius - 100f

        // Neck dimensions
        neckHalfWidth = bulbRadius * 0.32f
        val neckHeight = bulbRadius * 1.05f
        shoulderY = bulbCenterY - bulbRadius * 0.65f
        neckTopY = shoulderY - neckHeight
        dangerLineY = neckTopY + (shoulderY - neckTopY) * 0.35f

        rebuildGeometry()
    }

    private fun rebuildGeometry() {
        wallSegments.clear()
        pathsInitialized = false

        val leftNeckX = centerX - neckHalfWidth
        val rightNeckX = centerX + neckHalfWidth
        val flareXOffset = neckHalfWidth * 0.45f
        val flareYOffset = 25f

        val leftMouth = Vector2D(leftNeckX - flareXOffset, neckTopY - flareYOffset)
        val leftNeckTop = Vector2D(leftNeckX, neckTopY)
        val leftShoulderTop = Vector2D(leftNeckX, shoulderY)

        val rightMouth = Vector2D(rightNeckX + flareXOffset, neckTopY - flareYOffset)
        val rightNeckTop = Vector2D(rightNeckX, neckTopY)
        val rightShoulderTop = Vector2D(rightNeckX, shoulderY)

        val connectAngle = Math.toRadians(40.0).toFloat()
        val bulbLeftConnect = Vector2D(
            centerX - bulbRadius * cos(connectAngle),
            bulbCenterY - bulbRadius * sin(connectAngle)
        )
        val bulbRightConnect = Vector2D(
            centerX + bulbRadius * cos(connectAngle),
            bulbCenterY - bulbRadius * sin(connectAngle)
        )

        // 1. Left mouth flare
        wallSegments.add(Pair(leftMouth, leftNeckTop))
        // 2. Left neck vertical wall
        wallSegments.add(Pair(leftNeckTop, leftShoulderTop))

        // 3. Left shoulder curve
        val leftShoulderSegments = 5
        var prevLeft = leftShoulderTop
        for (i in 1..leftShoulderSegments) {
            val t = i.toFloat() / leftShoulderSegments
            val p0 = leftShoulderTop
            val p1 = Vector2D(leftNeckX - bulbRadius * 0.15f, shoulderY + bulbRadius * 0.25f)
            val p2 = bulbLeftConnect
            val ix = (1 - t) * (1 - t) * p0.x + 2 * (1 - t) * t * p1.x + t * t * p2.x
            val iy = (1 - t) * (1 - t) * p0.y + 2 * (1 - t) * t * p1.y + t * t * p2.y
            val cur = Vector2D(ix, iy)
            wallSegments.add(Pair(prevLeft, cur))
            prevLeft = cur
        }

        // 4. Right mouth flare
        wallSegments.add(Pair(rightNeckTop, rightMouth))
        // 5. Right neck vertical wall
        wallSegments.add(Pair(rightShoulderTop, rightNeckTop))

        // 6. Right shoulder curve
        var prevRight = bulbRightConnect
        for (i in 1..leftShoulderSegments) {
            val t = i.toFloat() / leftShoulderSegments
            val p0 = bulbRightConnect
            val p1 = Vector2D(rightNeckX + bulbRadius * 0.15f, shoulderY + bulbRadius * 0.25f)
            val p2 = rightShoulderTop
            val ix = (1 - t) * (1 - t) * p0.x + 2 * (1 - t) * t * p1.x + t * t * p2.x
            val iy = (1 - t) * (1 - t) * p0.y + 2 * (1 - t) * t * p1.y + t * t * p2.y
            val cur = Vector2D(ix, iy)
            wallSegments.add(Pair(prevRight, cur))
            prevRight = cur
        }
    }

    private fun ensurePathsBuilt() {
        if (pathsInitialized) return
        pathsInitialized = true

        flaskOutlinePath.reset()
        flaskFillPath.reset()
        dangerLinePath.reset()

        val leftNeckX = centerX - neckHalfWidth
        val rightNeckX = centerX + neckHalfWidth
        val flareXOffset = neckHalfWidth * 0.45f
        val flareYOffset = 25f

        val leftMouth = Vector2D(leftNeckX - flareXOffset, neckTopY - flareYOffset)
        val leftNeckTop = Vector2D(leftNeckX, neckTopY)
        val leftShoulderTop = Vector2D(leftNeckX, shoulderY)

        val rightMouth = Vector2D(rightNeckX + flareXOffset, neckTopY - flareYOffset)
        val rightNeckTop = Vector2D(rightNeckX, neckTopY)
        val rightShoulderTop = Vector2D(rightNeckX, shoulderY)

        val connectAngle = Math.toRadians(40.0).toFloat()
        val bulbLeftConnect = Vector2D(
            centerX - bulbRadius * cos(connectAngle),
            bulbCenterY - bulbRadius * sin(connectAngle)
        )
        val bulbRightConnect = Vector2D(
            centerX + bulbRadius * cos(connectAngle),
            bulbCenterY - bulbRadius * sin(connectAngle)
        )

        // Build Paths for rendering
        flaskOutlinePath.moveTo(leftMouth.x, leftMouth.y)
        flaskOutlinePath.lineTo(leftNeckTop.x, leftNeckTop.y)
        flaskOutlinePath.lineTo(leftShoulderTop.x, leftShoulderTop.y)
        flaskOutlinePath.quadTo(
            leftNeckX - bulbRadius * 0.15f, shoulderY + bulbRadius * 0.25f,
            bulbLeftConnect.x, bulbLeftConnect.y
        )

        val bulbRect = RectF(
            centerX - bulbRadius,
            bulbCenterY - bulbRadius,
            centerX + bulbRadius,
            bulbCenterY + bulbRadius
        )
        val startAngleDeg = Math.toDegrees(
            kotlin.math.atan2(
                (bulbLeftConnect.y - bulbCenterY).toDouble(),
                (bulbLeftConnect.x - centerX).toDouble()
            )
        ).toFloat()
        val endAngleDeg = Math.toDegrees(
            kotlin.math.atan2(
                (bulbRightConnect.y - bulbCenterY).toDouble(),
                (bulbRightConnect.x - centerX).toDouble()
            )
        ).toFloat()
        val sweepAngleDeg = (360f - (startAngleDeg - endAngleDeg)) % 360f

        flaskOutlinePath.arcTo(bulbRect, startAngleDeg, sweepAngleDeg, false)
        flaskOutlinePath.quadTo(
            rightNeckX + bulbRadius * 0.15f, shoulderY + bulbRadius * 0.25f,
            rightShoulderTop.x, rightShoulderTop.y
        )
        flaskOutlinePath.lineTo(rightNeckTop.x, rightNeckTop.y)
        flaskOutlinePath.lineTo(rightMouth.x, rightMouth.y)

        flaskFillPath.set(flaskOutlinePath)
        flaskFillPath.close()

        dangerLinePath.moveTo(leftNeckX + 6f, dangerLineY)
        dangerLinePath.lineTo(rightNeckX - 6f, dangerLineY)

        glassInnerFillPaint.shader = LinearGradient(
            centerX, neckTopY,
            centerX, bulbCenterY + bulbRadius,
            Color.parseColor("#15223A5E"),
            Color.parseColor("#250A192F"),
            Shader.TileMode.CLAMP
        )
    }

    /**
     * Collides a ball against the flask boundaries (walls and circular bulb).
     * Returns true if collision occurred.
     */
    fun resolveBallCollision(ball: Ball, restitution: Float = 0.25f, friction: Float = 0.02f): Boolean {
        var collided = false

        // 1. Check against bulb circular chamber
        // Only if ball is below shoulder level or in bulb range
        if (ball.y + ball.radius > shoulderY) {
            val dx = ball.x - centerX
            val dy = ball.y - bulbCenterY
            val distSq = dx * dx + dy * dy
            val maxAllowedDist = bulbRadius - ball.radius

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
                    if (vn < 0) {
                        val vtx = ball.vx - vn * nx
                        val vty = ball.vy - vn * ny
                        ball.vx = -vn * restitution * nx + vtx * (1f - friction)
                        ball.vy = -vn * restitution * ny + vty * (1f - friction)
                        // Rolling friction / spin
                        ball.angularVelocity += (vtx * ny - vty * nx) * 0.005f
                    }
                    collided = true
                }
            }
        }

        // 2. Check against line segments (neck walls, shoulders, mouth flares)
        for (seg in wallSegments) {
            val p1 = seg.first
            val p2 = seg.second

            val segDx = p2.x - p1.x
            val segDy = p2.y - p1.y
            val segLenSq = segDx * segDx + segDy * segDy
            if (segLenSq < 0.001f) continue

            // Project ball center onto segment
            val t = ((ball.x - p1.x) * segDx + (ball.y - p1.y) * segDy) / segLenSq
            val clampedT = t.coerceIn(0f, 1f)

            val closestX = p1.x + clampedT * segDx
            val closestY = p1.y + clampedT * segDy

            val distDx = ball.x - closestX
            val distDy = ball.y - closestY
            val distSq = distDx * distDx + distDy * distDy

            if (distSq < ball.radius * ball.radius) {
                val dist = sqrt(distSq)
                val nx: Float
                val ny: Float
                val penetration: Float

                if (dist > 0.0001f) {
                    nx = distDx / dist
                    ny = distDy / dist
                    penetration = ball.radius - dist
                } else {
                    // Ball center exactly on segment, push towards container center
                    nx = if (ball.x < centerX) 1f else -1f
                    ny = 0f
                    penetration = ball.radius
                }

                // Push ball out of wall
                ball.x += nx * penetration
                ball.y += ny * penetration

                // Reflect velocity
                val vn = ball.vx * nx + ball.vy * ny
                if (vn < 0) {
                    val vtx = ball.vx - vn * nx
                    val vty = ball.vy - vn * ny
                    ball.vx = -vn * restitution * nx + vtx * (1f - friction)
                    ball.vy = -vn * restitution * ny + vty * (1f - friction)
                    ball.angularVelocity += (vtx * ny - vty * nx) * 0.005f
                }
                collided = true
            }
        }

        return collided
    }

    fun update(deltaTime: Float) {
        dangerPulseTime += deltaTime * 5f
    }

    /**
     * Renders the glass flask container.
     */
    fun draw(canvas: Canvas, isInDanger: Boolean) {
        ensurePathsBuilt()

        // 1. Inner glass ambient fill
        canvas.drawPath(flaskFillPath, glassInnerFillPaint)

        // 2. Outer glow
        canvas.drawPath(flaskOutlinePath, outerGlowPaint)

        // 3. Main neon outline
        canvas.drawPath(flaskOutlinePath, outlinePaint)

        // 4. Subtle inner highlight curve (gives 3D glass sheen)
        canvas.drawArc(
            RectF(
                centerX - bulbRadius + 14f,
                bulbCenterY - bulbRadius + 14f,
                centerX + bulbRadius - 14f,
                bulbCenterY + bulbRadius - 14f
            ),
            110f, 120f, false, glassHighlightPaint
        )

        // 5. Danger Line
        if (isInDanger) {
            val pulseAlpha = (120 + 135 * sin(dangerPulseTime.toDouble())).toInt().coerceIn(40, 255)
            dangerLinePaint.alpha = pulseAlpha
            dangerGlowPaint.alpha = (pulseAlpha * 0.4f).toInt()
            canvas.drawPath(dangerLinePath, dangerGlowPaint)
        } else {
            dangerLinePaint.alpha = 90
        }
        canvas.drawPath(dangerLinePath, dangerLinePaint)
    }
}
