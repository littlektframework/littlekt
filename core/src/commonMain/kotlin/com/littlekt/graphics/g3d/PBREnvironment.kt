package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.light.PointLight
import com.littlekt.graphics.g3d.shader.blocks.Standard
import com.littlekt.graphics.g3d.util.CameraLightBuffers
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableVec2f
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/10/2025
 */
class PBREnvironment(override val buffers: CameraLightBuffers) : Environment(buffers) {
    private var directionalLight = DirectionalLight()
    private var ambientLight = AmbientLight()
    private val pointLights = mutableListOf<PointLight>()

    private val outputSize = MutableVec2f()
    private var far = 0f
    private var near = 0f

    private val device = buffers.device
    private val boundsShader = Shader(device, Standard.Cluster.ClusterBoundsComputeShader())

    private val boundsPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                boundsShader.getOrCreatePipelineLayout(),
                ProgrammableStage(boundsShader.shaderModule, boundsShader.computeEntryPoint),
                label = "Computer Cluster Bounds Compute Pipeline",
            )
        )
    private val boundsCameraBindGroup =
        boundsShader.createBindGroup(BindingUsage.CAMERA, buffers.cameraUniformBufferBinding)
            ?: error("Unable to create ClusterBounds Camera BindGroup")
    private val boundsStorageBindGroup =
        boundsShader.createBindGroup(
            BindingUsage.CLUSTER_BOUNDS,
            buffers.clusterBuffers.clusterBoundsStorageBufferBinding,
        ) ?: error("Unable to create ClusterBounds Storage BindGroup")

    private val lightsShader = Shader(device, Standard.Cluster.ClusterLightsComputeShader())
    private val lightsPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                lightsShader.getOrCreatePipelineLayout(),
                ProgrammableStage(lightsShader.shaderModule, lightsShader.computeEntryPoint),
                label = "Compute Cluster Lights Compute Pipeline",
            )
        )
    private val lightsBindGroup =
        device.createBindGroup(
            BindGroupDescriptor(
                lightsShader.getBindGroupLayoutByUsage(BindingUsage.CLUSTER_LIGHTS),
                listOf(
                    BindGroupEntry(0, buffers.cameraUniformBufferBinding),
                    BindGroupEntry(1, buffers.clusterBuffers.clusterBoundsStorageBufferBinding),
                    BindGroupEntry(2, buffers.clusterBuffers.clusterLightsStorageBufferBinding),
                    BindGroupEntry(3, buffers.lightBuffer.bufferBinding),
                ),
            )
        )

    override fun update(camera: Camera, dt: Duration) {
        super.update(camera, dt)
        updateLights()
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
        computePassEncoder.setBindGroup(0, boundsCameraBindGroup)
        computePassEncoder.setBindGroup(1, boundsStorageBindGroup)
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

    private fun updateLights() {
        updateAmbientLight()
        updateDirectionLight()
        updatePointLights()
        buffers.lightBuffer.update()
    }

    private fun updateAmbientLight() {
        buffers.lightBuffer.ambient(ambientLight.color)
    }

    private fun updateDirectionLight() {
        buffers.lightBuffer.dirDirection(directionalLight.direction)
        buffers.lightBuffer.dirIntensity(directionalLight.intensity)
        buffers.lightBuffer.dirColor(directionalLight.color)
    }

    private fun updatePointLights() {
        buffers.lightBuffer.resetLightCount()

        pointLights.forEachIndexed { index, light ->
            buffers.lightBuffer.pointLight(
                index = index,
                position = light.position,
                color = light.color,
                intensity = light.intensity,
                range = light.range,
            )
        }
    }

    fun setDirectionalLight(light: DirectionalLight) {
        directionalLight = light
    }

    fun setAmbientLight(light: AmbientLight) {
        ambientLight = light
    }

    fun addPointLight(pointLight: PointLight) {
        pointLights += pointLight
    }

    fun removePointLight(pointLight: PointLight) {
        pointLights -= pointLight
    }
}
