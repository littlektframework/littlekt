package com.lehaine.littlekt.node.internal

import com.lehaine.littlekt.Scene
import com.lehaine.littlekt.Time
import com.lehaine.littlekt.node.Node

/**
 * @author Colton Daily
 * @date 9/29/2021
 */
class NodeList(val scene: Scene) {

    val nodes = mutableListOf<Node>()
    var nodesToAdd = mutableSetOf<Node>()
    var nodesToRemove = mutableSetOf<Node>()
    var isNodeListUnsorted = false
    val nodeDict = mutableMapOf<Int, MutableList<Node>>()
    val unsortedTags = mutableSetOf<Int>()
    var _tempNodeList = mutableSetOf<Node>()


    fun markNodeListUnsorted() {
        isNodeListUnsorted = true
    }

    fun markTagUnsorted(tag: Int) {
        unsortedTags.add(tag)
    }

    fun add(node: Node) {
        nodesToAdd.add(node)
    }

    fun remove(node: Node) {
        if (nodesToRemove.contains(node)) {
            //   Gdx.app.log("NodeList", "You are trying to remove an node (${node.name}) that you already removed.")
        }

        if (nodesToAdd.contains(node)) {
            nodesToAdd.remove(node)
            return
        }

        if (!nodesToRemove.contains(node)) {
            nodesToRemove.add(node)
        }
    }

    fun removeAllNodes() {
        unsortedTags.clear()
        nodesToAdd.clear()
        isNodeListUnsorted = false

        updateLists()

        nodes.forEach {
            it._isDestroyed = true
            it.onRemovedFromScene()
            it.scene = null
        }
        nodes.clear()
        nodeDict.clear()
    }

    fun contains(node: Node): Boolean {
        return nodes.contains(node) || nodesToAdd.contains(node)
    }

    fun forEach(action: (Node) -> Unit) = nodes.forEach(action)

    private fun getTagList(tag: Int): MutableList<Node> {
        val list = nodeDict[tag] ?: mutableListOf()
        nodeDict[tag] = list
        return list
    }

    internal fun addToTagList(node: Node) {
        val list = getTagList(node.tag)
        if (!list.contains(node)) {
            list.add(node)
            unsortedTags.add(node.tag)
        }
    }

    internal fun removeFromTagList(node: Node) {
        nodeDict[node.tag]?.remove(node)
    }

    internal fun update() {
        nodes.forEach {
            if (it.enabled && (it.updateInterval == 1 || Time.frameCount % it.updateInterval == 0)) {
                it.update()
            }
        }
    }


    fun updateLists() {
        if (nodesToRemove.isNotEmpty()) {
            val temp = nodesToRemove
            nodesToRemove = _tempNodeList
            _tempNodeList = temp


            _tempNodeList.forEach {
                removeFromTagList(it)
                nodes.remove(it)
                it.onRemovedFromScene()
                it.scene = null
            }

            _tempNodeList.clear()
        }

        if (nodesToAdd.isNotEmpty()) {
            val temp = nodesToAdd
            nodesToAdd = _tempNodeList
            _tempNodeList = temp

            _tempNodeList.forEach {
                nodes.add(it)
                it.scene = scene
                addToTagList(it)
            }

            _tempNodeList.forEach {
                it.onAddedToScene()
            }

            _tempNodeList.clear()
            isNodeListUnsorted = true
        }

        if (isNodeListUnsorted) {
            nodes.sort()
            isNodeListUnsorted = false
        }

        if (unsortedTags.isNotEmpty()) {
            unsortedTags.forEach {
                nodeDict[it]?.sort()
            }
            unsortedTags.clear()
        }
    }


    fun findNode(name: String): Node? {
        nodes.forEach {
            if (it.name == name) {
                return it
            }
        }

        nodesToAdd.forEach {
            if (it.name == name) {
                return it
            }
        }

        return null
    }

    fun nodesWithTag(tag: Int): MutableList<Node> {
        val list = getTagList(tag)
        val returnList = mutableListOf<Node>()
        list.forEach {
            returnList.add(it)
        }

        return returnList
    }

    inline fun <reified T : Node> nodesOfType(): MutableList<T> {
        val list = mutableListOf<T>()
        nodes.forEach {
            if (it is T) {
                list.add(it)
            }
        }

        nodesToAdd.forEach {
            if (it is T) {
                list.add(it)
            }
        }
        return list
    }
}

infix fun <A> A.swap(second: A): Pair<A, A> = second to this