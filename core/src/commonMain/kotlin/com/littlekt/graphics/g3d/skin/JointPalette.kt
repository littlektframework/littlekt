package com.littlekt.graphics.g3d.skin

import com.littlekt.Releasable
import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.g3d.util.ReleaseNotifier
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.UniqueId

/**
 * A GPU storage buffer of joint matrices that skinned instances index into. Used by [Skin] for a
 * single live pose and by [BakedAnimations] for every baked frame.
 *
 * @author Colton Daily
 * @date 11/11/2026
 */
internal class JointPalette(val device: Device, label: String, data: FloatBuffer) : Releasable {
    val id: Int = UniqueId.next<JointPalette>()
    val releaseNotifier = ReleaseNotifier()
    var lastUploadFrame = -1L

    private val buffer =
        device.createGPUFloatBuffer(label, data, BufferUsage.STORAGE or BufferUsage.COPY_DST)
    private val binding = BufferBinding(buffer)

    fun write(data: FloatBuffer) {
        device.queue.writeBuffer(buffer, data)
    }

    fun createBindGroup(shader: Shader): BindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                shader.getBindGroupLayoutByUsage(BindingUsage.SKIN),
                listOf(BindGroupEntry(0, binding)),
            )
        )

    override fun release() {
        releaseNotifier.notifyReleased(id)
        buffer.release()
    }
}
