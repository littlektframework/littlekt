package com.lehaine.littlekt.samples

import com.lehaine.littlekt.*
import com.lehaine.littlekt.async.KtScope
import com.lehaine.littlekt.audio.AudioStream
import com.lehaine.littlekt.file.ldtk.LDtkMapLoader
import com.lehaine.littlekt.file.vfs.readAtlas
import com.lehaine.littlekt.file.vfs.readBitmapFont
import com.lehaine.littlekt.file.vfs.readPixmap
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.createDefaultSceneGraphController
import com.lehaine.littlekt.graph.node.resource.HAlign
import com.lehaine.littlekt.graph.node.resource.NinePatchDrawable
import com.lehaine.littlekt.graph.node.resource.VAlign
import com.lehaine.littlekt.graph.node.resource.createDefaultTheme
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.*
import com.lehaine.littlekt.graphics.g2d.*
import com.lehaine.littlekt.graphics.g2d.font.BitmapFontCache
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorFragmentShader
import com.lehaine.littlekt.graphics.shader.shaders.SimpleColorVertexShader
import com.lehaine.littlekt.input.GameAxis
import com.lehaine.littlekt.input.GameButton
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.MutableVec2f
import com.lehaine.littlekt.util.MutableTextureAtlas
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(context: Context) : Game<Scene>(context) {

    private val assetProvider = AssetProvider(context)
    val testLoad by assetProvider.load<Texture>(resourcesVfs["person.png"])

    private var x = 0f
    private var y = 0f

    private var xVel = 0f
    private var yVel = 0f
    private val uiSignals = SceneGraph.UiInputSignals(
        uiLeft = InputMap.UI_LEFT,
        uiRight = InputMap.UI_RIGHT,
        uiUp = InputMap.UI_UP,
        uiDown = InputMap.UI_DOWN,
        uiAccept = InputMap.UI_ACCEPT,
        uiFocusNext = InputMap.UI_FOCUS_NEXT,
        uiFocusPrev = InputMap.UI_FOCUS_PREV
    )
    private val controller = createDefaultSceneGraphController(context.input, uiSignals)
    val viewport = ExtendViewport(960, 540)
    val camera = viewport.camera

    val shader = createShader(SimpleColorVertexShader(), SimpleColorFragmentShader())
    val colorBits = Color.RED.toFloatBits()

    val vertsData =
        floatArrayOf(
            0f, 0f, colorBits, 25f, 0f, colorBits, 25f, 25f, colorBits, 0f, 25f, colorBits,
            // 50f, 50f, 0f, colorBits, 66f, 50f, 0f, colorBits, 66f, 66f, 0f, colorBits, 50f, 66f, 0f, colorBits
        )
    val mesh = colorMesh(size = 8) {
        geometry.run {
            addVertex {
                position.x = 50f
                position.y = 50f
                colorPacked.value = colorBits
            }

            addVertex {
                position.x = 66f
                position.y = 50f
                colorPacked.value = colorBits
            }

            addVertex {
                position.x = 66f
                position.y = 66f
                colorPacked.value = colorBits
            }

            addVertex {
                position.x = 50f
                position.y = 66f
                colorPacked.value = colorBits
            }
        }
        geometry.add(vertsData, dstOffset = 12)
        geometry.indicesAsQuad()
    }


    enum class InputMap {
        UI_LEFT,
        UI_RIGHT,
        UI_UP,
        UI_DOWN,
        UI_ACCEPT,
        UI_FOCUS_NEXT,
        UI_FOCUS_PREV,
        MOVE_LEFT,
        MOVE_RIGHT,
        MOVE_UP,
        MOVE_DOWN,
        HORIZONTAL,
        VERTICAL,
        MOVEMENT,
        JUMP
    }

    init {
        Logger.setLevels(Logger.Level.DEBUG)

        controller.addBinding(InputMap.MOVE_LEFT, listOf(Key.A, Key.ARROW_LEFT), axes = listOf(GameAxis.LX))
        controller.addBinding(InputMap.MOVE_RIGHT, listOf(Key.D, Key.ARROW_RIGHT), axes = listOf(GameAxis.LX))
        controller.addBinding(InputMap.MOVE_UP, listOf(Key.W, Key.ARROW_UP), axes = listOf(GameAxis.LY))
        controller.addBinding(InputMap.MOVE_DOWN, listOf(Key.S, Key.ARROW_DOWN), axes = listOf(GameAxis.LY))
        controller.addBinding(InputMap.JUMP, listOf(Key.SPACE), buttons = listOf(GameButton.XBOX_A))
        controller.addAxis(InputMap.HORIZONTAL, InputMap.MOVE_RIGHT, InputMap.MOVE_LEFT)
        controller.addAxis(InputMap.VERTICAL, InputMap.MOVE_DOWN, InputMap.MOVE_UP)
        controller.addVector(
            InputMap.MOVEMENT,
            InputMap.MOVE_RIGHT,
            InputMap.MOVE_DOWN,
            InputMap.MOVE_LEFT,
            InputMap.MOVE_UP
        )
    }


    override suspend fun Context.start() {
        super.setSceneCallbacks(this)
        input.catchKeys += listOf(Key.TAB, Key.SPACE, Key.ARROW_LEFT, Key.ARROW_RIGHT, Key.ARROW_DOWN, Key.ARROW_UP)
        val batch = SpriteBatch(context, 8191)
        val pixelFontTexture = resourcesVfs["m5x7_16_0.png"].readTexture()
        val texture by assetProvider.load<Texture>(resourcesVfs["atlas.png"])
        val tiles: TextureAtlas = resourcesVfs["tiles.atlas.json"].readAtlas()
        // load the textures manually for the ortho map
        val cavernasTexture =
            resourcesVfs["tiled/Cavernas_by_Adam_Saltsman.png"].readPixmap().addBorderToSlices(context, 8, 8, 2)
        val background = resourcesVfs["ldtk/N2D - SpaceWallpaper1280x448.png"].readTexture()

        val atlas: TextureAtlas = MutableTextureAtlas(context)
            .add(tiles)
            .add(pixelFontTexture.slice(), "pixelFont")
            .add(cavernasTexture.slice(), "Cavernas_by_Adam_Saltsman.png")
            .add(background.slice(), "N2D - SpaceWallpaper1280x448.png")
            .toImmutable()

        // we need to dispose of them if we aren't using them since the atlas generates new textures
        cavernasTexture.dispose()
        background.dispose()

        val slices: Array<Array<TextureSlice>> by assetProvider.prepare { texture.slice(16, 16) }
        val person by assetProvider.prepare { slices[0][0] }
        val bossFrame by assetProvider.prepare { atlas.getByPrefix("bossAttack7") }
        val bossAttack by assetProvider.prepare { atlas.getAnimation("bossAttack") }
        val boss by assetProvider.prepare { AnimatedSprite(bossAttack.firstFrame) }
        val pixelFont = resourcesVfs["m5x7_16.fnt"].readBitmapFont(preloadedTextures = listOf(atlas["pixelFont"].slice))
        val cache = BitmapFontCache(pixelFont).also {
            it.addText("Test cache", 200f, 50f, scaleX = 4f, scaleY = 4f)
            it.addText(
                "Test wrapping lines should work",
                200f,
                85f,
                scaleX = 4f,
                scaleY = 4f,
                targetWidth = 200f,
                wrap = true
            )
            it.addText(
                "Test truncating lines should work",
                400f,
                85f,
                scaleX = 4f,
                scaleY = 4f,
                targetWidth = 500f,
                truncate = "...",
            )
            it.tint(Color.RED)
        }

        val ldtkMapLoader by assetProvider.load<LDtkMapLoader>(
            resourcesVfs["ldtk/sample-1.0.ldtk"],
            LDtkGameAssetParameter(atlas, 2)
        )
        val ldtkWorld by assetProvider.prepare {
            ldtkMapLoader.loadMap(true)
        }
        val ninepatchImg by assetProvider.load<Texture>(resourcesVfs["bg_9.png"])
        val ninepatch by assetProvider.prepare { NinePatch(ninepatchImg, 3, 3, 3, 4) }
        val greyButtonNinePatch = NinePatch(
            Textures.atlas.getByPrefix("grey_button").slice,
            5,
            5,
            5,
            4
        )

        val panelNinePatch = NinePatch(
            Textures.atlas.getByPrefix("grey_panel").slice,
            6,
            6,
            6,
            6
        )
        val theme = createDefaultTheme(
            extraDrawables = mapOf(
                "Button" to mapOf(
                    Button.themeVars.normal to NinePatchDrawable(greyButtonNinePatch)
                        .apply {
                            modulate = Color.GREEN
                            minHeight = 50f
                        }),
                "Panel" to mapOf(
                    Panel.themeVars.panel to NinePatchDrawable(panelNinePatch).apply {
                        modulate = Color.GREEN
                    }
                )
            )
        )

        val panel: Container
        val rootControl: Control
        val progressBar: ProgressBar

        val scene by assetProvider.prepare {
            sceneGraph(
                context,
                viewport = ExtendViewport(960, 540),
                batch = batch,
                uiSignals,
                controller = controller
            ) {
                rootControl = control {
                    anchorRight = 1f
                    anchorBottom = 1f

                    centerContainer {
                        anchorRight = 1f
                        anchorBottom = 1f
                        vBoxContainer {
                            separation = 20
                            label {
                                text = "Select a Sample:"
                            }

                            vBoxContainer {
                                separation = 10
                                button {
                                    text = "Platformer - Collect all the Diamonds!"
                                }
                                button {
                                    text = "Another!!"
                                }
                            }

                            button {
                                text = "Exit"
                                onPressed += {
                                    context.close()
                                }
                            }
                        }
                    }

                    paddedContainer {
                        padding(10)
                        hBoxContainer {
                            separation = 20
                            vBoxContainer {
                                separation = 20

                                button {
                                    text = "Top Left"
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(100.milliseconds)
                                        input.showSoftKeyboard()
                                    }
                                }
                                button {
                                    text = "Center Left"
                                    horizontalAlign = HAlign.RIGHT
                                    verticalAlign = VAlign.BOTTOM
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(200.milliseconds)
                                        input.hideSoftKeyboard()
                                    }
                                }
                                button {
                                    text = "Bottom Left"
                                    horizontalAlign = HAlign.LEFT
                                    verticalAlign = VAlign.TOP
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(300.milliseconds)
                                    }
                                }
                            }
                            vBoxContainer {
                                separation = 20

                                button {
                                    text = "Top Center"
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(100.milliseconds)
                                    }
                                }
                                button {
                                    text = "Center Center"
                                    horizontalAlign = HAlign.RIGHT
                                    verticalAlign = VAlign.BOTTOM
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(200.milliseconds)
                                    }
                                }
                                button {
                                    text = "Bottom Center"
                                    horizontalAlign = HAlign.LEFT
                                    verticalAlign = VAlign.TOP
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(300.milliseconds)
                                    }
                                }
                            }
                            vBoxContainer {
                                separation = 20

                                button {
                                    text = "Top Right"
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(100.milliseconds)
                                    }
                                }
                                button {
                                    text = "Center Right"
                                    horizontalAlign = HAlign.RIGHT
                                    verticalAlign = VAlign.BOTTOM
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(200.milliseconds)
                                    }
                                }
                                button {
                                    text = "Bottom Right"
                                    horizontalAlign = HAlign.LEFT
                                    verticalAlign = VAlign.TOP
                                    onPressed += {
                                        logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                                        input.vibrate(300.milliseconds)
                                    }
                                }
                            }
                        }
                    }

                    button {
                        x = 600f
                        y = 400f
                        text = "Over yonder"
                        onPressed += {
                            logger.info { "You pressed me!! I am at ${globalX},${globalY}" }
                            input.vibrate(100.milliseconds)
                        }
                    }

                    vBoxContainer {
                        x = 600f
                        y = 200f

                        label {
                            text = "Username:"
                        }
                        lineEdit {
                            placeholderText = "Enter Username"
                            minWidth = 150f
                        }

                        label {
                            text = "Password:"
                        }
                        lineEdit {
                            placeholderText = "Enter Password"
                            secret = true
                            minWidth = 150f
                        }
                        label {
                            text = "Your Code:"
                        }
                        lineEdit {
                            text = "542849"
                            editable = false
                            minWidth = 150f
                        }
                    }

                    textureRect {
                        slice = person
                        stretchMode = TextureRect.StretchMode.TILE
                        width = 75f
                        height = 60f
                        x = 500f
                        y = 400f
                    }

                    panelContainer {
                        x = 300f
                        y = 150f

                        width = 200f
                        height = 50f

                        panel = paddedContainer {
                            padding(10)
                            centerContainer {
                                vBoxContainer {
                                    separation = 50
                                    label {
                                        text = "Action"
                                        horizontalAlign = HAlign.CENTER
                                    }

                                    label {
                                        text = "E"
                                        horizontalAlign = HAlign.CENTER
                                    }
                                }
                            }
                        }
                    }

                    progressBar = progressBar {
                        anchorLeft = 1f
                        anchorRight = 1f
                        x = 200f
                        y = 300f
                        width = 200f

                        ratio = 0.27f
                    }

                    hBoxContainer {
                        x = 200f
                        y = 275f
                        separation = 10

                        vBoxContainer {
                            textureProgress {
                                this.progressBar = ninepatchImg.slice()
                                ratio = 0.43f
                                progressBarColor = Color.DARK_GREEN
                                backgroundColor = Color.DARK_GRAY
                                foregroundColor = Color(1f, 0f, 1f, 0.5f)

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }

                            textureProgress {
                                this.progressBar = ninepatchImg.slice()
                                fillMode = TextureProgress.FillMode.RIGHT_TO_LEFT
                                ratio = 0.43f
                                progressBarColor = Color.DARK_BLUE

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }
                            textureProgress {
                                this.progressBar = ninepatchImg.slice()
                                fillMode = TextureProgress.FillMode.TOP_TO_BOTTOM
                                ratio = 0.43f
                                progressBarColor = Color.DARK_ORANGE

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }

                            textureProgress {
                                this.progressBar = ninepatchImg.slice()
                                fillMode = TextureProgress.FillMode.BOTTOM_TO_TOP
                                ratio = 0.43f
                                progressBarColor = Color.DARK_RED

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }
                        }

                        vBoxContainer {
                            textureProgress {
                                this.background = ninepatchImg.slice()
                                foreground = ninepatchImg.slice()
                                this.progressBar = ninepatchImg.slice()
                                ratio = 0.43f
                                left = 3
                                right = 3
                                top = 3
                                bottom = 4
                                useNinePatch = true
                                minWidth = 80f
                                minHeight = 16f
                                progressBarColor = Color.DARK_GREEN
                                backgroundColor = Color.DARK_GRAY
                                foregroundColor = Color(1f, 0f, 1f, 0.5f)

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }

                            textureProgress {
                                this.progressBar = ninepatchImg.slice()
                                fillMode = TextureProgress.FillMode.RIGHT_TO_LEFT
                                ratio = 0.43f
                                left = 3
                                right = 3
                                top = 3
                                bottom = 4
                                useNinePatch = true
                                minWidth = 80f
                                minHeight = 16f
                                progressBarColor = Color.DARK_BLUE

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }
                            textureProgress {
                                this.progressBar = ninepatchImg.slice()
                                fillMode = TextureProgress.FillMode.TOP_TO_BOTTOM
                                ratio = 0.43f
                                left = 3
                                right = 3
                                top = 3
                                bottom = 4
                                useNinePatch = true
                                minWidth = 80f
                                minHeight = 16f
                                progressBarColor = Color.DARK_ORANGE

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }

                            textureProgress {
                                this.progressBar = ninepatchImg.slice()
                                fillMode = TextureProgress.FillMode.BOTTOM_TO_TOP
                                ratio = 0.43f
                                left = 3
                                right = 3
                                top = 3
                                bottom = 4
                                useNinePatch = true
                                minWidth = 80f
                                minHeight = 16f
                                progressBarColor = Color.DARK_RED

                                onUpdate += {
                                    if (input.isKeyPressed(Key.Z)) {
                                        value--
                                    }
                                    if (input.isKeyPressed(Key.X)) {
                                        value++
                                    }
                                }
                            }
                        }
                    }
                    panel {
                        x = 100f
                        y = 150f
                        width = 50f
                        height = 50f
                    }

                    label {
                        text = "I am a label!"
                        x = 150f
                        y = 350f
                    }

                    ninePatchRect {
                        this.texture = ninepatchImg.slice()
                        left = 3
                        right = 3
                        top = 3
                        bottom = 4
                        x = 650f
                        y = 10f
                        width = 200f
                        height = 50f
                    }
                }
            }.also { it.initialize() }
        }

        val music by assetProvider.load<AudioStream>(resourcesVfs["music_short.mp3"])
        assetProvider.prepare {
            KtScope.launch {
                music.play(0.05f, true)
            }
            KtScope.launch {
                delay(2500)
                music.pause()
                delay(2500)
                music.resume()
                delay(1000)
                music.stop()
                delay(2000)
                music.play(0.05f, true)
            }
        }

        assetProvider.prepare {
            boss.playLooped(bossAttack)
            boss.x = 450f
            boss.y = 250f
            boss.scaleX = 2f
            boss.scaleY = 2f
        }

        assetProvider.onFullyLoaded = {
            scene.resize(graphics.width, graphics.height, true)
        }
        input.inputProcessor {
            val temp = MutableVec2f()
            onTouchDown { screenX, screenY, pointer ->
                logger.info { "pointer down at $screenX,$screenY: $pointer" }
            }

            onMouseMoved { screenX, screenY ->
                camera.screenToWorld(context, screenX, screenY, viewport, temp)
                x = temp.x
                y = temp.y
            }

            onTouchDragged { screenX, screenY, pointer ->
                camera.screenToWorld(context, screenX, screenY, viewport, temp)
                x = temp.x
                y = temp.y
            }

            onKeyUp {
                logger.info { "key up: $it" }
            }

            onKeyDown {
                logger.info { "key down: $it" }
            }

            onKeyRepeat {
                logger.info { "key repeated: $it" }
            }

            onCharTyped {
                logger.info { "char typed: '$it'" }
            }
        }

        onResize { width, height ->
            viewport.update(width, height, context, true)

            println("${graphics.backBufferWidth},${graphics.backBufferHeight}")
            if (!assetProvider.fullyLoaded) return@onResize
            scene.resize(width, height, true)
        }

        var firstRender = true
        var done = false
        var firstLoaded = true
        onRender { dt ->
            if (!assetProvider.fullyLoaded) {
                assetProvider.update()
                return@onRender
            }
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)
            if (firstLoaded) {
                logger.info { "Finished loading!" }
                firstLoaded = false
            }

            if (!firstRender && !done) {
                done = true
                panel.run {
                    label {
                        text = "Added later!"
                        horizontalAlign = HAlign.CENTER
                        verticalAlign = VAlign.CENTER
                    }
                }
            } else {
                firstRender = false
            }
            xVel = 0f
            yVel = 0f

            val velocity = controller.vector(InputMap.MOVEMENT)
            xVel = velocity.x * 10f
            yVel = velocity.y * 10f

            if (controller.pressed(InputMap.JUMP)) {
                yVel -= 25f
            }
            if (input.isKeyJustPressed(Key.ENTER)) {
                if (rootControl.theme == null) {
                    rootControl.theme = theme
                } else {
                    rootControl.theme = null
                }
            }

            if (input.isKeyPressed(Key.Z)) {
                progressBar.value -= progressBar.step
            } else if (input.isKeyPressed(Key.X)) {
                progressBar.value += progressBar.step
            }

            viewport.apply(context)
            camera.update()
            boss.update(dt)
            batch.use(camera.viewProjection) {
                ldtkWorld.render(it, camera, scale = 4f)
                it.draw(
                    person,
                    x,
                    y,
                    scaleX = 10f,
                    scaleY = 10f,
                    originX = person.width / 2f,
                    originY = person.height / 2f
                )
                slices.forEachIndexed { rowIdx, row ->
                    row.forEachIndexed { colIdx, slice ->
                        it.draw(slice, 150f * (rowIdx * row.size + colIdx) + 50f, 50f, scaleX = 10f, scaleY = 10f)
                    }
                }
                boss.render(it)
                it.draw(bossFrame.slice, 450f, 350f)
                cache.draw(it)
                it.draw(Textures.white, 200f, 400f, scaleX = 5f, scaleY = 5f)
            }

            scene.update(dt)
            scene.render()

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
        onDispose {
            scene.dispose()
            mesh.dispose()
            texture.dispose()
            shader.dispose()
            batch.dispose()
        }
    }
}