package com.lehaine.littlekt

import com.lehaine.littlekt.node.Node
import com.lehaine.littlekt.node.node

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest : LittleKt() {

    override fun create() {
        super.create()

        scene = DisplayTestScene()
    }
}

class DisplayTestScene : Scene() {
    override fun Node.initialize() {

        node {
            name = "test"

            node { name = "test 2" }
        }
    }

    override fun onStart() {
        super.onStart()

        println(root.treeString())
    }

    override fun update() {
        super.update()
    }
}

fun main(args: Array<String>) {
    val game = DisplayTest()

    ApplicationContext().start(game)
}