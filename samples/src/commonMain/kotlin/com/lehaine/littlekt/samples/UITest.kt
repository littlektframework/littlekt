package com.lehaine.littlekt.samples

import com.lehaine.littlekt.Context
import com.lehaine.littlekt.Game
import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.graph.node.component.AnchorLayout
import com.lehaine.littlekt.graph.node.`2d`.ui.*
import com.lehaine.littlekt.graph.sceneGraph
import com.lehaine.littlekt.graphics.Color
import com.lehaine.littlekt.graphics.Textures
import com.lehaine.littlekt.graphics.font.TtfFont
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.viewport.ExtendViewport
import kotlin.time.Duration


/**
 * @author Colton Daily
 * @date 1/2/2022
 */
class UITest(context: Context) : Game<Scene>(context) {

    val freeSerif by load<TtfFont>(resourcesVfs["FreeSerif.ttf"])

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

                    textureRect {
                        slice = Textures.red
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.TOP_RIGHT)
                    debugColor = Color.BLUE


                    textureRect {
                        slice = Textures.blue
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.BOTTOM_LEFT)
                    debugColor = Color.GREEN


                    textureRect {
                        slice = Textures.green
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.BOTTOM_RIGHT)
                    debugColor = Color.YELLOW


                    textureRect {
                        slice = Textures.green
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
                        textureRect {
                            slice = Textures.white
                        }
                        textureRect {
                            slice = Textures.white
                        }

                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.CENTER_RIGHT)

                    textureRect {
                        slice = Textures.green
                    }
                }

                paddedContainer {
                    padding = 10f
                    anchor(AnchorLayout.CENTER_BOTTOM)

                    textureRect {
                        slice = Textures.blue
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
                        textureRect {
                            slice = Textures.red
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