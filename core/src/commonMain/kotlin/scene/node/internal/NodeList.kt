package scene.node.internal

import com.lehaine.littlekt.log.Logger
import scene.node.Node
import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * @author Colton Daily
 * @date 1/1/2022
 */
@PublishedApi
internal class NodeList {

    val size get() = nodes.size + nodesToAdd.size - nodesToRemove.size

    val nodes = mutableListOf<Node>()
    var nodesToAdd = mutableSetOf<Node>()
    var nodesToRemove = mutableSetOf<Node>()
    var isNodeListUnsorted = false
    var _tempNodeList = mutableSetOf<Node>()

    private var frameCount = 0

    fun add(node: Node) {
        if (!nodesToAdd.add(node)) {
            logger.warn { "You are trying to add an node (${node.name}) that you already added." }
        }
    }

    fun remove(node: Node) {
        if (nodesToAdd.contains(node)) {
            nodesToAdd.remove(node)
            return
        }

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

    fun removeAllNodes() {
        nodesToAdd.clear()
        isNodeListUnsorted = false

        updateLists()

        nodes.forEach {
            it.destroy()
        }
        nodes.clear()
    }

    fun contains(node: Node): Boolean {
        return nodes.contains(node) || nodesToAdd.contains(node)
    }

    inline fun forEach(action: (Node) -> Unit) {
        nodes.forEach(action)
        nodesToAdd.forEach(action)
    }

    inline fun forEachIndexed(action: (index: Int, node: Node) -> Unit) {
        var index = 0
        for (item in nodes) action(index++, item)
        for (item in nodesToAdd) action(index++, item)
    }

    inline fun forEachReversed(action: (Node) -> Unit) {
        val sortedNodesToAdd = nodesToAdd.sortedBy { it }
        for (i in nodesToAdd.size downTo 0) {
            val node = sortedNodesToAdd[i]
            action(node)
        }


        for (i in nodes.lastIndex downTo 0) {
            val node = nodes[i]
            action(node)
        }
    }

    /**
     * Should only be called once a frame.
     */
    fun update(dt: Duration) {
        nodes.forEach {
            if (it.enabled && (it.updateInterval == 1 || frameCount % it.updateInterval == 0)) {
                it._update(dt)
            }
        }
        frameCount++
    }

    fun updateLists() {
        if (nodesToRemove.isNotEmpty()) {
            val temp = nodesToRemove
            nodesToRemove = _tempNodeList
            _tempNodeList = temp

            _tempNodeList.sorted().forEach {
                nodes.remove(it)
            }

            _tempNodeList.clear()
        }

        if (nodesToAdd.isNotEmpty()) {
            val temp = nodesToAdd
            nodesToAdd = _tempNodeList
            _tempNodeList = temp

            _tempNodeList.sorted().forEach {
                nodes.add(it)
            }

            _tempNodeList.clear()
            isNodeListUnsorted = true
        }

        if (isNodeListUnsorted) {
            nodes.sort()
            isNodeListUnsorted = false
        }
    }


    inline fun <reified T : Node> findFirstNodeOfType(): T? = findFirstNodeOfType(T::class)

    @Suppress("UNCHECKED_CAST")
    fun <T : Node> findFirstNodeOfType(type: KClass<T>): T? {
        nodes.forEach {
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

    inline fun <reified T : Node> nodesOfType(): List<T> {
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

    fun toList(): List<Node> = if (nodesToAdd.isEmpty()) nodes else nodes + nodesToAdd

    companion object {
        private val logger = Logger<NodeList>()
    }
}