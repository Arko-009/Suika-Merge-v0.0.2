package com.suika.catmerge.game

import android.os.SystemClock
import com.suika.catmerge.game.audio.SoundManager
import com.suika.catmerge.game.data.GamePreferences
import com.suika.catmerge.game.effects.ParticleSystem
import com.suika.catmerge.game.physics.Ball
import com.suika.catmerge.game.physics.FlaskContainer
import com.suika.catmerge.game.physics.PhysicsEngine

/**
 * Core game state manager, drop logic, scoring, combos, and danger monitoring.
 */
class GameEngine(
    val flask: FlaskContainer,
    val physics: PhysicsEngine,
    val soundManager: SoundManager,
    val particleSystem: ParticleSystem,
    val prefs: GamePreferences
) : PhysicsEngine.MergeListener {

    enum class State {
        READY,
        PLAYING,
        PAUSED,
        GAME_OVER
    }

    var state = State.READY
        private set

    // Scores
    var currentScore = 0
        private set
    var highScore = prefs.highScore
        private set
    var highestLevelUnlocked = prefs.highestLevelUnlocked
        private set

    // Combo system
    var comboCount = 1
        private set
    private var lastMergeTime = 0L
    private val comboWindowMs = 1200L

    // Dropper state at the flask opening
    var dropperX: Float = flask.centerX
    var currentDropperLevel: Int = 0
        private set
    var nextBallLevel: Int = 0
        private set

    // Auto drop timer: 1 second interval as requested ("falling from above one png every second")
    val dropInterval: Float = 1.0f
    var dropCountdown: Float = dropInterval
        private set
    var canDrop: Boolean = true

    // Overflow / Danger state
    var dangerTimer: Float = 0f
        private set
    val maxDangerDuration: Float = 3.0f
    var isWarningActive: Boolean = false
        private set

    // Callbacks for UI updates
    interface GameEngineListener {
        fun onScoreChanged(score: Int, highScore: Int)
        fun onNextBallChanged(nextLevel: Int)
        fun onCombo(combo: Int)
        fun onGameOver(finalScore: Int, highestLevel: Int, isNewHighScore: Boolean)
        fun onHapticFeedback(heavy: Boolean)
    }

    var listener: GameEngineListener? = null

    init {
        physics.mergeListener = this
        prepareNextBall()
    }

    fun startNewGame() {
        physics.clear()
        particleSystem.clear()
        currentScore = 0
        comboCount = 1
        dangerTimer = 0f
        isWarningActive = false
        dropCountdown = dropInterval
        canDrop = true
        dropperX = flask.centerX
        currentDropperLevel = 0
        prepareNextBall()

        state = State.PLAYING
        listener?.onScoreChanged(currentScore, highScore)
        listener?.onNextBallChanged(nextBallLevel)
    }

    fun pause() {
        if (state == State.PLAYING) {
            state = State.PAUSED
        }
    }

    fun resume() {
        if (state == State.PAUSED) {
            state = State.PLAYING
        }
    }

    private fun prepareNextBall() {
        // Roll next ball: mainly level 0, occasional 1 or 2 as score grows
        val roll = Math.random()
        nextBallLevel = when {
            currentScore > 500 && roll < 0.15 -> 1
            currentScore > 1500 && roll < 0.25 -> if (roll < 0.08) 2 else 1
            else -> 0
        }
        listener?.onNextBallChanged(nextBallLevel)
    }

    /**
     * Drops the current ball down into the flask neck.
     */
    fun dropBall() {
        if (state != State.PLAYING || !canDrop) return

        // Clamp dropper within flask mouth opening
        val minX = flask.centerX - flask.neckHalfWidth * 0.7f
        val maxX = flask.centerX + flask.neckHalfWidth * 0.7f
        val clampedX = dropperX.coerceIn(minX, maxX)
        val dropY = flask.neckTopY - 20f

        val ball = physics.addBall(clampedX, dropY, currentDropperLevel)
        // Give slight downwards velocity and random subtle spin
        ball.vy = 120f
        ball.angularVelocity = (Math.random().toFloat() - 0.5f) * 4f

        soundManager.playDropSound()
        listener?.onHapticFeedback(false)

        // Queue next ball
        currentDropperLevel = nextBallLevel
        prepareNextBall()

        // Reset timer
        dropCountdown = dropInterval
    }

    fun setDropperPosition(x: Float) {
        val minX = flask.centerX - flask.neckHalfWidth * 0.85f
        val maxX = flask.centerX + flask.neckHalfWidth * 0.85f
        dropperX = x.coerceIn(minX, maxX)
    }

    fun stirFlask() {
        if (state != State.PLAYING) return
        physics.applyStirImpulse(1.0f)
        soundManager.playBounceSound(0.5f)
        listener?.onHapticFeedback(true)
    }

    fun update(deltaTime: Float) {
        if (state != State.PLAYING) return

        // 1. Auto-drop countdown
        dropCountdown -= deltaTime
        if (dropCountdown <= 0f) {
            dropBall()
        }

        // 2. Physics simulation step
        physics.step(deltaTime)

        // 3. Flask animation & danger monitoring
        flask.update(deltaTime)
        checkDanger(deltaTime)

        // 4. Update juice effects
        particleSystem.update(deltaTime)

        // 5. Combo expiration check
        val now = SystemClock.uptimeMillis()
        if (comboCount > 1 && (now - lastMergeTime) > comboWindowMs) {
            comboCount = 1
        }
    }

    private fun checkDanger(deltaTime: Float) {
        // Any ball resting above danger line triggers danger
        var anyBallInDanger = false
        val dangerY = flask.dangerLineY

        for (ball in physics.balls) {
            // Ball is considered above danger line if its bottom is above danger line
            // and it is resting (low downward velocity)
            if (ball.y - ball.radius < dangerY && ball.vy < 50f) {
                anyBallInDanger = true
                break
            }
        }

        if (anyBallInDanger) {
            isWarningActive = true
            dangerTimer += deltaTime
            if (dangerTimer >= maxDangerDuration) {
                triggerGameOver()
            }
        } else {
            isWarningActive = false
            dangerTimer = (dangerTimer - deltaTime * 1.5f).coerceAtLeast(0f)
        }
    }

    private fun triggerGameOver() {
        state = State.GAME_OVER
        val isNewHigh = currentScore > highScore
        if (isNewHigh) {
            highScore = currentScore
            prefs.highScore = currentScore
        }
        listener?.onGameOver(currentScore, highestLevelUnlocked, isNewHigh)
        listener?.onHapticFeedback(true)
    }

    override fun onBallsMerged(newBall: Ball, ballA: Ball, ballB: Ball) {
        if (state != State.PLAYING) return

        // Combo calculation
        val now = SystemClock.uptimeMillis()
        if (now - lastMergeTime <= comboWindowMs) {
            comboCount++
            listener?.onCombo(comboCount)
        } else {
            comboCount = 1
        }
        lastMergeTime = now

        // Score award
        val points = newBall.scoreValue * comboCount
        currentScore += points

        if (currentScore > highScore) {
            highScore = currentScore
            prefs.highScore = currentScore
        }

        if (newBall.level > highestLevelUnlocked) {
            highestLevelUnlocked = newBall.level
            prefs.highestLevelUnlocked = highestLevelUnlocked
        }

        prefs.totalMerges++

        // Audio & Visual juice
        soundManager.playLevelSound(newBall.level)
        particleSystem.triggerMergeBurst(newBall.x, newBall.y, newBall.radius, newBall.level)
        particleSystem.triggerScorePopup(newBall.x, newBall.y - newBall.radius, points, comboCount)

        listener?.onScoreChanged(currentScore, highScore)
        listener?.onHapticFeedback(newBall.level >= 5)
    }
}

