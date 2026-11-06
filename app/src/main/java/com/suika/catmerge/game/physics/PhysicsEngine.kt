package com.suika.catmerge.game.physics

import java.util.concurrent.atomic.AtomicLong
import kotlin.math.sqrt

/**
 * 2D rigid circle physics engine with continuous collision resolution,
 * multi-substep integration, and merge event dispatching.
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
    var restitution: Float = 0.28f // bounciness
    var friction: Float = 0.04f // friction coefficient
    var linearDamping: Float = 0.998f

    private val substeps = 8

    // Merge callback interface
    interface MergeListener {
        fun onBallsMerged(newBall: Ball, ballA: Ball, ballB: Ball)
    }

    var mergeListener: MergeListener? = null

    // Pending merge pairs to resolve cleanly after collision substeps
    private data class MergeCandidate(val ballA: Ball, val ballB: Ball)
    private val pendingMerges = ArrayList<MergeCandidate>()

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
        pendingMerges.clear()
    }

    fun step(deltaTime: Float) {
        val dt = (deltaTime / substeps).coerceAtMost(0.004f)

        pendingMerges.clear()

        for (sub in 0 until substeps) {
            // 1. Apply gravity & integrate
            for (i in balls.indices) {
                val ball = balls[i]
                ball.vy += gravity * dt
                ball.x += ball.vx * dt
                ball.y += ball.vy * dt
                ball.angle += ball.angularVelocity * dt
                ball.angularVelocity *= (1f - friction * 0.2f)
                ball.vx *= linearDamping
                ball.vy *= linearDamping
            }

            // 2. Resolve flask boundary collisions
            for (i in balls.indices) {
                flask.resolveBallCollision(balls[i], restitution, friction)
            }

            // 3. Resolve ball vs ball collisions & detect merges
            val ballCount = balls.size
            for (i in 0 until ballCount) {
                val b1 = balls[i]
                for (j in i + 1 until ballCount) {
                    val b2 = balls[j]

                    val dx = b2.x - b1.x
                    val dy = b2.y - b1.y
                    val distSq = dx * dx + dy * dy
                    val minDist = b1.radius + b2.radius

                    if (distSq < minDist * minDist) {
                        val dist = sqrt(distSq)

                        // Check for merge condition (same level and neither is at max level or already merging)
                        if (b1.level == b2.level &&
                            b1.level < Ball.MAX_LEVEL &&
                            !b1.isMerging &&
                            !b2.isMerging
                        ) {
                            b1.isMerging = true
                            b2.isMerging = true
                            pendingMerges.add(MergeCandidate(b1, b2))
                            continue
                        }

                        // If either ball is already merging with another ball, skip collision response
                        if (b1.isMerging || b2.isMerging) {
                            continue
                        }

                        // Normal collision resolution (push apart + impulse)
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

                        // Mass-weighted position correction
                        val totalInvMass = b1.invMass + b2.invMass
                        if (totalInvMass > 0f) {
                            val push1 = (penetration * (b1.invMass / totalInvMass))
                            val push2 = (penetration * (b2.invMass / totalInvMass))

                            b1.x -= nx * push1
                            b1.y -= ny * push1
                            b2.x += nx * push2
                            b2.y += ny * push2

                            // Impulse resolution
                            val rvx = b2.vx - b1.vx
                            val rvy = b2.vy - b1.vy
                            val velAlongNormal = rvx * nx + rvy * ny

                            if (velAlongNormal < 0f) {
                                val impulseMag = -(1f + restitution) * velAlongNormal / totalInvMass
                                val ix = nx * impulseMag
                                val iy = ny * impulseMag

                                b1.vx -= ix * b1.invMass
                                b1.vy -= iy * b1.invMass
                                b2.vx += ix * b2.invMass
                                b2.vy += iy * b2.invMass

                                // Tangential friction and spin
                                val tx = -ny
                                val ty = nx
                                val velAlongTangent = rvx * tx + rvy * ty
                                val frictionImpulse = -velAlongTangent * friction / totalInvMass
                                val fix = tx * frictionImpulse
                                val fiy = ty * frictionImpulse

                                b1.vx -= fix * b1.invMass
                                b1.vy -= fiy * b1.invMass
                                b2.vx += fix * b2.invMass
                                b2.vy += fiy * b2.invMass

                                b1.angularVelocity += frictionImpulse * 0.002f
                                b2.angularVelocity -= frictionImpulse * 0.002f
                            }
                        }
                    }
                }
            }
        }

        // 4. Update visual animations (pop scale)
        for (i in balls.indices) {
            balls[i].update(0f) // scale updates smoothly
        }

        // 5. Process merges
        if (pendingMerges.isNotEmpty()) {
            for (merge in pendingMerges) {
                val a = merge.ballA
                val b = merge.ballB

                balls.remove(a)
                balls.remove(b)

                // Spawn new merged ball at midpoint
                val midX = (a.x + b.x) * 0.5f
                val midY = (a.y + b.y) * 0.5f
                val newLevel = a.level + 1

                val newBall = Ball(nextId.getAndIncrement(), midX, midY, newLevel, baseRadius).apply {
                    // Conserve average velocity with small upwards bounce
                    vx = (a.vx + b.vx) * 0.45f
                    vy = (a.vy + b.vy) * 0.45f - 60f
                    triggerPopEffect(1.38f)
                }

                balls.add(newBall)
                mergeListener?.onBallsMerged(newBall, a, b)
            }
            pendingMerges.clear()
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
                // Tangential vortex impulse (swirl) + slight upward lift
                val tx = -dy / dist
                val ty = dx / dist
                ball.vx += tx * 260f * strength + (Math.random().toFloat() - 0.5f) * 120f
                ball.vy += ty * 180f * strength - 150f * strength
                ball.angularVelocity += (Math.random().toFloat() - 0.5f) * 6f
            }
        }
    }
}
