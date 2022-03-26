package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readLDtkMapLoader
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.node.FrameBufferNode
import com.lehaine.littlekt.graph.node.canvasLayer
import com.lehaine.littlekt.graph.node.frameBuffer
import com.lehaine.littlekt.graph.node.node
import com.lehaine.littlekt.graph.node.node2d.node2d
import com.lehaine.littlekt.graph.node.render.Material
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.TextureSlice
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.floor
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.math.nextPowerOfTwo
import com.lehaine.littlekt.samples.shaders.PixelSmoothFragmentShader
import com.lehaine.littlekt.samples.shaders.PixelSmoothVertexShader
import com.lehaine.littlekt.util.seconds
import kotlin.math.floor

/**
 * @author Colton Daily
 * @date 2/24/2022
 */
class PixelSmoothCameraSceneGraphTest(context: Context) : ContextListener(context) {
    var pxWidth = 0
    var pxHeight = 0
    val targetHeight = 160
    val worldUnitScale = 16f
    val worldUnitInvScale = 1f / worldUnitScale

    override suspend fun Context.start() {
        val mapLoader = resourcesVfs["ldtk/world.ldtk"].readLDtkMapLoader()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val world = mapLoader.loadLevel(0)

        val pixelSmoothShader =
            ShaderProgram(PixelSmoothVertexShader(), PixelSmoothFragmentShader()).also { it.prepare(this) }

        val graph = sceneGraph(context) {
            val fbo: FrameBufferNode
            canvasLayer {
                var scaledDistX = 0f
                var scaledDistY = 0f
                var subpixelX = 0f
                var subPixelY = 0f

                fbo = frameBuffer {
                    onResize += { width, height ->
                        pxHeight = height / (height / targetHeight)
                        pxWidth = (width / (height / pxHeight))
                        resizeFbo(pxWidth.nextPowerOfTwo, pxHeight.nextPowerOfTwo)
                        canvasCamera.ortho(this.width * worldUnitInvScale, this.height * worldUnitInvScale)
                        canvasCamera.update()
                    }

                    node2d {
                        onRender += { batch, camera ->
                            world.render(batch, camera, 0f, 0f, worldUnitInvScale)
                            batch.draw(icon,
                                0f,
                                0f,
                                scaleX = worldUnitInvScale,
                                scaleY = worldUnitInvScale,
                                rotation = 45.degrees)
                        }
                    }


                    node {
                        val cameraDir = MutableVec2f()
                        val targetPosition = MutableVec2f()
                        val velocity = MutableVec2f()
                        val tempVec2f = MutableVec2f()
                        var useBilinearFilter = false
                        val speed = 1f

                        onUpdate += {
                            cameraDir.set(0f, 0f)
                            if (input.isKeyPressed(Key.W)) {
                                cameraDir.y = -1f
                            } else if (input.isKeyPressed(Key.S)) {
                                cameraDir.y = 1f
                            }

                            if (input.isKeyPressed(Key.D)) {
                                cameraDir.x = 1f
                            } else if (input.isKeyPressed(Key.A)) {
                                cameraDir.x = -1f
                            }

                            tempVec2f.set(cameraDir).norm().scale(speed)
                            velocity.mulAdd(tempVec2f, dt.seconds * speed)
                            velocity.lerp(Vec2f.ZERO, 0.7f * (1f - cameraDir.norm().length()))

                            targetPosition += velocity

                            val tx = (targetPosition.x * worldUnitScale).floor() / worldUnitScale
                            val ty = (targetPosition.y * worldUnitScale).floor() / worldUnitScale

                            scaledDistX = (targetPosition.x - tx) * worldUnitScale
                            scaledDistY = (targetPosition.y - ty) * worldUnitScale

                            subpixelX = 0f
                            subPixelY = 0f

                            if (useBilinearFilter) {
                                subpixelX = scaledDistX - floor(scaledDistX)
                                subPixelY = scaledDistY - floor(scaledDistY)
                            }

                            scaledDistX -= subpixelX
                            scaledDistY -= subPixelY

                            (parent as? FrameBufferNode)?.let {
                                it.canvasCamera.position.set(tx, ty, 0f).add((it.width / 2) * worldUnitInvScale,
                                    ((it.height - pxHeight) / 2) * worldUnitInvScale,
                                    0f)
                            }
                            if (input.isKeyJustPressed(Key.B)) {
                                useBilinearFilter = !useBilinearFilter
                            }
                        }
                    }
                }

                node2d {
                    var slice: TextureSlice? = null
                    material = Material(pixelSmoothShader)

                    fbo.onFboChanged.connect(this) {
                        slice = TextureSlice(it, 0, 0, pxWidth, pxHeight)
                    }

                    onRender += { batch, camera ->
                        slice?.let {
                            pixelSmoothShader.vertexShader.uTextureSizes.apply(pixelSmoothShader,
                                fbo.width.toFloat(),
                                fbo.height.toFloat(),
                                0f,
                                0f)
                            pixelSmoothShader.vertexShader.uSampleProperties.apply(pixelSmoothShader,
                                subpixelX,
                                subPixelY,
                                scaledDistX,
                                scaledDistY)
                            batch.draw(it,
                                0f,
                                0f,
                                width = context.graphics.width.toFloat(),
                                height = context.graphics.height.toFloat(),
                                flipY = true)
                        }
                    }

                }
            }
        }.also { it.initialize() }


        onResize { width, height ->
            graph.resize(width, height)
        }

        onRender { dt ->
            gl.clearColor(Color.DARK_GRAY)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            graph.update(dt)
            graph.render()

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}
