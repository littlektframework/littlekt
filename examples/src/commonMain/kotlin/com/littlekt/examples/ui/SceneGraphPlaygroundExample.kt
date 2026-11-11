package com.littlekt.examples.ui

import com.littlekt.Context
import com.littlekt.ContextListener
import com.littlekt.configureSurfaceOnResize
import com.littlekt.examples.addCloseOnEsc
import com.littlekt.examples.addStatsHandler
import com.littlekt.file.vfs.readTexture
import com.littlekt.graph.node.Node
import com.littlekt.graph.node.canvasLayer
import com.littlekt.graph.node.node2d.camera2d
import com.littlekt.graph.node.node2d.node2d
import com.littlekt.graph.node.resource.ThemePalette
import com.littlekt.graph.node.resource.createDefaultTheme
import com.littlekt.graph.node.ui.*
import com.littlekt.graph.sceneGraph
import com.littlekt.graphics.Color
import com.littlekt.graphics.HAlign
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
 * scrolling, textures, panels, containers and a canvas layer with 2D nodes.
 *
 * @author Colton Daily
 * @date 11/8/2026
 */
class SceneGraphPlaygroundExample(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {
        addStatsHandler()
        addCloseOnEsc()
        configureSurfaceOnResize()
        val icon = resourcesVfs["icon_16x16.png"].readTexture()
        val ninePatch = resourcesVfs["bg_9.png"].readTexture()
        val bigIcon = resourcesVfs["icon_32x32.png"].readTexture()
        val iconSlice = icon.slice()
        val srgb = graphics.preferredFormat.srgb
        val darkTheme = createDefaultTheme(palette = ThemePalette.DARK, srgb = srgb)
        val lightTheme = createDefaultTheme(palette = ThemePalette.LIGHT, srgb = srgb)
        var clearColor = graphics.surfaceColor(ThemePalette.DARK.background)

        val graph =
            sceneGraph(this, ExtendViewport(960, 540)) {
                    paddedContainer {
                        val themed = this
                        theme = darkTheme
                        anchor(Control.AnchorLayout.FULL)
                        paddingLeft = 10
                        paddingRight = 10
                        paddingTop = 10
                        paddingBottom = 10

                        column {
                            separation = 10

                            lateinit var status: Label
                            row {
                                separation = 20
                                label { text = "Scene Graph Playground" }
                                status = label {
                                    text = "Interact with something!"
                                    horizontalSizing = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND
                                    horizontalAlign = HAlign.RIGHT
                                }
                                button {
                                    text = "Light theme"
                                    toggleMode = true
                                    onToggled += { light ->
                                        val palette = if (light) ThemePalette.LIGHT else ThemePalette.DARK
                                        themed.theme = if (light) lightTheme else darkTheme
                                        clearColor = graphics.surfaceColor(palette.background)
                                    }
                                }
                            }

                            row {
                                separation = 10
                                verticalSizing = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND

                                section("Buttons") {
                                    var clicks = 0
                                    button {
                                        text = "Click me"
                                        onPressed += {
                                            clicks++
                                            status.text = "Clicked $clicks times"
                                        }
                                    }
                                    button {
                                        text = "Toggle"
                                        toggleMode = true
                                        onToggled += { status.text = "Toggled: $it" }
                                    }
                                    button {
                                        text = "Disabled"
                                        disabled = true
                                    }
                                    label { text = "Button group" }
                                    val group = ButtonGroup()
                                    repeat(3) {
                                        button {
                                            text = "Option ${it + 1}"
                                            toggleMode = true
                                            pressed = it == 0
                                            setButtonGroup(group)
                                            onPressed += { status.text = "Selected $text" }
                                        }
                                    }
                                }

                                section("Text") {
                                    label {
                                        text = "Left"
                                        horizontalAlign = HAlign.LEFT
                                    }
                                    label {
                                        text = "Center"
                                        horizontalAlign = HAlign.CENTER
                                    }
                                    label {
                                        text = "Right"
                                        horizontalAlign = HAlign.RIGHT
                                    }
                                    label {
                                        text = "uppercase label"
                                        uppercase = true
                                    }
                                    label {
                                        text = "This label is too long so it ends with an ellipsis"
                                        ellipsis = "..."
                                        horizontalSizing = Control.SizeFlag.FILL
                                        minWidth = 1f
                                    }
                                    label {
                                        text = "This label wraps its text onto more lines"
                                        wrap = true
                                        horizontalSizing = Control.SizeFlag.FILL
                                        minWidth = 1f
                                    }
                                    lineEdit {
                                        placeholderText = "Type here..."
                                        minWidth = 150f
                                    }
                                    lineEdit {
                                        text = "password"
                                        secret = true
                                        minWidth = 150f
                                    }
                                    lineEdit {
                                        text = "Read only"
                                        editable = false
                                        minWidth = 150f
                                    }
                                }

                                section("Ranges") {
                                    progressBar {
                                        onUpdate += { dt -> value = (value + dt.seconds * 20f) % max }
                                    }
                                    label { text = "Scroll bar" }
                                    val slider = progressBar { value = 50f }
                                    hScrollBar {
                                        page = 10f
                                        max = 110f
                                        value = 50f
                                        onValueChanged += { slider.value = it }
                                    }
                                    label { text = "Texture progress" }
                                    textureProgress {
                                        background = bigIcon.slice()
                                        progressBar = bigIcon.slice()
                                        backgroundColor = Color.DARK_GRAY
                                        fillMode = TextureProgress.FillMode.BOTTOM_TO_TOP
                                        onUpdate += { dt -> value = (value + dt.seconds * 30f) % max }
                                    }
                                    row {
                                        verticalSizing = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND
                                        scrollContainer {
                                            horizontalSizing = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND
                                            column {
                                                repeat(20) { label { text = "Scroll item $it" } }
                                            }
                                        }
                                        vScrollBar {
                                            page = 20f
                                            max = 120f
                                            onValueChanged += { status.text = "Vertical: ${it.toInt()}" }
                                        }
                                    }
                                }

                                section("Textures") {
                                    row {
                                        separation = 5
                                        textureRect {
                                            slice = iconSlice
                                            stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                                            minWidth = 32f
                                            minHeight = 32f
                                        }
                                        textureRect {
                                            slice = iconSlice
                                            stretchMode = TextureRect.StretchMode.SCALE
                                            minWidth = 32f
                                            minHeight = 32f
                                        }
                                        textureRect {
                                            slice = iconSlice
                                            stretchMode = TextureRect.StretchMode.TILE
                                            minWidth = 48f
                                            minHeight = 32f
                                        }
                                        textureRect {
                                            slice = iconSlice
                                            stretchMode = TextureRect.StretchMode.KEEP_CENTERED
                                            flipX = true
                                            flipY = true
                                            minWidth = 32f
                                            minHeight = 32f
                                        }
                                    }
                                    label { text = "Nine patch" }
                                    ninePatchRect {
                                        texture = ninePatch.slice()
                                        left = 2
                                        right = 2
                                        top = 2
                                        bottom = 4
                                        color = Color.LIGHT_BLUE
                                        minHeight = 30f
                                    }
                                    label { text = "Center container" }
                                    panel {
                                        minHeight = 40f
                                        centerContainer {
                                            anchor(Control.AnchorLayout.FULL)
                                            label { text = "Centered" }
                                        }
                                    }
                                    label { text = "Canvas layer" }
                                    canvasLayerContainer {
                                        stretch = true
                                        verticalSizing = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND
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
                            }
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

    private inline fun Node.section(title: String, content: VBoxContainer.() -> Unit) {
        panelContainer {
            horizontalSizing = Control.SizeFlag.FILL or Control.SizeFlag.EXPAND
            paddedContainer {
                paddingLeft = 8
                paddingRight = 8
                paddingTop = 8
                paddingBottom = 8
                column {
                    separation = 6
                    label { text = title }
                    content()
                }
            }
        }
    }
}
