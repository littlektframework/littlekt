package com.lehaine.littlekt

/**
 * @author Colton Daily
 * @date 9/28/2021
 */
/**
 * Temporal Unit used for animation descriptions
 */
typealias Milliseconds = Long

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
 * Degree: value between 0 and 360
 */
typealias Degree = Number

/**
 * Coordinate in the related context (in general: world unit)
 */
typealias Coordinate = Number

/**
 * Pixel unit. In general related to an image or the screen.
 */
typealias Pixel = Int

/**
 * Ratio. Used in general to deal with screen size.
 */
typealias Ratio = Float

fun Number.toPercent(): Float {
    val v = this.toFloat()
    require(v in 0.0..1.0)
    return v
}

/**
 * Position in the screen device
 */
typealias DevicePosition = Float

/**
 * Position in the game device (ie: in the game viewport)
 */
typealias GamePosition = Float