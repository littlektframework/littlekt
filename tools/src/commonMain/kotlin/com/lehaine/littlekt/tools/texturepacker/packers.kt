package com.lehaine.littlekt.tools.texturepacker

import kotlin.math.max

/**
 * @author Colton Daily
 * @date 1/28/2022
 */
interface Packer {
    fun add(width: Int, height: Int, data: Map<String, Any> = mapOf()): Rect =
        add(Rect(width = width, height = height, data = data))

    fun add(rect: Rect): Rect
    fun add(rects: List<Rect>) = sort(rects).forEach { add(it) }
    fun reset()
    fun repack()

    fun next(): Int

    fun sort(rects: List<Rect>): List<Rect>

    fun save(): List<Bin>
}

class MaxRectsPacker(val options: PackingOptions) : Packer {
    val width get() = options.maxWidth
    val height get() = options.maxHeight

    private val _bins = mutableListOf<BaseBin>()
    val bins: List<BaseBin> get() = _bins

    val dirty: Boolean get() = bins.any { it.dirty }
    val rects: List<Rect> get() = bins.flatMap { it.rects }

    private var currentBinIndex = 0

    override fun add(rect: Rect): Rect {
        if (rect.width > width || rect.height > height) {
            _bins += OversizedElementBin(rect)
        } else {
            val added = bins.drop(currentBinIndex).find { it.add(rect) != null }
            if (added == null) {
                MaxRectsBin(options).apply {
                    add(rect)
                }.also {
                    _bins += it
                }
            }
        }
        return rect
    }

    override fun reset() {
        _bins.clear()
        currentBinIndex = 0
    }

    override fun repack() {
        bins.forEach { bin ->
            if (bin.dirty) {
                bin.repack().forEach {
                    add(it)
                }
            }
        }
    }

    override fun sort(rects: List<Rect>) = rects.sortedByDescending { max(it.width, it.height) }

    override fun next(): Int {
        currentBinIndex = bins.size
        return currentBinIndex
    }

    override fun save(): List<Bin> {
        val saveBins = mutableListOf<Bin>()
        val tempList = mutableListOf<Rect>()
        bins.forEach { bin ->
            tempList.clear()
            bin.freeRects.forEach {
                tempList.add(Rect(it.x, it.y, it.width, it.height))
            }
            saveBins += SimpleBin(bin.width, bin.height, freeRects = tempList.toList(), options = options)
        }
        return saveBins.toList()
    }
}