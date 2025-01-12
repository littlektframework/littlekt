package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.shader.ClusterBoundsShader
import com.littlekt.graphics.g3d.shader.ClusterLightsShader
import com.littlekt.graphics.g3d.util.CameraLightBuffers
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableVec2f
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/10/2025
 */
class PBREnvironment(override val buffers: CameraLightBuffers) : Environment(buffers) {

    private val outputSize = MutableVec2f()
    private var far = 0f
    private var near = 0f

    private val device = buffers.device
    private val boundsShader = ClusterBoundsShader(device)
    private val boundsBindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                boundsShader.layouts[0],
                listOf(BindGroupEntry(0, buffers.clusterBuffers.clusterBoundsStorageBufferBinding)),
            )
        )
    private val boundsPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                device.createPipelineLayout(
                    PipelineLayoutDescriptor(
                        listOf(buffers.bindGroupLayout, boundsShader.layouts[0])
                    )
                ),
                ProgrammableStage(boundsShader.shaderModule, boundsShader.computeEntryPoint),
            )
        )
    private val lightsShader = ClusterLightsShader(device)
    private val lightsBindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                lightsShader.layouts[0],
                listOf(
                    BindGroupEntry(0, buffers.cameraUniformBufferBinding),
                    BindGroupEntry(1, buffers.clusterBuffers.clusterBoundsStorageBufferBinding),
                    BindGroupEntry(2, buffers.clusterBuffers.clusterLightsStorageBufferBinding),
                    BindGroupEntry(3, buffers.lightBuffer.bufferBinding),
                ),
            )
        )
    private val lightsPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                lightsShader.pipelineLayout,
                ProgrammableStage(lightsShader.shaderModule, lightsShader.computeEntryPoint),
            )
        )

    override fun update(camera: Camera, dt: Duration) {
        super.update(camera, dt)
        updateClusterBounds(camera)
        updateClusterLights()
    }

    private fun updateClusterBounds(camera: Camera) {
        if (
            outputSize.x == camera.virtualWidth &&
                outputSize.y == camera.virtualHeight &&
                near == camera.near &&
                far == camera.far
        ) {
            return
        }

        outputSize.set(camera.virtualWidth, camera.virtualHeight)
        near = camera.near
        far = camera.far

        val commandEncoder = device.createCommandEncoder("Cluster Bounds Command Encoder")
        val computePassEncoder = commandEncoder.beginComputePass("Cluster Bounds Compute Pass")
        computePassEncoder.setPipeline(boundsPipeline)
        computePassEncoder.setBindGroup(0, buffers.bindGroup)
        computePassEncoder.setBindGroup(1, boundsBindGroup)
        computePassEncoder.dispatchWorkgroups(
            buffers.clusterBuffers.workGroupSizeX,
            buffers.clusterBuffers.workGroupSizeY,
            buffers.clusterBuffers.workGroupSizeZ,
        )
        computePassEncoder.end()
        computePassEncoder.release()

        device.queue.submit(commandEncoder.finish())

        commandEncoder.release()
    }

    private fun updateClusterLights() {
        buffers.clusterBuffers.resetClusterLightsOffsetToZero()
        val commandEncoder = device.createCommandEncoder("Cluster Lights Command Encoder")

        val computePassEncoder = commandEncoder.beginComputePass("Cluster Lights Compute Pass")
        computePassEncoder.setPipeline(lightsPipeline)
        computePassEncoder.setBindGroup(0, lightsBindGroup)
        computePassEncoder.dispatchWorkgroups(
            buffers.clusterBuffers.workGroupSizeX,
            buffers.clusterBuffers.workGroupSizeY,
            buffers.clusterBuffers.workGroupSizeZ,
        )
        computePassEncoder.end()
        computePassEncoder.release()

        device.queue.submit(commandEncoder.finish())

        commandEncoder.release()
    }
}
