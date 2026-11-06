package com.suika.catmerge.game.effects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * High-performance particle system for merge bursts, star sparkles, and floating score texts.
 */
class ParticleSystem {

    private class Particle {
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var size = 0f
        var alpha = 1f
        var color = 0
        var life = 0f
        var maxLife = 0f
        var isStar = false
        var rotation = 0f
        var rotSpeed = 0f
        var isAlive = false
    }

    private class Shockwave {
        var x = 0f
        var y = 0f
        var radius = 0f
        var maxRadius = 0f
        var alpha = 1f
        var color = 0
        var isAlive = false
    }

    private class ScorePopup {
        var x = 0f
        var y = 0f
        var vy = 0f
        var text = ""
        var alpha = 1f
        var scale = 1.2f
        var life = 0f
        var maxLife = 0.8f
        var color = 0
        var isAlive = false
    }

    // Object pools to prevent runtime allocations
    private val particlePool = Array(200) { Particle() }
    private val shockwavePool = Array(20) { Shockwave() }
    private val popupPool = Array(30) { ScorePopup() }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val shockwavePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val scorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        textSize = 42f
        color = Color.WHITE
        setShadowLayer(8f, 0f, 0f, Color.parseColor("#80000000"))
    }

    private val starPath = Path()

    // Screen shake state
    var shakeIntensity = 0f
    var shakeOffsetX = 0f
    var shakeOffsetY = 0f

    private val colors = intArrayOf(
        Color.parseColor("#FFD700"), // Gold
        Color.parseColor("#00E5FF"), // Cyan
        Color.parseColor("#FF4081"), // Pink
        Color.parseColor("#E040FB"), // Purple
        Color.parseColor("#76FF03"), // Neon Green
        Color.WHITE
    )

    fun triggerMergeBurst(x: Float, y: Float, ballRadius: Float, level: Int) {
        // 1. Shockwave ring
        val sw = shockwavePool.firstOrNull { !it.isAlive }
        if (sw != null) {
            sw.x = x
            sw.y = y
            sw.radius = ballRadius * 0.5f
            sw.maxRadius = ballRadius * 1.8f
            sw.alpha = 1f
            sw.color = colors[level % colors.size]
            sw.isAlive = true
        }

        // 2. Sparkle particles
        val count = (14 + level * 2).coerceAtMost(30)
        for (i in 0 until count) {
            val p = particlePool.firstOrNull { !it.isAlive } ?: break
            val angle = (Math.random() * 2 * PI).toFloat()
            val speed = (180f + Math.random().toFloat() * 320f)
            p.x = x
            p.y = y
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed - 60f // upward bias
            p.size = (6f + Math.random().toFloat() * 12f)
            p.alpha = 1f
            p.maxLife = (0.4f + Math.random().toFloat() * 0.4f)
            p.life = 0f
            p.color = colors[(Math.random() * colors.size).toInt()]
            p.isStar = (i % 2 == 0)
            p.rotation = (Math.random() * 360).toFloat()
            p.rotSpeed = ((Math.random() - 0.5) * 720).toFloat()
            p.isAlive = true
        }

        // 3. Screen shake
        shakeIntensity = (shakeIntensity + 4f + level * 1.2f).coerceAtMost(25f)
    }

    fun triggerScorePopup(x: Float, y: Float, points: Int, combo: Int = 1) {
        val popup = popupPool.firstOrNull { !it.isAlive } ?: return
        popup.x = x
        popup.y = y
        popup.vy = -140f
        popup.text = if (combo > 1) "+$points x$combo!" else "+$points"
        popup.alpha = 1f
        popup.scale = if (combo > 1) 1.4f else 1.15f
        popup.life = 0f
        popup.maxLife = 0.9f
        popup.color = if (combo > 1) Color.parseColor("#FFD54F") else Color.WHITE
        popup.isAlive = true
    }

    fun update(deltaTime: Float) {
        // Update particles
        for (p in particlePool) {
            if (!p.isAlive) continue
            p.life += deltaTime
            if (p.life >= p.maxLife) {
                p.isAlive = false
                continue
            }
            p.vy += 450f * deltaTime // gravity
            p.x += p.vx * deltaTime
            p.y += p.vy * deltaTime
            p.rotation += p.rotSpeed * deltaTime
            p.alpha = (1f - (p.life / p.maxLife)).coerceIn(0f, 1f)
        }

        // Update shockwaves
        for (sw in shockwavePool) {
            if (!sw.isAlive) continue
            sw.radius += (sw.maxRadius - sw.radius) * 12f * deltaTime
            sw.alpha -= 2.2f * deltaTime
            if (sw.alpha <= 0f) {
                sw.isAlive = false
            }
        }

        // Update score popups
        for (sp in popupPool) {
            if (!sp.isAlive) continue
            sp.life += deltaTime
            if (sp.life >= sp.maxLife) {
                sp.isAlive = false
                continue
            }
            sp.y += sp.vy * deltaTime
            sp.vy *= 0.94f
            sp.alpha = (1f - (sp.life / sp.maxLife)).coerceIn(0f, 1f)
            if (sp.scale > 1.0f) {
                sp.scale -= 0.5f * deltaTime
            }
        }

        // Screen shake decay
        if (shakeIntensity > 0.01f) {
            shakeOffsetX = ((Math.random() - 0.5) * shakeIntensity * 2).toFloat()
            shakeOffsetY = ((Math.random() - 0.5) * shakeIntensity * 2).toFloat()
            shakeIntensity *= 0.88f
        } else {
            shakeOffsetX = 0f
            shakeOffsetY = 0f
            shakeIntensity = 0f
        }
    }

    fun draw(canvas: Canvas) {
        // Draw shockwaves
        for (sw in shockwavePool) {
            if (!sw.isAlive) continue
            shockwavePaint.color = sw.color
            shockwavePaint.alpha = (sw.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawCircle(sw.x, sw.y, sw.radius, shockwavePaint)
        }

        // Draw particles
        for (p in particlePool) {
            if (!p.isAlive) continue
            particlePaint.color = p.color
            particlePaint.alpha = (p.alpha * 255).toInt().coerceIn(0, 255)

            if (p.isStar) {
                drawStar(canvas, p.x, p.y, p.size, p.rotation, particlePaint)
            } else {
                canvas.drawCircle(p.x, p.y, p.size * 0.5f, particlePaint)
            }
        }

        // Draw score popups
        for (sp in popupPool) {
            if (!sp.isAlive) continue
            scorePaint.color = sp.color
            scorePaint.alpha = (sp.alpha * 255).toInt().coerceIn(0, 255)
            scorePaint.textSize = 38f * sp.scale
            canvas.drawText(sp.text, sp.x, sp.y, scorePaint)
        }
    }

    private fun drawStar(canvas: Canvas, cx: Float, cy: Float, radius: Float, rotation: Float, paint: Paint) {
        starPath.reset()
        val numPoints = 4
        val step = PI / numPoints
        val rotRad = Math.toRadians(rotation.toDouble()).toFloat()

        for (i in 0 until numPoints * 2) {
            val r = if (i % 2 == 0) radius else radius * 0.35f
            val angle = i * step + rotRad
            val px = cx + cos(angle).toFloat() * r
            val py = cy + sin(angle).toFloat() * r
            if (i == 0) starPath.moveTo(px, py) else starPath.lineTo(px, py)
        }
        starPath.close()
        canvas.drawPath(starPath, paint)
    }

    fun clear() {
        particlePool.forEach { it.isAlive = false }
        shockwavePool.forEach { it.isAlive = false }
        popupPool.forEach { it.isAlive = false }
        shakeIntensity = 0f
        shakeOffsetX = 0f
        shakeOffsetY = 0f
    }
}

