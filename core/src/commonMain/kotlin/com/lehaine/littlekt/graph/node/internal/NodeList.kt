package com.lehaine.littlekt.graph.node.internal

import com.lehaine.littlekt.graph.node.Node
import com.lehaine.littlekt.log.Logger
import com.lehaine.littlekt.util.fastForEach
import kotlin.reflect.KClass

/**
 * @author Colton Daily
 * @date 1/1/2022
 */
class NodeList {

    val size get() = nodes.size + nodesToAdd.size - nodesToRemove.size

    @PublishedApi
    internal val nodes = mutableListOf<Node>()

    @PublishedApi
    internal val sortedNodes = mutableListOf<Node>()

    @PublishedApi
    internal var nodesToAdd = mutableSetOf<Node>()
    internal var nodesToRemove = mutableSetOf<Node>()
    internal var isNodeListUnsorted = false
    internal var _tempNodeList = mutableSetOf<Node>()

    /**
     * Allow custom sorting when updating internal node lists.
     */
    var sort: Comparator<Node>? = null
        set(value) {
            field = value
            isNodeListUnsorted = true
            sortedNodes.clear()
            if (field != null) {
                sortedNodes.addAll(nodes)
            }
        }

    private var frameCount = 0

    internal fun add(node: Node) {
        if (!nodesToAdd.add(node)) {
            logger.warn { "You are trying to add an node (${node.name}) that you already added." }
        }
    }

    internal fun remove(node: Node) {
        if (!nodesToRemove.add(node)) {
            logger.warn { "You are trying to remove an node (${node.name}) that you already removed." }
        }
    }

    operator fun get(idx: Int): Node? {
        return if (idx > nodes.size - 1) {
            val newIdx = idx - nodes.size
            nodesToAdd.elementAt(newIdx)
        } else {
            nodes[idx]
        }
    }

    internal fun removeAllNodes() {
        nodesToAdd.clear()
        isNodeListUnsorted = false

        updateLists()

        nodes.fastForEach {
            it.destroy()
        }
        nodes.clear()
        sortedNodes.clear()
    }

    fun contains(node: Node): Boolean {
        return nodes.contains(node) || nodesToAdd.contains(node)
    }

    /**
     * Iterate through the nodes in normal sorted order. To iterate the sorted list see [forEachSorted].
     */
    inline fun forEach(action: (Node) -> Unit) {
        nodes.fastForEach(action)
        nodesToAdd.forEach(action)
    }


    /**
     * Iterate through the sorted list. Mainly used for custom render sorting while keep the original update order. See [NodeList.sort].
     */
    inline fun forEachSorted(action: (Node) -> Unit) {
        sort?.let {
            sortedNodes.fastForEach(action)
        } ?: run {
            nodes.fastForEach(action)
        }
        nodesToAdd.forEach(action)
    }

    inline fun forEachIndexed(action: (index: Int, node: Node) -> Unit) {
        var index = 0
        for (item in nodes) action(index++, item)
        for (item in nodesToAdd) action(index++, item)
    }

    inline fun forEachReversed(action: (Node) -> Unit) {
        val sortedNodesToAdd = nodesToAdd.sortedBy { it }
        for (i in nodesToAdd.size - 1 downTo 0) {
            val node = sortedNodesToAdd[i]
            action(node)
        }


        for (i in nodes.lastIndex downTo 0) {
            val node = nodes[i]
            action(node)
        }
    }

    internal fun preUpdate() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagatePreUpdate()
            }
        }
    }

    /**
     * Should only be called once a frame.
     */
    internal fun update() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagateUpdate()
            }
        }
        frameCount++
    }

    internal fun postUpdate() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagatePostUpdate()
            }
        }
    }

    internal fun fixedUpdate() {
        nodes.fastForEach {
            if (it.enabled && !it.isDestroyed && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it.propagateFixedUpdate()
            }
        }
    }

    internal fun updateLists() {
        if (nodesToRemove.isNotEmpty()) {
            val temp = nodesToRemove
            nodesToRemove = _tempNodeList
            _tempNodeList = temp

            _tempNodeList.sorted().fastForEach {
                nodes.remove(it)
                sort?.run {
                    sortedNodes.remove(it)
                }
                nodesToAdd.remove(it)
            }

            _tempNodeList.clear()
        }

        if (nodesToAdd.isNotEmpty()) {
            val temp = nodesToAdd
            nodesToAdd = _tempNodeList
            _tempNodeList = temp

            _tempNodeList.sorted().fastForEach {
                nodes.add(it)
                sort?.run {
                    sortedNodes.add(it)
                }
            }

            _tempNodeList.clear()
            isNodeListUnsorted = true
        }

        if (isNodeListUnsorted || sort != null) {
            nodes.sort()
            sort?.let {
                sortedNodes.sortWith(it)
            } ?: run {
                sortedNodes.sort()
            }
            isNodeListUnsorted = false
        }
    }


    inline fun <reified T : Node> findFirstNodeOfType(): T? = findFirstNodeOfType(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Node> findFirstNodeOfType(type: KClass<T>): T? {
        nodes.fastForEach {
            if (type.isInstance(it)) {
                return it as T
            }
            return it.nodes.findFirstNodeOfType(type)
        }
        nodesToAdd.forEach {
            if (type.isInstance(it)) {
                return it as T
            }
            return it.nodes.findFirstNodeOfType(type)
        }
        return null
    }

    fun findNode(name: String): Node? {
        nodes.fastForEach {
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

    inline fun <reified T : Node> nodesOfType(): List<T> {
        val list = mutableListOf<T>()
        nodes.fastForEach {
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

    override fun toString(): String {
        return "[$nodes, $nodesToAdd]"
    }

    companion object {
        private val logger = Logger<NodeList>()
    }
}