package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.createShader
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readTexture
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
class DisplayTest(context: Context) : ContextListener(context), InputProcessor {

    val batch = SpriteBatch(context)

    var loading = true

    lateinit var texture: Texture
    lateinit var slices: Array<Array<TextureSlice>>
    lateinit var person: TextureSlice
    lateinit var atlas: TextureAtlas

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

    val camera = OrthographicCamera().apply {
        left = 0f
        right = graphics.width.toFloat()
        bottom = 0f
        top = graphics.height.toFloat()
    }
    lateinit var bossAttack: Animation<TextureSlice>
    private var x = 0f
    private var y = 0f

    private var xVel = 0f
    private var yVel = 0f

    init {
        logger.level = Logger.Level.DEBUG
        vfs.launch {
            texture = get("atlas.png").readTexture()
            slices = texture.slice(16, 16)
            person = slices[0][0]
            atlas = get("tiles.atlas.json").readAtlas()
            bossAttack = atlas.getAnimation("bossAttack")
            bossAttack.playLooped()
            loading = false
        }
        input.inputProcessor = this
    }

    override fun render(dt: Duration) {
        if (loading) {
            return
        }
        xVel = 0f
        yVel = 0f

        if (input.isKeyPressed(Key.W)) {
            yVel += 10f
        }
        if (input.isKeyPressed(Key.S)) {
            yVel -= 10f
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
