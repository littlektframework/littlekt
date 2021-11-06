package com.lehaine.littlekt

import com.lehaine.littlekt.node.Node
import com.lehaine.littlekt.node.internal.NodeList

/**
 * @author Colton Daily
 * @date 9/29/2021
 */
open class Scene {

    /**
     * The default scene [Camera].
     */
   // var camera = Camera()

    /**
     * The default scene [com.badlogic.gdx.utils.viewport.Viewport]. Defaults to [ScreenViewport].
     */
    //var viewport = ScreenViewport(camera)

    /**
     * The list of [Node]s in this scene.
     */
    val nodes by lazy { NodeList(this) }

    /**
     * THe root node of the scene.
     */
    val root = Node().also { addNode(it) }


    //internal val renderers = mutableListOf<Renderer>()

    /**
     * Lifecycle method. This is called whenever the [Core.scene] is set before [begin] is called.
     */
    open fun Node.initialize() {}

    /**
     * Lifecycle method. This is called when [Core] sets this scene at the active scene.
     */
    open fun onStart() {}

    /**
     * Lifecycle method. Do any necessary unloading / disposing here. This is called when [Core] removes this scene
     * from the active slot.
     */
    open fun unload() {}

//    fun addRenderer(renderer: Renderer) {
//        renderers.add(renderer)
//        renderer.onAddedToScene(this)
//    }
//
//    fun removeRenderer(renderer: Renderer) {
//        renderers.removeValue(renderer, false)
//        renderer.unload()
//    }

    internal fun begin() {
//        if (renderers.isEmpty) {
//            renderers.add(DefaultRenderer().also { it.onAddedToScene(this) })
//        }
        onStart()
    }

    internal fun end() {
//        renderers.forEach {
//            it.unload()
//        }

        nodes.removeAllNodes()
        unload()
    }

    fun resize(width: Int, height: Int) {
   //     viewport.update(width, height)
    }

    open fun update() {
        nodes.updateLists()
        nodes.update()
    }

    internal fun render() {
//        renderers.forEach {
//            it.render(this)
//        }
    }

    /**
     * Finds the first [Node] with the given name
     * @param name the name of the Node
     * @return the Node if found; `null` otherwise
     */
    fun findNode(name: String) = nodes.findNode(name)

    /**
     * Finds all the [Node]s with the given tag.
     * @param tag the tag to use to search
     * @return a [GdxArray] of nodes.
     */
    fun findNodesWithTag(tag: Int) = nodes.nodesWithTag(tag)
    inline fun <reified T : Node> findNodesOfType() = nodes.nodesOfType<T>()

    /**
     * Adds a a [Node] to the scene root.
     * @param node the node to add to the scene root
     * @return the node that was added to the scene root
     */
    fun addNode(node: Node): Node {
        check(!nodes.contains(node)) {
            "You are attempting to add the same node to a scene twice: $node"
        }
        nodes.add(node)
        node.scene = this
        node.children.forEach {
            addNode(it)
        }
        return node
    }

}