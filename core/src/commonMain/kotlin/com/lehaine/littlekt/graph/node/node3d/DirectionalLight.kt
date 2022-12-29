package com.lehaine.littlekt.graph.node.node3d

/**
 * @author Colton Daily
 * @date 12/26/2022
 */
class DirectionalLight : Light() {

    override fun onAddedToScene() {
        super.onAddedToScene()
        scene?.environment?.lights?.add(this)
    }

    override fun onRemovedFromScene() {
        super.onRemovedFromScene()
        scene?.environment?.lights?.remove(this)
    }
}