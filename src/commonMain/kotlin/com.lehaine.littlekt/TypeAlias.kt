package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 9/28/2021
 */

/**
 * Temporal unit
 */
typealias Seconds = Float

/**
 * Percent: values between 0.0 and 1.0
 */
typealias Percent = Number

/**
 * Bytemask: value can be changed using binary operation.
 */
typealias ByteMask = Int

/**
 * Pixel unit. In general related to an image or the screen.
 */
typealias Pixel = Int


fun Number.toPercent(): Float {
    val v = this.toFloat()
    require(v in 0.0..1.0)
    return v
}