package com.littlekt.graphics.g3d

import com.littlekt.graphics.Camera
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.light.PointLight
import com.littlekt.graphics.g3d.shader.blocks.Standard
import com.littlekt.graphics.g3d.util.CameraLightBuffers
import com.littlekt.graphics.shader.Shader
import com.littlekt.graphics.util.BindingUsage
import com.littlekt.graphics.util.CameraBuffersViaCamera
import com.littlekt.graphics.webgpu.*
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.datastructure.fastForEachWithIndex
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/10/2025
 */
class PBREnvironment(override val buffers: CameraLightBuffers) : Environment(buffers) {
    private var directionalLight = DirectionalLight()
    private var ambientLight = AmbientLight()
    private val pointLights = mutableListOf<PointLight>()

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

    private val lightsShader = Shader(device, Standard.Cluster.ClusterLightsComputeShader())
    private val lightsPipeline =
        device.createComputePipeline(
            ComputePipelineDescriptor(
                lightsShader.getOrCreatePipelineLayout(),
                ProgrammableStage(lightsShader.shaderModule, lightsShader.computeEntryPoint),
                label = "Compute Cluster Lights Compute Pipeline",
            )
        )

    private val clusterViews = mutableListOf<ClusterView>()

    override fun createViewBuffers(): CameraBuffersViaCamera =
        CameraLightBuffers(device, buffers.lightBuffer)

    override fun updateView(
        index: Int,
        view: CameraBuffersViaCamera,
        camera: Camera,
        dt: Duration,
    ) {
        super.updateView(index, view, camera, dt)
        if (index == 0) {
            updateLights()
        }
        while (clusterViews.size <= index) {
            clusterViews += ClusterView(getView(clusterViews.size) as CameraLightBuffers)
        }
        val clusterView = clusterViews[index]
        clusterView.updateClusterBounds(camera)
        clusterView.updateClusterLights()
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

        pointLights.fastForEachWithIndex { index, light ->
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

    override fun release() {
        clusterViews.fastForEach { it.release() }
        clusterViews.clear()
        for (i in 0 until totalViews) {
            (getView(i) as CameraLightBuffers).clusterBuffers.release()
        }
        buffers.lightBuffer.release()
        super.release()
        boundsPipeline.release()
        lightsPipeline.release()
        boundsShader.release()
        lightsShader.release()
    }

    private inner class ClusterView(private val viewBuffers: CameraLightBuffers) {
        private var width = 0f
        private var height = 0f
        private val projection = FloatArray(16)

        private val boundsCameraBindGroup =
            boundsShader.createBindGroup(
                BindingUsage.CAMERA,
                viewBuffers.cameraUniformBufferBinding,
            ) ?: error("Unable to create ClusterBounds Camera BindGroup")
        private val boundsStorageBindGroup =
            boundsShader.createBindGroup(
                BindingUsage.CLUSTER_BOUNDS,
                viewBuffers.clusterBuffers.clusterBoundsStorageBufferBinding,
            ) ?: error("Unable to create ClusterBounds Storage BindGroup")

        private val lightsBindGroup =
            device.createBindGroup(
                BindGroupDescriptor(
                    lightsShader.getBindGroupLayoutByUsage(BindingUsage.CLUSTER_LIGHTS),
                    listOf(
                        BindGroupEntry(0, viewBuffers.cameraUniformBufferBinding),
                        BindGroupEntry(
                            1,
                            viewBuffers.clusterBuffers.clusterBoundsStorageBufferBinding,
                        ),
                        BindGroupEntry(
                            2,
                            viewBuffers.clusterBuffers.clusterLightsStorageBufferBinding,
                        ),
                        BindGroupEntry(3, viewBuffers.lightBuffer.bufferBinding),
                    ),
                )
            )

        fun updateClusterBounds(camera: Camera) {
            if (
                width == camera.virtualWidth &&
                    height == camera.virtualHeight &&
                    projection.contentEquals(camera.projection.data)
            ) {
                return
            }

            width = camera.virtualWidth
            height = camera.virtualHeight
            camera.projection.data.copyInto(projection)

            val commandEncoder = device.createCommandEncoder("Cluster Bounds Command Encoder")
            val computePassEncoder = commandEncoder.beginComputePass("Cluster Bounds Compute Pass")
            computePassEncoder.setPipeline(boundsPipeline)
            computePassEncoder.setBindGroup(0, boundsCameraBindGroup)
            computePassEncoder.setBindGroup(1, boundsStorageBindGroup)
            computePassEncoder.dispatchWorkgroups(
                viewBuffers.clusterBuffers.workGroupSizeX,
                viewBuffers.clusterBuffers.workGroupSizeY,
                viewBuffers.clusterBuffers.workGroupSizeZ,
            )
            computePassEncoder.end()
            computePassEncoder.release()

            val commandBuffer = commandEncoder.finish()
            device.queue.submit(commandBuffer)
            commandBuffer.release()
            commandEncoder.release()
        }

        fun updateClusterLights() {
            viewBuffers.clusterBuffers.resetClusterLightsOffsetToZero()
            val commandEncoder = device.createCommandEncoder("Cluster Lights Command Encoder")

            val computePassEncoder = commandEncoder.beginComputePass("Cluster Lights Compute Pass")
            computePassEncoder.setPipeline(lightsPipeline)
            computePassEncoder.setBindGroup(0, lightsBindGroup)
            computePassEncoder.dispatchWorkgroups(
                viewBuffers.clusterBuffers.workGroupSizeX,
                viewBuffers.clusterBuffers.workGroupSizeY,
                viewBuffers.clusterBuffers.workGroupSizeZ,
            )
            computePassEncoder.end()
            computePassEncoder.release()

            val commandBuffer = commandEncoder.finish()
            device.queue.submit(commandBuffer)
            commandBuffer.release()
            commandEncoder.release()
        }

        fun release() {
            boundsCameraBindGroup.release()
            boundsStorageBindGroup.release()
            lightsBindGroup.release()
        }
    }
}
