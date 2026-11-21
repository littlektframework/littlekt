package com.littlekt.examples.g3d

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.audio.AudioAttenuation
import com.littlekt.audio.DistanceModel
import com.littlekt.audio.set
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addFlyController
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.vfs.readAudioClip
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
import com.littlekt.math.MutableVec3f
import com.littlekt.renderFrame
import com.littlekt.renderTarget
import com.littlekt.resources.Fonts
import com.littlekt.resources.Textures
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ScreenViewport
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * An example of spatial audio. A looping speaker circles the middle of the floor and is heard from
 * the camera, which moves with the fly controller. Space plays a one shot sound at a random spot
 * that falls off linearly to silence.
 *
 * @author Colton Daily
 * @date 11/21/2026
 */
class SpatialAudioExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val device = graphics.device
        val preferredFormat = graphics.preferredFormat

        val camera = PerspectiveCamera(graphics.width, graphics.height)
        camera.position.set(0f, 4f, 14f)
        camera.lookAt(0f, 0f, 0f)
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
                sizeX = 60f
                sizeY = 60f
            }
        }
        val material =
            PBRMaterial(
                device,
                metallicFactor = 0f,
                roughnessFactor = 0.6f,
                baseColorTexture = Textures.textureWhite,
            )
        val floor =
            MeshNode(listOf(MeshPrimitive(floorMesh, material))).apply {
                color(Color(0.25f, 0.27f, 0.3f, 1f))
            }
        val sphereMesh = fullIndexedMesh().generate { uvSphere { radius = 0.5f } }
        val speaker =
            MeshNode(listOf(MeshPrimitive(sphereMesh, material))).apply { color(Color.YELLOW) }
        val cubeMesh = fullIndexedMesh().generate {
            cube {
                width = 0.4f
                height = 3f
                depth = 0.4f
            }
        }
        val marker =
            MeshNode(listOf(MeshPrimitive(cubeMesh, material))).apply {
                color(Color.RED)
                position(0f, -10f, 0f)
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
            text.setText(
                "WASD & mouse to move, space for a sound at the red marker",
                16f,
                viewport.virtualHeight - 32f,
            )
        }
        updateText()

        val clip = resourcesVfs["random.wav"].readAudioClip()
        val speakerAttenuation = AudioAttenuation(refDistance = 2f, rolloff = 1.5f)
        val pingAttenuation =
            AudioAttenuation(
                refDistance = 1f,
                maxDistance = 20f,
                distanceModel = DistanceModel.LINEAR,
            )
        var angle = 0f
        val orbit = 6f
        val speakerId = clip.playAt(orbit, 1f, 0f, volume = 0.4f, loop = true, attenuation = speakerAttenuation)

        onResize { width, height ->
            camera.virtualWidth = width.toFloat()
            camera.virtualHeight = height.toFloat()
            camera.update()
            viewport.update(width, height, true)
            updateText()
        }

        addFlyController(camera, 0.005f)
        onUpdate { dt ->
            camera.update()
            audio.listener.set(camera)

            angle += dt.seconds * 0.8f
            val x = cos(angle) * orbit
            val z = sin(angle) * orbit
            speaker.position(x, 1f, z)
            clip.setPosition(speakerId, x, 1f, z)

            if (input.isKeyJustPressed(Key.SPACE)) {
                val px = Random.nextFloat() * 30f - 15f
                val pz = Random.nextFloat() * 30f - 15f
                marker.position(px, 1.5f, pz)
                clip.playAt(px, 1f, pz, attenuation = pingAttenuation)
            }

            floor.update(dt)
            speaker.update(dt)
            marker.update(dt)

            renderFrame("Spatial Audio Command Encoder") { frame, commandEncoder ->
                val pass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(
                            colorAttachment(frame, clearColor),
                            depthStencilAttachment = depthTarget.depthAttachment(),
                            label = "Spatial Audio Render Pass",
                        )
                    )
                modelBatch.render(floor, environment)
                modelBatch.render(speaker, environment)
                modelBatch.render(marker, environment)
                modelBatch.flush(pass, camera, dt)
                pass.end()
                pass.release()

                val uiPass =
                    commandEncoder.beginRenderPass(
                        RenderPassDescriptor(colorAttachment(frame), label = "UI Render Pass")
                    )
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
            clip.release()
            modelBatch.release()
            environment.release()
            batch.release()
            floorMesh.release()
            sphereMesh.release()
            cubeMesh.release()
            material.release()
        }
    }
}
