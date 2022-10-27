package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.ContextListener
import com.lehaine.littlekt.file.vfs.readTexture
import com.lehaine.littlekt.graph.node.component.AlignMode
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graph.node.node
import com.lehaine.littlekt.graph.node.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.gl.ClearBufferMask
import com.lehaine.littlekt.graphics.slice
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.math.random
import com.lehaine.littlekt.util.fastForEach
import com.lehaine.littlekt.util.viewport.ExtendViewport

/**
 * @author Colton Daily
 * @date 10/26/2022
 */
class UiPlayground(context: Context) : ContextListener(context) {

    override suspend fun Context.start() {

        val ninepatchImg = resourcesVfs["bg_9.png"].readTexture()
        val ninepatchSlice = ninepatchImg.slice()
        val graph = sceneGraph(context, ExtendViewport(960, 540)) {
            panel {
                anchorRight = 1f
                anchorBottom = 1f

                paddedContainer {
                    padding(10)

                    hBoxContainer {
                        separation = 20

                        vBoxContainer {
                            separation = 20

                            vBoxContainer {
                                separation = 10
                                label { text = "**Labels**" }
                                label {
                                    text = "Different colored label"
                                    horizontalAlign = HAlign.CENTER
                                    fontColor = Color.RED
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
                                    horizontalAlign = HAlign.CENTER
                                }
                                label {
                                    text = "Right"
                                    horizontalAlign = HAlign.RIGHT
                                }

                                label { text = "Vertical Alignment:" }
                                hBoxContainer {
                                    separation = 10
                                    minHeight = 50f
                                    align = AlignMode.CENTER
                                    label {
                                        text = "Top"
                                        verticalSizeFlags = Control.SizeFlag.FILL
                                    }
                                    label {
                                        text = "Center"
                                        verticalAlign = VAlign.CENTER
                                        verticalSizeFlags = Control.SizeFlag.FILL
                                    }
                                    label {
                                        text = "Bottom"
                                        verticalAlign = VAlign.BOTTOM
                                        verticalSizeFlags = Control.SizeFlag.FILL
                                    }
                                }
                            }

                            vBoxContainer {
                                separation = 10
                                label { text = "**Line Edits (Text Fields)**" }
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

                        vBoxContainer {
                            separation = 20

                            vBoxContainer {
                                separation = 10

                                label { text = "**Buttons**" }

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
                            }

                            vBoxContainer {
                                separation = 10
                                label { text = "**Ranges**" }
                                label { text = "Progress bar:" }
                                hBoxContainer {
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
                                hBoxContainer {
                                    separation =10
                                    vBoxContainer {
                                        align = AlignMode.CENTER
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
                                    vBoxContainer {
                                        separation =10
                                        hBoxContainer {
                                            separation = 10
                                            label { text = "Left to right:" }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                ratio = 0.5f
                                                progressBarColor = Color.DARK_GREEN
                                                backgroundColor = Color.DARK_GRAY
                                                minWidth = 100f
                                            }
                                        }

                                        hBoxContainer {
                                            separation = 10
                                            label { text = "Right to left: " }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                fillMode = TextureProgress.FillMode.RIGHT_TO_LEFT
                                                ratio = 0.5f
                                                progressBarColor = Color.DARK_BLUE
                                                backgroundColor = Color.DARK_GRAY
                                                minWidth = 100f
                                            }
                                        }

                                        hBoxContainer {
                                            separation = 10
                                            label { text = "Top to bottom: " }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                fillMode = TextureProgress.FillMode.TOP_TO_BOTTOM
                                                ratio = 0.5f
                                                progressBarColor = Color.DARK_ORANGE
                                                backgroundColor = Color.DARK_GRAY
                                                minWidth = 100f
                                            }
                                        }

                                        hBoxContainer {
                                            separation = 10
                                            label { text = "Bottom to top:" }
                                            progessBars += textureProgress {
                                                progressBar = ninepatchSlice
                                                background = ninepatchSlice
                                                fillMode = TextureProgress.FillMode.BOTTOM_TO_TOP
                                                ratio = 0.5f
                                                progressBarColor = Color.DARK_RED
                                                backgroundColor = Color.DARK_GRAY
                                                minWidth = 100f
                                            }
                                        }
                                    }
                                }
                            }

                            vBoxContainer {
                                separation = 10
                                label { text = "**Textures**" }
                            }
                        }
                    }
                }
            }
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