package com.littlekt.graphics.g3d.util

import com.littlekt.graphics.VertexAttribute
import com.littlekt.graphics.g3d.material.Material
import com.littlekt.graphics.webgpu.Device

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
interface MaterialProvider {
    fun getMaterial(device: Device, attributes: List<VertexAttribute>): Material
}
