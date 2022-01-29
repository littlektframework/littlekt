package com.lehaine.littlekt.tools.texturepacker

/**
 * @author Colton Daily
 * @date 1/28/2022
 */
interface Packer

class MaxRectsPacker(val options: PackingOptions) : Packer {
    val width get() = options.maxWidth
    val height get() = options.maxHeight

    private val _bins = mutableListOf<BaseBin>()
    val bins: List<BaseBin> get() = _bins

    private var currentBinIndex = 0


    fun add(rect: Rect): Rect {
        if (rect.width > width || rect.height > height) {
            // TODO add oversized element bin
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

    fun reset() {
        _bins.clear()
        currentBinIndex = 0
    }

    fun repack() {
        bins.forEach { bin ->
            if (bin.dirty) {
                bin.repack().forEach {
                    add(it)
                }
            }
        }
    }

    fun next(): Int {
        currentBinIndex = bins.size
        return currentBinIndex
    }

    fun save(): List<Bin> {
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