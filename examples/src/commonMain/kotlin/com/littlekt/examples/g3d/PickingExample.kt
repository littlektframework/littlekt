package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
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
import com.littlekt.input.Pointer
import com.littlekt.math.MutableQuaternion
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Ray
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.radians
import com.littlekt.math.spatial.BoundingBox
import com.littlekt.math.spatial.SpatialHash
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * An example of picking & spatial queries. A thousand moving units are kept in a [SpatialHash].
 * The unit under the cursor is found with a raycast that tests each unit's triangles, and clicking
 * the ground moves a marker that highlights every unit within its radius.
 *
 * @author Colton Daily
 * @date 11/12/2026
 */
class PickingExample(context: Context) : ContextListener(context) {

    private class Walker(val node: Node3D, val center: Vec3f, val radius: Float, val speed: Float) {
        var angle = Random.nextFloat() * 6.28f
        val bounds = BoundingBox()
        val rotation = MutableQuaternion()
        var color = Color.WHITE
    }

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.position.set(0f, 38f, 32f)
        camera.up.set(Vec3f.Y_AXIS)
        camera.lookAt(0f, 0f, 2f)
        val environment =
            PBREnvironment(device).apply {
                setDirectionalLight(
                    DirectionalLight(
                        color = Color(1f, 0.95f, 0.85f),
                        direction = MutableVec3f(0.4f, 1f, 0.3f).norm(),
                        intensity = 2.5f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.15f, 0.15f, 0.18f)))
            }

        val floorMesh = fullIndexedMesh().generate {
            grid {
                sizeX = 80f
                sizeY = 80f
            }
        }
        val floorMaterial =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                baseColorTexture = Textures.textureWhite,
                baseColorFactor = Color(0.25f, 0.27f, 0.3f, 1f),
            )
        val floor = MeshNode(listOf(MeshPrimitive(floorMesh, floorMaterial)))

        val unitMaterial =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                roughnessFactor = 0.6f,
                baseColorTexture = Textures.textureWhite,
            )
        val cubeMesh = fullIndexedMesh().generate {
            cube {
                width = 0.8f
                height = 1.6f
                depth = 0.8f
            }
        }
        val sphereMesh = fullIndexedMesh().generate { uvSphere { radius = 0.6f } }
        val cube = MeshNode(listOf(MeshPrimitive(cubeMesh, unitMaterial)))
        val sphere = MeshNode(listOf(MeshPrimitive(sphereMesh, unitMaterial)))

        val units =
            List(1000) { i ->
                val node = (if (i % 3 == 0) sphere else cube).copy()
                Walker(
                    node,
                    Vec3f(Random.nextFloat() * 70f - 35f, 0.8f, Random.nextFloat() * 70f - 35f),
                    radius = 0.5f + Random.nextFloat() * 2f,
                    speed = (0.3f + Random.nextFloat()) * if (Random.nextBoolean()) 1f else -1f,
                )
            }
        val hash = SpatialHash<Walker>(cellSize = 4f, cellSizeY = 100f)
        val precise = SpatialHash.RayTest<Walker> { unit, ray, _ -> unit.node.intersectRay(ray, precise = true) }
        val ray = Ray()
        val marker = MutableVec3f(0f, 0f, 0f)
        val markerRadius = 6f
        val inRange = mutableListOf<Walker>()
        var hovered: Walker? = null

        val markerMesh = fullIndexedMesh().generate {
            cube {
                width = 0.4f
                height = 4f
                depth = 0.4f
            }
        }
        val markerNode = MeshNode(listOf(MeshPrimitive(markerMesh, unitMaterial))).apply { color(Color.RED) }

        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)
        val hoverColor = Color.YELLOW
        val rangeColor = Color(1f, 0.35f, 0.3f, 1f)

        fun Walker.paint(newColor: Color) {
            if (color != newColor) {
                color = newColor
                node.color(newColor)
            }
        }

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        onUpdate { dt ->
            camera.update()
            units.fastForEach { unit ->
                unit.angle += dt.seconds * unit.speed
                unit.node.position(
                    unit.center.x + cos(unit.angle) * unit.radius,
                    unit.center.y,
                    unit.center.z + sin(unit.angle) * unit.radius,
                )
                unit.node.rotation(unit.rotation.identity().rotate((-unit.angle).radians, Vec3f.Y_AXIS))
                unit.node.update(dt)
                hash.put(unit, unit.node.globalBounds(unit.bounds))
            }

            hovered = null
            if (camera.computePickRay(this, ray, input.x.toFloat(), input.y.toFloat())) {
                hovered = hash.raycast(ray, test = precise)
                if (input.isJustTouched(Pointer.POINTER1)) {
                    val distance = ray.intersectPlane(Vec3f.Y_AXIS, 0f)
                    if (distance >= 0f) {
                        ray.pointAt(distance, marker)
                        markerNode.position(marker.x, 2f, marker.z)
                    }
                }
            }
            markerNode.update(dt)

            inRange.clear()
            hash.queryRadius(marker.x, 0.8f, marker.z, markerRadius, inRange)
            units.fastForEach { it.paint(Color.WHITE) }
            inRange.fastForEach { it.paint(rangeColor) }
            hovered?.paint(hoverColor)
            floor.update(dt)
        }

        onUpdate { dt ->
            renderFrame("Picking Command Encoder") { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Picking Render Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                modelBatch.render(markerNode, environment)
                units.fastForEach { modelBatch.render(it.node, camera, environment) }
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
            cubeMesh.release()
            sphereMesh.release()
            markerMesh.release()
            unitMaterial.release()
        }
    }
}
