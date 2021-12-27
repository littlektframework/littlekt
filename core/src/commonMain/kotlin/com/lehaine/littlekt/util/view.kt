package com.lehaine.littlekt.util

import com.lehaine.littlekt.graphics.Camera
import com.lehaine.littlekt.math.Rect
import kotlin.math.abs

/**
 * @author Colton Daily
 * @date 12/27/2021
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