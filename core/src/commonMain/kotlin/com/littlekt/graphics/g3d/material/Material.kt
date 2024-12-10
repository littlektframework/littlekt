package com.littlekt.graphics.g3d.material

import com.littlekt.Releasable
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.webgpu.*

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
open class Material(val shader: Shader) : Releasable {

    open fun upload(device: Device) = Unit

    override fun release() {}
}
