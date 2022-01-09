package com.lehaine.littlekt.samples

import com.lehaine.littlekt.BitmapFontAssetParameter
import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graph.node.component.AnchorLayout
import com.lehaine.littlekt.graph.node.component.HAlign
import com.lehaine.littlekt.graph.node.component.VAlign
import com.lehaine.littlekt.graph.node.node2d.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Fonts
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.font.BitmapFont
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.graphics.gl.TexMagFilter
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.math.Vec2f
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration


/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class UITest(context: Context) : Game<Scene>(context) {

    val freeSerif by load<TtfFont>(resourcesVfs["FreeSerif.ttf"])
    val defaultFont by load<BitmapFont>(
        resourcesVfs[Fonts.small],
        BitmapFontAssetParameter(magFilter = TexMagFilter.LINEAR)
    )

    val sceneGraph by prepare {
        sceneGraph(context, ExtendViewport(480, 270)) {
            control {
                anchorRight = 1f
                anchorBottom = 1f
                debugColor = Color.WHITE

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.TOP_LEFT)
                    debugColor = Color.RED

                    label {
                        font = defaultFont
                        text = "Top left"
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.TOP_RIGHT)
                    debugColor = Color.BLUE

                    label {
                        font = defaultFont
                        text = "Top right"
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.BOTTOM_LEFT)
                    debugColor = Color.GREEN

                    label {
                        font = defaultFont
                        text = "Bottom left"
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.BOTTOM_RIGHT)
                    debugColor = Color.YELLOW

                    label {
                        font = defaultFont
                        text = "Bottom right"
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.CENTER)
                    name = "orange"
                    debugColor = Color.ORANGE

                    textureRect {
                        slice = Textures.white
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.CENTER_LEFT)

                    vBoxContainer {
                        separation = 10

                        label {
                            font = defaultFont
                            text = "Center left vbox"
                        }
                        textureRect {
                            slice = Textures.red
                        }
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.CENTER_RIGHT)

                    label {
                        font = defaultFont
                        fontScale = Vec2f(2f, 2f)
                        text = "Center right"
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.CENTER_BOTTOM)

                    label {
                        font = defaultFont
                        horizontalAlign = HAlign.CENTER
                        text = "Center\nbottom"
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.CENTER_TOP)

                    hBoxContainer {
                        separation = 10
                        textureRect {
                            slice = Textures.red
                        }
                        label {
                            font = defaultFont
                            verticalAlign = VAlign.TOP
                            text = "Center top\nvert top align"
                        }
                        label {
                            font = defaultFont
                            verticalAlign = VAlign.CENTER
                            horizontalAlign = HAlign.CENTER
                            text = "Center top\nvert center align"
                        }
                        label {
                            font = defaultFont
                            verticalAlign = VAlign.BOTTOM
                            horizontalAlign = HAlign.RIGHT
                            text = "Center top\nvert bottom align"
                        }
                    }
                }
            }
        }
    }

    init {
        Logger.setLevels(Logger.Level.DEBUG)
    }


    override fun create() {
        sceneGraph.initialize()
        logger.info { "\n" + sceneGraph.root.treeString() }
    }

    override fun update(dt: Duration) {
        gl.clearColor(Color.DARK_GRAY)
        sceneGraph.update(dt)
        sceneGraph.render()
    }

    override fun dispose() {
        sceneGraph.dispose()
    }
}