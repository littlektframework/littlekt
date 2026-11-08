package com.littlekt.graphics.g3d.util

import com.littlekt.file.FloatBuffer
import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.ibl.EnvironmentMap
import com.littlekt.graphics.g3d.shadow.DirectionalShadowMap
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.util.CameraBuffersViaCamera
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.seconds
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/5/2025
 */
class CameraLightBuffers(
    val device: Device,
    val lightBuffer: LightBuffer,
    val clusterBuffers: ClusterBuffers = ClusterBuffers(device),
    val shadowMap: DirectionalShadowMap? = null,
    val environmentMap: EnvironmentMap? = null,
) : CameraBuffersViaCamera {
    private val camFloatBuffer = FloatBuffer(BUFFER_SIZE) // 56 floats required

    /**
     * The [GPUBuffer] that holds the camera data.
     *
     * @see update
     */
    private val cameraUniformBuffer =
        device.createGPUFloatBuffer(
            "camera",
            camFloatBuffer,
            BufferUsage.UNIFORM or BufferUsage.COPY_DST,
        )
    override val cameraDynamicSize: Int = 1

    /** The [BufferBinding] for [cameraUniformBufferBinding]. */
    override val cameraUniformBufferBinding = BufferBinding(cameraUniformBuffer)

    /** The camera uniform bind group. */
    private var bindGroups = mutableMapOf<Int, BindGroup>()

    override val bindingUsage: BindingUsage = run {
        var usage = BindingUsage.CAMERA or BindingUsage.LIGHT or BindingUsage.CLUSTER_LIGHTS
        if (shadowMap != null) usage = usage or BindingUsage.SHADOW
        if (environmentMap != null) usage = usage or BindingUsage.IBL
        usage
    }

    override fun getOrCreateBindGroup(shader: Shader): BindGroup {
        return bindGroups[shader.id]
            ?: createBindGroup(shader)?.also { bindGroups[shader.id] = it }
            ?: error("Unable to create bind group for shader: ${shader.id}!")
    }

    private fun createBindGroup(shader: Shader): BindGroup? {
        val resources =
            mutableListOf<IntoBindingResource>(
                cameraUniformBufferBinding,
                lightBuffer.bufferBinding,
                clusterBuffers.clusterLightsStorageBufferBinding,
            )
        shadowMap?.let {
            resources += it.view
            resources += it.sampler
            resources += it.uniformBufferBinding
        }
        environmentMap?.let {
            resources += it.irradianceView
            resources += it.prefilteredView
            resources += it.brdfView
            resources += it.sampler
            resources += it.uniformBufferBinding
        }
        return shader.createBindGroup(bindingUsage, *resources.toTypedArray())
    }

    override fun update(camera: Camera, dt: Duration, dynamicOffset: Long) {
        camFloatBuffer.put(camera.projection.data, dstOffset = 0)
        camFloatBuffer.put(camera.invProj.data)
        camFloatBuffer.put(camera.view.data)
        camFloatBuffer.put(camera.position.fields)
        camFloatBuffer.put(dt.seconds)
        camFloatBuffer.put(camera.virtualWidth)
        camFloatBuffer.put(camera.virtualHeight)
        camFloatBuffer.put(camera.near)
        camFloatBuffer.put(camera.far)
        device.queue.writeBuffer(cameraUniformBuffer, camFloatBuffer)
    }

    override fun release() {
        bindGroups.values.forEach { it.release() }
        cameraUniformBuffer.release()
    }

    companion object {
        /** Size in total floats (NOT BYTE SIZE). */
        const val BUFFER_SIZE = 56
    }
}
