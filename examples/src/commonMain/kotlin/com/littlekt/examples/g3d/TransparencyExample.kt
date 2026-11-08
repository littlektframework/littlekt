package com.littlekt.examples.g3d

import com.littlekt.examples.*
import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.file.vfs.TextureOptions
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.Node3D
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.webgpu.*
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.resources.Textures
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds
import kotlin.math.cos
import kotlin.math.sin

/**
 * An example of transparent materials. Instanced glass panes and cubes, each with different
 * materials, are sorted back to front by the [ModelBatch] while the camera orbits around them.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class TransparencyExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        val environment =
            PBREnvironment(device).apply {
                setDirectionalLight(
                    DirectionalLight(
                        color = Color(1f, 0.95f, 0.85f),
                        direction = MutableVec3f(0.4f, 1f, 0.3f).norm(),
                        intensity = 2.5f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.1f, 0.1f, 0.12f)))
            }

        val checkered =
            resourcesVfs["checkered.png"].readTexture(
                options =
                    TextureOptions(
                        format = preferredFormat.toSRGB(),
                        samplerDescriptor =
                            SamplerDescriptor(
                                addressModeU = AddressMode.REPEAT,
                                addressModeV = AddressMode.REPEAT,
                            ),
                    )
            )
        val floorMesh =
            fullIndexedMesh().generate {
                vertexModFun = { uv.set(position.x, position.z) }
                grid {
                    sizeX = 30f
                    sizeY = 30f
                }
            }
        val floorMaterial = PBRMaterial(device, metallicFactor = 0f, baseColorTexture = checkered)
        val floor = MeshNode(listOf(MeshPrimitive(floorMesh, floorMaterial)))

        fun glass(color: Color) =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                roughnessFactor = 0.2f,
                baseColorTexture = Textures.textureWhite,
                baseColorFactor = color,
                transparent = true,
                doubleSided = true,
                depthWrite = false,
            )

        val nodes = mutableListOf<Node3D>()
        val paneMesh =
            fullIndexedMesh().generate {
                cube {
                    width = 3f
                    height = 3f
                    depth = 0.05f
                }
            }
        val paneMaterial = glass(Color(1f, 1f, 1f, 0.35f))
        val pane = MeshNode(listOf(MeshPrimitive(paneMesh, paneMaterial)))
        val paneColors =
            listOf(
                Color(1f, 0.3f, 0.3f, 1f),
                Color(0.3f, 1f, 0.3f, 1f),
                Color(0.3f, 0.5f, 1f, 1f),
                Color(1f, 1f, 0.3f, 1f),
            )
        for (row in 0 until 4) {
            for (column in 0 until 3) {
                nodes +=
                    pane.copy().apply {
                        translate((column - 1) * 4f, 1.5f, (row - 1.5f) * 3f)
                        color(paneColors[row])
                    }
            }
        }
        val cubeMesh = fullIndexedMesh().generate { cube {} }
        val cubeMaterials =
            listOf(glass(Color(1f, 0.5f, 0f, 0.5f)), glass(Color(0.7f, 0f, 1f, 0.5f)))
        for (i in 0 until 6) {
            nodes +=
                MeshNode(listOf(MeshPrimitive(cubeMesh, cubeMaterials[i % cubeMaterials.size])))
                    .apply { translate((i % 3 - 1) * 4f, 0.75f, (i / 3 * 2 - 1) * 3f) }
        }

        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        var orbit = 0.6f
        onUpdate { dt ->
            orbit += dt.seconds * 0.25f
            camera.position.set(cos(orbit) * 14f, 5f, sin(orbit) * 14f)
            camera.up.set(Vec3f.Y_AXIS)
            camera.lookAt(0f, 1f, 0f)
            floor.update(dt)
            nodes.fastForEach { it.update(dt) }
        }

        onUpdate { dt ->
            renderFrame("Transparency Command Encoder") { frame, commandEncoder ->
                camera.update()
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            listOf(colorAttachment(frame, clearColor)),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Transparency Render Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                nodes.fastForEach { modelBatch.render(it, camera, environment) }
                modelBatch.flush(renderPass, camera, dt)
                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            modelBatch.release()
            environment.release()
            floorMesh.release()
            floorMaterial.release()
            paneMesh.release()
            paneMaterial.release()
            cubeMesh.release()
            cubeMaterials.fastForEach { it.release() }
            checkered.release()
        }
    }
}
