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
    fun load(bins: List<Bin>)
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
            val added = bins.drop(currentBinIndex).find { it.add(rect) != null } != null
            if (!added) {
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

        bins.forEach { bin ->
            saveBins += SimpleBin(
                bin.width,
                bin.height,
                rects = bin.rects.map { Rect(it.x, it.y, it.width, it.height) },
                freeRects = bin.freeRects.map { Rect(it.x, it.y, it.width, it.height) },
                options = options,
                data = bin.data
            )
        }
        return saveBins.toList()
    }

    override fun load(bins: List<Bin>) {
        _bins.clear()
        bins.forEach { bin ->
            if (bin.maxWidth > width || bin.maxHeight > height) {
                _bins += OversizedElementBin(bin.width, bin.height)
            } else {
                val newBin = MaxRectsBin(bin.options.clone().apply {
                    this.maxWidth = width
                    this.maxHeight = height
                }).apply {
                    _freeRects.clear()
                }
                newBin._freeRects = bin.freeRects.map { Rect(it.x, it.y, it.width, it.height) }.toMutableList()
                newBin._rects = bin.rects.map { Rect(it.x, it.y, it.width, it.height) }.toMutableList()
                newBin.width = bin.width
                newBin.height = bin.height
                _bins += newBin
            }
        }
    }
}