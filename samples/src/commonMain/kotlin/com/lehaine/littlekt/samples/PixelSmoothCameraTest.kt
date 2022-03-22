package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readLDtkMapLoader
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.gl.State
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.graphics.gl.TexMinFilter
import com.lehaine.littlekt.graphics.shader.FragmentShaderModel
import com.lehaine.littlekt.graphics.shader.ShaderParameter
import com.lehaine.littlekt.graphics.shader.ShaderProgram
import com.lehaine.littlekt.graphics.shader.VertexShaderModel
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.math.floor
import com.lehaine.littlekt.math.geom.degrees
import com.lehaine.littlekt.math.nextPowerOfTwo
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

        var fbo =
            FrameBuffer(1, 1, minFilter = TexMinFilter.NEAREST, magFilter = TexMagFilter.NEAREST).also {
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
            fbo =
                FrameBuffer(pxWidth.nextPowerOfTwo,
                    pxHeight.nextPowerOfTwo,
                    minFilter = TexMinFilter.NEAREST,
                    magFilter = TexMagFilter.NEAREST).also {
                    it.prepare(this)
                }
            fboRegion = TextureSlice(fbo.colorBufferTexture, 0, 0, pxWidth, pxHeight)
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

            sceneCamera.position.set(tx, ty, 0f).add((fbo.width / 2) * worldUnitInvScale,
                ((fbo.height - pxHeight) / 2) * worldUnitInvScale,
                0f)
            sceneCamera.update()

            fbo.begin()
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            batch.use(sceneCamera.viewProjection) {
                world.render(it, sceneCamera, 0f, 0f, worldUnitInvScale)
                it.draw(icon, 0f, 0f, scaleX = worldUnitInvScale, scaleY = worldUnitInvScale, rotation = 45.degrees)
            }
            fbo.end()

            batch.shader = pixelSmoothShader
            viewportCamera.ortho(graphics.width, graphics.height)
            viewportCamera.update()
            batch.use(viewportCamera.viewProjection) {
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
                it.draw(fboRegion,
                    0f,
                    0f,
                    width = context.graphics.width.toFloat(),
                    height = context.graphics.height.toFloat(),
                    flipY = true)
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

private class PixelSmoothVertexShader : VertexShaderModel() {
    val uProjTrans = ShaderParameter.UniformMat4("u_projTrans")
    val uTextureSizes = ShaderParameter.UniformVec4("u_textureSizes")
    val uSampleProperties = ShaderParameter.UniformVec4("u_sampleProperties")
    val aPosition = ShaderParameter.Attribute("a_position")
    val aColor = ShaderParameter.Attribute("a_color")
    val aTexCoord0 = ShaderParameter.Attribute("a_texCoord0")

    override val parameters: MutableList<ShaderParameter> =
        mutableListOf(uProjTrans, uTextureSizes, uSampleProperties, aPosition, aColor, aTexCoord0)

    // language=GLSL
    override var source: String = """
        uniform mat4 u_projTrans;
        uniform vec4 u_textureSizes;
        uniform vec4 u_sampleProperties;
        attribute vec4 a_position;
        attribute vec4 a_color;
        attribute vec2 a_texCoord0;
        varying lowp vec4 v_color;
        varying vec2 v_texCoords;

        void main()
        {
            v_color = a_color;
            v_color.a = v_color.a * (255.0/254.0);

            vec2 uvSize = u_textureSizes.xy;

            v_texCoords.x = a_texCoord0.x + u_sampleProperties.z / uvSize.x;
            v_texCoords.y = a_texCoord0.y + u_sampleProperties.w / uvSize.y;

            gl_Position = u_projTrans * a_position;
        }
    """.trimIndent()
}


private class PixelSmoothFragmentShader : FragmentShaderModel() {
    val uTexture = ShaderParameter.UniformSample2D("u_texture")
    val uTextureSizes = ShaderParameter.UniformVec4("u_textureSizes")
    val uSampleProperties = ShaderParameter.UniformVec4("u_sampleProperties")

    override val parameters: MutableList<ShaderParameter> = mutableListOf(uTexture, uTextureSizes, uSampleProperties)

    // language=GLSL
    override var source: String = """
        uniform sampler2D u_texture;
        uniform vec4 u_textureSizes;
        uniform vec4 u_sampleProperties;
        varying lowp vec4 v_color;
        varying vec2 v_texCoords;

        void main()
        {
            vec2 uv = v_texCoords;
            vec2 uvSize = u_textureSizes.xy;


            float dU = 1.0 / uvSize.x;
            float dV = 1.0 / uvSize.y;

            vec4 c0 = texture2D(u_texture, uv);
            vec4 c1 = texture2D(u_texture, uv + vec2(dU, 0));
            vec4 c2 = texture2D(u_texture, uv + vec2(0, dV));
            vec4 c3 = texture2D(u_texture, uv + vec2(dU, dV));

            float subU = u_sampleProperties.x;
            float subV = u_sampleProperties.y;

            float w0 = 1.0 - subU;
            float w1 = subU;
            float w2 = 1.0 - subV;
            float w3 = subV;

            vec4 bilinear = c0 * w0 * w2 + c1 * w1 * w2 + c2 * w0 * w3 + c3 * w1 * w3;

            gl_FragColor = bilinear;
        }
    """.trimIndent()
}