package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextScene
import com.lehaine.littlekt.createShader
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorVertexShader
import com.lehaine.littlekt.input.InputProcessor
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(context: Context) : ContextScene(context), InputProcessor {

    val batch = SpriteBatch(context)

    val texture by load<Texture>(resourcesVfs["atlas.png"])
    val atlas: TextureAtlas by load(resourcesVfs["tiles.atlas.json"])
    val slices: Array<Array<TextureSlice>> by lazy { texture.slice(16, 16) }
    val person by lazy { slices[0][0] }
    val bossAttack by lazy { atlas.getAnimation("bossAttack") }

    val shader = createShader(SimpleColorVertexShader(), SimpleColorFragmentShader())
    val colorBits = Color.WHITE.toFloatBits()
    val mesh = colorMesh {
        maxVertices = 4
    }.apply {
        setVertex {
            x = 50f
            y = 50f
            colorPacked = colorBits
        }

        setVertex {
            x = 66f
            y = 50f
            colorPacked = colorBits
        }

        setVertex {
            x = 66f
            y = 66f
            colorPacked = colorBits
        }

        setVertex {
            x = 50f
            y = 66f
            colorPacked = colorBits
        }

        indicesAsQuad()
    }

    val camera = OrthographicCamera(graphics.width, graphics.height)

    private var x = 0f
    private var y = 0f

    private var xVel = 0f
    private var yVel = 0f

    init {
        logger.level = Logger.Level.DEBUG
        input.inputProcessor = this
        camera.translate(graphics.width / 2f, graphics.height / 2f, 0f)
    }

    override fun prepare() {
        bossAttack.playLooped()
    }

    override fun update(dt: Duration) {
        xVel = 0f
        yVel = 0f

        if (input.isKeyPressed(Key.W)) {
            yVel -= 10f
        }
        if (input.isKeyPressed(Key.S)) {
            yVel += 10f
        }
        if (input.isKeyPressed(Key.A)) {
            xVel -= 10f
        }
        if (input.isKeyPressed(Key.D)) {
            xVel += 10f
        }

        gl.clearColor(Color.DARK_GRAY)
        camera.update()
        bossAttack.update(dt)
        batch.use(camera.viewProjection) {
            it.draw(person, x, y, scaleX = 10f, scaleY = 10f)
            slices.forEachIndexed { rowIdx, row ->
                row.forEachIndexed { colIdx, slice ->
                    it.draw(slice, 150f * (rowIdx * row.size + colIdx) + 50f, 50f, scaleX = 10f, scaleY = 10f)
                }
            }
            it.draw(bossAttack.currentFrame, 450f, 250f, scaleX = 2f, scaleY = 2f, flipX = false)
            it.draw(bossAttack.currentFrame, 150f, 250f, scaleX = 2f, scaleY = 2f, flipX = true)
        }

        shader.bind()
        shader.uProjTrans?.apply(shader, camera.viewProjection)
        mesh.render(shader)

        x += xVel
        y += yVel


        if (input.isKeyJustPressed(Key.P)) {
            logger.debug { stats }
        }

        if (input.isKeyJustPressed(Key.ESCAPE)) {
            close()
        }
    }

    override fun resize(width: Int, height: Int) {
        logger.debug { "Resize to $width,$height" }
    }

    override fun dispose() {
        mesh.dispose()
        texture.dispose()
        shader.dispose()
        batch.dispose()
    }
}
