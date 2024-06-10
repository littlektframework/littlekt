package com.littlekt.graphics.g2d.tilemap.ldtk

/**
 * A id reference data holder for [LDtkEntity].
 *
 * @author Colton Daily
 * @date 3/3/2022
 */
data class LDtkEntityRef(
    val entityIid: String,
    val layerIid: String,
    val levelIid: String,
    val worldIid: String
)
