package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.graphics.Color
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.g3d.MeshNode
import com.littlekt.graphics.g3d.MeshPrimitive
import com.littlekt.graphics.g3d.ModelBatch
import com.littlekt.graphics.g3d.PBREnvironment
import com.littlekt.graphics.g3d.light.AmbientLight
import com.littlekt.graphics.g3d.light.DirectionalLight
import com.littlekt.graphics.g3d.material.PBRMaterial
import com.littlekt.graphics.g3d.util.PBRMaterialPipelineProvider
import com.littlekt.graphics.generate
import com.littlekt.graphics.webgpu.*
import com.littlekt.input.Key
import com.littlekt.math.MutableQuaternion
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.math.geom.degrees
import com.littlekt.math.spatial.BoundingBox
import com.littlekt.math.spatial.Capsule
import com.littlekt.math.spatial.Collision
import com.littlekt.math.spatial.SpatialHash
import com.littlekt.math.spatial.SweepHit
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Fonts
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
import com.littlekt.util.datastructure.fastForEach
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScreenViewport

/**
 * An example of overlaps & sweeps. A capsule moves with WASD and sweeps against walls kept in a
 * [SpatialHash], sliding along them with a few sweeps written in the example. The rotated walls are
 * tested by their triangles. Space fires a sphere that stops where its sweep first hits, and the
 * pad lights up while the capsule overlaps it.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class CollisionExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.position.set(0f, 22f, 16f)
        camera.up.set(Vec3f.Y_AXIS)
        camera.lookAt(0f, 0f, 1f)
        val environment =
            PBREnvironment(device).apply {
                setDirectionalLight(
                    DirectionalLight(
                        color = Color(1f, 0.95f, 0.85f),
                        direction = MutableVec3f(0.4f, 1f, 0.3f).norm(),
                        intensity = 2.5f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.2f, 0.2f, 0.24f)))
            }
        val material =
            PBRMaterial(device, metallicFactor = 0f, roughnessFactor = 0.7f, baseColorTexture = Textures.textureWhite)

        val floorMesh = fullIndexedMesh().generate {
            grid {
                sizeX = 40f
                sizeY = 40f
            }
        }
        val floor = MeshNode(listOf(MeshPrimitive(floorMesh, material))).apply { color(Color(0.25f, 0.27f, 0.3f, 1f)) }
        val cubeMesh = fullIndexedMesh().generate { cube { centered() } }
        val sphereMesh = fullIndexedMesh().generate { uvSphere { radius = 0.25f } }
        val radius = 0.5f
        val height = 2f
        val capsuleMesh = fullIndexedMesh().generate {
            vertexModFun = { position.y += if (position.y > 0f) height * 0.5f - radius else radius - height * 0.5f }
            uvSphere {
                this.radius = radius
                rings = 17
            }
        }

        val walls = mutableListOf<MeshNode>()
        fun wall(x: Float, z: Float, width: Float, depth: Float, angle: Float = 0f) {
            walls +=
                MeshNode(listOf(MeshPrimitive(cubeMesh, material))).apply {
                    position(x, 1f, z)
                    scaling(width, 2f, depth)
                    rotation(MutableQuaternion().rotate(angle.degrees, Vec3f.Y_AXIS))
                    color(Color(0.75f, 0.75f, 0.8f, 1f))
                }
        }
        wall(0f, -12f, 24f, 1f)
        wall(0f, 12f, 24f, 1f)
        wall(-12f, 0f, 1f, 24f)
        wall(12f, 0f, 1f, 24f)
        wall(-5f, -4f, 6f, 1f, 35f)
        wall(5f, 3f, 7f, 1f, -50f)
        wall(-4f, 6f, 1.5f, 1.5f)
        wall(3f, -7f, 2f, 2f, 20f)
        val pad = MeshNode(listOf(MeshPrimitive(cubeMesh, material))).apply {
            position(7f, 0.05f, -7f)
            scaling(3f, 0.1f, 3f)
        }

        val hash = SpatialHash<MeshNode>(cellSize = 4f, cellSizeY = 20f)
        val bounds = BoundingBox()
        walls.fastForEach {
            it.update(kotlin.time.Duration.ZERO)
            hash.put(it, it.globalBounds(bounds))
        }

        val player = MeshNode(listOf(MeshPrimitive(capsuleMesh, material))).apply { color(Color(0.3f, 0.6f, 1f, 1f)) }
        val capsule = Capsule().setUpright(0f, 0f, 0f, height, radius)
        val projectile = MeshNode(listOf(MeshPrimitive(sphereMesh, material))).apply { color(Color.YELLOW) }
        val marker = MeshNode(listOf(MeshPrimitive(sphereMesh, material))).apply {
            color(Color.RED)
            position(0f, -10f, 0f)
        }
        val projectilePosition = MutableVec3f(0f, -10f, 0f)
        val projectileVelocity = MutableVec3f()
        var projectileFlying = false
        val facing = MutableVec3f(0f, 0f, -1f)
        val move = MutableVec3f()
        val delta = MutableVec3f()
        val hit = SweepHit()
        val sweepBounds = BoundingBox()
        var lastHitWall: MeshNode? = null
        val skin = 0.01f
        val wallColor = Color(0.75f, 0.75f, 0.8f, 1f)
        val hitColor = Color(1f, 0.55f, 0.2f, 1f)

        fun sweepWalls(shape: Capsule, motion: Vec3f): MeshNode? =
            hash.sweepcast(shape.sweepBounds(motion, sweepBounds), hit) { wall, h ->
                wall.sweepCapsule(shape, motion, h, precise = true)
            }

        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val batch = SpriteBatch(device, graphics, preferredFormat)
        val viewport = ScreenViewport(graphics.width, graphics.height)
        viewport.update(graphics.width, graphics.height, true)
        val text = TextureFontCache(Fonts.default)
        val clearColor = graphics.surfaceColor(Color.DARK_GRAY)

        fun updateText() {
            text.setText("WASD to move, space to fire", 16f, viewport.virtualHeight - 32f)
        }
        updateText()

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
            viewport.update(width, height, true)
            updateText()
        }

        onUpdate { dt ->
            camera.update()
            move.set(0f, 0f, 0f)
            if (input.isKeyPressed(Key.W)) move.z -= 1f
            if (input.isKeyPressed(Key.S)) move.z += 1f
            if (input.isKeyPressed(Key.A)) move.x -= 1f
            if (input.isKeyPressed(Key.D)) move.x += 1f
            if (move.sqrLength() > 0f) {
                move.norm()
                facing.set(move)
                delta.set(move).scale(6f * dt.seconds)
                for (step in 0 until 3) {
                    val length = delta.length()
                    if (length < 1e-5f) break
                    if (sweepWalls(capsule, delta) == null) {
                        capsule.translate(delta)
                        break
                    }
                    val travel = maxOf(0f, hit.time - skin / length)
                    capsule.translate(delta.x * travel, delta.y * travel, delta.z * travel)
                    delta.scale(1f - travel)
                    val into = delta.dot(hit.normal)
                    delta.x -= hit.normal.x * into
                    delta.z -= hit.normal.z * into
                    delta.y = 0f
                }
            }
            player.position(capsule.a.x, height * 0.5f, capsule.a.z)

            if (input.isKeyJustPressed(Key.SPACE)) {
                val offset = radius + 0.3f
                projectilePosition.set(capsule.a.x + facing.x * offset, 1f, capsule.a.z + facing.z * offset)
                projectileVelocity.set(facing).scale(18f)
                projectileFlying = true
            }
            if (projectileFlying) {
                delta.set(projectileVelocity).scale(dt.seconds)
                Collision.sweepBounds(projectilePosition, 0.25f, delta, sweepBounds)
                val wall = hash.sweepcast(sweepBounds, hit) { w, h ->
                    w.sweepSphere(projectilePosition, 0.25f, delta, h, precise = true)
                }
                if (wall != null) {
                    projectilePosition.add(delta.scale(hit.time))
                    marker.position(hit.point)
                    lastHitWall?.color(wallColor)
                    wall.color(hitColor)
                    lastHitWall = wall
                    projectileFlying = false
                } else {
                    projectilePosition.add(delta)
                }
            }
            projectile.position(projectilePosition)

            pad.color(if (pad.overlapsCapsule(capsule)) Color.GREEN else Color(0.2f, 0.35f, 0.25f, 1f))

            floor.update(dt)
            pad.update(dt)
            player.update(dt)
            projectile.update(dt)
            marker.update(dt)

            renderFrame("Collision Command Encoder") { frame, commandEncoder ->
                val pass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Collision Render Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                walls.fastForEach { modelBatch.render(it, environment) }
                modelBatch.render(pad, environment)
                modelBatch.render(player, environment)
                modelBatch.render(projectile, environment)
                modelBatch.render(marker, environment)
                modelBatch.flush(pass, camera, dt)
                pass.end()
                pass.release()

                val uiPass =
                    commandEncoder.beginRenderPass(RenderPassDescriptor(colorAttachment(frame), label = "UI Render Pass"))
                viewport.camera.update()
                batch.begin(viewport.camera.viewProjection)
                text.draw(batch)
                batch.flush(uiPass)
                batch.end()
                uiPass.end()
                uiPass.release()
            }
        }

        onRelease {
            modelBatch.release()
            environment.release()
            batch.release()
            floorMesh.release()
            cubeMesh.release()
            sphereMesh.release()
            capsuleMesh.release()
            material.release()
        }
    }
}
