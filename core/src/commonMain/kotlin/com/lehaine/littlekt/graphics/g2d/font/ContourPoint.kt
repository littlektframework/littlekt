package com.lehaine.littlekt.graphics.g2d.font

data class ContourPoint(
    val x: Int = 0,
    val y: Int = 0,
    val onCurve: Boolean = false,
    val lastPointOfContour: Boolean = false
)