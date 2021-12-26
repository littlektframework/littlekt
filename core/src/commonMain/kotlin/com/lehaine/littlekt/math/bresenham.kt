/**
 * @author Colton Daily
 * @date 12/26/2021
 */
package com.lehaine.littlekt.math


import kotlin.math.abs
import kotlin.math.floor


/**
 * Cast a ray from a point to another point and checks if each point along
 * the way can be passed via the [rayCanPass] parameter.
 *
 * https://github.com/deepnight/deepnightLibs/blob/master/src/dn/Bresenham.hx
 */
inline fun castRay(fromX: Int, fromY: Int, toX: Int, toY: Int, rayCanPass: (Int, Int) -> Boolean): Boolean {
    var x0 = fromX
    var y0 = fromY
    var x1 = toX
    var y1 = toY
    if (rayCanPass(x0, y0) && rayCanPass(x1, y1)) {
        val swapXY = abs(y1 - y0) > abs(x1 - x0)
        var temp: Int
        if (swapXY) {
            // swap x0 and y0
            temp = x0
            x0 = y0
            y0 = temp

            // swap x1 and y1
            temp = x1
            x1 = y1
            y1 = temp
        }
        if (x0 > x1) {
            // swap x0 and x1
            temp = x0
            x0 = x1
            x1 = temp

            // swap y0 and y1
            temp = y0
            y0 = y1
            y1 = temp
        }
        val dx = x1 - x0
        val dy = abs(y1 - y0)
        var error = floor(dx / 2.0)
        var y = y0
        val yStep = if (y0 < y1) 1 else -1

        for (x in x0 until (x1 + 1)) {
            if (swapXY && !rayCanPass(y, x) || !swapXY && !rayCanPass(x, y)) {
                return false
            }
            error -= dy
            if (error < 0) {
                y += yStep
                error += dx
            }
        }
        return true
    }
    return false
}

inline fun castThickRay(fromX: Int, fromY: Int, toX: Int, toY: Int, rayCanPass: (Int, Int) -> Boolean): Boolean {
    var x0 = fromX
    var y0 = fromY
    var x1 = toX
    var y1 = toY
    if (rayCanPass(x0, y0) && rayCanPass(x1, y1)) {
        val swapXY = abs(y1 - y0) > abs(x1 - x0)
        var temp: Int
        if (swapXY) {
            // swap x0 and y0
            temp = x0
            x0 = y0
            y0 = temp

            // swap x1 and y1
            temp = x1
            x1 = y1
            y1 = temp
        }
        if (x0 > x1) {
            // swap x0 and x1
            temp = x0
            x0 = x1
            x1 = temp

            // swap y0 and y1
            temp = y0
            y0 = y1
            y1 = temp
        }
        val dx = x1 - x0
        val dy = abs(y1 - y0)
        var error = floor(dx / 2.0)
        var y = y0
        val yStep = if (y0 < y1) 1 else -1

        if (swapXY) {
            for (x in x0 until (x1 + 1)) {
                if (!rayCanPass(y, x)) {
                    return false
                }
                error -= dy
                if (error < 0) {
                    if (x < x1 && (!rayCanPass(y + yStep, x) || !rayCanPass(y, x + 1))) {
                        return false
                    }
                    y += yStep
                    error += dx
                }
            }
        } else {
            for (x in x0 until (x1 + 1)) {
                if (!rayCanPass(y, x)) {
                    return false
                }

                error -= dy
                if (error < 0) {
                    if (x < x1 && (!rayCanPass(x, y + yStep) || !rayCanPass(x + 1, y))) {
                        return false
                    }
                    y += yStep
                    error += dx
                }
            }
        }
        return true
    }
    return false
}