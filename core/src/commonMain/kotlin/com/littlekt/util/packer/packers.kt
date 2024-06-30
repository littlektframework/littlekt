package com.littlekt.util.packer

import kotlin.math.max

/**
 * @author Colton Daily
 * @date 1/28/2022
 */
interface Packer {
    fun add(width: Int, height: Int, data: Map<String, Any> = mapOf()): BinRect =
        add(BinRect(width = width, height = height, data = data))

    fun add(rect: BinRect): BinRect

    fun add(rects: List<BinRect>) = sort(rects).forEach { add(it) }

    fun reset()

    fun repack(quick: Boolean = true)

    fun next(): Int

    fun sort(rects: List<BinRect>): List<BinRect>

    fun save(): List<Bin>

    fun load(bins: List<Bin>)
}

class MaxRectsPacker(val options: PackingOptions) : Packer {
    val width
        get() = options.maxWidth

    val height
        get() = options.maxHeight

    private val _bins = mutableListOf<BaseBin>()
    val bins: List<BaseBin>
        get() = _bins

    val dirty: Boolean
        get() = bins.any { it.dirty }

    val rects: List<BinRect>
        get() = bins.flatMap { it.rects }

    private var currentBinIndex = 0

    override fun add(rect: BinRect): BinRect {
        if (rect.width > width || rect.height > height) {
            _bins += OversizedElementBin(rect)
        } else {
            val added = bins.drop(currentBinIndex).find { it.add(rect) != null } != null
            if (!added) {
                MaxRectsBin(options).apply { add(rect) }.also { _bins += it }
            }
        }
        return rect
    }

    override fun reset() {
        _bins.clear()
        currentBinIndex = 0
    }

    override fun repack(quick: Boolean) {
        if (quick) {
            val unpacked = mutableListOf<BinRect>()
            bins.forEach { bin ->
                if (bin.dirty) {
                    bin.repack().forEach { unpacked += it }
                }
            }
            add(unpacked)
            return
        }

        if (!dirty) return
        val rects = this.rects
        reset()
        add(rects)
    }

    override fun sort(rects: List<BinRect>) = rects.sortedByDescending { max(it.width, it.height) }

    override fun next(): Int {
        currentBinIndex = bins.size
        return currentBinIndex
    }

    override fun save(): List<Bin> {
        val saveBins = mutableListOf<Bin>()

        bins.forEach { bin ->
            saveBins +=
                SimpleBin(
                    bin.width,
                    bin.height,
                    rects = bin.rects.map { BinRect(it.x, it.y, it.width, it.height) },
                    freeRects = bin.freeRects.map { BinRect(it.x, it.y, it.width, it.height) },
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
                val newBin =
                    MaxRectsBin(
                            bin.options.clone().apply {
                                this.maxWidth = width
                                this.maxHeight = height
                            }
                        )
                        .apply { _freeRects.clear() }
                newBin._freeRects =
                    bin.freeRects.map { BinRect(it.x, it.y, it.width, it.height) }.toMutableList()
                newBin._rects =
                    bin.rects.map { BinRect(it.x, it.y, it.width, it.height) }.toMutableList()
                newBin.width = bin.width
                newBin.height = bin.height
                _bins += newBin
            }
        }
    }
}
