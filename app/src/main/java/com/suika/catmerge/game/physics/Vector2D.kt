package com.suika.catmerge.game.physics

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 2D Vector representation for high-performance physics calculations.
 */
data class Vector2D(var x: Float = 0f, var y: Float = 0f) {

    fun set(newX: Float, newY: Float): Vector2D {
        x = newX
        y = newY
        return this
    }

    fun set(other: Vector2D): Vector2D {
        x = other.x
        y = other.y
        return this
    }

    fun add(other: Vector2D): Vector2D {
        x += other.x
        y += other.y
        return this
    }

    fun add(dx: Float, dy: Float): Vector2D {
        x += dx
        y += dy
        return this
    }

    fun sub(other: Vector2D): Vector2D {
        x -= other.x
        y -= other.y
        return this
    }

    fun sub(dx: Float, dy: Float): Vector2D {
        x -= dx
        y -= dy
        return this
    }

    fun mul(scalar: Float): Vector2D {
        x *= scalar
        y *= scalar
        return this
    }

    fun lengthSquared(): Float = x * x + y * y

    fun length(): Float = sqrt(lengthSquared())

    fun distanceSquared(other: Vector2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return dx * dx + dy * dy
    }

    fun distance(other: Vector2D): Float = sqrt(distanceSquared(other))

    fun distance(ox: Float, oy: Float): Float {
        val dx = x - ox
        val dy = y - oy
        return sqrt(dx * dx + dy * dy)
    }

    fun normalize(): Vector2D {
        val len = length()
        if (len > 0.00001f) {
            x /= len
            y /= len
        }
        return this
    }

    fun dot(other: Vector2D): Float = x * other.x + y * other.y

    fun dot(ox: Float, oy: Float): Float = x * ox + y * oy

    fun rotate(angleRad: Float): Vector2D {
        val cosA = cos(angleRad)
        val sinA = sin(angleRad)
        val nx = x * cosA - y * sinA
        val ny = x * sinA + y * cosA
        x = nx
        y = ny
        return this
    }

    operator fun plus(other: Vector2D): Vector2D = Vector2D(x + other.x, y + other.y)
    operator fun minus(other: Vector2D): Vector2D = Vector2D(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vector2D = Vector2D(x * scalar, y * scalar)

    fun copy(): Vector2D = Vector2D(x, y)
}
