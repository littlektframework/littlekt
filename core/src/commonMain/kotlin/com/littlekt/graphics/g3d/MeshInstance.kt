package com.littlekt.graphics.g3d

import com.littlekt.graphics.webgpu.Device

/**
 * @author Colton Daily
 * @date 12/8/2024
 */
class MeshInstance : Node3D() {

    override fun update(device: Device) {
        super.update(device)
        if (parent is MeshNode) {
            error("MeshInstance must be parented to a MeshNode!")
        }
    }
}
