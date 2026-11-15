package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.*
import com.littlekt.file.vfs.readTexture
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.VAlign
import com.littlekt.graphics.MutableColor
import com.littlekt.graphics.PerspectiveCamera
import com.littlekt.graphics.fullIndexedMesh
import com.littlekt.graphics.g2d.font.MsdfStyle
import com.littlekt.graphics.g2d.SpriteBatch
import com.littlekt.graphics.g2d.font.TextureFontCache
import com.littlekt.graphics.g3d.BillboardBatch
import com.littlekt.graphics.g3d.BillboardMode
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
import com.littlekt.math.Mat4
import com.littlekt.math.MutableVec3f
import com.littlekt.math.Vec3f
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Fonts
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * An example of a [BillboardBatch]: health bars & name plates over units, floating damage numbers,
 * an icon that keeps its size on screen, a beam, and a trail built from a strip of recent
 * positions.
 *
 * @author Colton Daily
 * @date 11/15/2026
 */
class BillboardsExample(context: Context) : ContextListener(context) {

    private class Unit(val node: Node3D, val name: String) {
        var health = 1f
    }

    private class Damage {
        val position = MutableVec3f()
        var amount = 0
        var age = 0f
        var alive = false
    }

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
                        intensity = 2f,
                    )
                )
                setAmbientLight(AmbientLight(color = Color(0.12f, 0.12f, 0.15f)))
            }
        val material =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                roughnessFactor = 0.7f,
                baseColorTexture = Textures.textureWhite,
            )
        val floorMesh = fullIndexedMesh().generate {
            grid {
                sizeX = 30f
                sizeY = 30f
            }
        }
        val cubeMesh = fullIndexedMesh().generate {
            cube {
                width = 1f
                height = 2f
                depth = 1f
                origin.set(-0.5f, 0f, -0.5f)
            }
        }
        val floor =
            MeshNode(listOf(MeshPrimitive(floorMesh, material))).apply {
                color(Color(0.25f, 0.27f, 0.3f, 1f))
            }
        val cube = MeshNode(listOf(MeshPrimitive(cubeMesh, material)))
        val names = listOf("Grunt", "Archer", "Brute", "Shaman", "Scout", "Knight")
        val units =
            List(6) { i ->
                val angle = i / 6f * 6.2831853f
                Unit(cube.copy().apply { position(cos(angle) * 6f, 0f, sin(angle) * 6f) }, names[i])
            }
        val icon = resourcesVfs["icon_32x32.png"].readTexture()
        val font = Fonts.defaultTtf.msdf(device, 32f)
        val namePlateFont = font.withStyle(MsdfStyle(outlineWidth = 0.08f))
        val damageFont = font.withStyle(MsdfStyle(outlineWidth = 0.1f, outlineColor = Color(0.3f, 0f, 0f, 1f)))

        val depthTarget = renderTarget(TextureFormat.DEPTH24_PLUS_STENCIL8)
        val modelBatch =
            ModelBatch(device).apply {
                addPipelineProvider(PBRMaterialPipelineProvider())
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val sprites =
            SpriteBatch(device, graphics, preferredFormat).apply { depthFormat = depthTarget.format }
        val signTransform = Mat4().translate(-4f, 2.5f, -7f).scale(0.02f, 0.02f, 0.02f)
        val signText =
            TextureFontCache(font).apply {
                setText(
                    "A SpriteBatch sign in the world",
                    x = -210f,
                    y = 0f,
                    targetWidth = 420f,
                    align = HAlign.CENTER,
                    verticalAlign = VAlign.CENTER,
                )
            }
        val billboards =
            BillboardBatch(device).apply {
                colorFormat = preferredFormat
                depthFormat = depthTarget.format
            }
        val clearColor = graphics.surfaceColor(Color(0.08f, 0.08f, 0.1f, 1f))

        val damages = List(32) { Damage() }
        val trail = FloatArray(TRAIL_POINTS * 3)
        var trailHead = 0
        var trailCount = 0
        val tip = MutableVec3f()
        val barBack = Color(0f, 0f, 0f, 0.6f)
        val beamStart = Color(1f, 0.3f, 0.2f, 1f)
        val beamEnd = Color(1f, 0.8f, 0.4f, 1f)
        val barColor = MutableColor()
        val textColor = MutableColor()
        var time = 0f
        var nextHit = 0.3f
        var target = 0

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
        }

        onUpdate { dt ->
            val seconds = dt.seconds
            time += seconds
            camera.position.set(sin(time * 0.15f) * 14f, 8f, cos(time * 0.15f) * 14f)
            camera.up.set(Vec3f.Y_AXIS)
            camera.lookAt(0f, 1f, 0f)
            camera.update()

            if (time >= nextHit) {
                target = Random.nextInt(units.size)
                val unit = units[target]
                unit.health = (unit.health - Random.nextFloat() * 0.25f).let { if (it <= 0f) 1f else it }
                damages.firstOrNull { !it.alive }?.let {
                    it.alive = true
                    it.age = 0f
                    it.amount = 10 + Random.nextInt(90)
                    it.position.set(unit.node.globalPosition).add(Random.nextFloat() - 0.5f, 2.6f, Random.nextFloat() - 0.5f)
                }
                nextHit = time + 0.4f
            }
            damages.forEach {
                if (!it.alive) return@forEach
                it.age += seconds
                it.position.y += seconds * 1.2f
                if (it.age > 1.2f) it.alive = false
            }

            tip.set(cos(time * 2f) * 3f, 2.5f + sin(time * 4f) * 0.8f, sin(time * 2f) * 3f)
            trail[trailHead * 3] = tip.x
            trail[trailHead * 3 + 1] = tip.y
            trail[trailHead * 3 + 2] = tip.z
            trailHead = (trailHead + 1) % TRAIL_POINTS
            trailCount = minOf(trailCount + 1, TRAIL_POINTS)

            floor.update(dt)
            units.forEach { it.node.update(dt) }
        }

        onUpdate { dt ->
            renderFrame("Billboards Command Encoder") { frame, commandEncoder ->
                val renderPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Billboards Render Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                units.forEach { modelBatch.render(it.node, camera, environment) }
                modelBatch.flush(renderPass, camera, dt)

                billboards.begin(camera)
                units.forEach { unit ->
                    val p = unit.node.globalPosition
                    billboards.draw(Textures.white, p.x, p.y + 2.4f, p.z, 1.2f, 0.16f, barBack, mode = BillboardMode.AXIS_Y)
                    barColor.set(1f - unit.health, unit.health, 0.1f, 1f)
                    billboards.draw(
                        Textures.white,
                        p.x,
                        p.y + 2.4f,
                        p.z,
                        1.16f * unit.health,
                        0.12f,
                        barColor,
                        originX = 0.5f / unit.health,
                        mode = BillboardMode.AXIS_Y,
                    )
                    billboards.drawText(namePlateFont, unit.name, p.x, p.y + 2.8f, p.z, 0.35f, mode = BillboardMode.AXIS_Y)
                }
                val boss = units[target].node.globalPosition
                billboards.draw(icon, boss.x, boss.y + 3.5f, boss.z, 32f, 32f, sizeInPixels = true)

                billboards.blendState = BlendState.Add
                billboards.beginStrip()
                billboards.stripPoint(0f, 0.2f, 0f, 0.25f, beamStart)
                billboards.stripPoint(boss.x, boss.y + 1f, boss.z, 0.05f, beamEnd)
                billboards.endStrip()
                billboards.beginStrip()
                for (i in 0 until trailCount) {
                    val index = (trailHead - trailCount + i + TRAIL_POINTS) % TRAIL_POINTS
                    val t = i / (trailCount - 1).coerceAtLeast(1).toFloat()
                    textColor.set(0.3f, 0.8f, 1f, t)
                    billboards.stripPoint(trail[index * 3], trail[index * 3 + 1], trail[index * 3 + 2], 0.3f * t, textColor)
                }
                billboards.endStrip()
                billboards.blendState = BlendState.NonPreMultiplied

                damages.forEach {
                    if (!it.alive) return@forEach
                    textColor.set(1f, 0.85f, 0.2f, 1f - it.age / 1.2f)
                    billboards.drawText(damageFont, it.amount.toString(), it.position.x, it.position.y, it.position.z, 0.5f, textColor)
                }
                billboards.flush(renderPass)
                billboards.end()

                sprites.transformMatrix = signTransform
                sprites.begin(camera.viewProjection)
                sprites.draw(Textures.white, -210f, -40f, width = 420f, height = 80f, color = barBack)
                signText.draw(sprites)
                sprites.flush(renderPass)
                sprites.end()

                renderPass.end()
                renderPass.release()
            }
        }

        onRelease {
            sprites.release()
            billboards.release()
            modelBatch.release()
            environment.release()
            floorMesh.release()
            cubeMesh.release()
            material.release()
            icon.release()
            font.release()
        }
    }

    private companion object {
        const val TRAIL_POINTS = 60
    }
}
