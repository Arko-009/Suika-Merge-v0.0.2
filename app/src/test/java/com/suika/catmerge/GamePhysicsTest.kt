package com.suika.catmerge

import com.suika.catmerge.game.physics.Ball
import com.suika.catmerge.game.physics.FlaskContainer
import com.suika.catmerge.game.physics.PhysicsEngine
import com.suika.catmerge.game.physics.Vector2D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GamePhysicsTest {

    @Test
    fun testVectorOperations() {
        val v1 = Vector2D(3f, 4f)
        assertEquals(5f, v1.length(), 0.001f)

        v1.normalize()
        assertEquals(0.6f, v1.x, 0.001f)
        assertEquals(0.8f, v1.y, 0.001f)
        assertEquals(1f, v1.length(), 0.001f)

        val v2 = Vector2D(1f, 0f)
        val v3 = Vector2D(0f, 1f)
        assertEquals(0f, v2.dot(v3), 0.001f)
    }

    @Test
    fun testBallRadiusProgression() {
        val baseR = 30f
        val r0 = Ball.calculateRadius(0, baseR)
        val r1 = Ball.calculateRadius(1, baseR)
        val r13 = Ball.calculateRadius(13, baseR)

        assertEquals(30f, r0, 0.01f)
        assertTrue("Higher level ball must have larger radius", r1 > r0)
        assertTrue("Max level ball must be much larger", r13 > r1 * 3f)
    }

    @Test
    fun testScoreCalculation() {
        assertEquals(0, Ball.calculateScore(0))
        assertEquals(10, Ball.calculateScore(1))
        assertEquals(20, Ball.calculateScore(2))
        assertEquals(40, Ball.calculateScore(3))
        assertEquals(40960, Ball.calculateScore(13))
    }

    @Test
    fun testPhysicsEngineMergeDetection() {
        val flask = FlaskContainer()
        // Setup mock flask dimensions
        flask.setup(800f, 1200f)

        val engine = PhysicsEngine(flask, baseRadius = 30f)

        // Add two Level 0 balls in overlapping position inside chamber
        val b1 = engine.addBall(400f, 850f, 0)
        val b2 = engine.addBall(405f, 850f, 0)

        var mergedLevel = -1
        engine.mergeListener = object : PhysicsEngine.MergeListener {
            override fun onBallsMerged(newBall: Ball, ballA: Ball, ballB: Ball) {
                mergedLevel = newBall.level
            }
        }

        // Run one physics step
        engine.step(0.016f)

        // Merging should have triggered into level 1
        assertEquals("Merging two level 0 balls should yield level 1", 1, mergedLevel)
        assertEquals(1, engine.balls.size)
        assertEquals(1, engine.balls[0].level)
    }

    @Test
    fun testDifferentLevelBallsDoNotOverlap() {
        val flask = FlaskContainer()
        flask.setup(800f, 1200f)

        val engine = PhysicsEngine(flask, baseRadius = 30f)

        // Level 0 (r=30) and Level 1 (r=34.8) placed initially overlapping
        val b1 = engine.addBall(400f, 850f, 0)
        val b2 = engine.addBall(400f, 860f, 1)

        // Run multiple steps to let PBD separate them
        for (i in 0 until 10) {
            engine.step(0.016f)
        }

        val dx = b2.x - b1.x
        val dy = b2.y - b1.y
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        val minDist = b1.radius + b2.radius

        assertTrue("Distance between different level balls must be >= sum of radii: dist=$dist, min=$minDist", dist >= minDist - 0.5f)
        assertEquals(2, engine.balls.size)
    }

    @Test
    fun testVisualScaleMatchesPhysicsHitbox() {
        val ball = Ball(1L, 0f, 0f, 0, 30f)
        assertEquals("Visual scale must default to 1.0f to prevent visual overlap", 1.0f, ball.visualScale, 0.001f)
    }

    @Test
    fun testBallEntersChamberSmoothlyWithoutSpikeOrSpin() {
        val flask = FlaskContainer()
        flask.setup(800f, 1200f)
        val engine = PhysicsEngine(flask, baseRadius = 30f)

        // Drop a ball from the neck top
        val ball = engine.addBall(flask.centerX, flask.neckTopY + 10f, 0)

        // Simulate 60 frames (1 second of falling down the neck into the chamber)
        for (i in 0 until 60) {
            engine.step(0.016f)

            // Verify velocity is always smooth and never supersonic
            val speed = kotlin.math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy)
            assertTrue("Ball speed must never glitch spike: speed=$speed at frame $i", speed <= 1200f)

            // Verify angular velocity is natural and not crazy spinning
            assertTrue("Ball spin must remain controlled: spin=${ball.angularVelocity} at frame $i", kotlin.math.abs(ball.angularVelocity) <= 10f)
        }

        // Verify the ball successfully reached the chamber (y > neckBottomY)
        assertTrue("Ball must enter the lower chamber smoothly", ball.y > flask.neckBottomY)
    }
}

