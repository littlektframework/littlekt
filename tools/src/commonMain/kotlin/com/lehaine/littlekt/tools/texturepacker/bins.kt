package com.lehaine.littlekt.tools.texturepacker

import kotlin.math.max
import kotlin.math.min

/**
 * @author Colton Daily
 * @date 1/27/2022
 */
interface Bin {
    val width: Int
    val height: Int
    val maxWidth: Int get() = options.maxWidth
    val maxHeight: Int get() = options.maxHeight
    val freeRects: List<Rect>
    val rects: List<Rect>
    val options: PackingOptions
    val data: Map<String, Any>
}

class SimpleBin(
    override val width: Int,
    override val height: Int,
    override val freeRects: List<Rect> = listOf(),
    override val rects: List<Rect> = listOf(),
    override val options: PackingOptions = PackingOptions(),
    override val data: Map<String, Any> = mapOf()
) : Bin {

    override fun toString(): String {
        return "SimpleBin(width=$width, height=$height, freeRects=$freeRects, rects=$rects, options=$options, data=$data)"
    }

}

abstract class BaseBin : Bin {

    protected val _data = mutableMapOf<String, Any>()
    override val data: Map<String, Any> get() = _data

    fun add(width: Int, height: Int) = add(Rect(width = width, height = height))
    abstract fun add(rect: Rect): Rect?
    abstract fun reset(deepReset: Boolean = false)
    abstract fun repack(): List<Rect>

    protected var _dirty = 0
    var dirty: Boolean
        get() = _dirty > 0 || rects.any { it.dirty }
        set(value) {
            _dirty = if (value) _dirty + 1 else 0
            if (!value) {
                rects.forEach {
                    if (it.dirty) {
                        it.dirty = false
                    }
                }
            }
        }

    abstract fun clone(): BaseBin
}

class MaxRectsBin(
    override val options: PackingOptions = PackingOptions()
) : BaseBin() {
    override var width: Int = 0
    override var height: Int = 0

    internal var _freeRects =
        mutableListOf(
            Rect(
                options.edgeBorder,
                options.edgeBorder,
                maxWidth + options.paddingHorizontal - options.edgeBorder * 2,
                maxHeight + options.paddingVertical - options.edgeBorder * 2
            )
        )
    override val freeRects: List<Rect>
        get() = _freeRects

    internal var _rects = mutableListOf<Rect>()
    override val rects: List<Rect>
        get() = _rects

    private var stage = Rect(width = width, height = height)
    private var verticalExpand = false

    override fun add(rect: Rect): Rect? {
        return place(rect)?.also {
            _rects += it
        }
    }

    override fun reset(deepReset: Boolean) {
        if (deepReset) {
            _data.clear()
            _rects.clear()
        }
        width = 0
        height = 0
        _freeRects.clear()
        _freeRects += Rect(
            options.edgeBorder,
            options.edgeBorder,
            maxWidth + options.paddingHorizontal - options.edgeBorder * 2,
            maxHeight + options.paddingVertical - options.edgeBorder * 2
        )
        stage = Rect(width = width, height = height)
        _dirty = 0
    }

    override fun repack(): List<Rect> {
        val unpacked = mutableListOf<Rect>()
        reset()
        _rects.sortByDescending { max(it.width, it.height) }
        rects.forEach {
            if (place(it) == null) {
                unpacked += it
            }
        }

        unpacked.forEach {
            _rects.remove(it)
        }
        return unpacked
    }

    override fun clone(): BaseBin {
        val bin = MaxRectsBin(options)
        rects.forEach {
            bin.add(it)
        }
        return bin
    }

    private fun place(rect: Rect): Rect? {
        println("attempt ot place $rect")
        val allowRotation = rect.allowRotation ?: options.allowRotation
        val node: Rect? =
            findNode(rect.width + options.paddingHorizontal, rect.height + options.paddingVertical, allowRotation)
        if (node != null) {
            updateBinSize(node)
            var numRectsToProcess = freeRects.size
            var i = 0
            while (i < numRectsToProcess) {
                if (splitNode(freeRects[i], node)) {
                    _freeRects.removeAt(i)
                    numRectsToProcess--
                    i--
                }
                i++
            }
            pruneFreeList()
            verticalExpand = width > height
            rect.x = node.x
            rect.y = node.y
            rect.isRotated = if (node.isRotated) !rect.isRotated else rect.isRotated
            _dirty++
            return rect
        }

        val tmpRect1 = Rect(
            x = width + options.paddingHorizontal - options.edgeBorder,
            y = options.edgeBorder,
            width = rect.width + options.paddingHorizontal,
            height = rect.height + options.paddingVertical
        )
        val tmpRect2 = Rect(
            x = options.edgeBorder,
            y = height + options.paddingVertical - options.edgeBorder,
            width = rect.width + options.paddingHorizontal,
            height = rect.height + options.paddingVertical
        )
        if (verticalExpand) {
            if (updateBinSize(tmpRect2) || updateBinSize(tmpRect1)) {
                return place(rect)
            }
        } else {
            if (updateBinSize(tmpRect1) || updateBinSize(tmpRect2)) {
                return place(rect)
            }
        }
        return null
    }

    private fun findNode(width: Int, height: Int, allowRotation: Boolean): Rect? {
        var score = Int.MAX_VALUE
        var areaFit: Int
        var bestNode: Rect? = null
        freeRects.forEach { rect ->
            if (rect.width >= width && rect.height >= height) {
                areaFit = min(rect.width - width, rect.height - height)
                if (areaFit < score) {
                    bestNode = Rect(rect.x, rect.y, width, height)
                    score = areaFit
                }
            }

            if (!allowRotation) return@forEach

            // test 90-degree rotated rectangle
            if (rect.width >= height && rect.height >= width) {
                areaFit = min(rect.height - width, rect.width - height)
                if (areaFit < score) {
                    bestNode = Rect(rect.x, rect.y, height, width, true)
                    score = areaFit
                }
            }
        }
        return bestNode
    }

    private fun splitNode(freeRect: Rect, usedNode: Rect): Boolean {
        if (!freeRect.collides(usedNode)) return false

        // do vertical split
        if (usedNode.x < freeRect.x + freeRect.width && usedNode.x + usedNode.width > freeRect.x) {
            // new node at the top side of the used node
            if (usedNode.y > freeRect.y && usedNode.y < freeRect.y + freeRect.height) {
                _freeRects += Rect(
                    x = freeRect.x,
                    y = freeRect.y,
                    width = freeRect.width,
                    height = usedNode.y - freeRect.y
                )
            }

            // new node at the bottom side of the used node
            if (usedNode.y + usedNode.height < freeRect.y + freeRect.height) {
                _freeRects += Rect(
                    x = freeRect.x,
                    y = usedNode.y + usedNode.height,
                    width = freeRect.width,
                    height = freeRect.y + freeRect.height - (usedNode.y + usedNode.height)
                )
            }
        }

        // do horizontal split
        if (usedNode.y < freeRect.y + freeRect.height && usedNode.y + usedNode.height > freeRect.y) {
            // new node at the left side of the used node
            if (usedNode.x > freeRect.x && usedNode.x < freeRect.x + freeRect.width) {
                _freeRects += Rect(
                    x = freeRect.x,
                    y = freeRect.y,
                    width = usedNode.x - freeRect.x,
                    height = freeRect.height
                )
            }

            // new node at the right side of the used node
            if (usedNode.x + usedNode.width < freeRect.x + freeRect.width) {
                _freeRects += Rect(
                    x = usedNode.x + usedNode.width,
                    y = freeRect.y,
                    width = freeRect.x + freeRect.width - (usedNode.x + usedNode.width),
                    height = freeRect.height
                )
            }
        }
        return true
    }

    private fun updateBinSize(node: Rect): Boolean {
        if (stage.contains(node)) return false

        var tmpWidth = max(width, node.x + node.width - options.paddingHorizontal + options.edgeBorder)
        var tmpHeight = max(height, node.y + node.height - options.paddingVertical + options.edgeBorder)

        if (options.allowRotation) {
            // if rotated node, check if it is a better choice
            val rotWidth = max(width, node.x + node.height - options.paddingHorizontal + options.edgeBorder)
            val rotHeight = max(height, node.y + node.width - options.paddingVertical + options.edgeBorder)

            if (rotWidth * rotHeight < tmpWidth * tmpHeight) {
                tmpWidth = rotWidth
                tmpHeight = rotHeight
            }
        }

        if (options.outputPagesAsPowerOfTwo) {
            tmpWidth = tmpWidth.nextPowerOfTwo
            tmpHeight = tmpHeight.nextPowerOfTwo
        }

        if (tmpWidth > maxWidth + options.paddingHorizontal || tmpHeight > maxHeight + options.paddingVertical) {
            return false
        }

        expandFreeRects(tmpWidth + options.paddingHorizontal, tmpHeight + options.paddingVertical)
        width = tmpWidth
        height = tmpHeight
        stage.width = tmpWidth
        stage.height = tmpHeight

        return true
    }

    private fun expandFreeRects(tw: Int, th: Int) {
        freeRects.forEach {
            if (it.x + it.width >= min(width + options.paddingHorizontal - options.edgeBorder, tw)) {
                it.width = tw - it.x - options.edgeBorder
            }
            if (it.y + it.height >= min(height + options.paddingVertical - options.edgeBorder, th)) {
                it.height = th - it.y - options.edgeBorder
            }
        }
        _freeRects += Rect(
            x = width + options.paddingHorizontal - options.edgeBorder,
            y = options.edgeBorder,
            width = tw - width - options.paddingHorizontal,
            height = th - options.edgeBorder * 2
        )
        _freeRects += Rect(
            x = options.edgeBorder,
            y = height + options.paddingVertical - options.edgeBorder,
            width = tw - options.edgeBorder * 2,
            height = th - height - options.paddingVertical
        )
        _freeRects =
            freeRects.filter { !(it.width <= 0 || it.height <= 0 || it.x < options.edgeBorder || it.y < options.edgeBorder) }
                .toMutableList()
        pruneFreeList()
    }

    private fun pruneFreeList() {
        var i = 0
        var j: Int
        var len = freeRects.size
        while (i < len) {
            j = i + 1
            val tmpRect1 = freeRects[i]
            while (j < len) {
                val tmpRect2 = freeRects[j]
                if (tmpRect2.contains(tmpRect1)) {
                    _freeRects.removeAt(i)
                    i--
                    len--
                    break
                }
                if (tmpRect1.contains(tmpRect2)) {
                    _freeRects.removeAt(j)
                    j--
                    len--
                }
                j++
            }
            i++
        }
    }

    override fun toString(): String {
        return "MaxRectsBin(options=$options, width=$width, height=$height, freeRects=$freeRects, rects=$rects, stage=$stage, verticalExpand=$verticalExpand)"
    }
}


class OversizedElementBin(
    rect: Rect
) : BaseBin() {
    constructor(width: Int, height: Int) : this(Rect(width = width, height = height))

    init {
        rect.oversized = true
    }

    override val width: Int = rect.width
    override val height: Int = rect.height
    override val freeRects: List<Rect> = emptyList()
    override val rects: List<Rect> = listOf(rect)
    override val options: PackingOptions = PackingOptions().apply {
        maxWidth = rect.width
        maxHeight = rect.height
        outputPagesAsPowerOfTwo = false
    }

    override fun add(rect: Rect): Rect? = null
    override fun reset(deepReset: Boolean) = Unit
    override fun repack(): List<Rect> = emptyList()
    override fun clone(): BaseBin = OversizedElementBin(rects[0])
}

private val Int.nextPowerOfTwo: Int
    get() {
        var v = this
        v--
        v = v or (v shr 1)
        v = v or (v shr 2)
        v = v or (v shr 4)
        v = v or (v shr 8)
        v = v or (v shr 16)
        v++
        return v
    }