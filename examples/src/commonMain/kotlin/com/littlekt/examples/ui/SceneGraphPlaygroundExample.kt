package com.littlekt.examples.ui

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.vfs.readTexture
import com.littlekt.graph.node.canvasLayer
import com.littlekt.graph.node.node2d.camera2d
import com.littlekt.graph.node.node2d.node2d
import com.littlekt.graph.node.resource.ThemePalette
import com.littlekt.graph.node.resource.createDefaultTheme
import com.littlekt.graph.node.ui.*
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
import com.littlekt.graphics.Texture
import com.littlekt.graphics.g2d.TextureSlice
import com.littlekt.graphics.slice
import com.littlekt.graphics.webgpu.RenderPassDescriptor
import com.littlekt.graphics.webgpu.colorAttachment
import com.littlekt.math.geom.radians
import com.littlekt.renderFrame
import com.littlekt.surfaceColor
import com.littlekt.util.seconds
import com.littlekt.util.viewport.ExtendViewport

/**
 * An example showing every built-in scene graph UI control: buttons, labels, text input, ranges,
 * scrolling, textures, panels, containers and a canvas layer with 2D nodes. Built with the layout
 * shortcuts and [Modifier]s.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class SceneGraphPlaygroundExample(context: Context) : ContextListener(context) {

    private lateinit var status: Label

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val ninePatch = resourcesVfs["bg_9.png"].readTexture()
        val bigIcon = resourcesVfs["icon_32x32.png"].readTexture()
        val srgb = graphics.preferredFormat.srgb
        val darkTheme = createDefaultTheme(palette = ThemePalette.DARK, srgb = srgb)
        val lightTheme = createDefaultTheme(palette = ThemePalette.LIGHT, srgb = srgb)
        var clearColor = graphics.surfaceColor(ThemePalette.DARK.background)

        val graph =
            sceneGraph(this, ExtendViewport(960, 540)) {
                    column(separation = 10, modifier = Modifier.fillParent(10f).theme(darkTheme)) {
                        val themed = this
                        row(separation = 20) {
                            label("Scene Graph Playground")
                            spacer()
                            status = label("Interact with something!")
                            button("Light theme") {
                                toggleMode = true
                                onToggled += { light ->
                                    val palette = if (light) ThemePalette.LIGHT else ThemePalette.DARK
                                    themed.theme = if (light) lightTheme else darkTheme
                                    clearColor = graphics.surfaceColor(palette.background)
                                }
                            }
                        }
                        row(separation = 10, modifier = Modifier.weight()) {
                            buttons()
                            text()
                            ranges(bigIcon)
                            textures(icon, icon.slice(), ninePatch.slice())
                        }
                    }
                }
                .also { it.initialize() }

        onResize { width, height -> graph.resize(width, height) }

        onUpdate { dt ->
            renderFrame("scene graph playground") { frame, commandEncoder ->
                graph.update(dt)
                graph.render(
                    commandEncoder,
                    RenderPassDescriptor(colorAttachment(frame, clearColor), label = "UI render pass"),
                )
            }
        }

        onRelease {
            graph.release()
            icon.release()
            bigIcon.release()
            ninePatch.release()
        }
    }

    private fun HBoxContainer.buttons() = section("Buttons") {
        var clicks = 0
        button("Click me") {
            onPressed += {
                clicks++
                status.text = "Clicked $clicks times"
            }
        }
        button("Toggle") {
            toggleMode = true
            onToggled += { status.text = "Toggled: $it" }
        }
        button("Disabled") { disabled = true }
        button("Aligned right", modifier = Modifier.align(HAlign.RIGHT))
        label("Button group")
        val group = ButtonGroup()
        repeat(3) {
            button("Option ${it + 1}") {
                toggleMode = true
                pressed = it == 0
                setButtonGroup(group)
                onPressed += { status.text = "Selected $text" }
            }
        }
    }

    private fun HBoxContainer.text() = section("Text") {
        label("Left")
        label("Center") { horizontalAlign = HAlign.CENTER }
        label("Right") { horizontalAlign = HAlign.RIGHT }
        label("uppercase label") { uppercase = true }
        label("This label is too long so it ends with an ellipsis", fitWidth) { ellipsis = "..." }
        label("This label wraps its text onto more lines", fitWidth) { wrap = true }
        lineEdit(placeholder = "Type here...", modifier = field)
        lineEdit("password", modifier = field) { secret = true }
        lineEdit("Read only", modifier = field) { editable = false }
    }

    private fun HBoxContainer.ranges(bigIcon: Texture) = section("Ranges") {
        progressBar { onUpdate += { dt -> value = (value + dt.seconds * 20f) % max } }
        label("Scroll bar")
        val slider = progressBar { value = 50f }
        hScrollBar {
            page = 10f
            max = 110f
            value = 50f
            onValueChanged += { slider.value = it }
        }
        label("Texture progress")
        textureProgress {
            background = bigIcon.slice()
            progressBar = bigIcon.slice()
            backgroundColor = Color.DARK_GRAY
            fillMode = TextureProgress.FillMode.BOTTOM_TO_TOP
            onUpdate += { dt -> value = (value + dt.seconds * 30f) % max }
        }
        row(modifier = Modifier.weight()) {
            scrollContainer(Modifier.weight()) {
                column { repeat(20) { label("Scroll item $it") } }
            }
            vScrollBar {
                page = 20f
                max = 120f
                onValueChanged += { status.text = "Vertical: ${it.toInt()}" }
            }
        }
    }

    private fun HBoxContainer.textures(
        icon: Texture,
        iconSlice: TextureSlice,
        ninePatch: TextureSlice,
    ) =
        section("Textures") {
            val iconSize = Modifier.minSize(32f, 32f)
            row(separation = 5) {
                textureRect(iconSize) {
                    slice = iconSlice
                    stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                }
                textureRect(iconSize) {
                    slice = iconSlice
                    stretchMode = TextureRect.StretchMode.SCALE
                }
                textureRect(Modifier.minSize(48f, 32f)) {
                    slice = iconSlice
                    stretchMode = TextureRect.StretchMode.TILE
                }
                textureRect(iconSize) {
                    slice = iconSlice
                    stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                    flipX = true
                    flipY = true
                }
            }
            label("Nine patch")
            ninePatchRect(Modifier.minSize(0f, 30f).tint(Color.LIGHT_BLUE)) {
                texture = ninePatch
                left = 2
                right = 2
                top = 2
                bottom = 4
            }
            label("Center container")
            panel(Modifier.minSize(0f, 40f)) {
                centerContainer(Modifier.fillParent()) { label("Centered") }
            }
            label("Canvas layer")
            canvasLayerContainer(Modifier.weight()) {
                stretch = true
                canvasLayer {
                    node2d { camera2d { active = true } }
                    node2d {
                        onUpdate += { dt -> rotation += (dt.seconds * 2f).radians }
                        onRender += { batch, _, _ ->
                            batch.draw(
                                icon,
                                globalX - 16f,
                                globalY - 16f,
                                originX = 16f,
                                originY = 16f,
                                width = 32f,
                                height = 32f,
                                rotation = globalRotation,
                            )
                        }
                    }
                }
            }
        }

    private fun HBoxContainer.section(title: String, content: VBoxContainer.() -> Unit) {
        column(separation = 6, modifier = Modifier.weight().card()) {
            label(title)
            content()
        }
    }

    private fun Modifier.card() = panel().padding(8)

    private val fitWidth = Modifier.minSize(1f, 0f)

    private val field = Modifier.minSize(150f, 0f)
}
