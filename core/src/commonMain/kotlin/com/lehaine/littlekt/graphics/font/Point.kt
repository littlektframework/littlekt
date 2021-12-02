package com.lehaine.littlekt.graphics.font

data class Point(
    val x: Int = 0,
    val y: Int = 0,
    val onCurve: Boolean = false,
    val lastPointOfContour: Boolean = false
)