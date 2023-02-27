package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readLDtkMapLoader
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.FrameBuffer
import com.lehaine.littlekt.graphics.OrthographicCamera
import com.lehaine.littlekt.graphics.g2d.SpriteBatch
import com.lehaine.littlekt.graphics.g2d.TextureSlice
import com.lehaine.littlekt.graphics.g2d.use
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.use
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
class PixelSmoothCameraTest(context: Context) : ContextListener(context) {
    var pxWidth = 0
    var pxHeight = 0
    val targetHeight = 160
    val worldUnitScale = 16f
    val worldUnitInvScale = 1f / worldUnitScale

    val sceneCamera = OrthographicCamera(1, 1)
    val viewportCamera = OrthographicCamera(context.graphics.width, context.graphics.height).apply {
        position.x = virtualWidth * 0.5f
        position.y = virtualHeight * 0.5f
        update()
    }

    override suspend fun Context.start() {
        val batch = SpriteBatch(this)
        val mapLoader = resourcesVfs["ldtk/world.ldtk"].readLDtkMapLoader()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val world = mapLoader.loadLevel(0)
        val pixelSmoothShader =
            ShaderProgram(PixelSmoothVertexShader(), PixelSmoothFragmentShader()).also { it.prepare(this) }

        var fbo = FrameBuffer(1, 1, minFilter = TexMinFilter.NEAREST, magFilter = TexMagFilter.NEAREST).also {
            it.prepare(this)
        }

        var fboRegion = TextureSlice(fbo.colorBufferTexture, 0, 0, fbo.width, fbo.height)
        val cameraDir = MutableVec2f()
        val targetPosition = MutableVec2f()
        val velocity = MutableVec2f()
        val tempVec2f = MutableVec2f()
        var useBilinearFilter = false
        val speed = 1f

        onResize { width, height ->
            pxHeight = height / (height / targetHeight)
            pxWidth = (width / (height / pxHeight))
            fbo.dispose()
            fbo = FrameBuffer(
                pxWidth.nextPowerOfTwo,
                pxHeight.nextPowerOfTwo,
                minFilter = TexMinFilter.NEAREST,
                magFilter = TexMagFilter.NEAREST
            ).also {
                it.prepare(this)
            }
            fboRegion = TextureSlice(fbo.colorBufferTexture, 0, fbo.height - pxHeight, pxWidth, pxHeight)
            sceneCamera.ortho(fbo.width * worldUnitInvScale, fbo.height * worldUnitInvScale)
        }
        onRender { dt ->
            gl.enable(State.SCISSOR_TEST)
            gl.scissor(0, 0, graphics.width, graphics.height)
            gl.clearColor(Color.DARK_GRAY)
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

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

            var scaledDistX = (targetPosition.x - tx) * worldUnitScale
            var scaledDistY = (targetPosition.y - ty) * worldUnitScale

            var subpixelX = 0f
            var subPixelY = 0f

            if (useBilinearFilter) {
                subpixelX = scaledDistX - floor(scaledDistX)
                subPixelY = scaledDistY - floor(scaledDistY)
            }

            scaledDistX -= subpixelX
            scaledDistY -= subPixelY

            sceneCamera.position.set(tx, ty, 0f)
                .add(fbo.width * worldUnitInvScale / 2f, fbo.height * worldUnitInvScale / 2f, 0f)
            sceneCamera.update()


            tempVec2f.x = input.x.toFloat()
            tempVec2f.y = input.y.toFloat()
            tempVec2f.x = (pxWidth / 100f) * ((100f / graphics.width) * input.x)
            tempVec2f.y = (pxHeight / 100f) * ((100f / graphics.height) * input.y)
            tempVec2f.x *= worldUnitInvScale
            tempVec2f.y *= worldUnitInvScale
            tempVec2f.x = tempVec2f.x - fbo.width * worldUnitInvScale * 0.5f + sceneCamera.position.x
            tempVec2f.y = tempVec2f.y - fbo.height * worldUnitInvScale * 0.5f + sceneCamera.position.y

            fbo.use {
                gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
                batch.use(sceneCamera.viewProjection) {
                    world.render(it, sceneCamera, 0f, 0f, worldUnitInvScale)
                    it.draw(icon, 0f, 0f, scaleX = worldUnitInvScale, scaleY = worldUnitInvScale, rotation = 45.degrees)
                    it.draw(
                        icon, 10f, 10f, scaleX = worldUnitInvScale, scaleY = worldUnitInvScale, rotation = 45.degrees
                    )
                }
            }

            batch.shader = pixelSmoothShader
            viewportCamera.ortho(graphics.width, graphics.height)
            viewportCamera.update()

            batch.use(viewportCamera.viewProjection) {
                pixelSmoothShader.vertexShader.uTextureSizes.apply(
                    pixelSmoothShader, fbo.width.toFloat(), fbo.height.toFloat(), 0f, 0f
                )
                pixelSmoothShader.vertexShader.uSampleProperties.apply(
                    pixelSmoothShader, subpixelX, subPixelY, scaledDistX, scaledDistY
                )
                it.draw(
                    fboRegion,
                    0f,
                    0f,
                    width = context.graphics.width.toFloat(),
                    height = context.graphics.height.toFloat(),
                    flipY = true
                )
            }
            batch.shader = batch.defaultShader

            if (input.isKeyJustPressed(Key.B)) {
                useBilinearFilter = !useBilinearFilter
            }

            if (input.isKeyJustPressed(Key.P)) {
                logger.info { stats }
            }

            if (input.isKeyJustPressed(Key.ESCAPE)) {
                close()
            }
        }
    }
}