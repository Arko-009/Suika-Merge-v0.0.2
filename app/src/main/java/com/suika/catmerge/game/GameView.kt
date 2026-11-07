package com.suika.catmerge.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.suika.catmerge.R
import com.suika.catmerge.game.physics.Ball
import com.suika.catmerge.game.physics.GameContainer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * SurfaceView rendering the game loop at high frame rates (60-120 FPS)
 * using double-buffered hardware canvas.
 */
class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : SurfaceView(context, attrs), SurfaceHolder.Callback, Runnable {

    var engine: GameEngine? = null

    private var renderThread: Thread? = null
    @Volatile
    private var isRunning = false

    // Bitmaps for 14 cat levels (0..13)
    private val ballBitmaps = arrayOfNulls<Bitmap>(14)
    private val ballMatrix = Matrix()

    // Paints
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val aimLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#804DEEEA")
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val dropperRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#FFD54F")
        strokeCap = Paint.Cap.ROUND
    }

    private val dropperBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = Color.parseColor("#33FFFFFF")
    }

    private val dangerTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FF5252")
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        setShadowLayer(10f, 0f, 0f, Color.RED)
    }

    private val dangerVignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // Floating background particles (ambient lab bubbles)
    private class AmbientBubble(var x: Float, var y: Float, var vy: Float, var radius: Float, var alpha: Float)
    private val ambientBubbles = ArrayList<AmbientBubble>()
    private val ambientPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#184DEEEA")
    }

    private var surfaceWidth = 0f
    private var surfaceHeight = 0f

    init {
        holder.addCallback(this)
        loadCatBitmaps()
    }

    private fun loadCatBitmaps() {
        val drawableIds = intArrayOf(
            R.drawable.ball_0,
            R.drawable.ball_1,
            R.drawable.ball_2,
            R.drawable.ball_3,
            R.drawable.ball_4,
            R.drawable.ball_5,
            R.drawable.ball_6,
            R.drawable.ball_7,
            R.drawable.ball_8,
            R.drawable.ball_9,
            R.drawable.ball_10,
            R.drawable.ball_11,
            R.drawable.ball_12,
            R.drawable.ball_13
        )

        for (i in drawableIds.indices) {
            try {
                ballBitmaps[i] = BitmapFactory.decodeResource(resources, drawableIds[i])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceWidth = width.toFloat()
        surfaceHeight = height.toFloat()

        setupAmbientBubbles()
        backgroundPaint.color = Color.BLACK

        resumeGame()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        surfaceWidth = width.toFloat()
        surfaceHeight = height.toFloat()
        engine?.flask?.setup(surfaceWidth, surfaceHeight)
        // Base ball radius scaled comfortably to fit any container shape
        val baseRadius = (engine?.flask?.circleRadius?.times(0.122f) ?: 32f).coerceIn(24f, 50f)
        engine?.physics?.baseRadius = baseRadius
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        pauseGame()
    }

    fun resumeGame() {
        if (!isRunning) {
            isRunning = true
            renderThread = Thread(this, "GameLoopThread").apply { start() }
        }
    }

    fun pauseGame() {
        val thread = renderThread
        isRunning = false
        if (thread != null) {
            try {
                thread.join(500)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
            renderThread = null
        }
    }

    private fun setupAmbientBubbles() {
        ambientBubbles.clear()
        for (i in 0 until 18) {
            ambientBubbles.add(
                AmbientBubble(
                    x = (Math.random() * surfaceWidth).toFloat(),
                    y = (Math.random() * surfaceHeight).toFloat(),
                    vy = -(15f + Math.random().toFloat() * 25f),
                    radius = (4f + Math.random().toFloat() * 10f),
                    alpha = (0.1f + Math.random().toFloat() * 0.25f)
                )
            )
        }
    }

    override fun run() {
        var lastTime = System.nanoTime()

        while (isRunning) {
            val now = System.nanoTime()
            var deltaTime = (now - lastTime) / 1_000_000_000f
            lastTime = now

            // Cap delta time to prevent spiral of death on lag spikes
            if (deltaTime > 0.05f) deltaTime = 0.05f

            // Update game engine
            engine?.update(deltaTime)
            updateAmbientBubbles(deltaTime)

            // Draw frame
            drawFrame()

            // Target ~120 / 60 FPS
            val frameTimeMs = (System.nanoTime() - now) / 1_000_000
            val sleepMs = 8 - frameTimeMs // ~120fps max
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs)
                } catch (ignored: InterruptedException) {
                }
            }
        }
    }

    private fun updateAmbientBubbles(deltaTime: Float) {
        for (b in ambientBubbles) {
            b.y += b.vy * deltaTime
            if (b.y < -20f) {
                b.y = surfaceHeight + 20f
                b.x = (Math.random() * surfaceWidth).toFloat()
            }
        }
    }

    private fun drawFrame() {
        if (!holder.surface.isValid) return

        val canvas: Canvas? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                holder.lockHardwareCanvas()
            } catch (e: Exception) {
                holder.lockCanvas()
            }
        } else {
            holder.lockCanvas()
        }

        if (canvas == null) return

        try {
            render(canvas)
        } finally {
            try {
                holder.unlockCanvasAndPost(canvas)
            } catch (ignored: Exception) {
            }
        }
    }

    private fun render(canvas: Canvas) {
        val eng = engine ?: return
        val flask = eng.flask
        val pSys = eng.particleSystem

        // 1. Screen shake transform
        canvas.save()
        if (pSys.shakeIntensity > 0.1f) {
            canvas.translate(pSys.shakeOffsetX, pSys.shakeOffsetY)
        }

        // 2. Background
        canvas.drawRect(0f, 0f, surfaceWidth, surfaceHeight, backgroundPaint)

        // 3. Ambient bubbles
        for (b in ambientBubbles) {
            ambientPaint.alpha = (b.alpha * 255).toInt()
            canvas.drawCircle(b.x, b.y, b.radius, ambientPaint)
        }

        // 4. Flask Container (Glass, Neon Wall, Danger Line)
        flask.draw(canvas, eng.isWarningActive)

        // 5. Active Balls inside the Flask
        for (ball in eng.physics.balls) {
            drawBall(canvas, ball)
        }

        // 6. Aim Guide & Queued Dropper Ball (when in PLAYING state)
        if (eng.state == GameEngine.State.PLAYING) {
            drawDropper(canvas, eng)
        }

        // 7. Visual Juice: Particles, Floating Scores, Shockwaves
        pSys.draw(canvas)

        // 8. Danger Warning Overlay
        if (eng.isWarningActive) {
            val remainingSec = (eng.maxDangerDuration - eng.dangerTimer).coerceAtLeast(0f)
            canvas.drawText(
                "OVERFLOW IN %.1fs!".format(remainingSec),
                flask.centerX,
                flask.neckTopY - 70f,
                dangerTextPaint
            )
        }

        canvas.restore()
    }

    private fun drawBall(canvas: Canvas, ball: Ball) {
        val idx = ball.level.coerceIn(0, ballBitmaps.size - 1)
        val bmp = ballBitmaps[idx] ?: return

        canvas.save()
        canvas.translate(ball.x, ball.y)
        canvas.rotate(Math.toDegrees(ball.angle.toDouble()).toFloat())

        val targetDiameter = ball.radius * 2f * ball.visualScale
        val scale = targetDiameter / bmp.width.toFloat()
        canvas.scale(scale, scale)

        // Draw centered
        canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, null)
        canvas.restore()
    }

    private fun drawDropper(canvas: Canvas, eng: GameEngine) {
        val flask = eng.flask
        val dropX = eng.dropperX
        val dropY = flask.neckTopY - 25f
        val currentLvl = eng.currentDropperLevel
        val baseR = eng.physics.baseRadius
        val ballRadius = Ball.calculateRadius(currentLvl, baseR)

        // Aim dotted line straight down into neck
        canvas.drawLine(dropX, dropY + ballRadius, dropX, flask.neckBottomY, aimLinePaint)

        // Draw upcoming queued ball at dropper
        val bmp = ballBitmaps[currentLvl.coerceIn(0, ballBitmaps.size - 1)]
        if (bmp != null) {
            canvas.save()
            canvas.translate(dropX, dropY)
            val scale = (ballRadius * 2f) / bmp.width.toFloat()
            canvas.scale(scale, scale)
            canvas.drawBitmap(bmp, -bmp.width / 2f, -bmp.height / 2f, null)
            canvas.restore()
        }

        // Circular Countdown Progress Ring around dropper ball
        val ringRect = RectF(
            dropX - ballRadius - 10f,
            dropY - ballRadius - 10f,
            dropX + ballRadius + 10f,
            dropY + ballRadius + 10f
        )
        // Background track
        canvas.drawArc(ringRect, 0f, 360f, false, dropperBgPaint)
        // Active countdown fill
        val progress = (eng.dropCountdown / eng.dropInterval).coerceIn(0f, 1f)
        val sweepAngle = progress * 360f
        canvas.drawArc(ringRect, -90f, sweepAngle, false, dropperRingPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val eng = engine ?: return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                if (eng.state == GameEngine.State.PLAYING) {
                    eng.setDropperPosition(event.x)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (eng.state == GameEngine.State.PLAYING) {
                    eng.setDropperPosition(event.x)
                    eng.dropBall()
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }
}

