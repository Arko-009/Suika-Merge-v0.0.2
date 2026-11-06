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
}

