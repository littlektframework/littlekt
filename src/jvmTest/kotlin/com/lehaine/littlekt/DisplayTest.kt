package com.lehaine.littlekt

import com.lehaine.littlekt.graphics.render.Texture
import com.lehaine.littlekt.input.Input
import com.lehaine.littlekt.input.Key
import com.lehaine.littlekt.io.get
import com.lehaine.littlekt.node.Node
import com.lehaine.littlekt.node.node

/**
 * @author Colton Daily
 * @date 11/6/2021
 */
class DisplayTest(application: Application) : LittleKt(application) {

    override fun create() {
        super.create()

        scene = DisplayTestScene(this)
    }
}

class DisplayTestScene(game: LittleKt) : Scene(game) {
    val person: Texture by fileHandler.get("person.png")

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

    override fun update(input: Input) {
        super.update(input)


        if (input.isKeyJustPressed(Key.ENTER)) {
            println(root.treeString())
        }

        if (input.isKeyJustPressed(Key.ESCAPE)) {
            exit()
        }

    }
}

fun main(args: Array<String>) {
    LittleKtAppBuilder(configBuilder = { ApplicationConfiguration("Display Test", 960, 540, true) },
        gameBuilder = { DisplayTest(it) }).start()
}