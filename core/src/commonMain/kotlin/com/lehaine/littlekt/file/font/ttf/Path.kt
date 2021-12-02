package com.lehaine.littlekt.file.font.ttf

import com.lehaine.littlekt.file.font.ttf.Path.CommandType.*
import com.lehaine.littlekt.math.Rect
import com.lehaine.littlekt.math.RectBuilder

/**
 * @author Colton Daily
 * @date 12/1/2021
 */
class Path {
    private val commands = mutableListOf<Command>()
    private var rectBuilder = RectBuilder()

    fun isEmpty() = commands.isEmpty()

    fun moveTo(x: Float, y: Float) {
        commands += Command(MOVE_TO, x, y)
    }

    fun lineTo(x: Float, y: Float) {
        commands += Command(LINE_TO, x, y)
    }

    fun curveTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) {
        commands += Command(CURVE_TO, x, y, x1, y1, x2, y2)
    }

    fun bezierCurveTo(x1: Float, y1: Float, x2: Float, y2: Float, x: Float, y: Float) = curveTo(x1, y1, x2, y2, x, y)

    fun quadTo(x1: Float, y1: Float, x: Float, y: Float) {
        commands += Command(QUADRATIC_CURVE_TO, x, y, x1, y1)
    }

    fun close() {
        commands += Command(CLOSE)
    }

    fun extend(path: Path) {
        commands += path.commands
    }

    fun extend(rect: Rect) {
        moveTo(rect.x, rect.y)
        lineTo(rect.x2, rect.y2)
        lineTo(rect.x2, rect.y)
        lineTo(rect.x2, rect.y2)
        lineTo(rect.x, rect.y2)
        close()
    }

    fun calculateBoundingBox(): Rect {
        var startX = 0f
        var startY = 0f
        var prevX = 0f
        var prevY = 0f
        commands.forEach { cmd ->
            when (cmd.type) {
                MOVE_TO -> {
                    rectBuilder.include(cmd.x, cmd.y)
                    startX = cmd.x
                    startY = cmd.y
                    prevX = cmd.x
                    prevY = cmd.y
                }
                LINE_TO -> {
                    rectBuilder.include(cmd.x, cmd.y)
                    prevX = cmd.x
                    prevY = cmd.y
                }
                CURVE_TO -> {
                    rectBuilder.addBezier(prevX, prevY, cmd.x1, cmd.y1, cmd.x2, cmd.y2, cmd.x, cmd.y)
                    prevX = cmd.x
                    prevY = cmd.y
                }
                QUADRATIC_CURVE_TO -> {
                    rectBuilder.addQuad(prevX, prevY, cmd.x1, cmd.y1, cmd.x, cmd.y)
                    prevX = cmd.x
                    prevY = cmd.y
                }
                CLOSE -> {
                    prevX = startX
                    prevY = startY
                }
            }
        }
        if (rectBuilder.isEmpty()) {
            rectBuilder.include(0, 0)
        }
        return rectBuilder.build()
    }

    enum class CommandType {
        MOVE_TO,
        LINE_TO,
        CURVE_TO,
        QUADRATIC_CURVE_TO,
        CLOSE
    }

    data class Command(
        val type: CommandType,
        val x: Float = 0f,
        val y: Float = 0f,
        val x1: Float = 0f,
        val y1: Float = 0f,
        val x2: Float = 0f,
        val y2: Float = 0f
    )
}