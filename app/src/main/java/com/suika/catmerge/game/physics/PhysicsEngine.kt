package com.suika.catmerge.game.physics

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/**
 * High-performance 2D rigid circle physics engine utilizing Position-Based Dynamics (PBD).
 * Guarantees zero penetration/sinking under gravity, stable stacking, and instant merging.
 */
class PhysicsEngine(
    val flask: FlaskContainer,
    var baseRadius: Float
) {
    val balls = ArrayList<Ball>()
    private val nextId = AtomicLong(1)

    // Physics tuning
    var gravityX: Float = 0f
    var gravityY: Float = 1600f // px/s^2
    var gravity: Float
        get() = gravityY
        set(value) { gravityY = value }
    var restitution: Float = 0.25f // bounciness
    var friction: Float = 0.03f

    private val substeps = 8

    // Merge callback interface
    interface MergeListener {
        fun onBallsMerged(newBall: Ball, ballA: Ball, ballB: Ball)
    }

    var mergeListener: MergeListener? = null

    fun addBall(x: Float, y: Float, level: Int): Ball {
        val ball = Ball(nextId.getAndIncrement(), x, y, level, baseRadius)
        balls.add(ball)
        return ball
    }

    fun removeBall(ball: Ball) {
        balls.remove(ball)
    }

    fun clear() {
        balls.clear()
    }

    fun step(deltaTime: Float) {
        val clampedDelta = deltaTime.coerceIn(0.001f, 0.033f)

        // ----------------------------------------------------
        // Step 1: Detect & resolve merges before physics step
        // ----------------------------------------------------
        val merges = ArrayList<Pair<Ball, Ball>>()
        val mergedIds = HashSet<Long>()

        val count = balls.size
        for (i in 0 until count) {
            val b1 = balls[i]
            if (mergedIds.contains(b1.id)) continue

            for (j in i + 1 until count) {
                val b2 = balls[j]
                if (mergedIds.contains(b2.id)) continue

                // Check merge: identical level, below max level, touching or within 4% tolerance
                if (b1.level == b2.level && b1.level < Ball.MAX_LEVEL) {
                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val distSq = dx * dx + dy * dy
                    val mergeDistance = (b1.radius + b2.radius) * 1.04f

                    if (distSq <= mergeDistance * mergeDistance) {
                        merges.add(Pair(b1, b2))
                        mergedIds.add(b1.id)
                        mergedIds.add(b2.id)
                        break // b1 has merged, stop pairing it with others
                    }
                }
            }
        }

        // Execute merges
        for (pair in merges) {
            val a = pair.first
            val b = pair.second

            balls.remove(a)
            balls.remove(b)

            val midX = (a.x + b.x) * 0.5f
            val midY = (a.y + b.y) * 0.5f
            val newLevel = a.level + 1

            val newBall = Ball(nextId.getAndIncrement(), midX, midY, newLevel, baseRadius).apply {
                vx = (a.vx + b.vx) * 0.5f
                vy = (a.vy + b.vy) * 0.5f - 30f
                triggerPopEffect(1.15f)
            }

            balls.add(newBall)
            mergeListener?.onBallsMerged(newBall, a, b)
        }

        // ----------------------------------------------------
        // Step 2: Position-Based Dynamics (PBD) Simulation
        // ----------------------------------------------------
        val subDt = clampedDelta / substeps

        for (sub in 0 until substeps) {
            // Predict positions
            val n = balls.size
            for (i in 0 until n) {
                val b = balls[i]
                b.vx += gravityX * subDt
                b.vy += gravityY * subDt

                b.prevX = b.x
                b.prevY = b.y

                b.x += b.vx * subDt
                b.y += b.vy * subDt

                b.angle += b.angularVelocity * subDt
            }

            // Boundary constraints (flask container)
            for (i in 0 until n) {
                flask.resolveBallCollision(balls[i], restitution, friction)
            }

            // Ball vs Ball separation constraints (rigid contacts)
            for (i in 0 until n) {
                val b1 = balls[i]
                for (j in i + 1 until n) {
                    val b2 = balls[j]

                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val distSq = dx * dx + dy * dy
                    val minDist = b1.radius + b2.radius

                    if (distSq < minDist * minDist) {
                        val dist = sqrt(distSq)
                        val nx: Float
                        val ny: Float
                        val penetration: Float

                        if (dist > 0.0001f) {
                            nx = dx / dist
                            ny = dy / dist
                            penetration = minDist - dist
                        } else {
                            nx = 0f
                            ny = -1f
                            penetration = minDist
                        }

                        val totalInvMass = b1.invMass + b2.invMass
                        if (totalInvMass > 0f) {
                            val push1 = penetration * (b1.invMass / totalInvMass)
                            val push2 = penetration * (b2.invMass / totalInvMass)

                            b1.x -= nx * push1
                            b1.y -= ny * push1
                            b2.x += nx * push2
                            b2.y += ny * push2

                            // Friction and spin exchange on contact
                            val tx = -ny
                            val ty = nx
                            val rvx = b2.vx - b1.vx
                            val rvy = b2.vy - b1.vy
                            val tangentVel = rvx * tx + rvy * ty
                            val frictionImpulse = -tangentVel * friction * 0.5f

                            b1.angularVelocity -= frictionImpulse * 0.005f
                            b2.angularVelocity += frictionImpulse * 0.005f
                        }
                    }
                }
            }

            // Update velocity from corrected positions (prevents sinking & resting overlap)
            for (i in 0 until n) {
                val b = balls[i]
                b.vx = (b.x - b.prevX) / subDt * 0.996f
                b.vy = (b.y - b.prevY) / subDt * 0.996f
                b.angularVelocity *= 0.992f
                b.update(subDt)
            }
        }
    }

    /**
     * Applies a fluid stirring vortex / shake impulse to unstick balls.
     */
    fun applyStirImpulse(strength: Float = 1.0f) {
        val cx = flask.centerX
        val cy = flask.circleCenterY

        for (ball in balls) {
            val dx = ball.x - cx
            val dy = ball.y - cy
            val dist = sqrt(dx * dx + dy * dy)
            if (dist > 0.001f) {
                val tx = -dy / dist
                val ty = dx / dist
                ball.vx += tx * 260f * strength + (Math.random().toFloat() - 0.5f) * 120f
                ball.vy += ty * 180f * strength - 150f * strength
                ball.angularVelocity += (Math.random().toFloat() - 0.5f) * 6f
            }
        }
    }
}
