package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.SceneGraph
import com.lehaine.littlekt.graph.node.node
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.slice
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.random
import com.lehaine.littlekt.util.fastForEach

/**
 * @author Colton Daily
 * @date 10/26/2022
 */
class UiPlayground(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {

        val ninepatchImg = resourcesVfs["bg_9.png"].readTexture()
        val ninepatchSlice = ninepatchImg.slice()
        val ktHeadSlice = resourcesVfs["ktHead.png"].readTexture().slice()

        fun SceneGraph<String>.initNodes() {
            panel {
                anchorRight = 1f
                anchorBottom = 1f

                paddedContainer {
                    padding(10)

                    row {
                        separation = 20

                        column {
                            separation = 20

                            column {
                                separation = 10
                                label {
                                    text = "----Labels----"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.CENTER
                                }
                                label {
                                    text = "Different colored label"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.CENTER
                                    fontColor = com.lehaine.littlekt.graphics.Color.RED
                                }
                                label {
                                    text = "Truncated long text that has ellipsis to continue"
                                    ellipsis = "..."
                                    width = 150f
                                }

                                label { text = "Horizontal Alignment:" }
                                label { text = "Left" }
                                label {
                                    text = "Center"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.CENTER
                                }
                                label {
                                    text = "Right"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.RIGHT
                                }

                                label { text = "Vertical Alignment:" }
                                row {
                                    separation = 10
                                    minHeight = 50f
                                    align = com.lehaine.littlekt.graph.node.resource.AlignMode.CENTER
                                    label {
                                        text = "Top"
                                        verticalSizeFlags = com.lehaine.littlekt.graph.node.ui.Control.SizeFlag.FILL
                                    }
                                    label {
                                        text = "Center"
                                        verticalAlign = com.lehaine.littlekt.graph.node.resource.VAlign.CENTER
                                        verticalSizeFlags = com.lehaine.littlekt.graph.node.ui.Control.SizeFlag.FILL
                                    }
                                    label {
                                        text = "Bottom"
                                        verticalAlign = com.lehaine.littlekt.graph.node.resource.VAlign.BOTTOM
                                        verticalSizeFlags = com.lehaine.littlekt.graph.node.ui.Control.SizeFlag.FILL
                                    }
                                }
                            }

                            column {
                                separation = 10
                                label { text = "----Line Edits (Text Fields)----" }
                                label { text = "Normal:" }
                                lineEdit {
                                    text = "you can see this"
                                    minWidth = 150f
                                }

                                label { text = "Secret text:" }
                                lineEdit {
                                    secret = true
                                    text = "you can't see this"
                                    minWidth = 150f
                                }
                            }
                        }

                        column {
                            separation = 20

                            column {
                                separation = 10

                                label {
                                    text = "----Buttons----"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.CENTER
                                }

                                separation = 10
                                button {
                                    var presses = 0
                                    text = "Press Me!"
                                    onPressed += {
                                        presses++
                                        text = "Pressed $presses time(s)!"
                                    }
                                }

                                button {
                                    text = "Press to change font color"
                                    onPressed += {
                                        fontColor =
                                            Color((0f..1f).random(), (0f..1f).random(), (0f..1f).random(), 1f)
                                    }
                                }

                                button {
                                    text = "Left Align"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.LEFT
                                }

                                button {
                                    text = "Right Align"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.RIGHT
                                }

                                val panel = panelContainer {
                                    visible = false

                                    paddedContainer {
                                        padding(10)

                                        label {
                                            text = "I'm visible!"
                                        }
                                    }
                                }

                                button {
                                    text = "Press to toggle a panel."
                                    onPressed += {
                                        panel.visible = !panel.visible
                                    }
                                }
                            }

                            column {
                                separation = 10
                                label {
                                    text = "----Ranges----"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.CENTER
                                }
                                label { text = "Progress bar:" }
                                row {
                                    val progressBar = ProgressBar()
                                    button {
                                        text = "-"
                                        onUpdate += {
                                            if (pressed) {
                                                progressBar.value -= progressBar.step
                                            }
                                        }
                                    }
                                    node(progressBar) {
                                        ratio = 0.66f
                                        minWidth = 100f
                                    }
                                    button {
                                        text = "+"
                                        onUpdate += {
                                            if (pressed) {
                                                progressBar.value += progressBar.step
                                            }
                                        }
                                    }
                                }

                                label { text = "Texture Progress bars:" }

                                val progessBars = mutableListOf<TextureProgress>()
                                row {
                                    separation = 10
                                    column {
                                        align = com.lehaine.littlekt.graph.node.resource.AlignMode.CENTER
                                        button {
                                            text = "-"
                                            onUpdate += {
                                                if (pressed) {
                                                    progessBars.fastForEach { it.value -= it.step }
                                                }
                                            }
                                        }

                                        button {
                                            text = "+"
                                            onUpdate += {
                                                if (pressed) {
                                                    progessBars.fastForEach { it.value += it.step }
                                                }
                                            }
                                        }
                                    }
                                    column {
                                        separation = 10
                                        row {
                                            separation = 10
                                            label { text = "Left to right:" }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                ratio = 0.5f
                                                progressBarColor = com.lehaine.littlekt.graphics.Color.DARK_GREEN
                                                backgroundColor = com.lehaine.littlekt.graphics.Color.DARK_GRAY
                                            }
                                        }

                                        row {
                                            separation = 10
                                            label { text = "Right to left: " }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                fillMode =
                                                    com.lehaine.littlekt.graph.node.ui.TextureProgress.FillMode.RIGHT_TO_LEFT
                                                ratio = 0.5f
                                                progressBarColor = com.lehaine.littlekt.graphics.Color.DARK_BLUE
                                                backgroundColor = com.lehaine.littlekt.graphics.Color.DARK_GRAY
                                            }
                                        }

                                        row {
                                            separation = 10
                                            label { text = "Top to bottom: " }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                fillMode =
                                                    com.lehaine.littlekt.graph.node.ui.TextureProgress.FillMode.TOP_TO_BOTTOM
                                                ratio = 0.5f
                                                progressBarColor = com.lehaine.littlekt.graphics.Color.DARK_ORANGE
                                                backgroundColor = com.lehaine.littlekt.graphics.Color.DARK_GRAY
                                            }
                                        }

                                        row {
                                            separation = 10
                                            label { text = "Bottom to top:" }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                fillMode =
                                                    com.lehaine.littlekt.graph.node.ui.TextureProgress.FillMode.BOTTOM_TO_TOP
                                                ratio = 0.5f
                                                progressBarColor = com.lehaine.littlekt.graphics.Color.DARK_RED
                                                backgroundColor = com.lehaine.littlekt.graphics.Color.DARK_GRAY
                                            }
                                        }
                                    }
                                }

                                label { text = "Scrollbars:" }
                                vScrollBar {
                                    page = 10f
                                    minHeight = 100f
                                }
                                hScrollBar {
                                    page = 10f
                                }
                            }
                        }

                        column {
                            separation = 20
                            column {
                                separation = 5
                                label {
                                    text = "----Texture & Ninepatch Rects----"
                                    horizontalAlign = com.lehaine.littlekt.graph.node.resource.HAlign.CENTER
                                }

                                ninePatchRect {
                                    texture = ninepatchSlice
                                    left = 3
                                    right = 3
                                    top = 3
                                    bottom = 4
                                    width = 100f
                                    minHeight = 50f
                                    color = com.lehaine.littlekt.graphics.Color.DARK_GRAY
                                }

                                textureRect {
                                    slice = ktHeadSlice
                                    stretchMode =
                                        com.lehaine.littlekt.graph.node.ui.TextureRect.StretchMode.KEEP_CENTERED
                                }

                                label { text = "Stretch Modes:" }

                                row {
                                    label { text = "Keep:" }
                                    textureRect {
                                        slice = ktHeadSlice
                                    }
                                }

                                row {
                                    label { text = "Keep Aspect:" }
                                    textureRect {
                                        slice = ktHeadSlice
                                        stretchMode =
                                            com.lehaine.littlekt.graph.node.ui.TextureRect.StretchMode.KEEP_ASPECT
                                        minWidth = 64f
                                    }
                                }


                                row {
                                    label { text = "Keep Aspect Covered:" }
                                    textureRect {
                                        slice = ktHeadSlice
                                        stretchMode =
                                            com.lehaine.littlekt.graph.node.ui.TextureRect.StretchMode.KEEP_ASPECT_COVERED
                                        minWidth = 64f
                                    }
                                }


                                row {
                                    label { text = "Keep Aspect Centered:" }
                                    textureRect {
                                        slice = ktHeadSlice
                                        stretchMode =
                                            com.lehaine.littlekt.graph.node.ui.TextureRect.StretchMode.KEEP_ASPECT_CENTERED
                                        minWidth = 64f
                                    }
                                }

                                row {
                                    label { text = "Scale:" }
                                    textureRect {
                                        slice = ktHeadSlice
                                        stretchMode = com.lehaine.littlekt.graph.node.ui.TextureRect.StretchMode.SCALE
                                        minWidth = 64f
                                    }
                                }

                                row {
                                    label { text = "Tile:" }
                                    textureRect {
                                        slice = ktHeadSlice
                                        stretchMode = com.lehaine.littlekt.graph.node.ui.TextureRect.StretchMode.TILE
                                        minWidth = 96f
                                        minHeight = 64f
                                    }
                                }
                            }
                        }

                        column {
                            separation = 20

                            column {
                                separation = 10

                                label { text = "----Containers----" }

                                panelContainer {
                                    label { text = "Panel Container" }
                                }


                                panelContainer {
                                    paddedContainer {
                                        padding(10)
                                        label { text = "Padded Container" }
                                    }
                                }

                                panelContainer {
                                    paddedContainer {
                                        padding(10)
                                        column {
                                            separation = 20
                                            label { text = "Scrolling Container:" }

                                            val labels: VBoxContainer
                                            scrollContainer {
                                                minHeight = 100f
                                                labels = column {
                                                    repeat(20) {
                                                        label {
                                                            text =
                                                                "I am really super duper long and awesome label ${it + 1}"
                                                        }
                                                    }
                                                }
                                            }

                                            button {
                                                text = "Press to add another label"
                                                onPressed += {
                                                    labels.apply {
                                                        label {
                                                            text = "I am a new label and placed at ${labels.childCount}"
                                                        }
                                                    }
                                                }
                                            }



                                            button {
                                                text = "Press to remove a label"
                                                onPressed += {
                                                    if (labels.isNotEmpty()) {
                                                        labels.removeChildAt(labels.childCount - 1)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                panelContainer {
                                    paddedContainer {
                                        padding(10)
                                        column {
                                            separation = 10
                                            label { text = "VBox / Column" }
                                            repeat(4) {
                                                label { text = "I am label ${it + 1}" }
                                            }
                                        }
                                    }
                                }

                                panelContainer {
                                    enabled = false
                                    paddedContainer {
                                        padding(10)
                                        column {
                                            separation = 10
                                            label { text = "HBox / Row" }

                                            row {
                                                separation = 5
                                                repeat(4) {
                                                    label {
                                                        text = "Label ${it + 1}"
                                                        if (it != 3) {
                                                            text += ","
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                panelContainer {
                                    paddedContainer {
                                        padding(10)
                                        centerContainer {
                                            label { text = "Center Container" }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        val graph = sceneGraph(context) {
            initNodes()
        }.also { it.initialize() }

        onResize { width, height ->
            graph.resize(width, height, true)
        }
        onRender { dt ->
            gl.clear(ClearBufferMask.COLOR_BUFFER_BIT)

            graph.update(dt)
            graph.render()

            if (input.isKeyJustPressed(Key.ENTER)) {
                graph.showDebugInfo = !graph.showDebugInfo
            }

            if (input.isKeyJustPressed(Key.R)) {
                graph.root.destroyAllChildren()
                graph.initNodes()
            }

            if (input.isKeyJustPressed(Key.T)) {
                logger.info { "\n" + graph.root.treeString() }
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