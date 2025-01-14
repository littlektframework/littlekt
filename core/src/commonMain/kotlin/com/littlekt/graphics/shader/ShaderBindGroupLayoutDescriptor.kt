package com.littlekt.graphics.shader

import com.littlekt.graphics.webgpu.BindGroupLayoutDescriptor

/**
 * @author Colton Daily
 * @date 1/14/2025
 */
data class ShaderBindGroupLayoutDescriptor(
    val requiresExternal: Boolean,
    val bindGroupLayoutDescriptor: BindGroupLayoutDescriptor?,
) {
    init {
        if (requiresExternal) {
            if (bindGroupLayoutDescriptor != null)
                error(
                    "ShaderBindGroupLayoutDescriptor: ${bindGroupLayoutDescriptor.label} is set to requiresExternal as true. If this is the case then the bindGroupLayout must be passed in and not set here."
                )
        } else {
            if (bindGroupLayoutDescriptor == null) {
                error(
                    "ShaderBindGroupLayoutDescriptor: is set to requiresExternal as false but the bindGroupLayoutDescriptor is null. Either set requiresExternal to true and pass in the BindGroupLayout or set the descriptor."
                )
            }
        }
    }
}
