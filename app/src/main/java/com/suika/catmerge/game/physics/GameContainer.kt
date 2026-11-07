package com.suika.catmerge.game.physics

import android.graphics.Canvas

enum class ContainerType(val displayName: String, val icon: String, val tagline: String) {
    FLASK("Flask", "🧪", "Chemistry Flask with Vertical Neck"),
    BOX("Box", "📦", "Arcade Rectangular Open Crate"),
    HEXAGON("Hexagon", "⬡", "Cyber Funnel with Sloped Sliding Walls")
}

interface GameContainer {
    val containerType: ContainerType

    val centerX: Float
    val circleCenterY: Float
    val circleRadius: Float
    val neckHalfWidth: Float
    val neckTopY: Float
    val neckBottomY: Float
    val dangerLineY: Float

    fun setup(viewWidth: Float, viewHeight: Float)
    fun update(deltaTime: Float)
    fun resolveBallCollision(ball: Ball, restitution: Float = 0.25f, friction: Float = 0.03f): Boolean
    fun draw(canvas: Canvas, isInDanger: Boolean)
}

object ContainerFactory {
    fun create(type: ContainerType): GameContainer {
        return when (type) {
            ContainerType.FLASK -> FlaskContainer()
            ContainerType.BOX -> BoxContainer()
            ContainerType.HEXAGON -> HexagonContainer()
        }
    }
}

