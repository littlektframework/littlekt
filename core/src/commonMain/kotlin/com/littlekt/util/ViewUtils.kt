package com.littlekt.util

import com.littlekt.graphics.Camera
import com.littlekt.math.Rect
import kotlin.math.abs

/**
 * Calculate the view bounds from the corresponding camera and update this [Rect] with the results.
 *
 * @param camera the camera view
 */
fun Rect.calculateViewBounds(camera: Camera) {
    val width = camera.virtualWidth * camera.zoom
    val height = camera.virtualHeight * camera.zoom
    val w = width * abs(camera.up.y) + height * abs(camera.up.x)
    val h = height * abs(camera.up.y) + width * abs(camera.up.x)
    this.x = camera.position.x - w / 2
    this.y = camera.position.y - h / 2
    this.width = w
    this.height = h
}
